package org.georchestra.mapfishapp.addons.osm2geor;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
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
        // TODO better checks ...
    }
}
