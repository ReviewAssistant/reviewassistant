package com.github.nexception.reviewassistant;

import com.google.gerrit.extensions.annotations.Export;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@Export("/print")
@Singleton
public class ReviewServlet extends HttpServlet {

    private final Storage storage;

    @Inject
    public ReviewServlet(Storage storage) {
        super();
        this.storage = storage;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BufferedReader reader = req.getReader();
        storage.fetchCalculation(reader.readLine());
    }
}
