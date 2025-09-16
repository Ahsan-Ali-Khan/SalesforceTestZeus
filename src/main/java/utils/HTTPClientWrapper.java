package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import base.BaseTest;
import utils.MetadataCache.FieldInfo;

public class HTTPClientWrapper {
	/*
	 * @author: Robin Gupta
	 * 
	 * @Date: 29 Sep 2021
	 * 
	 * @Purpose: This class acts as a bridge for interacting with Salesforce REST
	 * APIs ⚙
	 */

	private static String REST_ENDPOINT = "/services/data";
	private static String API_VERSION = "/v64.0";
	private static String baseUri;
	private static Header oauthHeader;
	private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    private static final Map<String, Map<String, String>> describeCache = new HashMap<>();
	private static HttpPost httpPost;
	private static String loginInstanceUrl;
	

	// Initial steps for API access
	// reset api token for user and enable connected app access in the Profile for
	// System admin or user at hand
	// UAT is appended as a shorthand reference for Pre-production testing, and is
	// intended to keep the code easy to read. 📑
	
	public static String SFLogin_API(String envName, String roleName) {
	    try {
	        String apiUrl = EnvironmentConfigDto.getApiUrl(envName);
	        String grantType = EnvironmentConfigDto.getGrantType(envName);
	        String clientId = EnvironmentConfigDto.getClientId(envName);
	        String clientSecret = EnvironmentConfigDto.getClientSecret(envName);

	        String postUri;

	        if ("password".equalsIgnoreCase(grantType)) {
	            Map<String, String> creds = EnvironmentConfigDto.getRoleCredentials(envName, "SystemAdmin");
	            postUri = apiUrl + "/services/oauth2/token?grant_type=password"
	                    + "&client_id=" + clientId
	                    + "&client_secret=" + clientSecret
	                    + "&username=" + creds.get("username")
	                    + "&password=" + creds.get("password");
	        } else if ("client_credentials".equalsIgnoreCase(grantType)) {
	            postUri = apiUrl + "/services/oauth2/token?grant_type=client_credentials"
	                    + "&client_id=" + clientId
	                    + "&client_secret=" + clientSecret;
	        } else {
	            throw new UnsupportedOperationException("❌ Unsupported grantType: " + grantType);
	        }

	        HttpClient httpclient = HttpClientBuilder.create().build();
	        httpPost = new HttpPost(postUri);
	        httpPost.addHeader("Content-Type", "application/json");

	        HttpResponse response = httpclient.execute(httpPost);

	        int statusCode = response.getStatusLine().getStatusCode();
	        if (statusCode != 200) {
	            String body = EntityUtils.toString(response.getEntity());
	            throw new RuntimeException("❌ API Login failed for " + envName 
	                    + " (" + statusCode + "): " + body);
	        }

	        JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
	        String accessToken = json.getString("access_token");
	        loginInstanceUrl = json.getString("instance_url");

	        baseUri = loginInstanceUrl + REST_ENDPOINT + API_VERSION;
	        oauthHeader = new BasicHeader("Authorization", "OAuth " + accessToken);

	        return loginInstanceUrl;

	    } catch (Exception e) {
	        throw new RuntimeException("❌ Error in Salesforce API Login for env=" + envName + ", role=" + roleName, e);
	    }
	}

	public static String SFLogin_API(String SFAPILOGINURL_UAT, String SFAPIGRANTSERVICE, String SFAPICLIENTID_UAT,
			String SFAPICLIENTSECRET_UAT, String SFAPIUSERNAME_UAT, String SFAPIPASSWORD_UAT) {

		HttpClient httpclient = HttpClientBuilder.create().build();

		// Login requests must be POSTs
		String postUri = SFAPILOGINURL_UAT + SFAPIGRANTSERVICE + "&client_id=" + SFAPICLIENTID_UAT + "&client_secret="
				+ SFAPICLIENTSECRET_UAT + "&username=" + SFAPIUSERNAME_UAT + "&password=" + SFAPIPASSWORD_UAT;

		System.out.println("POST URI is " + postUri);
		httpPost = new HttpPost(postUri);

		httpPost.addHeader("Content-Type", "application/json");

		HttpResponse response = null;

		System.out.println("POST Request is" + httpPost.toString());

		System.out.println("Login Post headers are below:");
		Header[] headers = httpPost.getAllHeaders();

		for (Header header : headers) {
			System.out.println(header.getName() + ": " + header.getValue());
		}

		try {
			// Execute the login POST
			// request-----------------------------------------------------
			response = httpclient.execute(httpPost);
			System.out.println("Executing the login POST request");
			System.out.println("Login response is" + response);
		} catch (Exception e) {
			System.out.println("Exception in SF API Login" + e.getMessage());
		}

		// verify response is HTTP OK
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			System.out.println("Error authenticating to Force.com: " + statusCode);
			// Error is in EntityUtils.toString(response.getEntity())
			return null;
		}

		String getResult = null;
		try {
			getResult = EntityUtils.toString(response.getEntity());
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		JSONObject jsonObject = null;
		String loginAccessToken = null;

		try {
			jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
			loginAccessToken = jsonObject.getString("access_token");
			loginInstanceUrl = jsonObject.getString("instance_url");
		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
		}

		baseUri = loginInstanceUrl + REST_ENDPOINT + API_VERSION;
		oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken);
		System.out.println("oauthHeader1: " + oauthHeader);
		System.out.println("\n" + response.getStatusLine());

		System.out.println("instance URL: " + loginInstanceUrl);
		System.out.println("baseUri: " + baseUri);
		System.out.println("Created POST connection for SF REST API for Login purpose");
		return loginInstanceUrl;
	}

	public static void SFLogout_API() {
		httpPost.releaseConnection();
		System.out.println("Releasing connection from SF REST API via HTTPClientWrapper");
	}
	
	public static String getLoginInstanceUrl() {
	    return loginInstanceUrl;
	}
	
	public static String getRecordTypeId(String objectName, String developerName) {
	    try {
	        String soql = String.format(
	            "SELECT Id FROM RecordType WHERE SobjectType='%s' AND DeveloperName='%s' LIMIT 1",
	            objectName, developerName
	        );

	        String url = baseUri + "/query/?q=" + URLEncoder.encode(soql, StandardCharsets.UTF_8);
	        HttpGet httpGet = new HttpGet(url);
	        httpGet.addHeader(oauthHeader);
	        httpGet.addHeader(prettyPrintHeader);

	        HttpResponse response = HttpClientBuilder.create().build().execute(httpGet);
	        String responseBody = EntityUtils.toString(response.getEntity());

	        JSONObject json = new JSONObject(responseBody);
	        JSONArray records = json.getJSONArray("records");

	        if (records.isEmpty()) {
	            throw new RuntimeException("RecordType not found for " + objectName + ":" + developerName);
	        }

	        return records.getJSONObject(0).getString("Id");
	    } catch (Exception e) {
	        throw new RuntimeException("Error fetching RecordTypeId for " + objectName + ":" + developerName, e);
	    }
	}
	
	
	public static String getUserIdByRole(String roleName) {
	    String envName = BaseTest.environmentName;
	    try {
	        // 1. Get Username for the role from config
	        Map<String, String> creds = EnvironmentConfigDto.getRoleCredentials(envName, roleName);
	        String roleUsername = creds.get("username");
	        if (roleUsername == null) {
	            throw new RuntimeException("❌ No username found for role: " + roleName);
	        }

	        // 2. Ensure login is already done (SFLogin_API must have been called before)
	        if (oauthHeader == null || baseUri == null) {
	            throw new IllegalStateException("❌ Salesforce is not logged in. Call SFLogin_API() first.");
	        }

	        // 3. Build SOQL query
	        String soql = "SELECT Id FROM User WHERE Username = '" + roleUsername + "' LIMIT 1";
	        String queryUrl = baseUri + "/query/?q=" + URLEncoder.encode(soql, "UTF-8");

	        HttpClient httpClient = HttpClientBuilder.create().build();
	        HttpGet httpGet = new HttpGet(queryUrl);
	        httpGet.addHeader(oauthHeader);
	        httpGet.addHeader(prettyPrintHeader);

	        HttpResponse queryResponse = httpClient.execute(httpGet);
	        int queryStatus = queryResponse.getStatusLine().getStatusCode();

	        if (queryStatus != 200) {
	            String body = EntityUtils.toString(queryResponse.getEntity());
	            throw new RuntimeException("❌ Failed to query User for role=" + roleName
	                    + " (" + queryStatus + "): " + body);
	        }

	        JSONObject queryJson = new JSONObject(EntityUtils.toString(queryResponse.getEntity()));
	        JSONArray records = queryJson.getJSONArray("records");

	        if (records.length() == 0) {
	            throw new RuntimeException("❌ No user found with username: " + roleUsername);
	        }

	        // 4. Return the User Id
	        return records.getJSONObject(0).getString("Id");

	    } catch (Exception e) {
	        throw new RuntimeException("❌ Error in getUserIdByRole for env=" + envName 
	                + ", role=" + roleName, e);
	    }
	}


	public static JSONObject runGetRequest(String uri) {
		System.out.println("\n_______________ sObject Get Request _______________");

		try {

			// Set up the HTTP objects needed to make the request.
			HttpClient httpClient = HttpClientBuilder.create().build();

			System.out.println("GET URI is " + baseUri + uri);

			HttpGet httpGet = new HttpGet(baseUri + uri);
			httpGet.addHeader(oauthHeader);
			httpGet.addHeader(prettyPrintHeader);

			// Make the request.
			HttpResponse response = httpClient.execute(httpGet);

			// Process the result
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				String response_string = EntityUtils.toString(response.getEntity());
				try {
					JSONObject json = new JSONObject(response_string);

					return json;

				} catch (JSONException je) {
					je.printStackTrace();
				}
			} else {
				System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
				System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
				System.out.println(getBody(response.getEntity().getContent()));
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		return null;

	}

	public static void update_sObjectDetails(String uri, JSONObject json) {

		// Update sObjects using REST HttpPatch. We have to create the HTTPPatch, as it
		// does not exist in the standard library
		// Since the PATCH method was only recently standardized and is not yet
		// implemented in Apache HttpClient

		System.out.println("\n_______________ sObject UPDATE _______________");

		try {

			System.out.println("JSON for update of  record:\n" + json.toString(1));

			// Set up the objects necessary to make the request.
			// DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpClient httpClient = HttpClientBuilder.create().build();

			HttpPatch httpPatch = new HttpPatch(baseUri + uri);
			httpPatch.addHeader(oauthHeader);
			httpPatch.addHeader(prettyPrintHeader);
			StringEntity body = new StringEntity(json.toString(1));
			body.setContentType("application/json");
			httpPatch.setEntity(body);

			// Make the request
			HttpResponse response = httpClient.execute(httpPatch);

			// Process the response
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 204) {
				System.out.println("Updated the sObject successfully.");
			} else {
				System.out.println("sObject update NOT successfully. Status code is " + statusCode);
				
			}
		} catch (JSONException e) {
			System.out.println("Issue creating JSON or processing results");
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}

	}

	public static JSONObject create_sObject(String uri, JSONObject jsondata) {
		System.out.println("\n_______________ sObject INSERT _______________");

		try {

			System.out.println("JSON for sObject record to be inserted:\n" + jsondata.toString(1));

			// Construct the objects needed for the request
			HttpClient httpClient = HttpClientBuilder.create().build();

			HttpPost httpPost = new HttpPost(baseUri + uri);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);
			// The message we are going to post
			StringEntity body = new StringEntity(jsondata.toString(1));
			body.setContentType("application/json");
			httpPost.setEntity(body);
			System.out.println("post value: " + httpPost);
			// Make the request
			HttpResponse response = httpClient.execute(httpPost);

			// Process the results
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 201) {
				String response_string = EntityUtils.toString(response.getEntity());
				JSONObject json = new JSONObject(response_string);

				return json;

			} else {
				System.out.println("Insertion unsuccessful. Status code returned is " + statusCode);
				String response_string = EntityUtils.toString(response.getEntity());
				System.out.println(response_string);
				
			}
		} catch (JSONException e) {
			System.out.println("Issue creating JSON or processing results");
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		return null;

	}

	// Extend the Apache HttpPost method to implement an HttpPatch
	private static class HttpPatch extends HttpPost {
		public HttpPatch(String uri) {
			super(uri);
		}

		@Override
		public String getMethod() {
			return "PATCH";
		}
	}

	private static String getBody(InputStream inputStream) {
		String result = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				result += inputLine;
				result += "\n";
			}
			in.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

	public static String runDeleteRequest(String uri) {
		System.out.println("\n_______________ sObject Delete Request _______________");

		try {

			// Set up the HTTP objects needed to make the request.
			HttpClient httpClient = HttpClientBuilder.create().build();

			System.out.println("Delete URI is" + baseUri + uri);

			HttpDelete httpDelete = new HttpDelete(baseUri + uri);
			httpDelete.addHeader(oauthHeader);
			httpDelete.addHeader(prettyPrintHeader);

			// Make the request.
			HttpResponse response = httpClient.execute(httpDelete);

			// Process the result
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200 || statusCode == 204) {
				try {
					try {
						String response_string = EntityUtils.toString(response.getEntity());

						JSONArray json = new JSONArray(response_string);

						String deleteresponse = json.toString();
						return deleteresponse;
					} catch (JSONException je) {
						je.printStackTrace();
					}
				} catch (Exception e) {
					System.out.println(
							"Delete request is successful with no Response body for the status code : " + statusCode);
				}
			} else {
				System.out.println("Delete request was unsuccessful. Status code returned is " + statusCode);
				System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
				System.out.println(getBody(response.getEntity().getContent()));
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		return null;

	}
	
	// ------------------------------
    // 🔹 Label-driven CREATE
    // ------------------------------
	public JSONObject createByLabels(String objectName, Map<String, Object> labelValueMap) throws Exception {
	    Map<String, MetadataCache.FieldInfo> fields = MetadataCache.getAllFields(objectName);
	    JSONObject body = new JSONObject();

	    for (Map.Entry<String, Object> entry : labelValueMap.entrySet()) {
	        String label = entry.getKey();
	        String apiName = resolveApiName(label, fields); 
	        body.put(apiName, entry.getValue());
	    }

	    String path = "/sobjects/" + objectName;
	    return create_sObject(path, body);
	}

	

    // ------------------------------
    // 🔹 Label-driven UPDATE
    // ------------------------------
	public void updateByLabels(String objectName, String recordId, Map<String, Object> labelValueMap) throws Exception {
	    // Get field metadata mapping for the object
		Map<String, MetadataCache.FieldInfo> fields = MetadataCache.getAllFields(objectName);

	    JSONObject body = new JSONObject();

	    for (Map.Entry<String, Object> entry : labelValueMap.entrySet()) {
	        String inputKey = entry.getKey();

	        // Resolve the API name using MetadataCache.FieldInfo
	        String apiName = resolveApiName(inputKey, fields);

	        body.put(apiName, entry.getValue());
	    }

	    String path = "/sobjects/" + objectName + "/" + recordId;
	    update_sObjectDetails(path, body);
	}

    // ------------------------------
    // 🔹 Label-driven GET
    // ------------------------------
    public JSONObject getByLabels(String objectName, String recordId, List<String> labels) throws Exception {
    	Map<String, String> labelToApi = getLabelToApiMap(objectName);

        List<String> apiFields = new ArrayList<>();
        for (String label : labels) {
            if (!labelToApi.containsKey(label)) {
                throw new IllegalArgumentException("Invalid field label: " + label);
            }
            apiFields.add(labelToApi.get(label));
        }

        String query = String.join(",", apiFields);
        String path = "/sobjects/" + objectName + "/" + recordId + "?fields=" + query;

        return runGetRequest(path); // assumes you already have get() implemented
    }

    // ------------------------------
    // 🔹 DELETE
    // ------------------------------
    public void deleteRecord(String objectName, String recordId) throws Exception {
    	String path = "/sobjects/" + objectName + "/" + recordId;
        runDeleteRequest(path); // assumes you already have delete() implemented
    }

    // ------------------------------
    // 🔹 Label-driven QUERY
    // ------------------------------
    public JSONObject queryByLabels(String objectName, List<String> labels, String whereClause) throws Exception {
    	Map<String, String> labelToApi = getLabelToApiMap(objectName);

        List<String> apiFields = new ArrayList<>();
        for (String label : labels) {
            if (!labelToApi.containsKey(label)) {
                throw new IllegalArgumentException("Invalid field label: " + label);
            }
            apiFields.add(labelToApi.get(label));
        }

        String query = "SELECT " + String.join(",", apiFields) + " FROM " + objectName;
        if (whereClause != null && !whereClause.isEmpty()) {
            query += " WHERE " + whereClause;
        }

        String path = "/query?q=" + query;
        return runGetRequest(path);
    }

    // ------------------------------
    // 🔹 Describe & Cache
    // ------------------------------
    
 // Cache for combined UI-API + Describe labels
    private static final Map<String, Map<String, String>> labelToApiCache = new HashMap<>();

    // ------------------------------
    // 🔹 Combined Label-to-API mapping
    // ------------------------------
    public Map<String, String> getLabelToApiMap(String objectName) throws Exception {
        // 1️⃣ Check cache first
        return describeCache.computeIfAbsent(objectName, key -> {
            try {
                // 2️⃣ Try UI API first
                String uiApiPath = "/ui-api/object-info/" + key;
                JSONObject uiApiResponse = runGetRequest(uiApiPath);
                JSONObject fields = uiApiResponse.getJSONObject("fields");
                return buildFieldMap(fields, true);
            } catch (Exception uiEx) {
                // 3️⃣ On failure → fallback to sObject describe
                try {
                    String describePath = "/sobjects/" + key + "/describe";
                    JSONObject describeResponse = runGetRequest(describePath);
                    JSONArray fieldsArray = describeResponse.getJSONArray("fields");
                    return buildFieldMap(fieldsArray, false);
                } catch (Exception descEx) {
                    throw new RuntimeException("Failed to fetch field map for: " + key, descEx);
                }
            }
        });
    }
    
    private String resolveApiName(String inputLabel, Map<String, MetadataCache.FieldInfo> fields) {
        List<String> suffixes = Arrays.asList("__c", "__r", "__kav"); // Extend as needed

        // 1. Exact match
        if (fields.containsKey(inputLabel)) {
            return fields.get(inputLabel).apiName;
        }

        // 2. Replace spaces with underscores
        String underscored = inputLabel.replace(" ", "_");
        if (fields.containsKey(underscored)) {
            return fields.get(underscored).apiName;
        }

        // 3. Try appending suffixes
        for (String suffix : suffixes) {
            String withSuffix = underscored + suffix;
            if (fields.containsKey(withSuffix)) {
                System.out.println("Found with suffix >> " + withSuffix);
                return fields.get(withSuffix).apiName;
            }
        }

        throw new IllegalArgumentException("Invalid field label: '" + inputLabel +
                "'. Allowed fields (sample): " + fields.keySet());
    }

    /**
     * Builds a mapping of Label → API name and API → API name.
     * Handles both UI-API (JSONObject fields) and sObject describe (JSONArray fields).
     */
    private Map<String, String> buildFieldMap(Object fields, boolean fromUiApi) {
        Map<String, String> map = new HashMap<>();

        if (fromUiApi) {
            JSONObject fieldsObj = (JSONObject) fields;
            for (String apiName : fieldsObj.keySet()) {
                JSONObject field = fieldsObj.getJSONObject(apiName);
                putFieldMapping(map, field.getString("label"), apiName);
            }
        } else {
            JSONArray fieldsArray = (JSONArray) fields;
            for (int i = 0; i < fieldsArray.length(); i++) {
                JSONObject field = fieldsArray.getJSONObject(i);
                putFieldMapping(map, field.getString("label"), field.getString("name"));
            }
        }
        return map;
    }

    /**
     * Adds both label → apiName and apiName → apiName mapping (no duplication).
     */
    private void putFieldMapping(Map<String, String> map, String label, String apiName) {
        map.putIfAbsent(label, apiName);
        map.putIfAbsent(apiName, apiName);
    }
    
    
}
