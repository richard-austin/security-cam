package security.cam.commands

import grails.validation.Validateable
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONException
import org.grails.web.json.JSONObject

class UpdateCamerasCommand implements Validateable {
    String camerasJSON

    static constraints = {
        camerasJSON(nullable: false,
        validator: {
            if(isJSONValid(it))
                return
            return 'The configuration data is not valid JSON'
        })
    }
    private static  boolean isJSONValid(String test) {
        try {
            new JSONObject(test)
        } catch (JSONException ignore) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test)
            } catch (JSONException ignored) {
                return false
            }
        }
        return true
    }

}
