package org.openimis.imisclaims;

import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.FileProvider;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.openimis.imisclaims.tools.ApiException;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.tools.StorageManager;
import org.openimis.imisclaims.util.FileUtils;
import org.openimis.imisclaims.util.JsonUtils;
import org.openimis.imisclaims.util.StringUtils;
import org.openimis.imisclaims.util.XmlUtils;
import org.openimis.imisclaims.util.ZipUtils;
import org.xmlpull.v1.XmlSerializer;

public class SynchronizeService extends JobIntentService {
    private static final int JOB_ID = 6541259; //Random unique Job id
    private static final String LOG_TAG = "SYNCSERVICE";

    private static final String ACTION_UPLOAD_CLAIMS = "SynchronizeService.ACTION_UPLOAD_CLAIMS";
    private static final String ACTION_UPDATE_CLAIMS = "SynchronizeService.ACTION_UPDATE_CLAIMS";
    private static final String ACTION_EXPORT_CLAIMS = "SynchronizeService.ACTION_EXPORT_CLAIMS";
    private static final String ACTION_CLAIM_COUNT = "SynchronizeService.ACTION_CLAIM_COUNT";

    public static final String ACTION_SYNC_SUCCESS = "SynchronizeService.ACTION_SYNC_SUCCESS";
    public static final String ACTION_SYNC_ERROR = "SynchronizeService.ACTION_SYNC_ERROR";
    public static final String ACTION_EXPORT_SUCCESS = "SynchronizeService.ACTION_EXPORT_SUCCESS";
    public static final String ACTION_EXPORT_ERROR = "SynchronizeService.ACTION_EXPORT_ERROR";
    public static final String ACTION_CLAIM_COUNT_RESULT = "SynchronizeService.ACTION_CLAIM_COUNT_RESULT";
    public static final String ACTION_UPDATE_SUCCESS = "SynchronizeService.ACTION_UPDATE_SUCCESS";
    public static final String ACTION_UPDATE_ERROR = "SynchronizeService.ACTION_UPDATE_ERROR";

    public static final String EXTRA_CLAIM_RESPONSE = "SynchronizeService.EXTRA_CLAIM_RESPONSE";
    public static final String EXTRA_ERROR_MESSAGE = "SynchronizeService.EXTRA_ERROR_MESSAGE";
    public static final String EXTRA_CLAIM_COUNT_ENTERED = "SynchronizeService.EXTRA_CLAIM_COUNT_ENTERED";
    public static final String EXTRA_CLAIM_COUNT_ACCEPTED = "SynchronizeService.EXTRA_CLAIM_COUNT_ACCEPTED";
    public static final String EXTRA_CLAIM_COUNT_REJECTED = "SynchronizeService.EXTRA_CLAIM_COUNT_REJECTED";
    public static final String EXTRA_UPDATE_RESPONSE = "SynchronizeService.EXTRA_UPDATE_RESPONSE";
    public static final String EXTRA_EXPORT_URI = "SynchronizeService.EXTRA_EXPORT_URI";

    private static final String claimResponseLine = "[%s] %s";
    private static final String LAST_UPDATE_DATE_PREF_KEY = "lastUpdateDate";
    private static final String LAST_UPDATE_DATE_DEFAULT = "1970-01-01T00:00:00";

    public static class ClaimResponse {
        public static final int Success = 2001;
        public static final int InvalidHFCode = 2002;
        public static final int DuplicateClaimCode = 2003;
        public static final int InvalidInsuranceNumber = 2004;
        public static final int EndDateIsBeforeStartDate = 2005;
        public static final int InvalidICDCode = 2006;
        public static final int InvalidItem = 2007;
        public static final int InvalidService = 2008;
        public static final int InvalidClaimAdmin = 2009;
        public static final int Rejected = 2010;
        public static final int UnexpectedException = 2999;
    }

    private Global global;
    private String lastAction;
    private ToRestApi toRestApi;
    private SQLHandler sqlHandler;
    private StorageManager storageManager;

    @Override
    public void onCreate() {
        super.onCreate();
        global = (Global) getApplicationContext();
        toRestApi = new ToRestApi();
        sqlHandler = new SQLHandler(this);
        storageManager = StorageManager.of(this);
    }

    /**
     * Upload all claims in the background
     *
     * @param context Current context
     */
    public static void uploadClaims(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPLOAD_CLAIMS);
        enqueueWork(context, SynchronizeService.class, JOB_ID, intent);
    }

    /**
     * Update all available claims in the background
     *
     * @param context Current context
     */
    public static void updateClaims(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATE_CLAIMS);
        enqueueWork(context, SynchronizeService.class, JOB_ID, intent);
    }

    /**
     * Export all available claims as XML files in the background
     *
     * @param context Current context
     */
    public static void exportClaims(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_EXPORT_CLAIMS);
        enqueueWork(context, SynchronizeService.class, JOB_ID, intent);
    }

    /**
     * Count all claims in the database, groupong by status in the background
     *
     * @param context Current context
     */
    public static void getClaimCount(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_CLAIM_COUNT);
        enqueueWork(context, SynchronizeService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        lastAction = intent.getAction();

        switch (lastAction) {
            case ACTION_UPLOAD_CLAIMS:
                handleUploadClaims();
                break;
            case ACTION_EXPORT_CLAIMS:
                handleExportClaims();
                break;
            case ACTION_CLAIM_COUNT:
                handleGetClaimCount();
                break;
            case ACTION_UPDATE_CLAIMS:
                handleUpdateClaims();
                break;
            default:
                Log.w(LOG_TAG, "Unknown action: " + lastAction);
        }
    }

    private void handleUploadClaims() {
        if (!global.isNetworkAvailable()) {
            broadcastError(ACTION_SYNC_ERROR, getResources().getString(R.string.CheckInternet));
            return;
        }

        JSONArray claims = sqlHandler.getAllPendingClaims();

        if (claims.length() < 1) {
            broadcastError(ACTION_SYNC_ERROR, getResources().getString(R.string.NoClaim));
            return;
        }

        HttpResponse response = toRestApi.postToRestApiToken(claims, "claim");
        if (response != null) {
            int statusCode = response.getStatusLine().getStatusCode();
            String statusReason = response.getStatusLine().getReasonPhrase();
            String content = toRestApi.getContent(response); //content is an array
            String errorMessage = toRestApi.getHttpError(this, statusCode, statusReason, null);

            if (errorMessage != null) {
                broadcastError(ACTION_SYNC_ERROR, errorMessage);
                return;
            }

            try {
                JSONArray claimResponseArray = new JSONArray(content);
                JSONArray claimStatus = processClaimResponse(claimResponseArray);
                broadcastSyncSuccess(claimStatus);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error while processing claim response", e);
                broadcastError(ACTION_SYNC_ERROR, getResources().getString(R.string.ErrorOccurred));
            }
        }
    }

    private JSONArray processClaimResponse(JSONArray claimResponseArray) throws JSONException {
        JSONArray result = new JSONArray();
        String date = AppInformation.DateTimeInfo.getDefaultIsoDatetimeFormatter().format(new Date());

        for (int i = 0; i < claimResponseArray.length(); i++) {
            JSONObject claimResponse = claimResponseArray.getJSONObject(i);
            String claimCode = claimResponse.getString("claimCode");
            String claimUUID = sqlHandler.getClaimUuid(claimCode);
            int claimResponseCode = claimResponse.getInt("response");

            if (claimResponseCode == ClaimResponse.Success) {
                sqlHandler.insertClaimUploadStatus(claimUUID, date, SQLHandler.CLAIM_UPLOAD_STATUS_ACCEPTED, null);
            } else {
                if (claimResponseCode == ClaimResponse.Rejected) {
                    sqlHandler.insertClaimUploadStatus(claimUUID, date, SQLHandler.CLAIM_UPLOAD_STATUS_REJECTED, null);
                } else {
                    sqlHandler.insertClaimUploadStatus(claimUUID, date, SQLHandler.CLAIM_UPLOAD_STATUS_ERROR, claimResponse.getString("message"));
                }
                result.put(String.format(claimResponseLine, claimCode, claimResponse.getString("message")));
            }
        }
        return result;
    }

    private void handleExportClaims() {
        JSONArray claims = sqlHandler.getAllPendingClaims();
        ArrayList<File> exportedClaims = new ArrayList<>();

        if (claims.length() < 1) {
            broadcastError(ACTION_SYNC_ERROR, getResources().getString(R.string.NoClaim));
            return;
        }

        for (int i = 0; i < claims.length(); i++) {
            try {
                JSONObject claim = claims.getJSONObject(i);
                JSONObject details = claim.getJSONObject("details");

                File claimFile = createClaimFile(details);
                if (claimFile == null) {
                    Log.e(LOG_TAG, "Creating claim temp file failed");
                    continue;
                }

                writeClaimToXmlFile(claimFile, claim);
                exportedClaims.add(claimFile);

                sqlHandler.insertClaimUploadStatus(sqlHandler.getClaimUuid(details.getString("ClaimCode")),
                        AppInformation.DateTimeInfo.getDefaultIsoDatetimeFormatter().format(new Date()),
                        SQLHandler.CLAIM_UPLOAD_STATUS_EXPORTED, null);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Exception while exporting claims", e);
            }
        }

        if (exportedClaims.size() > 0) {
            Uri exportUri = createClaimExportZip(exportedClaims);
            if (exportUri != null) {
                broadcastExportSuccess(exportUri);
            } else {
                broadcastError(ACTION_SYNC_ERROR, getResources().getString(R.string.XmlExportFailed));
            }
        } else {
            broadcastError(ACTION_SYNC_ERROR, getResources().getString(R.string.XmlExportFailed));
        }
    }

    private File createClaimFile(JSONObject details) {
        try {
            Calendar cal = Calendar.getInstance();
            String d = AppInformation.DateTimeInfo.getDefaultDateFormatter().format(cal.getTime());

            String filename = "Claim_" + details.getString("HFCode") + "_" + details.getString("ClaimCode") + "_" + d + ".xml";
            return storageManager.createTempFile("exports/claim/" + filename);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Parsing claim JSON failed", e);
        }
        return null;
    }

    private void writeClaimToXmlFile(File claimFile, JSONObject claim) {
        try (FileOutputStream out = new FileOutputStream(claimFile)) {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(out, "UTF-8");
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            XmlUtils.serializeXml(serializer, "Claim", claim);
            serializer.endDocument();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Writing XML file failed", e);
        }
    }

    private Uri createClaimExportZip(ArrayList<File> exportedClaims) {
        Calendar cal = Calendar.getInstance();
        String d = AppInformation.DateTimeInfo.getDefaultFileDatetimeFormatter().format(cal.getTime());
        String zipFilename = "Claims" + "_" + global.getOfficerCode() + "_" + d + ".zip";
        File zipFile = storageManager.createTempFile("exports/claim/" + zipFilename, true);

        String password = global.getRarPwd();
        ZipUtils.zipFiles(exportedClaims, zipFile, password);
        FileUtils.deleteFiles(exportedClaims.toArray(new File[0]));

        return FileProvider.getUriForFile(this,
                String.format("%s.fileprovider", BuildConfig.APPLICATION_ID),
                zipFile);
    }

    private void handleGetClaimCount() {
        JSONObject counts = sqlHandler.getClaimCounts();

        int enteredCount = counts.optInt(SQLHandler.CLAIM_UPLOAD_STATUS_ENTERED, 0);
        int acceptedCount = counts.optInt(SQLHandler.CLAIM_UPLOAD_STATUS_ACCEPTED, 0);
        int rejectedCount = counts.optInt(SQLHandler.CLAIM_UPLOAD_STATUS_REJECTED, 0);
        broadcastClaimCount(enteredCount, acceptedCount, rejectedCount);
    }

    private void handleUpdateClaims() {
        String lastUpdateDate = global.getStringKey(LAST_UPDATE_DATE_PREF_KEY, LAST_UPDATE_DATE_DEFAULT);

        JSONObject args = new JSONObject();
        try {
            args.put("claim_administrator_code", global.getOfficerCode());
            args.put("last_update_date", lastUpdateDate);

            Date newLastUpdateDate = new Date();
            SimpleDateFormat format = AppInformation.DateTimeInfo.getDefaultIsoShortDatetimeFormatter();
            global.setStringKey(LAST_UPDATE_DATE_PREF_KEY, format.format(newLastUpdateDate));

            JSONArray claimUpdates = getClaimUpdates(args);
            JSONObject updateStatus = processClaimUpdates(claimUpdates);
            broadcastUpdateSuccess(updateStatus);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON Error while updating claims", e);
            broadcastError(ACTION_UPDATE_ERROR, e.getMessage());
        } catch (ApiException e) {
            Log.e(LOG_TAG, "API Error while updating claims", e);
            broadcastError(ACTION_UPDATE_ERROR, e.getMessage());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unexpected Error while updating claims", e);
            broadcastError(ACTION_UPDATE_ERROR, e.getMessage());
        }
    }

    private JSONArray getClaimUpdates(JSONObject args) throws ApiException, JSONException {
        HttpResponse response = toRestApi.postToRestApiToken(args, "claim/GetClaims/");

        int statusCode = response.getStatusLine().getStatusCode();
        String statusReason = response.getStatusLine().getReasonPhrase();
        String content = toRestApi.getContent(response);
        String errorMessage = toRestApi.getHttpError(this, statusCode, statusReason, content);

        if (errorMessage != null) {
            throw new ApiException(errorMessage);
        }

        JSONObject contentObject = new JSONObject(content);
        return contentObject.optJSONArray("data");
    }

    private JSONObject processClaimUpdates(JSONArray claims) throws JSONException {
        int claimUpdates = 0;
        int newClaims = 0;
        for (int i = 0; i < claims.length(); i++) {
            JSONObject claim = claims.getJSONObject(i);
            JSONArray items = claim.optJSONArray("items");
            JSONArray services = claim.optJSONArray("services");

            String claimNo = claim.optString("claim_number");
            String hfCode = claim.optString("health_facility_code");

            if (StringUtils.isEmpty(claimNo, true) || StringUtils.isEmpty(hfCode, true)) {
                Log.w(LOG_TAG, String.format("Invalid claim update identifiers (hfCode=%s, claimNo=%s)", hfCode, claimNo));
                continue;
            }

            // Currently claim app UUID and server UUID are not the same
            String claimUuid = sqlHandler.getClaimUuid(claimNo, hfCode);

            SimpleDateFormat isoFormat = AppInformation.DateTimeInfo.getDefaultIsoShortDatetimeFormatter();
            String updateDate = isoFormat.format(new Date());

            if (claimUuid == null) {
                // Claims not present on the phone will be inserted
                insertClaim(claim, items, services, updateDate);
                newClaims++;
            } else {
                updateClaim(claimUuid, claim, items, services, updateDate);
                claimUpdates++;
            }
        }

        return new JSONObject(String.format("{\"newClaims\":%s, \"claimUpdates\":%s}", newClaims, claimUpdates));
    }

    private void insertClaim(JSONObject claim, JSONArray items, JSONArray services, String updateDate) throws JSONException {
        claim.put("main_dg", sqlHandler.getDiseaseCode(claim.getString("main_dg")));
        claim.put("sec_dg_1", sqlHandler.getDiseaseCode(claim.getString("sec_dg_1")));
        claim.put("sec_dg_2", sqlHandler.getDiseaseCode(claim.getString("sec_dg_2")));
        claim.put("sec_dg_3", sqlHandler.getDiseaseCode(claim.getString("sec_dg_3")));
        claim.put("sec_dg_4", sqlHandler.getDiseaseCode(claim.getString("sec_dg_4")));
        ContentValues claimCv = getClaimCv(claim);
        String claimUuid = claim.getString("claim_uuid");
        claimCv.put("ClaimUUID", claimUuid);
        claimCv.put("VisitType", claimCv.get("VisitType").toString().substring(0, 1));
        claimCv.put("ClaimAdmin", global.getOfficerCode());

        List<ContentValues> itemsCvs = new ArrayList<>();
        if (items != null) {
            for (int j = 0; j < items.length(); j++) {
                ContentValues itemCv = getClaimItemCv(items.getJSONObject(j));
                itemCv.put("LastUpdated", updateDate);
                itemCv.put("ClaimUUID", claimUuid);
                itemsCvs.add(itemCv);
            }
        }

        List<ContentValues> servicesCvs = new ArrayList<>();
        if (services != null) {
            for (int j = 0; j < services.length(); j++) {
                ContentValues serviceCv = getClaimServiceCv(services.getJSONObject(j));
                serviceCv.put("LastUpdated", updateDate);
                serviceCv.put("ClaimUUID", claimUuid);
                servicesCvs.add(serviceCv);
            }
        }

        claimCv.put("LastUpdated", updateDate);
        sqlHandler.insertClaim(claimCv, itemsCvs, servicesCvs);
        sqlHandler.insertClaimUploadStatus(claimUuid, updateDate, claim.getString("claim_status"), "Synchronized Claim (Insert)");
    }

    private void updateClaim(String claimUuid, JSONObject claim, JSONArray items, JSONArray services, String updateDate) throws JSONException {
        claim.put("main_dg", sqlHandler.getDiseaseCode(claim.getString("main_dg")));
        claim.put("sec_dg_1", sqlHandler.getDiseaseCode(claim.getString("sec_dg_1")));
        claim.put("sec_dg_2", sqlHandler.getDiseaseCode(claim.getString("sec_dg_2")));
        claim.put("sec_dg_3", sqlHandler.getDiseaseCode(claim.getString("sec_dg_3")));
        claim.put("sec_dg_4", sqlHandler.getDiseaseCode(claim.getString("sec_dg_4")));
        ContentValues claimCv = getClaimCv(claim);
        claimCv.put("VisitType", claimCv.get("VisitType").toString().substring(0, 1));
        claimCv.put("ClaimAdmin", global.getOfficerCode());

        List<ContentValues> itemsCvs = new ArrayList<>();
        if (items != null) {
            for (int j = 0; j < items.length(); j++) {
                ContentValues itemCv = getClaimItemCv(items.getJSONObject(j));
                itemCv.put("LastUpdated", updateDate);
                itemsCvs.add(itemCv);
            }
        }

        List<ContentValues> servicesCvs = new ArrayList<>();
        if (services != null) {
            for (int j = 0; j < services.length(); j++) {
                ContentValues serviceCv = getClaimServiceCv(services.getJSONObject(j));
                serviceCv.put("LastUpdated", updateDate);
                servicesCvs.add(serviceCv);
            }
        }

        claimCv.put("LastUpdated", updateDate);
        sqlHandler.updateClaimAdjustment(claimUuid, claimCv, itemsCvs, servicesCvs);
        sqlHandler.insertClaimUploadStatus(claimUuid, updateDate, claim.getString("claim_status"), "Synchronized Claim (Update)");
    }

    private ContentValues getClaimCv(JSONObject claim) {
        Map<String, String> fields = new HashMap<>();
        fields.put("health_facility_code", "HFCode");
        fields.put("date_claimed", "ClaimDate");
        fields.put("claim_number", "ClaimCode");
        fields.put("guarantee_number", "GuaranteeNumber");
        fields.put("insurance_number", "InsureeNumber");
        fields.put("visit_date_from", "StartDate");
        fields.put("visit_date_to", "EndDate");
        fields.put("claimed", "Total");
        fields.put("approved", "TotalApproved");
        fields.put("adjusted", "TotalApproved");
        fields.put("explanation", "Explanation");
        fields.put("adjustment", "Adjustment");
        fields.put("visit_type", "VisitType");
        fields.put("main_dg", "ICDCode");
        fields.put("sec_dg_1", "ICDCode1");
        fields.put("sec_dg_2", "ICDCode2");
        fields.put("sec_dg_3", "ICDCode3");
        fields.put("sec_dg_4", "ICDCode4");

        return JsonUtils.jsonToContentValues(claim, fields);
    }

    private ContentValues getClaimItemCv(JSONObject item) {
        Map<String, String> fields = Map.of(
                "item_code", "ItemCode",
                "item_qty", "ItemQuantity",
                "item_price", "ItemPrice",
                "item_adjusted_qty", "ItemQuantityAdjusted",
                "item_adjusted_price", "ItemPriceAdjusted",
                "item_explination", "ItemExplanation",
                "item_justificaion", "ItemJustification",
                "item_valuated", "ItemValuated",
                "item_result", "ItemResult"
        );

        return JsonUtils.jsonToContentValues(item, fields);
    }

    private ContentValues getClaimServiceCv(JSONObject service) {
        Map<String, String> fields = Map.of(
                "service_code", "ServiceCode",
                "service_qty", "ServiceQuantity",
                "service_price", "ServicePrice",
                "service_adjusted_qty", "ServiceQuantityAdjusted",
                "service_adjusted_price", "ServicePriceAdjusted",
                "service_explination", "ServiceExplanation",
                "service_justificaion", "ServiceJustification",
                "service_valuated", "ServiceValuated",
                "service_result", "ServiceResult"
        );

        return JsonUtils.jsonToContentValues(service, fields);
    }

    private void broadcastSyncSuccess(JSONArray claimResponse) {
        Intent successIntent = new Intent(ACTION_SYNC_SUCCESS);
        successIntent.putExtra(EXTRA_CLAIM_RESPONSE, claimResponse.toString());
        sendBroadcast(successIntent);
        Log.i(LOG_TAG, String.format(Locale.US, "%s finished with %s, messages count: %d", lastAction, ACTION_SYNC_SUCCESS, claimResponse.length()));
    }

    private void broadcastUpdateSuccess(JSONObject updateStatus) {
        Intent successIntent = new Intent(ACTION_UPDATE_SUCCESS);
        successIntent.putExtra(EXTRA_UPDATE_RESPONSE, updateStatus.toString());
        sendBroadcast(successIntent);
        Log.i(LOG_TAG, String.format("%s finished with %s, status: %s", lastAction, ACTION_UPDATE_SUCCESS, updateStatus));
    }

    private void broadcastExportSuccess(Uri exportUri) {
        Intent successIntent = new Intent(ACTION_EXPORT_SUCCESS);
        successIntent.putExtra(EXTRA_EXPORT_URI, exportUri.toString());
        sendBroadcast(successIntent);
        Log.i(LOG_TAG, String.format("%s finished with %s, export uri: %s", lastAction, ACTION_EXPORT_SUCCESS, exportUri));
    }

    private void broadcastError(String action, String errorMessage) {
        Intent errorIntent = new Intent(action);
        errorIntent.putExtra(EXTRA_ERROR_MESSAGE, errorMessage);
        sendBroadcast(errorIntent);
        Log.i(LOG_TAG, String.format("%s finished with %s, error message: %s", lastAction, action, errorMessage));
    }

    private void broadcastClaimCount(int entered, int accepted, int rejected) {
        Intent resultIntent = new Intent(ACTION_CLAIM_COUNT_RESULT);
        resultIntent.putExtra(EXTRA_CLAIM_COUNT_ENTERED, entered);
        resultIntent.putExtra(EXTRA_CLAIM_COUNT_ACCEPTED, accepted);
        resultIntent.putExtra(EXTRA_CLAIM_COUNT_REJECTED, rejected);
        sendBroadcast(resultIntent);
        Log.i(LOG_TAG, String.format(Locale.US, "%s finished with %s, result:  p: %d,a: %d,r: %d", lastAction, ACTION_CLAIM_COUNT_RESULT, entered, accepted, rejected));
    }
}