package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.common.collect.Ordering;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ListChangesOption;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Patch.ChangeType;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.account.AccountByEmailCache;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.patch.PatchList;
import com.google.gerrit.server.patch.PatchListCache;
import com.google.gerrit.server.patch.PatchListEntry;
import com.google.gerrit.server.patch.PatchListNotAvailableException;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

enum AddReason {PLUS_TWO, EXPERIENCE}


/**
 * A class for calculating recommended review time and
 * recommended reviewers.
 */
public class ReviewAssistant implements Runnable {

    public static boolean realUser;
    private final AccountByEmailCache emailCache;
    private final AccountCache accountCache;
    private final Change change;
    private final PatchListCache patchListCache;
    private final PatchSet ps;
    private final Repository repo;
    private final RevCommit commit;
    private static final Logger log = LoggerFactory.getLogger(ReviewAssistant.class);
    private final Project.NameKey projectName;
    private final GerritApi gApi;
    private final int maxReviewers;
    private final boolean loadBalancing;
    private final int plusTwoAge;
    private final int plusTwoLimit;
    private final boolean plusTwoRequired;


    public interface Factory {
        ReviewAssistant create(RevCommit commit, Change change, PatchSet ps, Repository repo,
            Project.NameKey projectName);
    }

    @Inject
    public ReviewAssistant(final PatchListCache patchListCache, final AccountCache accountCache,
        final GerritApi gApi, final AccountByEmailCache emailCache,
        final PluginConfigFactory cfg,
        @Assisted final RevCommit commit, @Assisted final Change change,
        @Assisted final PatchSet ps, @Assisted final Repository repo,
        @Assisted final Project.NameKey projectName) {
        this.commit = commit;
        this.change = change;
        this.ps = ps;
        this.patchListCache = patchListCache;
        this.repo = repo;
        this.accountCache = accountCache;
        this.emailCache = emailCache;
        this.projectName = projectName;
        this.gApi = gApi;
        int tmpMaxReviewers;
        boolean tmpLoadBalancing;
        int tmpPlusTwoAge;
        int tmpPlusTwoLimit;
        boolean tmpPlusTwoRequired;
        try {
            tmpMaxReviewers =
                cfg.getProjectPluginConfigWithInheritance(projectName, "reviewassistant")
                    .getInt("reviewers", "maxReviewers", 3);
            tmpLoadBalancing =
                cfg.getProjectPluginConfigWithInheritance(projectName, "reviewassistant")
                    .getBoolean("reviewers", "enableLoadBalancing", false);
            tmpPlusTwoAge =
                cfg.getProjectPluginConfigWithInheritance(projectName, "reviewassistant")
                    .getInt("reviewers", "plusTwoAge", 8);
            tmpPlusTwoLimit =
                cfg.getProjectPluginConfigWithInheritance(projectName, "reviewassistant")
                    .getInt("reviewers", "plusTwoLimit", 10);
            tmpPlusTwoRequired =
                cfg.getProjectPluginConfigWithInheritance(projectName, "reviewassistant")
                    .getBoolean("reviewers", "plusTwoRequired", true);
        } catch (NoSuchProjectException e) {
            log.error(e.getMessage(), e);
            tmpMaxReviewers = 3;
            tmpLoadBalancing = false;
            tmpPlusTwoAge = 8;
            tmpPlusTwoLimit = 10;
            tmpPlusTwoRequired = true;
        }
        this.maxReviewers = tmpMaxReviewers;
        this.loadBalancing = tmpLoadBalancing;
        this.plusTwoAge = tmpPlusTwoAge;
        this.plusTwoLimit = tmpPlusTwoLimit;
        this.plusTwoRequired = tmpPlusTwoRequired;
    }

    /**
     * Returns a Calculation object with all relevant information
     * regarding a review for a patch set.
     *
     * @param info the data for a patch set
     * @return the Calculation object for a review
     */
    public static Calculation calculate(ChangeInfo info, double reviewTimeModifier) {
        log.debug("Received event: " + info.currentRevision);    //Commit-ID
        Calculation calculation = new Calculation();
        calculation.commitId = info.currentRevision;
        calculation.totalReviewTime = calculateReviewTime(info, reviewTimeModifier);
        calculation.hours = calculation.totalReviewTime / 60;
        calculation.minutes = calculation.totalReviewTime % 60;
        calculation.sessions = calculateReviewSessions(calculation.totalReviewTime);

        return calculation;
    }

    /**
     * Returns the total amount of time in minutes recommended for a review.
     * Adds all line insertions and deletions for a patch set and calculates
     * the amount of minutes needed.
     * This calculation is based on the optimum review rate of 5 LOC in 1 minute.
     *
     * @param info the data for a patch set
     * @return the total amount of time recommended for a review
     */
    private static int calculateReviewTime(ChangeInfo info, double reviewTimeModifier) {
        //TODO Make reviewTimeModifier initialize independently
        log.debug("reviewTimeModifier: {}", reviewTimeModifier);
        int lines = info.insertions + Math.abs(info.deletions);
        int minutes = (int) Math.ceil(lines * reviewTimeModifier / 5);
        minutes = (int) Math.ceil(minutes / 5.0);
        minutes = minutes * 5;
        if (minutes < 5) {
            minutes = 5;
        }
        return minutes;
    }

    /**
     * Returns the recommended amount of review sessions for a review.
     * Divides the total amount of review time up in 60 minute sessions.
     *
     * @param minutes the total amount of time recommended for a review
     * @return the recommended amount of review sessions
     */
    private static int calculateReviewSessions(int minutes) {
        int sessions = (int) Math.round(minutes / 60.0);
        if (sessions < 1) {
            sessions = 1;
        }
        return sessions;
    }

    /**
     * Fetches all users with +2 rights from the list of changeInfo.
     *
     * @return a list of emails of users with +2 rights
     */
    private List<Entry<Account, Integer>> getApprovalAccounts() {
        Map<Account, Integer> reviewersApproved = new HashMap<>();
        try {
            List<ChangeInfo> infoList =
                gApi.changes().query(
                    "status:merged -age:" + plusTwoAge + "weeks limit:" + plusTwoLimit
                        + " -label:Code-Review=2," + change.getOwner().get()
                        + " label:Code-Review=2 project:" +
                        projectName.toString())
                    .withOptions(ListChangesOption.LABELS, ListChangesOption.DETAILED_ACCOUNTS)
                    .get();
            for (ChangeInfo info : infoList) {
                //TODO Check if this is good enough
                try {
                    Account account =
                        accountCache.getByUsername(info.labels.get("Code-Review").approved.username)
                            .getAccount();
                    if (reviewersApproved.containsKey(account)) {
                        reviewersApproved.put(account, reviewersApproved.get(account) + 1);
                    } else {
                        reviewersApproved.put(account, 1);
                    }
                } catch (NullPointerException e) {
                    log.error("No username for this account found in cache {}", e);
                }

            }
        } catch (RestApiException e) {
            log.error(e.getMessage(), e);
        }

        log.debug("getApprovalAccounts found " + reviewersApproved.size() + " reviewers");

        try {
            List<Entry<Account, Integer>> approvalAccounts =
                Ordering.from(new Comparator<Entry<Account, Integer>>() {
                    @Override
                    public int compare(Entry<Account, Integer> o1, Entry<Account, Integer> o2) {
                        return o1.getValue() - o2.getValue();
                    }
                }).greatestOf(reviewersApproved.entrySet(), reviewersApproved.size());
            return approvalAccounts;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Calculates blame data for a given file and commit.
     *
     * @param commit the commit to base the blame command on
     * @param file   the file for which to calculate blame data
     * @return BlameResult
     */
    private BlameResult calculateBlame(RevCommit commit, PatchListEntry file) {
        BlameCommand blameCommand = new BlameCommand(repo);
        blameCommand.setStartCommit(commit);
        blameCommand.setFilePath(file.getNewName());
        try {
            BlameResult blameResult = blameCommand.call();
            blameResult.computeAll();
            return blameResult;
        } catch (GitAPIException e) {
            log.error("Could not call blame command for commit {}", commit.getName(), e);
        } catch (IOException e) {
            log.error("Could not compute blame result for commit {}", commit.getName(), e);
        }
        return null;
    }

    /**
     * Calculates all reviewers based on a blame result. The result is a map of accounts and integers
     * where the integer represents the number of occurrences of the account in the commit history.
     *
     * @param edits       list of edited rows for a file
     * @param blameResult result from git blame for a specific file
     * @return a list of accounts and integers
     */
    private List<Entry<Account, Integer>> getReviewers(List<Edit> edits, BlameResult blameResult) {
        Map<Account, Integer> blameData = new HashMap<>();
        try {
            for (Edit edit : edits) {
                for (int i = edit.getBeginA(); i < edit.getEndA(); i++) {
                    RevCommit commit = blameResult.getSourceCommit(i);
                    Set<Account.Id> idSet =
                        emailCache.get(commit.getAuthorIdent().getEmailAddress());
                    for (Account.Id id : idSet) {
                        Account account = accountCache.get(id).getAccount();
                        // Check if account is active and not owner of change
                        if (account.isActive() && !change.getOwner().equals(account.getId())) {
                            Integer count = blameData.get(account);
                            if (count == null) {
                                count = 1;
                            } else {
                                count = count + 1;
                            }
                            blameData.put(account, count);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        List<Entry<Account, Integer>> topReviewers =
            Ordering.from(new Comparator<Entry<Account, Integer>>() {
                @Override
                public int compare(Entry<Account, Integer> itemOne,
                    Entry<Account, Integer> itemTwo) {
                    return itemOne.getValue() - itemTwo.getValue();
                }
            }).greatestOf(blameData.entrySet(), maxReviewers * 2);
        //TODO Check if maxReviewers * 2 is sufficient
        log.debug("getReviewers found " + topReviewers.size() + " reviewers");
        return topReviewers;
    }

    /**
     * Adds reviewers to the change.
     *
     * @param change the change for which reviewers should be added
     * @param map    map of reviewers and their reasons for being added
     */
    private void addReviewers(Change change, Map<Account, AddReason> map) {
        try {
            ChangeApi cApi = gApi.changes().id(change.getId().get());
            for (Entry<Account, AddReason> entry : map.entrySet()) {
                cApi.addReviewer(entry.getKey().getId().toString());
                String reason;
                switch (entry.getValue()) {
                    case PLUS_TWO:
                        reason = "+2";
                        break;
                    case EXPERIENCE:
                        reason = "experience";
                        break;
                    default:
                        reason = "unknown reason";
                }
                log.info("{} was added to change {} ({})", entry.getKey().getPreferredEmail(),
                    change.getChangeId(), reason);
            }
        } catch (RestApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Sorts a list by open changes in ascending order.
     *
     * @param list the list to be sorted
     * @return a sorted list
     */
    private List sortByOpenChanges(List<Entry<Account, Integer>> list) {
        //TODO: There is probably room for improvement here
        ArrayList<Entry<Account, Integer>> modifiableList = new ArrayList<>(list);
        for (int i = 0; i < modifiableList.size(); i++) {
            Account account = modifiableList.get(i).getKey();
            try {
                int openChanges =
                    gApi.changes().query("status:open reviewer:" + account.getId().get()).get()
                        .size();
                modifiableList.get(i).setValue(openChanges);
                Collections.sort(modifiableList, new Comparator<Entry<Account, Integer>>() {
                    @Override
                    public int compare(Entry<Account, Integer> o1, Entry<Account, Integer> o2) {
                        return o1.getValue() - o2.getValue();
                    }
                });
            } catch (RestApiException e) {
                log.error(e.getMessage(), e);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return modifiableList;
    }

    @Override
    public void run() {
        log.info(
            "CONFIG: maxReviewers: " + maxReviewers + ", enableLoadBalancing: " + loadBalancing +
                ", plusTwoAge: " + plusTwoAge + ", plusTwoLimit: " + plusTwoLimit
                + ", plusTwoRequired: " + plusTwoRequired);
        PatchList patchList;
        try {
            patchList = patchListCache.get(change, ps);
        } catch (PatchListNotAvailableException e) {
            log.error("Patchlist is not available for {}", change.getKey(), e);
            return;
        }

        if (commit.getParentCount() != 1) {
            log.error("No merge/initial");
            return;
        }

        List<Entry<Account, Integer>> mergeCandidates;
        if (plusTwoRequired) {
            mergeCandidates = getApprovalAccounts();
        } else {
            //TODO Fugly
            mergeCandidates = new ArrayList<>();
        }
        List<Entry<Account, Integer>> blameCandidates = new LinkedList<>();
        for (PatchListEntry entry : patchList.getPatches()) {
            /*
             * Only git blame at the moment. If other methods are used in the future,
             * other change types might be required.
             */
            if (entry.getChangeType() == ChangeType.MODIFIED
                || entry.getChangeType() == ChangeType.DELETED) {
                BlameResult blameResult = calculateBlame(commit.getParent(0), entry);
                if (blameResult != null) {
                    List<Edit> edits = entry.getEdits();
                    blameCandidates.addAll(getReviewers(edits, blameResult));
                } else {
                    log.error("calculateBlame returned null for commit {}", commit);
                }
            }
        }

        if (loadBalancing) {
            blameCandidates = sortByOpenChanges(blameCandidates);
            if (!mergeCandidates.isEmpty()) {
                mergeCandidates = sortByOpenChanges(mergeCandidates);
            }
        }

        Map<Account, AddReason> finalMap = new HashMap<>();
        Iterator<Entry<Account, Integer>> itr = blameCandidates.iterator();
        if (!mergeCandidates.isEmpty()) {
            finalMap.put(mergeCandidates.get(0).getKey(), AddReason.PLUS_TWO);
        }
        while (finalMap.size() < maxReviewers && itr.hasNext()) {
            Account account = itr.next().getKey();
            if (!finalMap.containsKey(account)) {
                finalMap.put(account, AddReason.EXPERIENCE);
            }
        }

        //TODO Move into addReviewers?
        realUser = true;
        //        addReviewers(change, finalSet);
        addReviewers(change, finalMap);
        realUser = false;

    }
}
