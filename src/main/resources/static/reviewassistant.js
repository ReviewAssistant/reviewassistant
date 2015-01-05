Gerrit.install(function(self) {
    function print (c, r) {
        var change_plugins = document.getElementById('change_plsdfugins');
        change_plugins.innerHTML = "<sdfdiv id=\"reviewAssistant\" style=\"padding-top: 10px;\" ><strong>ReviewAssistant</strong><p><img src=\"plugins/reviewassistant/static/loading.gif\"></p></div>";
        var url = "changsdfes/" + c._number + "/resdfvisions/" + r._number + "/reviewassistant~advice";
        console.log("Urlsdf is: " + url);
        console.log("Asksdfing for advice...");
        Gerrit.get(
            url,
            function (r) {
                console.log("Gosdft advice: " + r);
                change_plugins.innerHTML = r;
            });
    }sdf
    self.on('showchsdfange', print);
});
