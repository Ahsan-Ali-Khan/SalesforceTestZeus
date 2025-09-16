package utils;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectPrefixCache {

    private static final class Holder {
        static final Map<String, String> prefixToObjectMap = loadPrefixMap();
    }

    private static Map<String, String> loadPrefixMap() {
        try {
            String path = "/sobjects";
            JSONObject response = HTTPClientWrapper.runGetRequest(path);
            JSONArray sobjects = response.getJSONArray("sobjects");

            Map<String, String> map = new ConcurrentHashMap<>();
            for (int i = 0; i < sobjects.length(); i++) {
                JSONObject obj = sobjects.getJSONObject(i);
                String name = obj.getString("name");
                String prefix = obj.optString("keyPrefix");
                if (prefix != null && !prefix.isEmpty()) {
                    map.put(prefix, name);
                }
            }
            System.out.println(" Loaded prefix map: " + map);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load prefix map", e);
        }
    }

    public static String getObjectName(String prefix) {
        return Holder.prefixToObjectMap.get(prefix); // Triggers loading on first call
    }
}