function highlight(uuid){

        var w=$( "#" + uuid );
//        var c= zk.Widget.$(w);
//        c.set("width","400px");


        w.resizable({
            stop: function(event,ui){

                var wgt=zk(ui.originalElement).$();
                zAu.send(new zk.Event(wgt,'onDesignerChange',["width",ui.size.width+"px"]));
            }
        });


        $("[class~=focused]").removeClass("focused");
        w.addClass("focused");

        w.mouseover(function() {
            if ($("#"+uuid+" [class~=focused]").length==0){
                w.addClass("focused");
            } else {
                w.removeClass("focused");
            }
        });

        w.mouseleave(function() {
            w.removeClass("focused");
        });

        w.tooltip({
              items: "*",
              track: true,
              show: {delay: 1000},
              position: { my: "center bottom-20", at: "center top" },
              content: function() {
                var wgt=zk(this.id).$();
                if (wgt){
                    return wgt.widgetName + " [" + wgt.uuid + "]";
                }
              }
        });


}