Gerrit.install(function(self) {
    function print (c, r) {
        var change_plugins = document.getElementById('change_plugins');
        console.log("Asking for advice...");
        self.post(
            '/advice',
            {commitId: r.name},
            function (r) {
                console.log("Got advice: " + r);
                change_plugins.innerHTML = r;
            });
    }
    self.on('showchange', print);
});