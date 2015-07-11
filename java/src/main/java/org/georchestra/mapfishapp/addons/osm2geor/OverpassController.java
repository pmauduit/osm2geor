package org.georchestra.mapfishapp.addons.osm2geor;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class OverpassController {

    @RequestMapping(value = "/osm2geor/q")
    public void queryOverPassTurbo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject objs = new JSONObject();

        response.getOutputStream().write(objs.toString().getBytes());
        return;
    }
}
