package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * This implementation of AdviceCache writes to the plugin's data directory ({gerrit url}/plugins/ReviewAssistant).
 * The structure follows that of git's object directory, which means that the first two letters of the
 * commit's SHA-1 is used as name for the sub directory, and the rest of the SHA-1 is used as file name.
 */
public class AdviceCacheImplementation implements AdviceCache {

    private static final Logger log = LoggerFactory.getLogger(AdviceCacheImplementation.class);
    private File dir;
    private GerritApi gApi;
    private PluginConfigFactory cfg;

    @Inject
    AdviceCacheImplementation(@PluginData File dir, GerritApi gApi, PluginConfigFactory cfg) {
        this.dir = dir;
        this.gApi = gApi;
        this.cfg = cfg;
    }

    @Override
    public void storeCalculation(Calculation calculation) {
        File file = new File(dir, calculation.commitId.substring(0, 2)
                + File.separator + calculation.commitId.substring(2));
        log.debug("Writing calculation to {}", file);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = Files
            .newBufferedWriter(file.toPath(), Charset.forName("UTF-8"))) {
            Gson gson = new Gson();
            String s = gson.toJson(calculation);
            writer.write(s, 0, s.length());
            log.debug("Cached calculation in file {}", file);
        } catch (FileNotFoundException e) {
            log.error("Could not find file {}", file);
            log.error(e.toString());
        } catch (IOException e) {
            log.error("Could not write to file {}", file);
            log.error(e.toString());
        }
    }

    @Override
    public Calculation fetchCalculation(RevisionResource resource) {
        File file = new File(dir, resource.getPatchSet().getRevision().get().substring(0, 2)
                + File.separator+ resource.getPatchSet().getRevision().get().substring(2));
        Calculation calculation = null;
        log.debug("Loading calculation from {}", file);
        try (BufferedReader reader = Files
            .newBufferedReader(file.toPath(), Charset.forName("UTF-8"))) {
            Gson gson = new Gson();
            calculation = gson.fromJson(reader.readLine(), Calculation.class);
            log.info("Returning Calculation {}", calculation.toString());
        } catch (IOException e) {
            log.error("Could not read calculation file for {}",
                resource.getPatchSet().getRevision().get());
            log.error(e.toString());
        }

        if (calculation == null || calculation.totalReviewTime == 0) {
            log.debug("Corrupt or missing calculation. Will recalculate for {}",
                resource.getPatchSet().getRevision().get());
            try {
                ChangeApi cApi = gApi.changes().id(resource.getChange().getChangeId());
                ChangeInfo info = cApi.get();
                double reviewTimeModifier = cfg.getProjectPluginConfigWithInheritance(resource.getChange()
                        .getProject(), "reviewassistant").getInt("time", "reviewTimeModifier", 100);
                calculation = ReviewAssistant.calculate(info, reviewTimeModifier / 100);
                storeCalculation(calculation);
            } catch (RestApiException e) {
                log.error("Could not get ChangeInfo for change {}",
                    resource.getChange().getChangeId());
            } catch (NoSuchProjectException e) {
                log.error(e.getMessage(), e);
            }
        }
        return calculation;
    }
}
