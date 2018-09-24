package eu.nimble.service.catalogue.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class DataModelUtil {

    public static void removeHjidFields(JSONObject jsonObject) {

        if (jsonObject.has("hjid")) {
            jsonObject.remove("hjid");
        }
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                removeHjidFields((JSONObject) value);

            } else if (value instanceof JSONArray) {
                removeHjidFields((JSONArray) value);
            }
        }
    }

    public static void removeHjidFields(JSONArray jsonArray) {
        if(jsonArray.length() > 0) {
            if (jsonArray.get(0) instanceof JSONObject) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    removeHjidFields((JSONObject) jsonArray.get(i));
                }
            } else if (jsonArray.get(0) instanceof JSONArray) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    removeHjidFields((JSONArray) jsonArray.get(i));
                }
            }
        }
    }

}
