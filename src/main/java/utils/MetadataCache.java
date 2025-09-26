package utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
//Use ConcurrentHashMap for thread safety
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class MetadataCache {

    // cache: objectName -> field map
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, FieldInfo>> objectFieldCache =
            new ConcurrentHashMap<>();

    // ---------- Public API ----------

    // Old method (backward compatible)
    public static ConcurrentHashMap<String, FieldInfo> getAllFields(String objectName) throws Exception {
        return getAllFields(objectName, "SObject");
    }

    // New overload with type support
    public static ConcurrentHashMap<String, FieldInfo> getAllFields(String name, String type) throws Exception {
        if (objectFieldCache.containsKey(name)) return objectFieldCache.get(name);

        synchronized (MetadataCache.class) {
            if (objectFieldCache.containsKey(name)) return objectFieldCache.get(name);

            ConcurrentHashMap<String, FieldInfo> fieldsMap;
            if ("Flow".equalsIgnoreCase(type)) {
                fieldsMap = fetchFlowFields(name);    // Flow variables
            } else {
                fieldsMap = fetchObjectFields(name);  // Normal sObject fields
            }

            objectFieldCache.put(name, fieldsMap);
            return fieldsMap;
        }
    }

    // ---------- Helpers ----------

    /**
     * Fetch fields for a normal sObject using UI API describe.
     */
    private static ConcurrentHashMap<String, FieldInfo> fetchObjectFields(String objectApiName) throws Exception {
        ConcurrentHashMap<String, FieldInfo> fieldsMap = new ConcurrentHashMap<>();

        // call UI API: /services/data/vXX.X/ui-api/object-info/{objectApiName}
        String path = "/ui-api/object-info/" + objectApiName;
        JSONObject response = (JSONObject) HTTPClientWrapper.runGetRequest(path);

        JSONArray fields = response.getJSONObject("fields").names();
        if (fields != null) {
            for (int i = 0; i < fields.length(); i++) {
                String apiName = fields.getString(i);
                JSONObject field = response.getJSONObject("fields").getJSONObject(apiName);

                String label = field.optString("label", apiName);
                String dataType = field.optString("dataType", "String");

                FieldInfo fi = new FieldInfo(apiName, dataType);

                // multiple keys for flexibility
                fieldsMap.put(apiName, fi);
                fieldsMap.put(label, fi);
                fieldsMap.put(objectApiName + " " + label, fi);
            }
        }

        return fieldsMap;
    }

    /**
     * Fetch input variables for a Flow from Tooling API.
     */
    private static ConcurrentHashMap<String, FieldInfo> fetchFlowFields(String flowDevName) throws Exception {
        ConcurrentHashMap<String, FieldInfo> fieldsMap = new ConcurrentHashMap<>();

        if (flowDevName == null || flowDevName.trim().isEmpty()) return fieldsMap;

        // Escape SOQL single quotes
        String safeName = flowDevName.replace("'", "\\'");

        // Step 1: Get FlowDefinition Id by DeveloperName
        String defQuery = "SELECT+Id+FROM+FlowDefinition+WHERE+DeveloperName='" + safeName + "'+LIMIT+1";
        String defPath = "/tooling/query/?q=" + defQuery;
        JSONObject defResp = (JSONObject) HTTPClientWrapper.runGetRequest(defPath);
        JSONArray defRecords = defResp.optJSONArray("records");

        if (defRecords == null || defRecords.isEmpty()) {
            return fieldsMap; // no flowdefinition found
        }

        String definitionId = defRecords.getJSONObject(0).optString("Id");
        if (definitionId == null || definitionId.isEmpty()) {
            return fieldsMap;
        }

        // Step 2: Get Flow record(s) by DefinitionId
        String flowQuery = "SELECT+Id,DefinitionId,MasterLabel,FullName,Metadata+FROM+Flow"
                + "+WHERE+DefinitionId='" + definitionId + "'+AND+Status='Active'+ORDER+BY+VersionNumber+DESC+LIMIT+1";
        String flowPath = "/tooling/query/?q=" + flowQuery;
        JSONObject flowResp = (JSONObject) HTTPClientWrapper.runGetRequest(flowPath);
        JSONArray flowRecords = flowResp.optJSONArray("records");

        if (flowRecords == null || flowRecords.isEmpty()) {
            return fieldsMap; // no active flow found
        }

        JSONObject flow = flowRecords.getJSONObject(0);
        JSONObject metadata = flow.optJSONObject("Metadata");
        JSONArray variables = metadata != null ? metadata.optJSONArray("variables") : null;

        if (variables != null) {
            for (int i = 0; i < variables.length(); i++) {
                JSONObject v = variables.getJSONObject(i);
                String apiName = v.optString("name");
                if (apiName == null || apiName.isEmpty()) continue;

                String label = v.optString("fieldText", apiName);
                String dataType = v.optString("dataType", "String");

                FieldInfo fi = new FieldInfo(apiName, dataType);

                fieldsMap.put(apiName, fi);
                fieldsMap.put(label, fi);
                fieldsMap.put(flowDevName + " " + label, fi);
            }
        }

        return fieldsMap;
    }

    // ---------- Inner class ----------

    public static class FieldInfo {
        public final String apiName;
        public final String dataType;

        public FieldInfo(String apiName, String dataType) {
            this.apiName = apiName;
            this.dataType = dataType;
        }
    }
}