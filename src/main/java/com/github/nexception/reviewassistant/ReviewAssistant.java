package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.common.collect.Ordering;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Patch.ChangeType;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.account.AccountByEmailCache;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.patch.PatchList;
import com.google.gerrit.server.patch.PatchListCache;
import com.google.gerrit.server.patch.PatchListEntry;
import com.google.gerrit.server.patch.PatchListNotAvailableException;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A class for calculating recommended review time and
 * recommended reviewers.
 */
public class ReviewAssistant implements Runnable {

    private final AccountByEmailCache emailCache;
    private final AccountCache accountCache;
    private final Change change;
    private final PatchListCache patchListCache;
    private final PatchSet ps;
    private final Repository repo;
    private final RevCommit commit;
    private final GerritApi api;
    private final PluginConfigFactory cfg;

    private static final Logger log = LoggerFactory.getLogger(ReviewAssistant.class);

    public interface Factory {
        ReviewAssistant create(RevCommit commit, Change change, PatchSet ps, Repository repo);
    }

    @Inject
    public ReviewAssistant(final PatchListCache patchListCache, final AccountCache accountCache,
                           final GerritApi api,
                           final AccountByEmailCache emailCache, final PluginConfigFactory cfg,
                           @Assisted final RevCommit commit, @Assisted final Change change,
                           @Assisted final PatchSet ps, @Assisted final Repository repo) {
        this.commit = commit;
        this.change = change;
        this.ps = ps;
        this.patchListCache = patchListCache;
        this.repo = repo;
        this.accountCache = accountCache;
        this.emailCache = emailCache;
        this.cfg = cfg;
        this.api = api;
    }

    /**
     * Returns a Calculation object with all relevant information
     * regarding a review for a patch set.
     * @param event the event for a patch set
     * @return      the Calculation object for a review
     */
    public static Calculation calculate(PatchSetCreatedEvent event) {
        log.info("Received event: " + event.patchSet.revision);
        Calculation calculation = new Calculation();
        calculation.commitId = event.patchSet.revision;
        calculation.totalReviewTime = calculateReviewTime(event);
        calculation.hours = calculateReviewTime(event) / 60;
        calculation.minutes = calculateReviewTime(event) % 60;
        calculation.sessions = calculateReviewSessions(calculateReviewTime(event));
        calculation.sessionTime = 60;

        return calculation;
    }

    /**
     * Returns the total amount of time in minutes recommended for a review.
     * Adds all line insertions and deletions for a patch set and calculates
     * the amount of minutes needed.
     * This calculation is based on the optimum review rate of 5 LOC in 1 minute.
     * @param event the event for a patch set
     * @return      the total amount of time recommended for a review
     */
    private static int calculateReviewTime(PatchSetCreatedEvent event) {
        int lines = event.patchSet.sizeInsertions + Math.abs(event.patchSet.sizeDeletions);
        int minutes = (int) Math.ceil(lines / 5);
        if(minutes < 5) {
            minutes = 5;
        }
        return minutes;
    }

    /**
     * Returns the recommended amount of review sessions for a review.
     * Divides the total amount of review time up in 60 minute sessions.
     * @param minutes the total amount of time recommended for a review
     * @return        the recommended amount of review sessions
     */
    private static int calculateReviewSessions(int minutes) {
        int sessions = Math.round(minutes / 60);
        if (sessions < 1) {
            sessions = 1;
        }
        return sessions;
    }

    /**
     * Calculates blame data for a given file and commit.
     * @param commit the commit to base the blame command on
     * @param file the file for which to calculate blame data
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
     * @param edits list of edited rows for a file
     * @param blameResult result from git blame for a specific file
     * @return a list of accounts and integers
     */
    private List<Entry<Account, Integer>> getReviewers(List<Edit> edits, BlameResult blameResult) {
        Map<Account, Integer> blameData = new HashMap<>();
        int maxReviewers = cfg.getGlobalPluginConfig("reviewassistant").getInt("reviewers", "reviewers", 3);
        for (Edit edit : edits) {
            for (int i = edit.getBeginA(); i < edit.getEndA(); i++) {
                RevCommit commit = blameResult.getSourceCommit(i);
                Set<Account.Id> idSet = emailCache.get(commit.getAuthorIdent().getEmailAddress());
                for (Account.Id id : idSet) {
                    Account account = accountCache.get(id).getAccount();
                    // Check if account is active and not owner of change
                    if (account.isActive() && !change.getOwner().equals(account.getId())) {
                        Integer count = blameData.get(account);
                        if (count == null) {
                            count = 1;
                        } else {
                            count = count.intValue() + 1;
                        }
                        blameData.put(account, count);
                    }
                }
            }
        }

        List<Entry<Account, Integer>> topReviewers = Ordering.from(new Comparator<Entry<Account, Integer>>() {
            @Override
            public int compare(Entry<Account, Integer> itemOne, Entry<Account, Integer> itemTwo) {
                return itemOne.getValue() - itemTwo.getValue();
            }
        }).greatestOf(blameData.entrySet(), maxReviewers);

        log.info("getReviewers found " + topReviewers.size() + " reviewers");
        return topReviewers;
    }

    private void addReviewers(Change change, List<Entry<Account, Integer>> list) {
        try {
            log.info("addReviewers started");
            ChangeApi cApi = api.changes().id(change.getId().get());
            for (Entry<Account, Integer> entry : list) {
                cApi.addReviewer(entry.getKey().getId().toString());
                log.info(entry.getKey() + " was added to this change");
            }

        } catch (RestApiException e) {
            log.error("Could not add reviewers", e);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    @Override
    public void run() {
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

        List<Entry<Account, Integer>> reviewers = new LinkedList<>();
        for (PatchListEntry entry : patchList.getPatches()) {
            /*
             * Only git blame at the moment. If other methods are used in the future,
             * other change types might be required.
             */
            if (entry.getChangeType() == ChangeType.MODIFIED ||
                    entry.getChangeType() == ChangeType.DELETED) {
                BlameResult blameResult = calculateBlame(commit.getParent(0), entry);
                if (blameResult != null) {
                    List<Edit> edits = entry.getEdits();
                    reviewers.addAll(getReviewers(edits, blameResult));
                    for (int i = 0; i < reviewers.size(); i++) {
                        log.info("Candidate " + (i + 1) + ": " + reviewers.get(i).getKey().getPreferredEmail() +
                                ", blame score: " + reviewers.get(i).getValue());
                    }
                } else {
                    log.error("calculateBlame returned null for commit {}", commit);
                }
            }
        }
        addReviewers(change, reviewers);
    }
}
