package org.openimis.imisclaims;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openimis.imisclaims.domain.entity.ChequeImport;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.domain.entity.Insuree;
import org.openimis.imisclaims.domain.entity.Medication;
import org.openimis.imisclaims.domain.entity.PendingClaim;
import org.openimis.imisclaims.domain.entity.Service;
import org.openimis.imisclaims.domain.entity.SubServiceItem;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.tools.StorageManager;
import org.openimis.imisclaims.usecase.CreateClaim;
import org.openimis.imisclaims.usecase.FetchChequeNumber;
import org.openimis.imisclaims.usecase.FetchInsuree;
import org.openimis.imisclaims.usecase.FetchInsureeInquire;
import org.openimis.imisclaims.usecase.PostNewClaims;
import org.openimis.imisclaims.usecase.ValidateClaimCode;
import org.openimis.imisclaims.util.DateUtils;
import org.openimis.imisclaims.util.FileUtils;
import org.openimis.imisclaims.util.JsonUtils;
import org.openimis.imisclaims.util.XmlUtils;
import org.openimis.imisclaims.util.ZipUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import okhttp3.Response;

public class SynchronizeService extends JobIntentService {
    private static final int JOB_ID = 6541259; //Random unique Job id
    private static final String LOG_TAG = "SYNCSERVICE";

    private static final String ACTION_UPLOAD_CLAIMS = "SynchronizeService.ACTION_UPLOAD_CLAIMS";
    private static final String ACTION_EXPORT_CLAIMS = "SynchronizeService.ACTION_EXPORT_CLAIMS";
    private static final String ACTION_CLAIM_COUNT = "SynchronizeService.ACTION_CLAIM_COUNT";

    public static final String ACTION_SYNC_SUCCESS = "SynchronizeService.ACTION_SYNC_SUCCESS";
    public static final String ACTION_SYNC_ERROR = "SynchronizeService.ACTION_SYNC_ERROR";
    public static final String ACTION_EXPORT_SUCCESS = "SynchronizeService.ACTION_EXPORT_SUCCESS";
    public static final String ACTION_EXPORT_ERROR = "SynchronizeService.ACTION_EXPORT_ERROR";
    public static final String ACTION_CLAIM_COUNT_RESULT = "SynchronizeService.ACTION_CLAIM_COUNT_RESULT";

    public static final String EXTRA_CLAIM_RESPONSE = "SynchronizeService.EXTRA_CLAIM_RESPONSE";
    public static final String EXTRA_ERROR_MESSAGE = "SynchronizeService.EXTRA_ERROR_MESSAGE";
    public static final String EXTRA_CLAIM_COUNT_ENTERED = "SynchronizeService.EXTRA_CLAIM_COUNT_ENTERED";
    public static final String EXTRA_CLAIM_COUNT_ACCEPTED = "SynchronizeService.EXTRA_CLAIM_COUNT_ACCEPTED";
    public static final String EXTRA_CLAIM_COUNT_REJECTED = "SynchronizeService.EXTRA_CLAIM_COUNT_REJECTED";
    public static final String EXTRA_EXPORT_URI = "SynchronizeService.EXTRA_EXPORT_URI";

    private static final String claimResponseLine = "[%s] %s";

    private Global global;
    private SQLHandler sqlHandler;
    private StorageManager storageManager;

    @Override
    public void onCreate() {
        super.onCreate();
        global = (Global) getApplicationContext();
        sqlHandler = new SQLHandler(this);
        storageManager = StorageManager.of(this);
    }

    public static void uploadClaims(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPLOAD_CLAIMS);
        enqueueWork(context, SynchronizeService.class, JOB_ID, intent);
    }

    public static void exportClaims(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_EXPORT_CLAIMS);
        enqueueWork(context, SynchronizeService.class, JOB_ID, intent);
    }

    public static void getClaimCount(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_CLAIM_COUNT);
        enqueueWork(context, SynchronizeService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String action = intent.getAction();
        if (ACTION_UPLOAD_CLAIMS.equals(action)) {
            handleUploadClaims();
        } else if (ACTION_EXPORT_CLAIMS.equals(action)) {
            handleExportClaims();
        } else if (ACTION_CLAIM_COUNT.equals(action)) {
            handleGetClaimCount();
        }
    }

    private void handleUploadClaims() {
        if (!global.isNetworkAvailable()) {
            broadcastError(getResources().getString(R.string.CheckInternet), ACTION_UPLOAD_CLAIMS);
            return;
        }

        JSONArray claimsArray = sqlHandler.getAllPendingClaims();
        if (claimsArray.length() < 1) {
            broadcastError(getResources().getString(R.string.NoClaim), ACTION_UPLOAD_CLAIMS);
            return;
        }

        try {
            String adminId = sqlHandler.getClaimAdminInfo(global.getOfficerCode(),"Id");

            List<Claim> claims = claimFromJSONObject(claimsArray);
            String hfId = sqlHandler.getClaimAdminInfo(global.getOfficerCode(),"HFId");

            List<PostNewClaims.Result> results = new ArrayList<>();
            for (Claim claim: claims) {
                int insureeId = 0;
                int programId = sqlHandler.getProgamId(claim.getClaimProgram());
                int diagnosisId = sqlHandler.getDiagnosisId(claim.getMainDg());
                String programCode = sqlHandler.getProgamCode(claim.getClaimProgram());
                try{
                    boolean isValidClaimCode = new ValidateClaimCode().execute(claim.getClaimNumber());
                    if(isValidClaimCode){
                        if(claim.getClaimProgram().equals("Cheque Santé") || claim.getClaimProgram().equals("Chèque Santé")){
                            List<ChequeImport> cheques = new FetchChequeNumber().execute(claim.getClaimPrefix());
                            if(cheques.size() == 0){
                                PostNewClaims.Result result = new PostNewClaims.Result(claim.getClaimNumber(), PostNewClaims.Result.Status.ERROR,getResources().getString(R.string.InvalidChequeNumber));
                                results.add(result);
                            }else if(cheques.get(0).getStatus().equals("New")){
                                PostNewClaims.Result result = new PostNewClaims.Result(claim.getClaimNumber(), PostNewClaims.Result.Status.ERROR,getResources().getString(R.string.NonUsedChequeNumber));
                                results.add(result);
                            }else{
                                String insuree = new FetchInsuree().execute(claim.getInsuranceNumber());
                                insureeId = Integer.valueOf(insuree);
                                Response response = new CreateClaim().execute(claim, Integer.valueOf(adminId),Integer.valueOf(hfId),insureeId,programId, diagnosisId, programCode);
                                if(response.code() == 200){
                                    PostNewClaims.Result result = new PostNewClaims.Result(claim.getClaimNumber(), PostNewClaims.Result.Status.SUCCESS,null);
                                    results.add(result);
                                }else{
                                    PostNewClaims.Result result = new PostNewClaims.Result(claim.getClaimNumber(), PostNewClaims.Result.Status.ERROR,response.message());
                                    results.add(result);
                                }
                            }
                        }else {
                            String insuree = new FetchInsuree().execute(claim.getInsuranceNumber());
                            insureeId = Integer.valueOf(insuree);
                            Response response = new CreateClaim().execute(claim, Integer.valueOf(adminId),Integer.valueOf(hfId),insureeId,programId, diagnosisId, programCode);
                            if(response.code() == 200){
                                PostNewClaims.Result result = new PostNewClaims.Result(claim.getClaimNumber(), PostNewClaims.Result.Status.SUCCESS,null);
                                results.add(result);
                            }else{
                                PostNewClaims.Result result = new PostNewClaims.Result(claim.getClaimNumber(), PostNewClaims.Result.Status.ERROR,response.message());
                                results.add(result);
                            }
                        }
                    }else{
                        PostNewClaims.Result result = new PostNewClaims.Result(claim.getClaimNumber(), PostNewClaims.Result.Status.ERROR,getResources().getString(R.string.ClaimNumberExist));
                        results.add(result);
                    }

                }catch(Exception e){
                    PostNewClaims.Result result = new PostNewClaims.Result(claim.getClaimNumber(), PostNewClaims.Result.Status.ERROR,getResources().getString(R.string.NoInsureeFound));
                    results.add(result);
                }

            }
            JSONArray claimStatus = processClaimResponse(results);
            broadcastSyncSuccess(claimStatus);
        } catch (Exception e) {
            e.printStackTrace();
            broadcastError(getResources().getString(R.string.ErrorOccurred) + ": " + e.getMessage(), ACTION_UPLOAD_CLAIMS);
        }
    }

    private List<Claim> claimFromJSONObject(
            @NonNull JSONArray array
    )throws JSONException {
        List<Claim> claims = new ArrayList<>();
        for (int i=0; i<array.length();i++){
            JSONArray arrayItems = array.getJSONObject(i).getJSONArray("items");
            JSONArray arrayServices = array.getJSONObject(i).getJSONArray("services");
            claims.add( new Claim(
                    /* uuid = */ null,
                    /* hfCode = */ array.getJSONObject(i).getJSONObject("details").getString("HFCode"),
                    /* hfName = */ null,
                    /* insureeNumber = */ Objects.requireNonNull(array.getJSONObject(i).getJSONObject("details").getString("CHFID")),
                    /* patientName = */ null,
                    /* claimNumber = */ Objects.requireNonNull(array.getJSONObject(i).getJSONObject("details").getString("ClaimCode")),
                    /* claimProgram = */ array.getJSONObject(i).getJSONObject("details").getString("Program"),
                    /* dateClaimed = */ Objects.requireNonNull(JsonUtils.getDateOrDefault(array.getJSONObject(i).getJSONObject("details"), "ClaimDate")),
                    /* visitDatefrom = */ Objects.requireNonNull(JsonUtils.getDateOrDefault(array.getJSONObject(i).getJSONObject("details"), "StartDate")),
                    /* visitDateTo = */ Objects.requireNonNull(JsonUtils.getDateOrDefault(array.getJSONObject(i).getJSONObject("details"), "EndDate")),
                    /* visitType = */ array.getJSONObject(i).getJSONObject("details").getString("VisitType"),
                    /* status = */null,
                    /* mainDg = */ array.getJSONObject(i).getJSONObject("details").getString("ICDCode"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    /* testNumber = */ array.getJSONObject(i).getJSONObject("details").getString("TestNumber"),
                    /* tdr = */ array.getJSONObject(i).getJSONObject("details").getString("Tdr"),
                    null,
                    /* chequeNumber = */ array.getJSONObject(i).getJSONObject("details").getString("ClaimPrefix"),
                    /* services */ fromJSONObjectService(arrayServices),
                    /* items */ fromJSONObjectItem(arrayItems)
            ));
        }
        return claims;
    }

    private List<Claim.Medication> fromJSONObjectItem(
            @NonNull JSONArray arrItems
    ) throws JSONException{
        List<Claim.Medication> items = new ArrayList<>();
        for( int i = 0; i<arrItems.length(); i++){
            items.add(
                    new Claim.Medication(
                            /* id */ arrItems.getJSONObject(i).getString("ItemId"),
                            /* code */ arrItems.getJSONObject(i).getString("ItemCode"),
                            null,
                            /* price */ Double.valueOf(arrItems.getJSONObject(i).getString("ItemPrice")),
                            null,
                            /* qty provide = */ arrItems.getJSONObject(i).getString("ItemQuantity"),
                            null,
                            null,
                            null,
                            null,
                            null
                    )
            );
        }
        return items;
    }


    private List<Claim.Service> fromJSONObjectService(
            @NonNull JSONArray arrServices
    ) throws JSONException{
        List<Claim.Service> services = new ArrayList<>();
        for( int i = 0; i<arrServices.length(); i++){
            JSONArray arrSubServices = new JSONArray();
            JSONArray arrSubItems = new JSONArray();
            if (arrServices.getJSONObject(i).has("SubServicesItems")){
                JSONArray arrSubServicesItems = arrServices.getJSONObject(i).getJSONArray("SubServicesItems");
                for (int j=0; j< arrSubServicesItems.length();j++){
                    if(arrSubServicesItems.getJSONObject(j).getString("Type").equals("S")){
                        arrSubServices.put(arrSubServicesItems.getJSONObject(j));
                    }else if(arrSubServicesItems.getJSONObject(j).getString("Type").equals("I")){
                        arrSubItems.put(arrSubServicesItems.getJSONObject(j));
                    }
                }
            }

            services.add(
                    new Claim.Service(
                            /* id */ arrServices.getJSONObject(i).getString("ServiceId"),
                            /* code */ arrServices.getJSONObject(i).getString("ServiceCode"),
                            null,
                            /* price */ Double.valueOf(arrServices.getJSONObject(i).getString("ServicePrice")),
                            null,
                            /* packageType */ arrServices.getJSONObject(i).getString("ServicePackageType"),
                            /* quantityProvide */ arrServices.getJSONObject(i).getString("ServiceQuantity"),
                            null,
                            null,
                            null,
                            null,
                            null,
                            /* subservices = */ fromSubServiceItemJson(arrSubServices),
                            /* subItems = */ fromSubServiceItemJson(arrSubItems)
                    )
            );
        }
        return services;
    }

    private List<SubServiceItem> fromSubServiceItemJson(
            @NonNull JSONArray array
    ) throws JSONException{
        List<SubServiceItem> subServiceItems = new ArrayList<>();
        for(int i=0 ; i < array.length() ; i++){
            subServiceItems.add(new SubServiceItem(
                    null,
                    /* code = */ array.getJSONObject(i).getString("Code"),
                    /* quantity = */ Integer.valueOf(array.getJSONObject(i).getString("Quantity")),
                    /* price = */ array.getJSONObject(i).getString("Price")
            ));
        }
        return subServiceItems;
    }

    private JSONArray processClaimResponse(List<PostNewClaims.Result> results) {
        JSONArray jsonResults = new JSONArray();
        String date = AppInformation.DateTimeInfo.getDefaultIsoDatetimeFormatter().format(new Date());
        for (PostNewClaims.Result result : results) {
            String claimCode = result.getClaimCode();
            String claimUUID = sqlHandler.getClaimUUIDForCode(claimCode);
            PostNewClaims.Result.Status claimResponseCode = result.getStatus();

            if (claimResponseCode == PostNewClaims.Result.Status.SUCCESS) {
                sqlHandler.insertClaimUploadStatus(claimUUID, date, SQLHandler.CLAIM_UPLOAD_STATUS_ACCEPTED, null);
            } else {
                if (claimResponseCode == PostNewClaims.Result.Status.REJECTED) {
                    sqlHandler.insertClaimUploadStatus(claimUUID, date, SQLHandler.CLAIM_UPLOAD_STATUS_REJECTED, null);
                } //else {
                    //sqlHandler.insertClaimUploadStatus(claimUUID, date, SQLHandler.CLAIM_UPLOAD_STATUS_ERROR, result.getMessage());
                //}
                jsonResults.put(String.format(claimResponseLine, claimCode, result.getMessage()));
            }
        }
        return jsonResults;
    }

    private void handleExportClaims() {
        JSONArray claims = sqlHandler.getAllPendingClaims();
        ArrayList<File> exportedClaims = new ArrayList<>();

        if (claims.length() < 1) {
            broadcastError(getResources().getString(R.string.NoClaim), ACTION_EXPORT_CLAIMS);
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

                sqlHandler.insertClaimUploadStatus(sqlHandler.getClaimUUIDForCode(details.getString("ClaimCode")),
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
                broadcastError(getResources().getString(R.string.XmlExportFailed), ACTION_EXPORT_CLAIMS);
            }
        } else {
            broadcastError(getResources().getString(R.string.XmlExportFailed), ACTION_EXPORT_CLAIMS);
        }
    }

    private File createClaimFile(JSONObject details) {
        try {
            Calendar cal = Calendar.getInstance();
            String d = DateUtils.toDateString(cal.getTime());

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

    private void broadcastSyncSuccess(JSONArray claimResponse) {
        Intent successIntent = new Intent(ACTION_SYNC_SUCCESS);
        successIntent.putExtra(EXTRA_CLAIM_RESPONSE, claimResponse.toString());
        sendBroadcast(successIntent);
        Log.i(LOG_TAG, String.format(Locale.US, "%s finished with %s, messages count: %d", SynchronizeService.ACTION_UPLOAD_CLAIMS, ACTION_SYNC_SUCCESS, claimResponse.length()));
    }


    private void broadcastExportSuccess(Uri exportUri) {
        Intent successIntent = new Intent(ACTION_EXPORT_SUCCESS);
        successIntent.putExtra(EXTRA_EXPORT_URI, exportUri.toString());
        sendBroadcast(successIntent);
        Log.i(LOG_TAG, String.format("%s finished with %s, export uri: %s", SynchronizeService.ACTION_EXPORT_CLAIMS, ACTION_EXPORT_SUCCESS, exportUri));
    }

    private void broadcastError(String errorMessage, @NonNull String action) {
        Intent errorIntent = new Intent(ACTION_SYNC_ERROR);
        errorIntent.putExtra(EXTRA_ERROR_MESSAGE, errorMessage);
        sendBroadcast(errorIntent);
        Log.i(LOG_TAG, String.format("%s finished with %s, error message: %s", action, ACTION_SYNC_ERROR, errorMessage));
    }

    private void broadcastClaimCount(int entered, int accepted, int rejected) {
        Intent resultIntent = new Intent(ACTION_CLAIM_COUNT_RESULT);
        resultIntent.putExtra(EXTRA_CLAIM_COUNT_ENTERED, entered);
        resultIntent.putExtra(EXTRA_CLAIM_COUNT_ACCEPTED, accepted);
        resultIntent.putExtra(EXTRA_CLAIM_COUNT_REJECTED, rejected);
        sendBroadcast(resultIntent);
        Log.i(LOG_TAG, String.format(Locale.US, "%s finished with %s, result:  p: %d,a: %d,r: %d", ACTION_CLAIM_COUNT, ACTION_CLAIM_COUNT_RESULT, entered, accepted, rejected));
    }
}