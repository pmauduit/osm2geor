package org.georchestra.mapfishapp.addons.osm2geor;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OverpassJSONUtils {

    // Overpass-API
    private final static String OPASS_TYPE_KEY      = "type";
    private final static String OPASS_TYPE_NODE     = "node";
    private final static String OPASS_TYPE_WAY      = "way";
    private final static String OPASS_TYPE_RELATION = "relation";
    private final static String OPASS_ID_KEY        = "id";
    private final static String OPASS_TAGS_KEY      = "tags";
    private final static String OPASS_LON_KEY       = "lon";
    private final static String OPASS_LAT_KEY       = "lat";
    private final static String OPASS_NODES_KEY     = "nodes";
    private final static String OPASS_MEMBERS_KEY   = "members";
    private final static String OPASS_REF_KEY       = "ref";

    // GeoJSON
    private final static String GEOJSON_FEATURE_COLLECTION      = "FeatureCollection";
    private final static String GEOJSON_FEATURE                 = "Feature";
    private final static String GEOJSON_TYPE_KEY                = "type";
    private final static String GEOJSON_FEATURES                = "features";
    private final static String GEOJSON_TYPE_POINT              = "Point";
    private final static String GEOJSON_TYPE_POLYGON            = "Polygon";
    private final static String GEOJSON_TYPE_LINESTRING         = "LineString";
    private final static String GEOJSON_TYPE_GEOMETRYCOLLECTION = "GeometryCollection";
    private final static String GEOJSON_COORDS_KEY              = "coordinates";
    private final static String GEOJSON_GEOMETRY_KEY            = "geometry";
    private final static String GEOJSON_GEOMETRIES_KEY          = "geometries";
    private final static String GEOJSON_PROPERTIES_KEY          = "properties";

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
        HashMap<Long, JSONObject> relationWayCache = new HashMap<Long, JSONObject>();
        ArrayList<JSONObject> relationCache = new ArrayList<JSONObject>();
        ArrayList<JSONObject> wayCache = new ArrayList<JSONObject>();

        for (int i = 0; i < elements.length(); ++i) {
            JSONObject curElem = elements.getJSONObject(i);
            if (curElem.getString(OPASS_TYPE_KEY).equalsIgnoreCase(OPASS_TYPE_NODE)) {
                Long nodeId = curElem.getLong(OPASS_ID_KEY);
                nodeCache.put(nodeId, curElem);
            } else if (curElem.getString(OPASS_TYPE_KEY).equalsIgnoreCase(OPASS_TYPE_WAY)) {
                wayCache.add(curElem);
            } else if (curElem.getString(OPASS_TYPE_KEY).equalsIgnoreCase(OPASS_TYPE_RELATION)) {
                relationCache.add(curElem);
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
            JSONObject curWayGeoJSONFeature = createGeoJSONFeatureWay(curWay, nodeCache);
            out.getJSONArray(GEOJSON_FEATURES).put(curWayGeoJSONFeature);

            // Caches the way for further relation parsing
            Long wayId = curWay.getLong(OPASS_ID_KEY);
            relationWayCache.put(wayId, curWayGeoJSONFeature);
        }

        // And finishes with the relations
        for (JSONObject curRel : relationCache) {
            JSONObject gjRel = createGeoJSONFeatureRelation(curRel, nodeCache, relationWayCache);
            out.getJSONArray(GEOJSON_FEATURES).put(gjRel);
            // Iterates over the members
        }

        return out;
    }

    /**
     * Creates a GeoJSON node from a node object coming from the OverPass API.
     *
     * @param node a JSONObject representing the node feature as OverPass API
     * @return a JSONObject representing the node feature as GeoJSON.
     * @throws JSONException
     */
    private static JSONObject createGeoJSONFeatureNode(JSONObject node) throws JSONException {
        JSONObject out = new JSONObject();
        JSONObject geom = new JSONObject().put(GEOJSON_TYPE_KEY, GEOJSON_TYPE_POINT).put(GEOJSON_COORDS_KEY,
                new JSONArray().put(node.get(OPASS_LON_KEY)).put(node.get(OPASS_LAT_KEY)));

        out.put(GEOJSON_TYPE_KEY, GEOJSON_FEATURE).
            put(GEOJSON_GEOMETRY_KEY, geom).
            put(GEOJSON_PROPERTIES_KEY, node.getJSONObject(OPASS_TAGS_KEY));

        return out;
    }

    /**
     * Creates a GeoJSON linestring or polygon from a way object coming from the
     * OverPass API. If the way is closed (i.e. first coordinates equals last
     * coordinates) then a Polygon is returned.
     *
     * @param way the JSONObject describing the way as sent by the Overpass API
     * @param nodeCache a Map containing the cached nodes
     * @return a JSONObject representing the feature as GeoJSON.
     * @throws JSONException
     */

    private static JSONObject createGeoJSONFeatureWay(JSONObject way, HashMap<Long, JSONObject> nodeCache) throws JSONException {
        JSONObject out = new JSONObject();
        out.put(GEOJSON_TYPE_KEY, GEOJSON_FEATURE);
        // Note: if the way is member of a relation, it might
        // have no tags.
        try {
            out.put(GEOJSON_PROPERTIES_KEY, way.getJSONObject(OPASS_TAGS_KEY));
        } catch (JSONException e) {
            out.put(GEOJSON_PROPERTIES_KEY, new JSONObject());
        }
        JSONObject geom = new JSONObject();
        geom.put(GEOJSON_COORDS_KEY, new JSONArray());
        // iterates over the nodes
        JSONArray nodes = way.getJSONArray(OPASS_NODES_KEY);
        for (int i =0 ; i < nodes.length(); ++i) {
            Long nId = nodes.getLong(i);
            JSONObject node = nodeCache.get(nId);
            if (node == null) {
                // should not happen, skipping node
                continue;
            }
            geom.getJSONArray(GEOJSON_COORDS_KEY).put(new JSONArray().put(node.get(OPASS_LON_KEY)).put(node.get(OPASS_LAT_KEY)));
        }

        if (isSupposedPolygon(geom, out.getJSONObject(GEOJSON_PROPERTIES_KEY))) {
            // We need to encapsulate the geom into another JSONArray
            JSONArray oldGeom = geom.getJSONArray(GEOJSON_COORDS_KEY);
            geom.put(GEOJSON_COORDS_KEY, new JSONArray().put(oldGeom));
            geom.put(GEOJSON_TYPE_KEY, GEOJSON_TYPE_POLYGON);
        } else {
            geom.put(GEOJSON_TYPE_KEY, GEOJSON_TYPE_LINESTRING);
        }

        out.put(GEOJSON_GEOMETRY_KEY, geom);
        return out;
    }

    /**
     * Creates a GeoJSON GeometryCollection feature, from a relation coming from the Overpass API.
     *
     * @param rel the JSONObject describing the relation,
     * @param nodeCache a map containing the cached nodes,
     * @param wayCache a map containing the cached ways / polygons,
     * @return a JSONObject describing the GeoJSON feature, as GeometryCollection.
     * @throws JSONException
     */
    private static JSONObject createGeoJSONFeatureRelation(JSONObject rel, HashMap<Long, JSONObject> nodeCache,
            HashMap<Long, JSONObject> wayCache) throws JSONException {
        JSONObject out = new JSONObject();
        out.put(GEOJSON_TYPE_KEY, GEOJSON_FEATURE);

        JSONObject geomType = new JSONObject();
        geomType.put(GEOJSON_TYPE_KEY, GEOJSON_TYPE_GEOMETRYCOLLECTION);

        JSONArray geoms = new JSONArray();

        JSONArray members = rel.getJSONArray(OPASS_MEMBERS_KEY);
        for (int i = 0 ; i < members.length(); ++i) {
            JSONObject member = members.getJSONObject(i);
            Long memberId = member.getLong(OPASS_REF_KEY);
            // TODO: member role in the relation ? Ignoring it for now
            if (member.getString(OPASS_TYPE_KEY).equals(OPASS_TYPE_NODE)) {
                JSONObject resolvedNode = nodeCache.get(memberId);
                if (resolvedNode == null) {
                    continue;
                }
                JSONObject geom = new JSONObject().put(GEOJSON_TYPE_KEY, GEOJSON_TYPE_POINT).put(GEOJSON_COORDS_KEY,
                        new JSONArray().put(resolvedNode.get(OPASS_LON_KEY)).put(resolvedNode.get(OPASS_LAT_KEY)));
                geoms.put(geom);
            } else if (member.getString(OPASS_TYPE_KEY).equals(OPASS_TYPE_WAY)) {
                JSONObject resolvedWay = wayCache.get(memberId);
                // resolvedWay is already a GEJSON-friendly object (see the wayCache loop around line 90)
                // but we are only interested in the geometry inner member, not the whole feature
                geoms.put(resolvedWay.getJSONObject(GEOJSON_GEOMETRY_KEY));
            }
        }

        geomType.put(GEOJSON_GEOMETRIES_KEY, geoms);
        out.put(GEOJSON_GEOMETRY_KEY, geomType);
        out.put(GEOJSON_PROPERTIES_KEY, rel.getJSONObject(OPASS_TAGS_KEY));
        return out;
    }

    /**
     * Tries to guess if the feature is a linestring or a polygon, studying the
     * geometry and the associated tags. The same logic as the one used in the
     * Overpass API is applied here, see
     * http://wiki.openstreetmap.org/wiki/Overpass_turbo/Polygon_Features for
     * more infos.
     *
     * @param geom
     *            the geometry object as JSONObject,
     * @param attrs
     *            the attributes of the feature as JSONObject,

     * @return true if this is supposed to be a polygon, else false (by default,
     * or if an exception is raised, false is always returned).
     */
    private static boolean isSupposedPolygon(JSONObject geom, JSONObject attrs) {
        try {
            JSONArray firstCoords = geom.getJSONArray(GEOJSON_COORDS_KEY).getJSONArray(0);
            JSONArray lastCoords = geom.getJSONArray(GEOJSON_COORDS_KEY).getJSONArray(
                    geom.getJSONArray(GEOJSON_COORDS_KEY).length() - 1);

            // 1. "it forms a closed loop"
            if ((!firstCoords.get(0).equals(lastCoords.get(0))) || (!firstCoords.get(1).equals(lastCoords.get(1)))) {
                return false;
            }
            // 2. "not tagged area=no"
            if (attrs.has("area") && attrs.getString("area").equalsIgnoreCase("no")) {
                return false;
            }
            // 3. "either one of the following is true"
            // building
            if (attrs.has("building") && ! attrs.getString("building").equalsIgnoreCase("no")) {
                return true;
            }
            //highway
            if (attrs.has("highway") && ! attrs.getString("highway").equalsIgnoreCase("no")) {
                String highwayVal = attrs.getString("highway");
                if (highwayVal.equalsIgnoreCase("services") || highwayVal.equalsIgnoreCase("rest_area")
                        || highwayVal.equalsIgnoreCase("escape")) {
                    return true;
                }
            }
            // natural
            if (attrs.has("natural") && ! attrs.getString("natural").equalsIgnoreCase("no")) {
                String nVal = attrs.getString("natural");
                // coastline, cliff, ridge, arete, tree_row
                if (! nVal.equalsIgnoreCase("coastline") && ! nVal.equalsIgnoreCase("cliff")
                        && ! nVal.equalsIgnoreCase("ridge")&& ! nVal.equalsIgnoreCase("arete")
                        && ! nVal.equalsIgnoreCase("tree_row")) {
                    return true;
                }
            }
            // landuse
            if (attrs.has("landuse") && ! attrs.getString("landuse").equalsIgnoreCase("no")) {
                return true;
            }
            // waterway
            if (attrs.has("waterway") && ! attrs.getString("waterway").equalsIgnoreCase("no")) {
                String wVal = attrs.getString("waterway");
                if (wVal.equalsIgnoreCase("riverbank") || wVal.equalsIgnoreCase("dock")
                        || wVal.equalsIgnoreCase("boatyard")|| wVal.equalsIgnoreCase("dam")) {
                    return true;
                }
                else {
                    return false;
                }
            }
            // amenity
            if (attrs.has("amenity") && ! attrs.getString("amenity").equalsIgnoreCase("no")) {
                return true;
            }
            // leisure
            if (attrs.has("leisure") && ! attrs.getString("leisure").equalsIgnoreCase("no")) {
                return true;
            }
            // barrier
            // barrier=city_wall, barrier=ditch, barrier=hedge, barrier=retaining_wall, barrier=wall or barrier=spikes
            if (attrs.has("barrier") && ! attrs.getString("barrier").equalsIgnoreCase("no")) {
                String val = attrs.getString("barrier");
                if (val.equalsIgnoreCase("city_wall") || val.equalsIgnoreCase("ditch")
                        || val.equalsIgnoreCase("hedge") || val.equalsIgnoreCase("retaining_wall")
                        || val.equalsIgnoreCase("wall") || val.equalsIgnoreCase("spikes")) {
                    return true;
                }
            }
            // railway
            // there is a railway=* tag and its value is not railway=no and its value is either railway=station, railway=turntable, railway=roundhouse or railway=platform
            if (attrs.has("railway") && ! attrs.getString("railway").equalsIgnoreCase("no")) {
                String val = attrs.getString("railway");
                if (val.equalsIgnoreCase("station") || val.equalsIgnoreCase("turntable")
                        || val.equalsIgnoreCase("roundhouse") || val.equalsIgnoreCase("platform")) {
                    return true;
                }
            }
            // area
            // "there is a "area" tag" (case area=no already done)
            if (attrs.has("area")) {
                return true;
            }
            // boundary
            if (attrs.has("boundary") && ! attrs.getString("boundary").equalsIgnoreCase("no")) {
                return true;
            }
            // man_made
            // its value is neither man_made=cutline, man_made=embankment nor man_made=pipeline
            if (attrs.has("man_made") && ! attrs.getString("man_made").equalsIgnoreCase("no")) {
                String val = attrs.getString("man_made");
                if (! val.equalsIgnoreCase("cutline") && ! val.equalsIgnoreCase("embankment")
                        && ! val.equalsIgnoreCase("pipeline")) {
                    return true;
                }
            }
            // power
            // power=plant, power=substation, power=generator or power=transformer
            if (attrs.has("power") && ! attrs.getString("power").equalsIgnoreCase("no")) {
                String val = attrs.getString("power");
                if (val.equalsIgnoreCase("plant") || val.equalsIgnoreCase("substation")
                        || val.equalsIgnoreCase("generator") || val.equalsIgnoreCase("transformer")) {
                    return true;
                }
            }
            // place
            if (attrs.has("place") && ! attrs.getString("place").equalsIgnoreCase("no")) {
                return true;
            }
            // shop
            if (attrs.has("shop") && ! attrs.getString("shop").equalsIgnoreCase("no")) {
                return true;
            }
            // aeroway
            // there is a aeroway=* tag and its value is not aeroway=no and its value is not aeroway=taxiway
            if (attrs.has("aeroway") && ! attrs.getString("aeroway").equalsIgnoreCase("no")) {
                String val = attrs.getString("aeroway");
                if (! val.equalsIgnoreCase("taxiway")) {
                    return true;
                }
            }
            // tourism
            if (attrs.has("tourism") && ! attrs.getString("tourism").equalsIgnoreCase("no")) {
                return true;
            }
            // historic
            if (attrs.has("historic") && ! attrs.getString("historic").equalsIgnoreCase("no")) {
                return true;
            }
            // public_transport
            if (attrs.has("public_transport") && ! attrs.getString("public_transport").equalsIgnoreCase("no")) {
                return true;
            }
            // office
            if (attrs.has("office") && ! attrs.getString("office").equalsIgnoreCase("no")) {
                return true;
            }
            // building:part
            if (attrs.has("building:part") && ! attrs.getString("building:part").equalsIgnoreCase("no")) {
                return true;
            }
            // ruins
            if (attrs.has("ruins") && ! attrs.getString("ruins").equalsIgnoreCase("no")) {
                return true;
            }
            // area:highway
            if (attrs.has("area:highway") && ! attrs.getString("area:highway").equalsIgnoreCase("no")) {
                return true;
            }
            // craft
            if (attrs.has("craft") && ! attrs.getString("craft").equalsIgnoreCase("no")) {
                return true;
            }
            // golf
            if (attrs.has("golf") && ! attrs.getString("golf").equalsIgnoreCase("no")) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;

    }
}
