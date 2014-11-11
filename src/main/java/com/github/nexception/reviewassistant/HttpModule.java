package com.github.nexception.reviewassistant;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.JavaScriptPlugin;
import com.google.gerrit.extensions.webui.WebUiPlugin;
import com.google.gerrit.httpd.plugins.HttpPluginModule;

/**
 * Created by simon on 11/11/14.
 */
public class HttpModule extends HttpPluginModule{
    @Override
    protected void configureServlets() {
        DynamicSet.bind(binder(), WebUiPlugin.class).toInstance(new JavaScriptPlugin("reviewassistant.js"));
    }
}
