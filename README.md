# osm2geor
Mapfishapp addon to be able to retrieve data from osm (using overpass-api) into a vector layer

# Setup
If you are using *MY GEORCHESTRA FORK* (https://github.com/pmauduit/georchestra/tree/georchestra-datadir):

```
mkdir -p /path/to/your/georchestra/datadir/mapfishapp/app/addons/osm2geor
cp -r css/ img/ js/ config.json manifest.json /path/to/your/georchestra/datadir/mapfishapp/app/addons/osm2geor
cd java/
mvn clean package
```
Then just deploy the generated `target/osm2geor-mapfishapp-addon.jar`, into the `WEB-INF/lib/` directory of your mapfishapp deployment. No other configuration file to edit.


Regular geOrchestra setup:

```
mkdir -p /path/to/your/deployed/mapfishapp/app/addons/osm2geor
cp -r css/ img/ js/ config.json manifest.json /path/to/your/deployed/mapfishapp/app/addons/osm2geor
cd java/
mvn clean package
cp target/osm2geor-mapfishapp-addon.jar /path/to/your/deployed/mapfishapp/WEB-INF/lib/
```

Then edit your `/path/to/your/deployed/mapfishapp/WEB-INF/ws-servlet.xml` to add the following lines:

```xml
  <bean id="overpasscontroller" class="org.georchestra.mapfishapp.addons.osm2geor.OverpassController" />
```

Finally edit the `GEOR_custom.js` file to add the necessary configuration for this plugin:

```js
ADDONS: [...,
{
        "id": "osm2geor",
        "name": "Osm2Geor",
        "enabled": true,
        "title": {
            "fr": "osm2geor",
            "en": "osm2geor",
            "es": "osm2geor"
        },
        "description": {
            "fr": "osm2geOr est un greffon permettant de requêter l'overpass-API OpenStreetMap et de charger une nouvelle couche",
            "en": "osm2geOr is an addon which allows querying the OSM overpass-API and to load the result as a new layer",
            "es": "osm2geOr es un plugin para interrogar el OSM overpass-API y cargar el resultado como un nuevo layer"
        }
}], ...
```

Restart your tomcat containing mapfishapp, you should be good to go.


# Usage sample (video)

[![Using the plugin](http://img.youtube.com/vi/PZ1m3yg_et8/0.jpg)](http://www.youtube.com/watch?v=PZ1m3yg_et8)
