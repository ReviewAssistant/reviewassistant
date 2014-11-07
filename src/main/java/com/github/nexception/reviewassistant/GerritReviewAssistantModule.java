package com.github.nexception.reviewassistant;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.FactoryModule;

public class GerritReviewAssistantModule extends FactoryModule {

    @Override
    protected void configure() {
        DynamicSet.bind(binder(), ChangeListener.class).to(NewChangeEvent.class);
        factory(GerritReviewAssistant.Factory.class);


    }
}
