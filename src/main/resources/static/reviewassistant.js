Gerrit.install(function(self) {
    function print (c, r) {
        if ((c.status != 'NEW') || (c.current_revision != r.name)) {
            return;
        }
        var doc = document;
        var change_plugins = document.getElementById('change_plugins');
        var container = doc.createElement('div');
        container.id = "reviewAssistant";
        container.style = "padding-top: 10px;";
        var header = doc.createElement('div');
        header.innerHTML = "<strong>ReviewAssistant</strong>";
        container.appendChild(header);
        var advice = doc.createElement('div');
        advice.innerHtml = "<p><img src=\"plugins/reviewassistant/static/loading.gif\"></p>";
        container.appendChild(advice);
        change_plugins.appendChild(container);
        var url = "changes/" + c._number + "/revisions/" + r._number + "/reviewassistant~advice";
        console.log("Url is: " + url);
        console.log("Asking for advice...");
        Gerrit.get(url, function (r) {
            console.log("Got advice: " + r);
            advice.innerHTML = r;
        });
    }
    self.on('showchange', print);
});
