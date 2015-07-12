package org.georchestra.mapfishapp.addons.osm2geor;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OverpassJSONUtils {

    // Overpass-API
    private final static String OPASS_TYPE_KEY  = "type";
    private final static String OPASS_TYPE_NODE = "node";
    private final static String OPASS_TYPE_WAY  = "way";
    private final static String OPASS_ID_KEY    = "id";
    private final static String OPASS_TAGS_KEY  = "tags";
    private final static String OPASS_LON_KEY   = "lon";
    private final static String OPASS_LAT_KEY   = "lat";
    private final static String OPASS_NODES_KEY = "nodes";

    // GeoJSON
    private final static String GEOJSON_FEATURE_COLLECTION = "FeatureCollection";
    private final static String GEOJSON_FEATURE            = "Feature";
    private final static String GEOJSON_TYPE_KEY           = "type";
    private final static String GEOJSON_FEATURES           = "features";
    private final static String GEOJSON_TYPE_POINT         = "Point";
    private final static String GEOJSON_TYPE_LINESTRING    = "LineString";
    private final static String GEOJSON_COORDS_KEY         = "coordinates";
    private final static String GEOJSON_GEOMETRY_KEY       = "geometry";
    private final static String GEOJSON_PROPERTIES_KEY     = "properties";




    /**
     * This method takes a overpass API JSON query result, and transforms it into
     * GeoJSON.
     *
     * @param input the Overpass API response,
     * @return a JSONObject representing the input as GeoJSON.
     * @throws JSONException
     *
     */
    public static JSONObject toGeoJSON(JSONObject input) throws JSONException {
        JSONObject out = new JSONObject();
        JSONArray elements = input.getJSONArray("elements");

        HashMap<Long, JSONObject> nodeCache = new HashMap<Long, JSONObject>();
        ArrayList<JSONObject> wayCache = new ArrayList<JSONObject>();

        for (int i = 0; i < elements.length(); ++i) {
            JSONObject curElem = elements.getJSONObject(i);
            if (curElem.getString(OPASS_TYPE_KEY).equalsIgnoreCase(OPASS_TYPE_NODE)) {
                Long nodeId = curElem.getLong(OPASS_ID_KEY);
                nodeCache.put(nodeId, curElem);
            } else if (curElem.getString(OPASS_TYPE_KEY).equalsIgnoreCase(OPASS_TYPE_WAY)) {
                wayCache.add(curElem);
            }
        }
        // After this first pass, we shall be able to convert every features as GeoJSON
        out.put(GEOJSON_TYPE_KEY, GEOJSON_FEATURE_COLLECTION);
        out.put(GEOJSON_FEATURES, new JSONArray());

        // First, parses the nodeCache
        for (Long i : nodeCache.keySet()) {
            JSONObject curNode = nodeCache.get(i);
            // if the node does not have tags, it is likely to be used only
            // to describe a linestring / polygon
            if (! curNode.has(OPASS_TAGS_KEY)) {
                continue;
            } else {
                out.getJSONArray(GEOJSON_FEATURES).put(createGeoJSONFeatureNode(curNode));
            }
        }

        // Then, the wayCache
        for (JSONObject curWay : wayCache) {
            out.getJSONArray(GEOJSON_FEATURES).put(createGeoJSONFeatureWay(curWay, nodeCache));
        }

        return out;
    }

    private static JSONObject createGeoJSONFeatureNode(JSONObject node) throws JSONException {
        JSONObject out = new JSONObject();
        JSONObject geom = new JSONObject().put(GEOJSON_TYPE_KEY, GEOJSON_TYPE_POINT).put(GEOJSON_COORDS_KEY,
                new JSONArray().put(node.get(OPASS_LON_KEY)).put(node.get(OPASS_LAT_KEY)));

        out.put(GEOJSON_TYPE_KEY, GEOJSON_FEATURE).
            put(GEOJSON_GEOMETRY_KEY, geom).
            put(GEOJSON_PROPERTIES_KEY, node.getJSONObject(OPASS_TAGS_KEY));

        return out;
    }

    private static JSONObject createGeoJSONFeatureWay(JSONObject way, HashMap<Long, JSONObject> nodeCache) throws JSONException {
        JSONObject out = new JSONObject();
        out.put(GEOJSON_TYPE_KEY, GEOJSON_FEATURE).
            put(GEOJSON_PROPERTIES_KEY, way.getJSONObject(OPASS_TAGS_KEY));
        JSONObject geom = new JSONObject();
        geom.put(GEOJSON_TYPE_KEY, GEOJSON_TYPE_LINESTRING);
        geom.put(GEOJSON_COORDS_KEY, new JSONArray());
        // iterates over the nodes
        JSONArray nodes = way.getJSONArray(OPASS_NODES_KEY);
        for (int i =0 ; i < nodes.length(); ++i) {
            Long nId = nodes.getLong(i);
            JSONObject node = nodeCache.get(nId);
            if (node == null) {
                // should not happen, skip it
                System.out.println("unresolveable node, skipping: " +nId);
                continue;
            }
            geom.getJSONArray(GEOJSON_COORDS_KEY).put(new JSONArray().put(node.get(OPASS_LON_KEY)).put(node.get(OPASS_LAT_KEY)));
        }
        out.put(GEOJSON_GEOMETRY_KEY, geom);
        return out;
    }
}
