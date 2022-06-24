package org.openimis.imisclaims;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class SynchronizeActivity extends ImisActivity {
    private static final String LOG_TAG = "SYNCACTIVITY";
    private static final int PICK_FILE_REQUEST_CODE = 1;
    ArrayList<String> broadcastList;

    TextView tvUploadClaims, tvZipClaims;
    RelativeLayout uploadClaims, zipClaims, importMasterData, downloadMasterData;

    ProgressDialog pd;

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
                DownLoadInsureeNumbers(object1);
            } catch (JSONException | IOException e) {
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
                tvUploadClaims.setText(String.valueOf(intent.getIntExtra(SynchronizeService.EXTRA_CLAIM_COUNT_PENDING, 0)));
                tvZipClaims.setText(String.valueOf(intent.getIntExtra(SynchronizeService.EXTRA_CLAIM_COUNT_PENDING_XML, 0)));
                break;
            case SynchronizeService.ACTION_EXPORT_SUCCESS:
                showDialog(getResources().getString(R.string.ZipXMLCreated));
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
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri selectedFile = data.getData();
            pd = ProgressDialog.show(this, "", getResources().getString(R.string.Processing));
            MasterDataService.importMasterData(this, selectedFile);
        } else if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            showToast(R.string.importMasterDataCanceled);
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

    public void DownLoadInsureeNumbers(final JSONObject object) throws IOException {

        ToRestApi toRestApi = new ToRestApi();

        final String[] content = new String[1];
        final HttpResponse[] resp = {null};
        if (global.isNetworkAvailable()) {
            String progress_message = getResources().getString(R.string.InsuranceNumber);
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.Checking_For_Updates), progress_message);
            Thread thread = new Thread() {
                public void run() {
                    String insureeNumbers = null;
                    String error_occurred = null;
                    String error_message = null;

                    String functionName = "claim/GetInsureeNumbers";

                    try {
                        HttpResponse response = toRestApi.postToRestApi(object, functionName);
                        resp[0] = response;
                        HttpEntity respEntity = response.getEntity();
                        if (respEntity != null) {
                            final String[] code = {null};
                            // EntityUtils to get the response content

                            content[0] = EntityUtils.toString(respEntity);

                        }

                        JSONObject ob = null;
                        try {
                            ob = new JSONObject(content[0]);
                            if (String.valueOf(response.getStatusLine().getStatusCode()).equals("200")) {
                                insureeNumbers = ob.getString("insureeNumbers");

                                sqlHandler.ClearAll("tblInsureeNumbers");

                                //Insert InsureeNumbers
                                JSONArray arrInsureeNumbers = null;
                                JSONObject objInsureeNumbers = null;
                                arrInsureeNumbers = new JSONArray(insureeNumbers);
                                for (int i = 0; i < arrInsureeNumbers.length(); i++) {
                                    objInsureeNumbers = arrInsureeNumbers.getJSONObject(i);
                                    sqlHandler.InsertInsureeNumber(objInsureeNumbers.getString("number"), objInsureeNumbers.getString("statut"));
                                }

                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.installed_updates), Toast.LENGTH_LONG).show();
                                });

                            } else {
                                error_occurred = ob.getString("error_occured");
                                if (error_occurred.equals("true")) {
                                    error_message = ob.getString("error_message");

                                    final String finalError_message = error_message;
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(SynchronizeActivity.this, finalError_message, Toast.LENGTH_LONG).show();
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.SomethingWentWrongServer), Toast.LENGTH_LONG).show();
                                    });
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                            });
                            Toast.makeText(SynchronizeActivity.this, String.valueOf(e), Toast.LENGTH_LONG).show();

                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(SynchronizeActivity.this, resp[0].getStatusLine().getStatusCode() + "-" + getResources().getString(R.string.SomethingWentWrongServer), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            };

            thread.start();
        } else {
            runOnUiThread(() -> progressDialog.dismiss());
            ErrorDialogBox(getResources().getString(R.string.CheckInternet));
        }

    }

    public void ErrorDialogBox ( final String message){
        showDialog(message);
    }

}
