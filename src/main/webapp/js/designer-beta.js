var myCodeMirror;
var w4tjDesigner={
    _templateUUID: undefined,
    get templateUUID(){
        return this._templateUUID;
    },
    set templateUUID(uuid){
        this._templateUUID=zk(uuid).$();

        if (this._templateUUID){
            $(this._canvas.$n()).addClass("design");
        } else {
            $(this._canvas.$n()).removeClass("design");
            zAu.send(new zk.Event(w4tjDesigner.canvas, "onClientTemplateSet", null));
        }
    },

    _canvas: undefined,
    get canvas(){
        return this._canvas;
    },
    set canvas(uuid){
        this._canvas=zk(uuid).$();
    },

    _showTooltip: undefined,
    get showTooltip(){
        return this._showTooltip;
    },
    set showTooltip(v){
        this._showTooltip=v;

        //reset first
        $("#" + this._canvas.uuid + " *").each(function() {
            var $this = $(this);
            if($this.data("uiTooltip")){
               $this.tooltip("destroy");
            }
        });

        if (v){
            $("#" + this._canvas.uuid + " *").each(function () {
               if (zk(this).$()) {
                   $(this).tooltip({
                          items: "*",
                          track: true,
                          show: {delay: 1000},
                          position: { my: "center bottom-20", at: "center top" },
                          content: function() {
                            var wgt=zk(this).$();
                            if (wgt){
                                return wgt.widgetName + " [" + wgt.uuid + "]";
                            }
                          }
                    });
               }
           });
        }
    },

    _showTracking: undefined,
    get showTracking(){
        return this._showTracking;
    },
    set showTracking(v){
        this._showTracking=v;

        $(this._canvas.$n()).undelegate("*","mousemove"); //reset
        $(this._canvas.$n()).undelegate("*","mouseleave"); //reset

        if (v){
            $(this._canvas.$n()).delegate("*","mousemove",function(event) {
                if (event.target==this){
                    var wgt=zk(this).$();
                    if (wgt){
                        //unhover others first
                        $("#" + w4tjDesigner.canvas.uuid + " [class~=hovered]").removeClass("hovered");
                        $(wgt.$n()).addClass("hovered");
                    }
                }
            });

            $(this._canvas.$n()).delegate("*","mouseleave", function(event) {
                if (zk(this).$()){
                   $(zk(this).$().$n()).removeClass("hovered");
                }
            });
        }
    },

    monitorClicks: function () {
        //left clicks
       $(this._canvas.$n()).undelegate("*", "click"); //reset
       $(this._canvas.$n()).delegate("*", "click", function (event) {
           var wgt = zk(this).$();
           if (wgt && w4tjDesigner.templateUUID) {  //design
               zAu.send(new zk.Event(w4tjDesigner.canvas, "onDesigneStart", {
                   templateUUID: w4tjDesigner.templateUUID,
                   parent: wgt.uuid
               }));
               event.stopPropagation();
           } else if (wgt) {  //select
                if (event.target==this && !$(wgt.$n()).is(".selected")) {
                    w4tjDesigner.select(wgt.$n());
                }
           }
       });

       //right clicks
       //$(this._canvas.$n()).bind("contextmenu", function(e) {return false;});
       $(this._canvas.$n()).undelegate("*", "mousedown"); //reset
       $(this._canvas.$n()).delegate("*", "mousedown", function (event) {
           if (event.button == 2){
               var wgt=zk(this).$();
               if (wgt) {
                   if (event.target==this) {
                       w4tjDesigner.select(wgt.$n());
                       w4tjDesigner.templateUUID=null;
                       zAu.send(new zk.Event(w4tjDesigner.canvas, "onContextMenu",{target:wgt.uuid}));
                   }
               }
           }
       });

    },

    select: function(e){
        $("#" + w4tjDesigner.canvas.uuid + " [class~=selected]").removeClass("selected");
        $(e).addClass("selected");
        zAu.send(new zk.Event(w4tjDesigner.canvas, "onSelection",{target:zk(e).$().uuid}));
    }

}

// && !$this.has(".selected").length

/*
               $("#" + w4tjDesigner.canvas.uuid + " *").each(function () {
                   if (zk(this).$()) {
                       if ($(this).is(".ui-resizable")) {
                           $(this).resizable("destroy");
                       }
                   }
               });

                //todo
               if (w.$instanceof(zul.tab.Tabbox)) {
                   $(w.$n()).resizable({ handles: "se" });
                   //event.stopPropagation();
               }
*/
