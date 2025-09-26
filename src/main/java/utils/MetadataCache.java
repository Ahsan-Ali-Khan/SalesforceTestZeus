package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
//Use ConcurrentHashMap for thread safety
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jayway.jsonpath.JsonPath;

public class MetadataCache {

    // cache: objectName -> field map
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, FieldInfo>> objectFieldCache =
            new ConcurrentHashMap<>();

    // ---------- Public API ----------

 // Old method (backward compatible, default type = SObject)
    public static ConcurrentHashMap<String, FieldInfo> getAllFields(String objectName) throws Exception {
        return getAllFields(objectName, "SObject");
    }

    // Type-aware method (single object)
    public static ConcurrentHashMap<String, FieldInfo> getAllFields(String name, String type) throws Exception {
        if (objectFieldCache.containsKey(name)) {
            return objectFieldCache.get(name);
        }

        synchronized (MetadataCache.class) {
            if (objectFieldCache.containsKey(name)) {
                return objectFieldCache.get(name);
            }

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

    // NEW: Merge fields from both SObject and Flow
    public static Map<String, FieldInfo> getAllFieldsMerged(String sObjectName, String flowName) throws Exception {
        Map<String, FieldInfo> allFields = new HashMap<>();

        if (sObjectName != null && !sObjectName.isEmpty()) {
            Map<String, FieldInfo> sObjectFields = fetchObjectFields(sObjectName);
            allFields.putAll(sObjectFields);
        }

        if (flowName != null && !flowName.isEmpty()) {
            Map<String, FieldInfo> flowFields = fetchFlowFields(flowName);
            allFields.putAll(flowFields);
        }

        return allFields;
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
        if (metadata == null) return fieldsMap;

        // Step 3: Use JSONPath to extract fields with name, fieldType, and fieldText
        List<Object> fields = JsonPath.read(metadata.toString(), "$..fields[?(@.name && @.fieldType && @.fieldText)]");

        for (Object obj : fields) {
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldMap = (Map<String, Object>) obj;

            String apiName = safeString(fieldMap.get("name"));
            if (apiName == null) continue;

            String label = safeString(fieldMap.get("fieldText"), apiName);
            String dataType = safeString(fieldMap.get("fieldType"), "String");

            FieldInfo fi = new FieldInfo(apiName, dataType);
            putFieldInMap(fieldsMap, apiName, label, flowDevName, fi);
        }

        return fieldsMap;
    }

    // Helper methods
    private static String safeString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private static String safeString(Object obj, String defaultValue) {
        return obj != null ? obj.toString() : defaultValue;
    }

    private static void putFieldInMap(Map<String, FieldInfo> map, String apiName, String label, String flowDevName, FieldInfo fi) {
        map.put(apiName, fi);
        map.put(label, fi);
        map.put(flowDevName + " " + label, fi);
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
    
    public class QuickActionContext {
        private static final ThreadLocal<String> currentSObject = ThreadLocal.withInitial(() -> null);
        private static final ThreadLocal<String> currentFlow = ThreadLocal.withInitial(() -> null);
        private static final ThreadLocal<String> currentObjectType = ThreadLocal.withInitial(() -> "SObject");

        // --- SObject ---
        public static void setCurrentSObject(String sObject) { currentSObject.set(sObject); }
        public static String getCurrentSObject() { return currentSObject.get(); }

        // --- Flow ---
        public static void setCurrentFlow(String flow) { currentFlow.set(flow); }
        public static String getCurrentFlow() { return currentFlow.get(); }

        // --- Object type ---
        public static void setCurrentObjectType(String type) { currentObjectType.set(type); }
        public static String getCurrentObjectType() { return currentObjectType.get(); }

        // --- Clear after test run ---
        public static void clear() {
            currentSObject.remove();
            currentFlow.remove();
            currentObjectType.remove();
        }
    }
}