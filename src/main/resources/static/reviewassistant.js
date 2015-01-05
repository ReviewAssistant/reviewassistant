Gerrit.install(function(self) {
    function print (c, r) {
        var change_plugins = document.getElementById('change_plugins');
        change_plugins.innerHTML = "<div id=\"reviewAssistant\" style=\"padding-top: 10px;\" ><strong>ReviewAssistant</strong><p><img src=\"plugins/reviewassistant/static/loading.gif\"></p></div>";
        var url = "changes/" + c._number + "/revisions/" + r._number + "/reviewassistant~advice";
        console.log("Url is: " + url);
        console.log("Asking for advice...");
        Gerrit.get(
            url,
            function (r) {
                console.log("Got advice: " + r);
                change_plugins.innerHTML = r;
            });
    }
    self.on('showchange', print);
});