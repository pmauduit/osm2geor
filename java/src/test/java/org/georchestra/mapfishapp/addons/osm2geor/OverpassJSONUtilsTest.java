package org.georchestra.mapfishapp.addons.osm2geor;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

public class OverpassJSONUtilsTest {

    @Test
    public void toGeoJSONTest() throws Exception {
        URL testFile = this.getClass().getResource("/opassapi.json");
        assertTrue("testFile not found: opassapi.json", testFile != null);
        JSONTokener t = new JSONTokener(FileUtils.readFileToString(new File(testFile.toURI())));
        JSONObject testInput = new JSONObject(t);

        JSONObject ret = OverpassJSONUtils.toGeoJSON(testInput);

        assertTrue("Expected 40 features, found " + ret.getJSONArray("features").length(),
                ret.getJSONArray("features").length() == 40);
    }

    @Test
    public void toGeJSONPolygonTest() throws Exception {
            URL testFile = this.getClass().getResource("/opassapipoly.json");
            assertTrue("testFile not found: opassapipoly.json", testFile != null);
            JSONTokener t = new JSONTokener(FileUtils.readFileToString(new File(testFile.toURI())));
            JSONObject testInput = new JSONObject(t);

            JSONObject ret = OverpassJSONUtils.toGeoJSON(testInput);

            assertTrue("Expected 1 feature, found " + ret.getJSONArray("features").length(),
                    ret.getJSONArray("features").length() == 1);
            // the feature should be a polygon
            assertTrue("Expected a polygon",
                    ret.getJSONArray("features").getJSONObject(0).
                    getJSONObject("geometry").getString("type").equals("Polygon"));
    }

    @Test
    public void toGeoJSONRelationTest() throws Exception {
        URL testFile = this.getClass().getResource("/opassapirel.json");
        assertTrue("testFile not found: opassapirel.json", testFile != null);
        JSONTokener t = new JSONTokener(FileUtils.readFileToString(new File(testFile.toURI())));
        JSONObject testInput = new JSONObject(t);

        JSONObject ret = OverpassJSONUtils.toGeoJSON(testInput);

        assertTrue("Expected 525 features, found " + ret.getJSONArray("features").length(),
                ret.getJSONArray("features").length() == 525);
        // The asset should contain at least one GeometryCollection
        JSONArray feat = ret.getJSONArray("features");
        int featCollectionCount = 0;
        for (int i = 0 ; i < feat.length(); i++) {
            JSONObject curf = feat.getJSONObject(i);
            if (curf.getJSONObject("geometry").getString("type").equals("GeometryCollection")) {
                featCollectionCount++;
            }
        }
        assertTrue("expected 1 feature collection, found " + featCollectionCount, featCollectionCount == 1);
    }
}
