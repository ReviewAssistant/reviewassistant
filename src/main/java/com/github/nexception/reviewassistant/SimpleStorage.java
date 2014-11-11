package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class SimpleStorage implements Storage {

    private static final Logger log = LoggerFactory.getLogger(SimpleStorage.class);
    private File dir;

    @Inject
    SimpleStorage(@PluginData java.io.File dir) {
        this.dir = dir;
    }

    @Override
    public void storeCalculation(Calculation calculation) {
        log.debug("Attempting to store calculation " + calculation.toString());
        try (PrintWriter printWriter = new PrintWriter(new File(dir, calculation.toString()))) {
            Gson gson = new Gson();
            printWriter.print(gson.toJson(calculation));
            printWriter.close();
            log.debug("Stored calculation in file " + calculation.toString());
        } catch (FileNotFoundException e) {
            log.error("Could not store file " + calculation.changeId + "-" + calculation.patchId);
            log.error(e.getStackTrace().toString());
        } catch (IOException e) {
            log.error("Could not store file " + calculation.changeId + "-" + calculation.patchId);
            log.error(e.getStackTrace().toString());
        }
    }

    @Override
    public Calculation fetchCalculation(String identifier) {
        //TODO
        return null;
    }
}
