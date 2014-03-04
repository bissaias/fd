var myCodeMirror;
var w4tjDesigner={
    _templateUUID: undefined,
    get templateUUID(){
        return this._templateUUID;
    },
    set templateUUID(uuid){
        this._templateUUID=zk(uuid).$();

        if (this._templateUUID){
            jq(this._canvas.$n()).addClass("design");
        } else {
            jq(this._canvas.$n()).removeClass("design");
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
        jq("#" + this._canvas.uuid + " *").each(function() {
            var $this = jq(this);
            if($this.data("uiTooltip")){
               $this.tooltip("destroy");
            }
        });

        if (v){
            jq("#" + this._canvas.uuid + " *").each(function () {
               if (zk(this).$()) {
                   jq(this).tooltip({
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

        jq(this._canvas.$n()).undelegate("*","mousemove"); //reset
        jq(this._canvas.$n()).undelegate("*","mouseleave"); //reset

        if (v){
            jq(this._canvas.$n()).delegate("*","mousemove",function(event) {
                if (event.target==this){
                    var wgt=zk(this).$();
                    if (wgt){
                        //unhover others first
                        jq("#" + w4tjDesigner.canvas.uuid + " [class~=hovered]").removeClass("hovered");
                        jq(wgt.$n()).addClass("hovered");
                    }
                }
            });

            jq(this._canvas.$n()).delegate("*","mouseleave", function(event) {
                if (zk(this).$()){
                   jq(zk(this).$().$n()).removeClass("hovered");
                }
            });
        }
    },

    monitorClicks: function () {
        //left clicks
       jq(this._canvas.$n()).undelegate("*", "click"); //reset
       jq(this._canvas.$n()).delegate("*", "click", function (event) {
           var wgt = zk(this).$();
           if (wgt && w4tjDesigner.templateUUID) {  //design
               zAu.send(new zk.Event(w4tjDesigner.canvas, "onDesigneStart", {
                   templateUUID: w4tjDesigner.templateUUID,
                   parent: wgt.uuid
               }));
               event.stopPropagation();
           } else if (wgt) {  //select
                if (event.target==this && !jq(wgt.$n()).is(".selected")) {
                    w4tjDesigner.select(wgt.$n());
                }
           }
       });

       //right clicks
       //jq(this._canvas.$n()).bind("contextmenu", function(e) {return false;});
       jq(this._canvas.$n()).undelegate("*", "mousedown"); //reset
       jq(this._canvas.$n()).delegate("*", "mousedown", function (event) {
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
        jq("#" + w4tjDesigner.canvas.uuid + " [class~=selected]").removeClass("selected");
        jq(e).addClass("selected");
        zAu.send(new zk.Event(w4tjDesigner.canvas, "onSelection",{target:zk(e).$().uuid}));
    }

}

// && !$this.has(".selected").length

/*
               jq("#" + w4tjDesigner.canvas.uuid + " *").each(function () {
                   if (zk(this).$()) {
                       if (jq(this).is(".ui-resizable")) {
                           jq(this).resizable("destroy");
                       }
                   }
               });

                //todo
               if (w.$instanceof(zul.tab.Tabbox)) {
                   jq(w.$n()).resizable({ handles: "se" });
                   //event.stopPropagation();
               }
*/
