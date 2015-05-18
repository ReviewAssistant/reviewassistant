package com.github.reviewassistant.reviewassistant;

import com.github.reviewassistant.reviewassistant.models.Calculation;
import com.google.common.collect.Ordering;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
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
import org.eclipse.jgit.lib.Config;
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
    private static final Logger log = LoggerFactory.getLogger(ReviewAssistant.class);
    private static final int DEFAULT_MAX_REVIEWERS = 3;
    private static final boolean DEFAULT_ENABLE_LOAD_BALANCING = false;
    private static final int DEFAULT_PLUS_TWO_AGE = 8;
    private static final int DEFAULT_PLUS_TWO_LIMIT = 10;
    private static final boolean DEFAULT_PLUS_TWO_REQUIRED = true;

    public static boolean realUser;
    private final AccountByEmailCache emailCache;
    private final AccountCache accountCache;
    private final Change change;
    private final PatchListCache patchListCache;
    private final PatchSet ps;
    private final Repository repo;
    private final RevCommit commit;
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
    public ReviewAssistant(PatchListCache patchListCache,
        AccountCache accountCache,
        GerritApi gApi,
        AccountByEmailCache emailCache,
        PluginConfigFactory cfg,
        @PluginName String pluginName,
        @Assisted RevCommit commit,
        @Assisted Change change,
        @Assisted PatchSet ps,
        @Assisted Repository repo,
        @Assisted Project.NameKey projectName) {
      this.accountCache = accountCache;
      this.patchListCache = patchListCache;
      this.commit = commit;
      this.gApi = gApi;
      this.emailCache = emailCache;
      this.change = change;
      this.ps = ps;
      this.repo = repo;
      this.projectName = projectName;

      Config pluginConfig = null;
      try {
        pluginConfig = cfg.getProjectPluginConfigWithInheritance(projectName, pluginName);
      } catch (NoSuchProjectException e) {
        log.error(e.getMessage(), e);
      }
      if (pluginConfig != null) {
        this.maxReviewers = pluginConfig.getInt("reviewers", "maxReviewers", DEFAULT_MAX_REVIEWERS);
        this.loadBalancing = pluginConfig.getBoolean("reviewers", "enableLoadBalancing", DEFAULT_ENABLE_LOAD_BALANCING);
        this.plusTwoAge = pluginConfig.getInt("reviewers", "plusTwoAge", DEFAULT_PLUS_TWO_AGE);
        this.plusTwoLimit = pluginConfig.getInt("reviewers", "plusTwoLimit", DEFAULT_PLUS_TWO_LIMIT);
        this.plusTwoRequired = pluginConfig.getBoolean("reviewers", "plusTwoRequired", DEFAULT_PLUS_TWO_REQUIRED);
      } else {
        this.maxReviewers = DEFAULT_MAX_REVIEWERS;
        this.loadBalancing = DEFAULT_ENABLE_LOAD_BALANCING;
        this.plusTwoAge = DEFAULT_PLUS_TWO_AGE;
        this.plusTwoLimit = DEFAULT_PLUS_TWO_LIMIT;
        this.plusTwoRequired = DEFAULT_PLUS_TWO_REQUIRED;
      }
    }

    /**
     * Returns a Calculation object with all relevant information
     * regarding a review for a patch set.
     *
     * @param info the data for a patch set
     * @return the Calculation object for a review
     */
    public static Calculation calculate(ChangeInfo info, double reviewTimeModifier) {
        log.debug("Received event: {}", info.currentRevision);
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
            List<ChangeInfo> infoList = gApi.changes().query(
                "status:merged -age:" + plusTwoAge + "weeks limit:" + plusTwoLimit
                    + " -label:Code-Review=2," + change.getOwner().get()
                    + " label:Code-Review=2 project:" +
                    projectName.toString())
                .withOptions(ListChangesOption.LABELS, ListChangesOption.DETAILED_ACCOUNTS).get();
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

        log.debug("getApprovalAccounts found {} reviewers", reviewersApproved.size());

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
                @Override public int compare(Entry<Account, Integer> itemOne,
                    Entry<Account, Integer> itemTwo) {
                    return itemOne.getValue() - itemTwo.getValue();
                }
            }).greatestOf(blameData.entrySet(), maxReviewers * 2);
        //TODO Check if maxReviewers * 2 is sufficient
        log.debug("getReviewers found {} reviewers", topReviewers.size());
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
    private List<Entry<Account, Integer>> sortByOpenChanges(List<Entry<Account, Integer>> list) {
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
            }
        }
        return modifiableList;
    }

    @Override public void run() {
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

        List<Entry<Account, Integer>> mergeCandidates = new ArrayList<>();
        if (plusTwoRequired) {
            mergeCandidates.addAll(getApprovalAccounts());
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

        for (Entry<Account, Integer> e : mergeCandidates) {
            log.debug("Merge candidate: {}, score: {}", e.getKey().getPreferredEmail(),
                e.getValue());
        }

        for (Entry<Account, Integer> e : blameCandidates) {
            log.debug("Blame candidate: {}, score: {}", e.getKey().getPreferredEmail(),
                e.getValue());
        }

        Map<Account, AddReason> finalMap = new HashMap<>();
        if (blameCandidates.size() < maxReviewers) {
            Iterator<Entry<Account, Integer>> mergeItr = mergeCandidates.iterator();
            for (Entry<Account, Integer> e : blameCandidates) {
                finalMap.put(e.getKey(), AddReason.EXPERIENCE);
                log.debug("Added {} ({})", e.getKey().getPreferredEmail(), AddReason.EXPERIENCE);
            }
            boolean plusTwoAdded = false;
            while (finalMap.size() < maxReviewers && mergeItr.hasNext()) {
                Account account = mergeItr.next().getKey();
                if (!finalMap.containsKey(account)) {
                    finalMap.put(account, AddReason.PLUS_TWO);
                    log.debug("Added {} ({})", account.getPreferredEmail(), AddReason.PLUS_TWO);
                    plusTwoAdded = true;
                }
            }
            if (!plusTwoAdded && plusTwoRequired) {
                finalMap.put(mergeCandidates.get(0).getKey(), AddReason.PLUS_TWO);
                log.debug("Changed reason for {} to {}",
                    mergeCandidates.get(0).getKey().getPreferredEmail(), AddReason.PLUS_TWO);
            }
        } else {
            Iterator<Entry<Account, Integer>> blameItr = blameCandidates.iterator();
            if (!mergeCandidates.isEmpty()) {
                finalMap.put(mergeCandidates.get(0).getKey(), AddReason.PLUS_TWO);
                log.debug("Added {} ({})", mergeCandidates.get(0).getKey().getPreferredEmail(),
                    AddReason.PLUS_TWO);
            }
            while (finalMap.size() < maxReviewers && blameItr.hasNext()) {
                Account account = blameItr.next().getKey();
                if (!finalMap.containsKey(account)) {
                    finalMap.put(account, AddReason.EXPERIENCE);
                    log.debug("Added {} ({})", account.getPreferredEmail(), AddReason.EXPERIENCE);
                }
            }
        }

        //TODO Move into addReviewers?
        realUser = true;
        addReviewers(change, finalMap);
        realUser = false;
    }
}
