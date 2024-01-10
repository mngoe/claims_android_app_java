package org.openimis.imisclaims;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
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

import org.openimis.imisclaims.domain.entity.ClaimAdmin;
import org.openimis.imisclaims.domain.entity.Control;
import org.openimis.imisclaims.domain.entity.DiagnosesServicesMedications;
import org.openimis.imisclaims.domain.entity.Diagnosis;
import org.openimis.imisclaims.domain.entity.Medication;
import org.openimis.imisclaims.domain.entity.PaymentList;
import org.openimis.imisclaims.domain.entity.Program;
import org.openimis.imisclaims.domain.entity.Service;
import org.openimis.imisclaims.domain.entity.SubServiceItem;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.tools.StorageManager;
import org.openimis.imisclaims.usecase.FetchClaimAdmins;
import org.openimis.imisclaims.usecase.FetchControls;
import org.openimis.imisclaims.usecase.FetchDiagnosesServicesItems;
import org.openimis.imisclaims.usecase.FetchMedications;
import org.openimis.imisclaims.usecase.FetchPaymentList;
import org.openimis.imisclaims.usecase.FetchPrograms;
import org.openimis.imisclaims.usecase.FetchServices;
import org.openimis.imisclaims.util.StreamUtils;
import org.openimis.imisclaims.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
            DownloadMasterData();
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
                if(downloadControls()){
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        downloadAdmins();
                    });
                }
            });
            thread.start();
        } else {
            ErrorDialogBox(getResources().getString(R.string.CheckInternet));
        }
    }


    public void downloadServices() {
        if (global.isNetworkAvailable()) {
            String progress_message = getResources().getString(R.string.Services);
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.initializing), progress_message);
            Thread thread = new Thread(() ->{
                try {
                    List<Service> services = new FetchServices().execute();
                    if (services.size() != 0) {
                        //get list of all services in database

                        //get pricelist service for health facility and user
                        PaymentList paymentList = new FetchPaymentList().execute(global.getOfficerCode());
                        List<Service> servicesPricelist = paymentList.getServices();

                        sqlHandler.ClearAll("tblServices");
                        sqlHandler.ClearAll("tblSubServices");
                        sqlHandler.ClearAll("tblSubItems");


                        for (Service service: services) {
                            String priceService="";

                            //get service price from pricelist
                            for(Service serv : servicesPricelist){
                                if(serv.getCode().equals(service.getCode())){
                                    priceService = String.valueOf(serv.getPrice());
                                }
                            }

                            //insert service in database
                            if( priceService != "" ){
                                sqlHandler.InsertService(service.getId(),
                                        service.getCode(),
                                        service.getName(), "S",
                                        priceService,
                                        service.getPackageType(),
                                        service.getProgram());
                            }

                            //insert subservices
                            if (service.getSubServices().size() != 0) {
                                List<SubServiceItem> subservices = service.getSubServices();
                                for (SubServiceItem subService: subservices) {
                                    sqlHandler.InsertSubServices(subService.getId(),
                                            service.getId(),String.valueOf(subService.getQty()),subService.getPrice());
                                }
                            }

                            //insert subItems
                            if (service.getSubItems().size() != 0) {
                                List<SubServiceItem> subItems = service.getSubItems();
                                for (SubServiceItem subItem: subItems) {
                                    sqlHandler.InsertSubItems(subItem.getId(),
                                            service.getId(), String.valueOf(subItem.getQty()),subItem.getPrice());
                                }

                            }

                        }

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            downloadItems();
                        });
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.downloadFail), Toast.LENGTH_LONG).show();
                        });
                    }
                } catch ( Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }else{
            ErrorDialogBox(getResources().getString(R.string.CheckInternet));
        }
    }

    public void downloadPricelist(){
        if (global.isNetworkAvailable()) {
            String progress_message = getResources().getString(R.string.Services) + ", " + getResources().getString(R.string.Items) + "...";
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.mapping), progress_message);
            Thread thread = new Thread() {
                public void run() {
                    try {
                        PaymentList paymentList = new FetchPaymentList().execute(global.getOfficerCode());
                        sqlHandler.ClearMapping("S");
                        sqlHandler.ClearMapping("I");

                        //Insert Services
                        for (Service service : paymentList.getServices()) {
                            sqlHandler.InsertMapping(service.getCode(), service.getName(), "S", service.getProgram());
                        }

                        //Insert Items
                        for (Medication medication : paymentList.getMedications()) {
                            sqlHandler.InsertMapping(medication.getCode(), medication.getName(), "I", medication.getProgram());
                        }
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.installed_updates), Toast.LENGTH_LONG).show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(SynchronizeActivity.this, e.getMessage() + "-" + getResources().getString(R.string.AccessDenied), Toast.LENGTH_LONG).show();
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

    public void downloadItems() {
        if (global.isNetworkAvailable()) {
            String progress_message = getResources().getString(R.string.Items);
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.initializing), progress_message);
            Thread thread = new Thread(() -> {

                try {
                    List<Medication> items = new FetchMedications().execute();
                    if (items.size() != 0) {

                        sqlHandler.ClearAll("tblItems");


                        for (Medication item : items) {

                            //insert item in database
                            sqlHandler.InsertItem(
                                    item.getId(),
                                    item.getCode(),
                                    item.getName(), "I",
                                    String.valueOf(item.getPrice()),
                                    item.getProgram());
                        }

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            downloadDiagnoses();
                        });
                    }else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.downloadFail), Toast.LENGTH_LONG).show();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> progressDialog.dismiss());
                }
            });
            thread.start();
        } else {
            ErrorDialogBox(getResources().getString(R.string.CheckInternet));
        }

    }

    public void downloadDiagnoses() {
        if (global.isNetworkAvailable()) {
            String progress_message = getResources().getString(R.string.Diagnoses) + ", " + getResources().getString(R.string.Services) + ", " + getResources().getString(R.string.Items) + "...";
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.Checking_For_Updates), progress_message);
            Thread thread = new Thread() {
                public void run() {
                    try {
                        DiagnosesServicesMedications diagnosesServicesMedications = new FetchDiagnosesServicesItems().execute();
                        ma.saveLastUpdateDate(diagnosesServicesMedications.getLastUpdated());
                        sqlHandler.ClearAll("tblReferences");
                        sqlHandler.ClearMapping("S");
                        sqlHandler.ClearMapping("I");
                        //Insert Diagnoses
                        for (Diagnosis diagnosis : diagnosesServicesMedications.getDiagnoses()) {
                            sqlHandler.InsertReferences(diagnosis.getCode(), diagnosis.getName(), "D", "");
                        }

                        //Insert Services
                        for (Service service : diagnosesServicesMedications.getServices()) {
                            sqlHandler.InsertReferences(service.getCode(), service.getName(), "S", String.valueOf(service.getPrice()));
                            sqlHandler.InsertMapping(service.getCode(), service.getName(), "S", service.getProgram());
                        }

                        //Insert Programs
                        List<Program> programs = new FetchPrograms().execute();
                        for (Program program : programs) {
                            sqlHandler.InsertPrograms(program.getIdProgram(), program.getCode(), program.getNameProgram());
                        }

                        //Insert Items
                        for (Medication medication : diagnosesServicesMedications.getMedications()) {
                            sqlHandler.InsertReferences(medication.getCode(), medication.getName(), "I", String.valueOf(medication.getPrice()));
                            sqlHandler.InsertMapping(medication.getCode(), medication.getName(), "I", medication.getProgram());
                        }

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            downloadPricelist();
                            Toast.makeText(SynchronizeActivity.this, getResources().getString(R.string.installed_updates), Toast.LENGTH_LONG).show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(SynchronizeActivity.this, e.getMessage() + "-" + getResources().getString(R.string.SomethingWentWrongServer), Toast.LENGTH_LONG).show();
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

    public boolean downloadControls(){
        if (global.isNetworkAvailable()) {
            String progress_message = getResources().getString(R.string.getControls);
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.initializing), progress_message);
            Thread thread = new Thread() {
                public void run() {
                    try {
                        List<Control> controls = new FetchControls().execute();
                        sqlHandler.ClearAll("tblControls");
                        for (Control control : controls) {
                            sqlHandler.InsertControls(control.getName(), control.getAdjustability());
                        }

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            ErrorDialogBox(e.getMessage());
                        });
                    }
                }
            };
            thread.start();
        } else {
            ErrorDialogBox(getResources().getString(R.string.CheckInternet));
            return false;
        }
        return true;
    }

    public void downloadAdmins(){
        if (global.isNetworkAvailable()) {
            String progress_message = getResources().getString(R.string.application);
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.initializing), progress_message);
            Thread thread = new Thread(() -> {
                try {
                    List<ClaimAdmin> claimAdmins = new FetchClaimAdmins().execute();
                    sqlHandler.ClearAll("tblClaimAdmins");
                    for (ClaimAdmin claimAdmin : claimAdmins) {
                        JSONArray programs = new JSONArray(claimAdmin.getPrograms());
                        sqlHandler.InsertClaimAdmins(
                                claimAdmin.getId(),
                                claimAdmin.getClaimAdminCode(),
                                claimAdmin.getHealthFacilityCode(),
                                claimAdmin.getDisplayName(),
                                claimAdmin.getHfId(),
                                programs.toString()
                        );
                    }

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        downloadServices();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> progressDialog.dismiss());
                }
            });
            thread.start();
        } else {
            ErrorDialogBox(getResources().getString(R.string.CheckInternet));
        }
    }


}
