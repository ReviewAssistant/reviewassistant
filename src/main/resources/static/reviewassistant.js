Gerrit.install(function(self) {
    function print (c, r) {
        var url = "changes/" + c._number + "/revisions/" + r._number + "/ReviewAssistant~advice";
        console.log("Url is: " + url);
        var change_plugins = document.getElementById('change_plugins');
        console.log("Asking for advice...");
        Gerrit.get(
            url,
            function (r) {
                console.log("Got advice: " + r);
                var totalTime = "<div>You should spend " + r.total_review_time + " minutes reviewing this change.";
                var sessions = "<div>Sessions: " + r.sessions +" for " + r.session_time + " minutes each.";
                change_plugins.innerHTML = "<div id=\"reviewAssistant\">"+ totalTime +" "+ sessions + "</div>";
            });
    }
    self.on('showchange', print);
});