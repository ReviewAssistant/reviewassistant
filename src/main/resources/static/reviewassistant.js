Gerrit.install(function(self) {
    function print(c, r) {
        var change_plugins = document.getElementById('change_plugins');
        console.log("logging once");
        change_plugins.innerHTML = 'Testing output';

        self.post(
            '/print',
            {start_build: true, platform_type: 'Linux'},
            function (r) {console.log("logging twice")});

    }
    self.on('showchange', print);
});