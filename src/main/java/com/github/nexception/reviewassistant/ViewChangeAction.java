package com.github.nexception.reviewassistant;

import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.webui.UiAction;
import com.google.gerrit.server.account.ChangeUserName;
import com.google.gerrit.server.change.RevisionResource;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Created by simon on 11/11/14.
 */
public class ViewChangeAction implements UiAction<RevisionResource>, RestModifyView<RevisionResource, ViewChangeAction.Input> {

    private Provider<ChangeUserName.CurrentUser> user;
    private Storage storage;

    @Inject
    public ViewChangeAction(Provider<ChangeUserName.CurrentUser> user, Storage storage) {
        this.user = user;
        this.storage = storage;
    }

    @Override
    public Object apply(RevisionResource resource, ViewChangeAction.Input input) throws AuthException, BadRequestException, ResourceConflictException, Exception {
        String message = "stuff";
        return message;

    }

    @Override
    public Description getDescription(RevisionResource resource) {
        return new Description().setLabel("ReviewAssistant").setTitle("Review recommendations");
    }

    static class Input {
        String message;
    }
}
