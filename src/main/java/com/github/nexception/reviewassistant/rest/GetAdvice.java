package com.github.nexception.reviewassistant.rest;


import com.github.nexception.reviewassistant.Storage;
import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.change.RevisionResource;
import com.google.inject.Inject;

public class GetAdvice implements RestReadView<RevisionResource> {

    private Storage storage;

    @Inject
    public GetAdvice(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Object apply(RevisionResource resource) throws AuthException, BadRequestException, ResourceConflictException, Exception {
        Calculation calculation = storage.fetchCalculation(resource.getPatchSet().getRevision().get());
        return resource.getPatchSet().getRevision().get(); //Prints commit-ID to "change_plugins"
    }

    public class Output {
        protected String name;
        //Add more here
    }


}
