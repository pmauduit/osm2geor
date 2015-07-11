Ext.namespace("GEOR.Addons");

GEOR.Addons.Osm2Geor = Ext.extend(GEOR.Addons.Base, {
    win: null,
    layer: null,
    item: null,

    init: function(record) {
        this.layer = new OpenLayers.Layer.Vector("__georchestra_osm2geor", {
            displayInLayerSwitcher: true,
            styleMap: new OpenLayers.StyleMap({})
        });
        this.item =  new Ext.menu.Item({
                text:    'OSM 2 geOrchestra',
                qtip:    'OSM to geOrchestra addon',
                iconCls: 'osm2geor-icon',
                handler: this.showWindow,
                scope:   this
        });
    },
    
    createWindow: function() {
        return new Ext.Window({
            closable: true,
            closeAction: 'hide',
            width: 330,
            height: 270,
            title: "OSM 2 geOrchestra",
            border: false,
            buttonAlign: 'left',
            layout: 'fit',
            items: [{
            	xtype     : 'textarea',
                name      : 'overpassApiQuery',
                fieldLabel: 'Overpass API query'
            }],
            fbar: ['->', {
                text: OpenLayers.i18n("Execute"),
                handler: function() {
                	alert("blah");
                },
                scope:this
            },
            {
                text: OpenLayers.i18n("Close"),
                handler: function() {
                    this.win.hide();
                },
                scope: this
            }]
        });
    },

    showWindow: function() {
        if (!this.win) {
            this.win = this.createWindow();
        }
        this.win.show();
    },

    destroy: function() {
        this.win && this.win.hide();
        this.layer = null;
        this.jsonFormat = null;
        this.modifyControl = null;
        
        GEOR.Addons.Base.prototype.destroy.call(this);
    }
});