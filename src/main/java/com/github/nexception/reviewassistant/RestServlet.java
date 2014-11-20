package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.annotations.Export;
import com.google.gerrit.server.account.ChangeUserName;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This servlet serves the review advice to be printed in the change UI.
 */
@Export("/advice")
@Singleton
public class RestServlet extends HttpServlet {

    private Storage storage;

    @Inject
    public RestServlet(Storage storage) {
        this.storage = storage;
    }

    private static final Logger log = LoggerFactory.getLogger(RestServlet.class);
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        Input input = gson.fromJson(req.getReader().readLine(), Input.class);
        log.info("Received request for " + input.toString());
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = resp.getWriter();

        /**
         * This is only placeholder text, but it does show that the plugin
         * prints to the change UI from the "Java side", and that the
         * information printed is read from file.
         */
        writer.write("<p>This text comes from RestServlet.java</p>");
        Calculation calculation = storage.fetchCalculation(input.commitId);
        resp.getWriter().write("");
    }

    private class Input {
        String commitId;

        @Override
        public String toString() {
            return "{commitId: \"" + commitId + "\"}";
        }
    }
}
