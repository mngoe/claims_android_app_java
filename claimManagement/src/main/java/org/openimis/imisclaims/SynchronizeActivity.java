package org.openimis.imisclaims;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.tools.StorageManager;
import org.openimis.imisclaims.util.StreamUtils;
import org.openimis.imisclaims.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SynchronizeActivity extends ImisActivity {
    private static final String LOG_TAG = "SYNCACTIVITY";
    private static final int PICK_FILE_REQUEST_CODE = 1;
    private static final int REQUEST_EXPORT_XML_FILE = 2;
    ArrayList<String> broadcastList;
    ToRestApi toRestApi;
    MainActivity ma;

    TextView tvUploadClaims, tvZipClaims;
    RelativeLayout uploadClaims, zipClaims, importMasterData, downloadMasterData;

    ProgressDialog pd;
    Uri exportUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synchronize);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        broadcastList = new ArrayList<>();
        broadcastList.add(SynchronizeService.ACTION_CLAIM_COUNT_RESULT);
        broadcastList.add(SynchronizeService.ACTION_SYNC_SUCCESS);
        broadcastList.add(SynchronizeService.ACTION_SYNC_ERROR);
        broadcastList.add(SynchronizeService.ACTION_EXPORT_SUCCESS);
        broadcastList.add(SynchronizeService.ACTION_EXPORT_ERROR);
        broadcastList.add(MasterDataService.ACTION_IMPORT_ERROR);
        broadcastList.add(MasterDataService.ACTION_IMPORT_SUCCESS);
        broadcastList.add(MasterDataService.ACTION_DOWNLOAD_ERROR);
        broadcastList.add(MasterDataService.ACTION_DOWNLOAD_SUCCESS);


        tvUploadClaims = findViewById(R.id.tvUploadClaims);
        tvZipClaims = findViewById(R.id.tvZipClaims);

        uploadClaims = findViewById(R.id.upload_claims);
        zipClaims = findViewById(R.id.zip_claims);
        importMasterData = findViewById(R.id.importMasterData);
        downloadMasterData = findViewById(R.id.downloadMasterData);

        uploadClaims.setOnClickListener(view -> doLoggedIn(this::confirmUploadClaims));
        zipClaims.setOnClickListener(view -> confirmXMLCreation());

        importMasterData.setOnClickListener(view -> requestPickDatabase());
        downloadMasterData.setOnClickListener(view -> {
            JSONObject object1 = new JSONObject();
            try {
                object1.put("claim_administrator_code", global.getOfficerCode());
                //DownLoadInsureeNumbers(object1);
                DownloadMasterData();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //fonction qui va permettre de télecharger les données et les stocker en local

        }); //TODO Not yet implemented
        //downloadMasterData.setVisibility(View.GONE);

    }

    @Override
    public void onResume() {
        super.onResume();
        SynchronizeService.getClaimCount(this);
    }

    @Override
    protected void onBroadcastReceived(Context context, Intent intent) {
        String action = intent.getAction();
        String errorMessage;

        switch (action) {
            case SynchronizeService.ACTION_CLAIM_COUNT_RESULT:
                tvUploadClaims.setText(String.valueOf(intent.getIntExtra(SynchronizeService.EXTRA_CLAIM_COUNT_ENTERED, 0)));
                tvZipClaims.setText(String.valueOf(intent.getIntExtra(SynchronizeService.EXTRA_CLAIM_COUNT_ENTERED, 0)));
                break;
            case SynchronizeService.ACTION_EXPORT_SUCCESS:
                exportUri = Uri.parse(intent.getStringExtra(SynchronizeService.EXTRA_EXPORT_URI));
                showDialog(getResources().getString(R.string.XmlExportCreated),
                        (dialog, which) -> StorageManager.of(this).requestCreateFile(
                                REQUEST_EXPORT_XML_FILE,
                                "application/octet-stream",
                                UriUtils.getDisplayName(this, exportUri)));
                break;
            case SynchronizeService.ACTION_SYNC_SUCCESS:
                try {
                    JSONArray result = new JSONArray(intent.getStringExtra(SynchronizeService.EXTRA_CLAIM_RESPONSE));
                    int resultLength = result.length();
                    if (resultLength > 0) {
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < resultLength; i++) {
                            String message = result.getString(i);
                            builder.append(message).append("\n");
                            Log.i(LOG_TAG, message);
                        }
                        showDialog(builder.toString());
                    } else {
                        showDialog(getResources().getString(R.string.BulkUpload));
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error while processing claim response", e);
                }
                break;
            case SynchronizeService.ACTION_EXPORT_ERROR:
            case SynchronizeService.ACTION_SYNC_ERROR:
                errorMessage = intent.getStringExtra(SynchronizeService.EXTRA_ERROR_MESSAGE);
                showDialog(errorMessage);
                break;
            case MasterDataService.ACTION_IMPORT_SUCCESS:
                showDialog(getResources().getString(R.string.importMasterDataSuccess));
                break;
            case MasterDataService.ACTION_IMPORT_ERROR:
                errorMessage = intent.getStringExtra(MasterDataService.EXTRA_ERROR_MESSAGE);
                showDialog(errorMessage);
                break;
        }

        if (pd != null && pd.isShowing()) pd.dismiss();

        if (!SynchronizeService.ACTION_CLAIM_COUNT_RESULT.equals(action)) {
            SynchronizeService.getClaimCount(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedFile = data.getData();
            pd = ProgressDialog.show(this, "", getResources().getString(R.string.Processing));
            MasterDataService.importMasterData(this, selectedFile);
        } else if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            showToast(R.string.importMasterDataCanceled);
        } else if (requestCode == REQUEST_EXPORT_XML_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri outputFileUri = data.getData();
                try (InputStream is = getContentResolver().openInputStream(exportUri);
                     OutputStream os = getContentResolver().openOutputStream(outputFileUri)) {
                    StreamUtils.bufferedStreamCopy(is, os);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Copying XML export failed", e);
                }
            } else {
                showDialog(getResources().getString(R.string.XmlExportRetry),
                        (dialog, which) -> StorageManager.of(this).requestCreateFile(
                                REQUEST_EXPORT_XML_FILE,
                                "application/octet-stream",
                                UriUtils.getDisplayName(this, exportUri)));
            }
        }
    }

    @Override
    protected ArrayList<String> getBroadcastList() {
        return broadcastList;
    }

    public void confirmXMLCreation() {
        showDialog(getResources().getString(R.string.AreYouSure), (dialogInterface, i) -> exportClaims(), (dialog, id) -> dialog.cancel());
    }

    public void confirmUploadClaims() {
        showDialog(getResources().getString(R.string.AreYouSure), (dialogInterface, i) -> uploadClaims(), (dialog, id) -> dialog.cancel());
    }

    public void requestPickDatabase() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void uploadClaims() {
        pd = ProgressDialog.show(this, "", getResources().getString(R.string.Processing));
        SynchronizeService.uploadClaims(this);
    }

    public void exportClaims() {
        pd = ProgressDialog.show(this, "", getResources().getString(R.string.Processing));
        SynchronizeService.exportClaims(this);
    }

    public void ErrorDialogBox(final String message) {
        showDialog(message);
    }

    public void DownloadMasterData() {
        if (global.isNetworkAvailable()) {
            String progress_message = getResources().getString(R.string.application);
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.initializing), progress_message);
            Thread thread = new Thread(() -> {

                if (downloadServices() == true && downloadItems() == true && downloadDiagnoses() == true &&
                downloadControls() == true && downloadAdmins() == true) {

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.installed_updates), Toast.LENGTH_LONG).show();
                    });

                } else {

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.downloadFail), Toast.LENGTH_LONG).show();
                    });

                }

            });
            thread.start();
        } else {
            ErrorDialogBox(getResources().getString(R.string.CheckInternet));
        }
    }

    public String getPriceService(String code, JSONArray arrPriceService){
        String price = null;
        try{
            for (int i = 0; i < arrPriceService.length();i++){
                JSONObject object = arrPriceService.getJSONObject(i);
                if (object.getString("code").equals(code)){
                    price = object.getString("price");
                }
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return price;
    }


    public boolean downloadServices() {

        toRestApi = new ToRestApi();
        String functionServices = "GetListServiceAllItems";

        String services = toRestApi.getFromRestApiVersion(functionServices, "2");

        JSONArray arrServices;
        JSONArray arrPriceListServices;


        try {
            //get list of all services in database
            arrServices = new JSONArray(services);

            //get pricelist service for health facility and user
            arrPriceListServices = new JSONArray(getServicesPriceList());

            sqlHandler.ClearAll("tblServices");
            sqlHandler.ClearAll("tblSubServices");
            sqlHandler.ClearAll("tblSubItems");
            sqlHandler.ClearMapping("S");

            //insert service with healthFacility price
            for (int i = 0; i < arrServices.length(); i++){

                JSONObject objServices = arrServices.getJSONObject(i);
                
                //get price of service
                String priceService = getPriceService(objServices.getString("ServCode"),arrPriceListServices);

                if(priceService != null){

                    sqlHandler.InsertService(objServices.getString("ServiceID"),
                            objServices.getString("ServCode"),
                            objServices.getString("ServName"), "S",
                            priceService,
                            objServices.getString("ServPackageType"));

                    sqlHandler.InsertMapping(objServices.getString("ServCode"),
                            objServices.getString("ServName"), "S");

                    if (objServices.has("SubService")) {

                        JSONArray arrSubService = new JSONArray(objServices.getString("SubService"));

                        //Insert SubServices
                        JSONObject objSubServices;
                        for (int s = 0; s < arrSubService.length(); s++) {
                            objSubServices = arrSubService.getJSONObject(s);
                            sqlHandler.InsertSubServices(objSubServices.getString("ServiceId"),
                                    objSubServices.getString("ServiceLinked"),objSubServices.getString("qty"),objSubServices.getString("price"));
                        }
                    }

                    if (objServices.has("SubItems")) {

                        JSONArray arrSubItem = new JSONArray(objServices.getString("SubItems"));

                        //Insert SubItems
                        JSONObject objSubItems;
                        for (int t = 0; t < arrSubItem.length(); t++) {
                            objSubItems = arrSubItem.getJSONObject(t);
                            sqlHandler.InsertSubItems(objSubItems.getString("ItemID"),
                                    objSubItems.getString("ServiceID"), objSubItems.getString("qty"),objSubItems.getString("price"));
                        }

                    }
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getServicesPriceList(){

        final HttpResponse[] resp = {null};
        String content = null;
        JSONObject object1 = new JSONObject();
        String ServicePriceList = null;

        if (global.isNetworkAvailable()) {

            String functionName = "claim/getpaymentlists";
            try {
                object1.put("claim_administrator_code", global.getOfficerCode());
                HttpResponse response = toRestApi.postToRestApiToken(object1, functionName);
                resp[0] = response;
                HttpEntity respEntity = response.getEntity();
                if (respEntity != null) {
                    final String[] code = {null};
                    // EntityUtils to get the response content
                    try {
                        content = EntityUtils.toString(respEntity);
                        android.util.Log.e("priceListServices", content);

                        JSONObject objResponse = new JSONObject(content);
                        ServicePriceList = objResponse.getString("pricelist_services");

                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }

        return ServicePriceList;
    }

    public boolean downloadItems() {

        toRestApi = new ToRestApi();
        String functionItems = "GetListMainItemItems";

        //download items
        String items = toRestApi.getFromRestApiVersion(functionItems, "2");

        JSONArray arrItems;

        try {
            arrItems = new JSONArray(items);

            sqlHandler.ClearAll("tblItems");
            sqlHandler.ClearMapping("I");
            //Insert Services
            JSONObject objItems;

            for (int i = 0; i < arrItems.length(); i++) {
                objItems = arrItems.getJSONObject(i);
                sqlHandler.InsertItem(objItems.getString("ItemID"),
                        objItems.getString("ItemCode"),
                        objItems.getString("ItemName"),
                        objItems.getString("ItemType"),
                        objItems.getString("ItemPrice"));
                sqlHandler.InsertMapping(objItems.getString("ItemCode"),
                        objItems.getString("ItemName"), "I");
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean downloadDiagnoses() {

        toRestApi = new ToRestApi();
        String functionDiagonses = "claim/GetDiagnosesServicesItems";

        //download diagnoses
        final HttpResponse[] resp = {null};
        final String[] content = new String[1];

        JSONObject object = new JSONObject();
        HttpResponse response = toRestApi.postToRestApi(object, functionDiagonses);
        resp[0] = response;
        HttpEntity respEntity = response.getEntity();

        try {
            if (respEntity != null) {
                final String[] code = {null};
                // EntityUtils to get the response content
                content[0] = EntityUtils.toString(respEntity);
            }

            JSONObject ob = null;
            ob = new JSONObject(content[0]);
            String diagnoses = ob.getString("diagnoses");
            String services = ob.getString("services");
            String items = ob.getString("items");

            sqlHandler.ClearAll("tblReferences");

            JSONArray arrDiagnoses = null;
            JSONObject objDiagnoses = null;
            arrDiagnoses = new JSONArray(diagnoses);
            for (int i = 0; i < arrDiagnoses.length(); i++) {
                objDiagnoses = arrDiagnoses.getJSONObject(i);
                sqlHandler.InsertReferences(objDiagnoses.getString("code"), objDiagnoses.getString("name"), "D", "");
            }

            //Insert Services references
            JSONArray arrServices;
            JSONObject objServices;
            arrServices = new JSONArray(services);
            for (int i = 0; i < arrServices.length(); i++) {
                objServices = arrServices.getJSONObject(i);
                sqlHandler.InsertReferences(objServices.getString("code"), objServices.getString("name"), "S", objServices.getString("price"));
            }

            //Insert Items references
            JSONArray arrItems;
            JSONObject objItems;
            arrItems = new JSONArray(items);
            for (int i = 0; i < arrItems.length(); i++) {
                objItems = arrItems.getJSONObject(i);
                sqlHandler.InsertReferences(objItems.getString("code"), objItems.getString("name"), "I", objItems.getString("price"));
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean downloadControls(){
        toRestApi = new ToRestApi();

        String controls = null;
        String error_occurred = null;
        String error_message = null;

        String functionName = "claim/Controls";
        try {
            String content = toRestApi.getFromRestApi(functionName);

            JSONObject ob;

            ob = new JSONObject(content);
            error_occurred = ob.getString("error_occured");
            if (error_occurred.equals("false")) {
                controls = ob.getString("controls");
                sqlHandler.ClearAll("tblControls");
                //Insert Controls
                JSONArray arrControls;
                JSONObject objControls;
                arrControls = new JSONArray(controls);
                for (int i = 0; i < arrControls.length(); i++) {
                    objControls = arrControls.getJSONObject(i);
                    sqlHandler.InsertControls(objControls.getString("fieldName"), objControls.getString("adjustibility"));
                }
            }

            error_occurred = "false";
            if (error_occurred.equals("false")) {
                sqlHandler.ClearAll("tblControls");
                sqlHandler.InsertControls("ClaimAdministrator", "N");
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean downloadAdmins(){
        toRestApi = new ToRestApi();

        String admins;

        String functionName = "claim/GetClaimAdmins";

        try{

            String content = toRestApi.getFromRestApi(functionName);

            JSONObject ob;

            ob = new JSONObject(content);
            admins = ob.getString("claim_admins");
            sqlHandler.ClearAll("tblClaimAdmins");
            //Insert Admins
            JSONArray arrAdmins;
            JSONObject objAdmins;
            arrAdmins = new JSONArray(admins);
            for (int i = 0; i < arrAdmins.length(); i++) {
                objAdmins = arrAdmins.getJSONObject(i);
                String lastName = objAdmins.getString("lastName");
                String otherNames = objAdmins.getString("otherNames");
                String hfCode = objAdmins.getString("hfCode");
                String name = lastName + " " + otherNames;
                sqlHandler.InsertClaimAdmins(objAdmins.getString("claimAdminCode"), hfCode, name);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


}
