package testscripts;

import utils.HTTPClientWrapper;
import org.json.JSONObject;
import org.testng.annotations.Test;

import base.BaseTest;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class AccountLabelDrivenAPITest extends BaseTest {

	@Test(priority = 1)
	public void LoginCreateFetchDeleteLogout() throws Exception {

        

        HTTPClientWrapper client = new HTTPClientWrapper();

        // -----------------------------
        // 2️⃣ Create Account (label-driven)
        // -----------------------------
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("Account Name", "LabelDrivenTestAccount");
        accountData.put("Prospect Type", "Advertiser");
        accountData.put("Billing City", "Stamford");
        accountData.put("Phone", "9948705203");

        JSONObject createdAccount = client.createByLabels("Account", accountData);
        String accountId = createdAccount.getString("id");
        System.out.println("Created Account Id: " + accountId);

        // -----------------------------
        // 3️⃣ Get account details by labels
        // -----------------------------
        List<String> fieldsToFetch = new ArrayList<>();
        fieldsToFetch.add("Account Name");
        fieldsToFetch.add("Prospect Type");
        fieldsToFetch.add("Billing City");
        fieldsToFetch.add("Phone");

        JSONObject accountDetails = client.getByLabels("Account", accountId, fieldsToFetch);

        System.out.println("\nAccount Details (Label -> API Name -> Value):");
        Map<String, String> labelToApiMap = client.getLabelToApiMap("Account");
        for (String label : fieldsToFetch) {
            String apiName = labelToApiMap.get(label);
            Object value = accountDetails.opt(apiName);
            System.out.println(label + " -> " + apiName + " -> " + value);
        }

        // -----------------------------
        // 4️⃣ Delete the account
        // -----------------------------
        client.deleteRecord("Account", accountId);
        System.out.println("\nDeleted Account: " + accountId);

        // -----------------------------
        // 5️⃣ Verify deletion
        // -----------------------------
        JSONObject deletedCheck = (JSONObject) HTTPClientWrapper.runGetRequest("/sobjects/Account/" + accountId);
        if (deletedCheck == null) {
            System.out.println("Verified: Account successfully deleted.");
        } else {
            System.out.println("Account still exists!");
        }

        // -----------------------------
        // Logout
        // -----------------------------
        HTTPClientWrapper.SFLogout_API();
    }
}
