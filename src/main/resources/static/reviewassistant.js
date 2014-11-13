Gerrit.install(function(self) {
    function print(c, r) {
        var change_plugins = document.getElementById('change_plugins');
        console.log("testar");
        change_plugins.innerHTML = 'bröd är gott';

        Gerrit.post(
            '/print',
            {start_build: true, platform_type: 'Linux'},
            function (r) {console.log("hästar")});

    }
    self.on('showchange', print);
});