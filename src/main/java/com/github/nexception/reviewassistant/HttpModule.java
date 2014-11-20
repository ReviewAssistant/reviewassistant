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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.JavaScriptPlugin;
import com.google.gerrit.extensions.webui.WebUiPlugin;
import com.google.gerrit.httpd.plugins.HttpPluginModule;

public class HttpModule extends HttpPluginModule{
    @Override
    protected void configureServlets() {
        DynamicSet.bind(binder(), WebUiPlugin.class).toInstance(new JavaScriptPlugin("reviewassistant.js"));
       // serve("/advice").with(RestServlet.class);
    }
}
