package org.georchestra.mapfishapp.addons.osm2geor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class OverpassController {

    @RequestMapping(value = "/osm2geor/q")
    public void queryOverPassTurbo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject objs = new JSONObject();

        String req = request.getParameter("data");

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://overpass-api.de/api/interpreter");
        List<NameValuePair> params = new ArrayList<NameValuePair>(1);
        params.add(new BasicNameValuePair("data", req));
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        try {
            HttpResponse oPassResponse = httpclient.execute(httppost);
            HttpEntity entity = oPassResponse.getEntity();

            int ret = oPassResponse.getStatusLine().getStatusCode();
            String statusLine = oPassResponse.getStatusLine().getReasonPhrase();
            if (ret == 200) {
            if (entity != null) {
                InputStream instream = entity.getContent();
                try {
                    objs = new JSONObject(IOUtils.toString(instream));
                } finally {
                    try {
                        instream.close();
                    } catch (Exception e) {

                    }
                }
            }
            } else {
                response.setStatus(ret);
                objs = new JSONObject().put("status", statusLine);
            }
        } catch (Exception e) {

        }
        try {
            response.getOutputStream().write(objs.toString(4).getBytes());
        } catch (JSONException e) {
            // Error occured when trying to dump the Overpass API response
        }
        return;
    }
}
