package com.github.reviewassistant.reviewassistant;

import com.github.reviewassistant.reviewassistant.rest.GetAdvice;
import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;

import static com.google.gerrit.server.change.RevisionResource.REVISION_KIND;

public class Module extends FactoryModule {

    @Override protected void configure() {
        DynamicSet.bind(binder(), EventListener.class).to(ChangeEventListener.class);
        bind(AdviceCache.class).to(AdviceCacheImpl.class);
        factory(ReviewAssistant.Factory.class);

        install(new RestApiModule() {
            @Override protected void configure() {
                get(REVISION_KIND, "advice").to(GetAdvice.class);
            }
        });
    }
}
