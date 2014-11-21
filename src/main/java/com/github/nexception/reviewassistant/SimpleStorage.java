package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.annotations.PluginData;
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
 * This implementation of Storage writes to the plugin's data directory ({gerrit url}/plugins/ReviewAssistant).
 * The structure follows that of git's object directory, which means that the first two letters of the
 * commit's SHA-1 is used as name for the sub directory, and the rest of the SHA-1 is used as file name.
 */
public class SimpleStorage implements Storage {

    private static final Logger log = LoggerFactory.getLogger(SimpleStorage.class);
    private File dir;

    @Inject
    SimpleStorage(@PluginData File dir) {
        this.dir = dir;
    }

    @Override
    public void storeCalculation(Calculation calculation) {
        File file = new File(dir, calculation.commitId.substring(0, 2) + File.separator + calculation.commitId.substring(2));
        log.debug("Writing calculation to " + file);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"))) {
            Gson gson = new Gson();
            String s = gson.toJson(calculation);
            writer.write(s, 0, s.length());
            log.debug("Stored calculation in file " + file);
        } catch (FileNotFoundException e) {
            log.error("Could not find file " + file);
            log.error(e.toString());
        } catch (IOException e) {
            log.error("Could not write to file " + file);
            log.error(e.toString());
        }
    }

    @Override
    public Calculation fetchCalculation(String commitId) {
        File file = new File(dir, commitId.substring(0, 2) + File.separator + commitId.substring(2));
        log.debug("Loading calculation from " + file);
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charset.forName("UTF-8"))) {
            Gson gson = new Gson();
            Calculation calculation = gson.fromJson(reader.readLine(), Calculation.class);
            log.info("Returning Calculation " + calculation.toString());
            return calculation;
        } catch (IOException e) {
            log.error("Could not read calculation file for " + commitId);
            log.error(e.toString());
        }

        /**
         * If no calculation is found, maybe one should be triggered?
         */
        return null;
    }
}
