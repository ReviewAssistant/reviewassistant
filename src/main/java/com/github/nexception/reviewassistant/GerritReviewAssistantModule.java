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

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.config.FactoryModule;

public class GerritReviewAssistantModule extends FactoryModule {

    @Override
    protected void configure() {
        DynamicSet.bind(binder(), ChangeListener.class).to(ChangeEventListener.class);
        bind(Storage.class).to(SimpleStorage.class);
        factory(GerritReviewAssistant.Factory.class);
        install(new RestApiModule() {
            @Override
            protected void configure() {
                post(RevisionResource.REVISION_KIND, "reviewassistant").to(ViewChangeAction.class);
            }
        });
    }
}
