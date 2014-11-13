Gerrit.install(function(self) {
    function onSayHello(c) {
        var f = c.textfield();
        var t = c.checkbox();
        var b = c.button('Say hello', {onclick: function(){
            c.call(
                {message: f.value, french: t.checked},
                function(r) {
                    c.hide();
                    window.alert(r);
                    c.refresh();
                });
        }});
        c.popup(c.div(
            c.prependLabel('Greeting message', f),
            c.br(),
            c.label(t, 'french'),
            c.br(),
            b));
        f.focus();
    }
    function printReviewChanges{


    }

    self.onAction('revision', 'reviewassistant', onSayHello);
});