package utils;

//Use ConcurrentHashMap for thread safety
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class MetadataCache {

 // FieldInfo holds apiName and dataType for each field
 public static class FieldInfo {
     public final String apiName;
     public final String dataType;

     public FieldInfo(String apiName, String dataType) {
         this.apiName = apiName;
         this.dataType = dataType;
     }
 }

 // Thread-safe cache: objectName -> (label -> FieldInfo)
 private static final ConcurrentHashMap<String, ConcurrentHashMap<String, FieldInfo>> objectFieldCache = new ConcurrentHashMap<>();

 /**
  * Retrieves all fields for the given object, fetching from Salesforce only once per object.
  * Supports concurrent access safely.
  */
 public static ConcurrentHashMap<String, FieldInfo> getAllFields(String objectName) throws Exception {
     // Double-checked locking pattern for efficiency
     if (objectFieldCache.containsKey(objectName)) {
         return objectFieldCache.get(objectName);
     }

     synchronized (MetadataCache.class) {
         if (objectFieldCache.containsKey(objectName)) {
             return objectFieldCache.get(objectName);
         }

         // Fetch from Salesforce UI API or sObject describe
         ConcurrentHashMap<String, FieldInfo> fieldsMap = new ConcurrentHashMap<>();
         try {
             String path = "/ui-api/object-info/" + objectName;
             JSONObject response = HTTPClientWrapper.runGetRequest(path);
             JSONObject fields = response.getJSONObject("fields");

             for (String apiName : fields.keySet()) {
                 JSONObject field = fields.getJSONObject(apiName);
                 String label = field.getString("label");
                 String dataType = field.getString("dataType");
                 fieldsMap.put(label, new FieldInfo(apiName, dataType));
                 fieldsMap.put(apiName, new FieldInfo(apiName, dataType));
             }
         } catch (Exception e) {
             // Fallback
             String path = "/sobjects/" + objectName + "/describe";
             JSONObject response = HTTPClientWrapper.runGetRequest(path);
             JSONArray fields = response.getJSONArray("fields");

             for (int i = 0; i < fields.length(); i++) {
                 JSONObject field = fields.getJSONObject(i);
                 String label = field.getString("label");
                 String apiName = field.getString("name");
                 String dataType = field.getString("type");
                 fieldsMap.put(label, new FieldInfo(apiName, dataType));
                 fieldsMap.put(apiName, new FieldInfo(apiName, dataType));
             }
         }
         objectFieldCache.put(objectName, fieldsMap);
         return fieldsMap;
     }
 }
}