/*
The MIT License (MIT)

Copyright (c) 2014 Gustav Jansson Ekstrand (gustav.jp@live.se), Simon Wessel (nllptr), William Phan (william.da.phan@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/



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
