package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.rest.GetAdvice;
import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.server.config.FactoryModule;

import static com.google.gerrit.server.change.RevisionResource.REVISION_KIND;

public class Module extends FactoryModule {

    @Override protected void configure() {
        DynamicSet.bind(binder(), ChangeListener.class).to(ChangeEventListener.class);
        bind(AdviceCache.class).to(AdviceCacheImpl.class);
        factory(ReviewAssistant.Factory.class);

        install(new RestApiModule() {
            @Override protected void configure() {
                get(REVISION_KIND, "advice").to(GetAdvice.class);
            }
        });
    }
}
