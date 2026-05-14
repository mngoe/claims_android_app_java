package org.openimis.imisclaims;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.inputmethodservice.Keyboard;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.util.DateUtils;
import org.openimis.imisclaims.util.StringUtils;
import org.openimis.imisclaims.util.TextViewUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ClaimActivity extends ImisActivity {
    private static final String LOG_TAG = "CLAIM";
    private static final int REQUEST_SCAN_QR_CODE = 1;
    static final int StartDate_Dialog_ID = 0;
    static final int EndDate_Dialog_ID = 1;
    final Calendar cal = Calendar.getInstance();

    public static ArrayList<HashMap<String, String>> lvItemList;
    public static ArrayList<HashMap<String, String>> lvServiceList;
    private static final String EXTRA_CLAIM_DATA = "claim";
    private static final String EXTRA_CLAIM_UUID = "claimUUID";
    public static final String EXTRA_READONLY = "readonly";
    public static String claimProgram;

    public static Intent newIntent(@NonNull Context context, @NonNull Claim claim) {
        return new Intent(context, ClaimActivity.class).putExtra(EXTRA_CLAIM_DATA, claim);
    }

    public static Intent newIntent(@NonNull Context context, @NonNull String claimUUID, boolean readOnly) {
        return new Intent(context, ClaimActivity.class)
                .putExtra(EXTRA_CLAIM_UUID, claimUUID)
                .putExtra(EXTRA_READONLY, readOnly);
    }


    private int year, month, day;
    int TotalItemService;
    String prefixProgramCode = "";
    String prefixYear = "";
    String prefixHfCode = "";
    String claimPrefix = "";

    EditText etStartDate, etEndDate, etClaimCode, etHealthFacility, etInsureeNumber, etClaimAdmin, etGuaranteeNo, etClaimPrefix, etTestNumber;
    AutoCompleteTextView etDiagnosis, etDiagnosis1, etDiagnosis2, etDiagnosis3, etDiagnosis4, etProgram, etVisitType;
    TextView tvItemTotal, tvServiceTotal;
    Button btnPost, btnNew;
    RadioGroup rgVisitType, rgTdr;
    RadioButton rbEmergency, rbReferral, rbOther, rbPositive, rbNegative;
    ImageButton btnScan;
    LinearLayout llFagepFields;
    TextInputLayout ettClaimPrefix, ettGuaranteeNo, ettClaimCode;
    private volatile ValidationResult latestValidationResult;

    private static class LocalClaimCodeValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private LocalClaimCodeValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
    }

    private static class ValidationResult {
        private final boolean valid;
        private final View errorView;
        private final String errorMessage;
        private final LocalClaimCodeValidationResult localClaimCodeValidationResult;

        private ValidationResult(boolean valid, View errorView, String errorMessage, LocalClaimCodeValidationResult localClaimCodeValidationResult) {
            this.valid = valid;
            this.errorView = errorView;
            this.errorMessage = errorMessage;
            this.localClaimCodeValidationResult = localClaimCodeValidationResult;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim);
        actionBar.setTitle(getResources().getString(R.string.app_name_claim));

        if (!global.isNetworkAvailable()) {
            setTitle(getResources().getString(R.string.app_name_claims) + "-" + getResources().getString(R.string.OfflineMode));
            setTitleColor(getResources().getColor(R.color.Red));
        }

        lvItemList = new ArrayList<>();
        lvServiceList = new ArrayList<>();

        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etDiagnosis = findViewById(R.id.etDiagnosis);
        etProgram = findViewById(R.id.etProgram);
        btnNew = findViewById(R.id.btnNew);
        btnPost = findViewById(R.id.btnPost);
        btnScan = findViewById(R.id.btnScan);
        etHealthFacility = findViewById(R.id.etHealthFacility);
        etClaimAdmin = findViewById(R.id.etClaimAdmin);
        etGuaranteeNo = findViewById(R.id.etGuaranteeNo);
        etClaimCode = findViewById(R.id.etClaimCode);
        etInsureeNumber = findViewById(R.id.etCHFID);
        tvItemTotal = findViewById(R.id.tvItemTotal);
        tvServiceTotal = findViewById(R.id.tvServiceTotal);
        etDiagnosis1 = findViewById(R.id.etDiagnosis1);
        etDiagnosis2 = findViewById(R.id.etDiagnosis2);
        etDiagnosis3 = findViewById(R.id.etDiagnosis3);
        etDiagnosis4 = findViewById(R.id.etDiagnosis4);
        rgVisitType = findViewById(R.id.rgVisitType);
        rbEmergency = findViewById(R.id.rbEmergency);
        rbReferral = findViewById(R.id.rbReferral);
        rbOther = findViewById(R.id.rbOther);
        etClaimPrefix = findViewById(R.id.etClaimPrefix);
        etTestNumber = findViewById(R.id.etTestNumber);
        rgTdr = findViewById(R.id.rgTdr);
        rbPositive = findViewById(R.id.rbPositive);
        rbNegative = findViewById(R.id.rbNegative);
        llFagepFields = findViewById(R.id.llFagepField);
        etVisitType = findViewById(R.id.etVisitType);
        ettClaimPrefix = findViewById(R.id.ettClaimPrefix);
        ettGuaranteeNo = findViewById(R.id.ettGuaranteeNo);
        View claimCodeParent = (View) etClaimCode.getParent();
        if (claimCodeParent instanceof TextInputLayout) {
            ettClaimCode = (TextInputLayout) claimCodeParent;
        }

        String[] visitTypes = getResources().getStringArray(R.array.visitType);
        ArrayAdapter<String> visitTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, visitTypes);
        visitTypeAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        etVisitType.setAdapter(visitTypeAdapter);
        etVisitType.setThreshold(100);
        etVisitType.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String item = adapterView.getItemAtPosition(position).toString();
                switch(item){
                    case "Emergency":
                        etVisitType.setTag("E");
                        break;
                    case "Referral":
                        etVisitType.setTag("R");
                        break;
                    case "Other":
                        etVisitType.setTag("O");
                        break;
                    default:
                        etVisitType.setTag("");
                        break;
                }
            }
        });

        rgVisitType.setVisibility(View.GONE);
        ettGuaranteeNo.setVisibility(View.GONE);

        TextWatcher claimCodeUniquenessWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // no-op
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateLocalClaimCodeUniqueness();
            }
        };
        etClaimCode.addTextChangedListener(claimCodeUniquenessWatcher);
        etClaimPrefix.addTextChangedListener(claimCodeUniquenessWatcher);

        tvItemTotal.setText("0");
        tvServiceTotal.setText("0");

        List<String> filterPrograms = new ArrayList<>();
        try {
            String hfId = sqlHandler.getClaimAdminInfo(global.getOfficerCode(), "HFId");
            String hfPrograms = sqlHandler.getHealthFacilityPrograms(hfId);
            String userPrograms = sqlHandler.getClaimAdminInfo(global.getOfficerCode(),"Programs");
            JSONArray arrayHfPrograms = new JSONArray(hfPrograms);
            JSONArray arrayAdminPrograms = new JSONArray(userPrograms);
            for (int i = 0 ; i< arrayHfPrograms.length(); i++){
                for (int j = 0 ; j < arrayAdminPrograms.length() ; j++){
                    if(arrayHfPrograms.get(i).toString().equals(arrayAdminPrograms.get(j).toString())){
                        filterPrograms.add(arrayHfPrograms.get(i).toString());
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ProgramAdapter progam_Adapter = new ProgramAdapter(ClaimActivity.this, sqlHandler, filterPrograms);
        etProgram.setAdapter(progam_Adapter);
        etProgram.setThreshold(1);
        etProgram.setOnItemClickListener((parent, view,position, l) ->{
            if(position >= 0){
                Cursor cursor = (Cursor)parent.getItemAtPosition(position);
                claimProgram = cursor.getString(cursor.getColumnIndexOrThrow("Id"));
                etClaimCode.setText("");
                lvServiceList.clear();
                lvItemList.clear();
                tvItemTotal.setText("0");
                tvServiceTotal.setText("0");
                if(cursor.getString(cursor.getColumnIndexOrThrow("Code")).equals("PAL")){
                    llFagepFields.setVisibility(View.VISIBLE);
                }else{
                    llFagepFields.setVisibility(View.GONE);
                }
                if(etProgram.getText().toString().equals("Cheque Santé") || etProgram.getText().toString().equals("Chèque Santé")){
                    claimPrefix = "";
                    etClaimPrefix.setText(claimPrefix);
                    ettClaimPrefix.setHint(getResources().getString(R.string.ChequeNumber));
                    etClaimPrefix.setEnabled(true);
                }else{
                    prefixProgramCode = cursor.getString(cursor.getColumnIndexOrThrow("Code"));
                    claimPrefix = prefixHfCode + "." + prefixYear + "." + prefixProgramCode + ".";
                    etClaimPrefix.setText(claimPrefix);
                    ettClaimPrefix.setHint(getResources().getString(R.string.Prefix));
                    etClaimPrefix.setEnabled(false);
                }
            }
        });


        DiseaseAdapter adapter = new DiseaseAdapter(ClaimActivity.this, sqlHandler);
        etDiagnosis.setAdapter(adapter);
        etDiagnosis.setThreshold(1);
        etDiagnosis.setOnItemClickListener(adapter);

        etDiagnosis1.setAdapter(adapter);
        etDiagnosis1.setThreshold(1);
        etDiagnosis1.setOnItemClickListener(adapter);

        etDiagnosis2.setAdapter(adapter);
        etDiagnosis2.setThreshold(1);
        etDiagnosis2.setOnItemClickListener(adapter);

        etDiagnosis3.setAdapter(adapter);
        etDiagnosis3.setThreshold(1);
        etDiagnosis3.setOnItemClickListener(adapter);

        etDiagnosis4.setAdapter(adapter);
        etDiagnosis4.setThreshold(1);
        etDiagnosis4.setOnItemClickListener(adapter);

        etStartDate.setOnTouchListener((v, event) -> {
            showDialog(StartDate_Dialog_ID);
            return false;
        });

        etEndDate.setOnTouchListener((v, event) -> {
            showDialog(EndDate_Dialog_ID);
            return false;
        });

        findViewById(R.id.ivAddItem).setOnClickListener(v -> addItem());
        findViewById(R.id.ivAddService).setOnClickListener(v -> addService());

        btnScan.setOnClickListener(v -> {
            Intent scanIntent = new Intent(this, com.google.zxing.client.android.CaptureActivity.class);
            scanIntent.setAction("com.google.zxing.client.android.SCAN");
            scanIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(scanIntent, REQUEST_SCAN_QR_CODE);
        });

        btnPost.setOnClickListener(v -> {
            progressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.Processing));
            runOnNewThread(
                    () -> {
                        ValidationResult validationResult = validateDataForSubmission();
                        latestValidationResult = validationResult;
                        if (!validationResult.valid) {
                            return false;
                        }
                        return saveClaim();
                    },
                    () -> runOnUiThread(() -> {
                        ClearForm();
                        progressDialog.dismiss();
                        showDialog(getResources().getString(R.string.ClaimPosted), ((dialog, which) -> {
                            Intent intent = getIntent();
                            if (intent.hasExtra(EXTRA_CLAIM_UUID)) {
                                finish();
                            }
                        }));
                    }),
                    () -> runOnUiThread(() -> {
                        if (latestValidationResult != null) {
                            renderValidationResult(latestValidationResult);
                        }
                        progressDialog.dismiss();
                    }),
                    500
            );
        });

        if (sqlHandler.getAdjustability("GuaranteeNo").equals("N")) {
            etGuaranteeNo.setVisibility(View.GONE);
        }
        if (sqlHandler.getAdjustability("ClaimAdministrator").equals("N")) {
            etClaimAdmin.setVisibility(View.GONE);
        }

        // hfCode and adminCode not editable
        disableView(etHealthFacility);
        disableView(etClaimAdmin);
        //etClaimPrefix.setEnabled(false);

        //hide fields
        etDiagnosis1.setVisibility(View.GONE);
        etDiagnosis2.setVisibility(View.GONE);
        etDiagnosis3.setVisibility(View.GONE);
        etDiagnosis4.setVisibility(View.GONE);
        llFagepFields.setVisibility(View.GONE);

        Intent intent = getIntent();

        if (intent.hasExtra(EXTRA_CLAIM_DATA)) {
            fillClaimFromRestore(intent.getParcelableExtra(EXTRA_CLAIM_DATA));
            btnNew.setVisibility(View.INVISIBLE);
        } else if (intent.hasExtra(EXTRA_CLAIM_UUID)) {
            fillClaimFromDatabase(intent.getStringExtra(EXTRA_CLAIM_UUID));

            if (isIntentReadonly()) {
                disableForm();
                btnNew.setText(R.string.ArchiveClaim);
                btnNew.setOnClickListener(v -> confirmArchive());
            } else {
                btnNew.setText(R.string.DeleteClaim);
                btnNew.setOnClickListener(v -> confirmDelete());
                //etClaimCode.setEnabled(false);
                if(etProgram.getText().toString().equals("Cheque Santé") || etProgram.getText().toString().equals("Chèque Santé")){
                    etClaimPrefix.setEnabled(true);
                }
                etProgram.setEnabled(false);
            }
            //etClaimPrefix.setVisibility(View.GONE);
        } else {
            if (global.getOfficerCode() != null) {
                etClaimAdmin.setText(global.getOfficerCode());
                etHealthFacility.setText(global.getOfficerHealthFacility());
                prefixHfCode = global.getOfficerHealthFacility();
                claimPrefix = prefixHfCode + "." + prefixYear + "." + prefixProgramCode + ".";
                etClaimPrefix.setText(claimPrefix);
            }
            btnNew.setOnClickListener(v -> {
                if (TotalItemService > 0) {
                    confirmNewDialog(getResources().getString(R.string.ConfirmDiscard));
                } else {
                    ClearForm();
                }
            });
        }
    }

    private boolean isIntentReadonly() {
        Intent intent = getIntent();
        return intent.getBooleanExtra(EXTRA_READONLY, false);
    }

    private void confirmDelete() {
        showDialog(getResources().getString(R.string.ConfirmDeleteClaim), (dialog, which) -> {
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.Processing), getResources().getString(R.string.DeleteClaim));
            Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_CLAIM_UUID)) {
                runOnNewThread(() -> sqlHandler.deleteClaim(intent.getStringExtra(EXTRA_CLAIM_UUID)), () -> {
                    progressDialog.dismiss();
                    runOnUiThread(this::finish);
                }, 500);
            } else {
                Log.e(LOG_TAG, "Delete claim invoked, but no claim UUID");
            }
        });
    }

    private void confirmArchive() {
        showDialog(getResources().getString(R.string.ConfirmArchiveClaim), (dialog, which) -> {
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.Processing), getResources().getString(R.string.ArchiveClaim));
            Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_CLAIM_UUID)) {
                runOnNewThread(() -> {
                    String date = AppInformation.DateTimeInfo.getDefaultIsoDatetimeFormatter().format(new Date());
                    sqlHandler.insertClaimUploadStatus(intent.getStringExtra(EXTRA_CLAIM_UUID), date, SQLHandler.CLAIM_UPLOAD_STATUS_ARCHIVED, null);
                }, () -> {
                    progressDialog.dismiss();
                    runOnUiThread(this::finish);
                }, 500);
            } else {
                Log.e(LOG_TAG, "Archive claim invoked, but no claim UUID");
            }
        });
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mif = getMenuInflater();
        mif.inflate(R.menu.menu, menu);
        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;
            //noinspection RestrictedApi
            m.setOptionalIconsVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuAddItems:
                addItem();
                return true;
            case R.id.mnuAddServices:
                addService();
                return true;
            default:
                onBackPressed();
                return true;
        }
    }

    private void addItem() {
        Intent addItemsIntent = new Intent(ClaimActivity.this, AddItems.class);
        addItemsIntent.putExtra(EXTRA_READONLY, isIntentReadonly());
        ClaimActivity.this.startActivity(addItemsIntent);
    }

    private  void addService() {
        Intent addServicesIntent = new Intent(this, AddServices.class);
        addServicesIntent.putExtra(EXTRA_READONLY, isIntentReadonly());
        ClaimActivity.this.startActivity(addServicesIntent);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

            case StartDate_Dialog_ID:

                year = cal.get(Calendar.YEAR);
                month = cal.get(Calendar.MONTH);
                day = cal.get(Calendar.DAY_OF_MONTH);

                return new DatePickerDialog(this, StartdatePickerListener, year, month, day);

            case EndDate_Dialog_ID:
                year = cal.get(Calendar.YEAR);
                month = cal.get(Calendar.MONTH);
                day = cal.get(Calendar.DAY_OF_MONTH);

                return new DatePickerDialog(this, EndDatePickerListner, year, month, day);
        }
        return null;
    }

    private final DatePickerDialog.OnDateSetListener StartdatePickerListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int Selectedyear, int SelectedMonth, int SelectedDay) {
            year = Selectedyear;
            month = SelectedMonth;
            day = SelectedDay;
            Date date = new Date(year - 1900, month, day);
            TextViewUtils.setDate(etStartDate, date);

            if (etEndDate.getText().length() == 0) {
                etEndDate.setText(etStartDate.getText().toString());
                if(!etProgram.getText().toString().equals("Cheque Santé") && !etProgram.getText().toString().equals("Chèque Santé")){
                    prefixYear = String.valueOf(year);
                    claimPrefix = prefixHfCode + "." + prefixYear + "." + prefixProgramCode + ".";
                    etClaimPrefix.setText(claimPrefix);
                }
            }
        }
    };

    private final DatePickerDialog.OnDateSetListener EndDatePickerListner = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int SelectedYear, int SelectedMonth, int SelectedDay) {
            year = SelectedYear;
            month = SelectedMonth;
            day = SelectedDay;
            Date date = new Date(year - 1900, month, day);
            TextViewUtils.setDate(etEndDate, date);
            if(!etProgram.getText().toString().equals("Cheque Santé") && !etProgram.getText().toString().equals("Chèque Santé")){
                prefixYear = String.valueOf(year);
                claimPrefix = prefixHfCode + "." + prefixYear + "." + prefixProgramCode + ".";
                etClaimPrefix.setText(claimPrefix);
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();

        int TotalItem = getTotalItem();
        int TotalService = getTotalService();
        TotalItemService = TotalItem + TotalService;

        tvItemTotal.setText(String.valueOf(TotalItem));
        tvServiceTotal.setText(String.valueOf(TotalService));
    }

    private void ClearForm() {
        etClaimCode.setText("");
        etGuaranteeNo.setText("");
        etInsureeNumber.setText("");
        etStartDate.setText("");
        etEndDate.setText("");
        etDiagnosis.setText("");
        etProgram.setText("");
        lvItemList.clear();
        lvServiceList.clear();
        tvItemTotal.setText("0");
        tvServiceTotal.setText("0");
        TotalItemService = 0;
        etDiagnosis1.setText("");
        etDiagnosis2.setText("");
        etDiagnosis3.setText("");
        etDiagnosis4.setText("");
        rgVisitType.clearCheck();
        etVisitType.setText("");
        etInsureeNumber.requestFocus();
        etClaimPrefix.setText("");
        etTestNumber.setText("");
        rgTdr.clearCheck();
        llFagepFields.setVisibility(View.GONE);
    }

    private void disableForm() {
        disableView(etClaimCode);
        disableView(etGuaranteeNo);
        disableView(etInsureeNumber);
        disableView(etStartDate);
        disableView(etEndDate);
        disableView(etProgram);
        disableView(etDiagnosis);
        disableView(etDiagnosis1);
        disableView(etDiagnosis2);
        disableView(etDiagnosis3);
        disableView(etDiagnosis4);
        disableView(rgVisitType);
        disableView(etClaimCode);
        disableView(btnPost);
        disableView(rbEmergency);
        disableView(rbReferral);
        disableView(rbOther);
        disableView(etClaimPrefix);
        disableView(etVisitType);
    }

    private void fillClaimFromRestore(Claim claim) {
        String newClaimNumber = getResources().getString(R.string.restoredClaimNoPrefix) + claim.getClaimNumber();
        etClaimCode.setText(newClaimNumber);

        if (etClaimAdmin.getVisibility() != View.GONE) {
            etClaimAdmin.setText(global.getOfficerCode());
        }
        etHealthFacility.setText(global.getOfficerHealthFacility());

        if (etGuaranteeNo.getVisibility() != View.GONE) {
            String guaranteeNumber = claim.getGuaranteeNumber();
            if ("".equals(guaranteeNumber) || "null".equals(guaranteeNumber))
                etGuaranteeNo.setText("");
            else etGuaranteeNo.setText(guaranteeNumber);
        }

        etInsureeNumber.setText(claim.getInsuranceNumber());
        if (Claim.Status.REJECTED != claim.getStatus()) {
            etInsureeNumber.setText("");
        }

        TextViewUtils.setDate(etStartDate, claim.getVisitDateFrom());
        TextViewUtils.setDate(etEndDate, claim.getVisitDateTo());

        etDiagnosis.setText(sqlHandler.getDiseaseCode(claim.getMainDg()));
        etDiagnosis1.setText(sqlHandler.getDiseaseCode(claim.getSecDg1()));
        etDiagnosis2.setText(sqlHandler.getDiseaseCode(claim.getSecDg2()));
        etDiagnosis3.setText(sqlHandler.getDiseaseCode(claim.getSecDg3()));
        etDiagnosis4.setText(sqlHandler.getDiseaseCode(claim.getSecDg4()));
        etProgram.setText(sqlHandler.getProgamName(claim.getClaimProgram()));

        switch (claim.getVisitType() != null ? claim.getVisitType() : "") {
            case "E":
                etVisitType.setText("Emergency");
                break;
            case "R":
                etVisitType.setText("Referral");
                break;
            case "O":
                etVisitType.setText("Other");
                break;
            default:
                etVisitType.setText("");
        }

        lvItemList.clear();
        for (Claim.Medication medication : claim.getMedications()) {
            HashMap<String, String> item = new HashMap<>();
            item.put("Name", medication.getName());
            item.put("Code", medication.getCode());
            item.put("Price", String.valueOf(medication.getPrice()));
            item.put("Quantity", medication.getQuantity());
            lvItemList.add(item);
        }

        tvItemTotal.setText(String.valueOf(lvItemList.size()));

        lvServiceList.clear();
        for (Claim.Service service : claim.getServices()) {
            HashMap<String, String> item = new HashMap<>();
            item.put("Name", service.getName());
            item.put("Code", service.getCode());
            item.put("Price", String.valueOf(service.getPrice()));
            item.put("Quantity", service.getQuantity());
            item.put("PackageType", service.getPackageType());
            lvServiceList.add(item);
        }
        tvServiceTotal.setText(String.valueOf(lvServiceList.size()));

        TotalItemService = lvItemList.size() + lvServiceList.size();

        etInsureeNumber.requestFocus();

    }

    private void fillClaimFromDatabase(String claimUUID) {
        new Thread(() -> {
            JSONObject claimObject = sqlHandler.getClaim(claimUUID);
            if (claimObject == null) {
                showDialog(getResources().getString(R.string.ClaimNotFound), (dialog, which) -> finish());
            } else {
                runOnUiThread(() -> {
                    try {
                        JSONObject claimDetails = claimObject.getJSONObject("details");

                        //etClaimCode.setText(claimDetails.getString("ClaimCode"));
                        if (etClaimAdmin.getVisibility() != View.GONE) {
                            etClaimAdmin.setText(claimDetails.getString("ClaimAdmin"));
                        }
                        etHealthFacility.setText(claimDetails.getString("HFCode"));

                        if (etGuaranteeNo.getVisibility() != View.GONE) {
                            etGuaranteeNo.setText(claimDetails.getString("GuaranteeNumber"));
                        }

                        etInsureeNumber.setText(claimDetails.getString("InsureeNumber"));
                        etStartDate.setText(claimDetails.getString("StartDate"));
                        etEndDate.setText(claimDetails.getString("EndDate"));

                        etDiagnosis.setText(claimDetails.getString("ICDCode"));
                        etDiagnosis1.setText(claimDetails.getString("ICDCode1"));
                        etDiagnosis2.setText(claimDetails.getString("ICDCode2"));
                        etDiagnosis3.setText(claimDetails.getString("ICDCode3"));
                        etDiagnosis4.setText(claimDetails.getString("ICDCode4"));
                        etProgram.setText(claimDetails.getString("Program"));
                        etClaimPrefix.setText(claimDetails.getString("ClaimPrefix"));

                        if(claimDetails.getString("Program").equals("Cheque Santé") || claimDetails.getString("Program").equals("Chèque Santé")){
                            etClaimCode.setText(claimDetails.getString("ClaimCode").split(claimDetails.getString("ClaimPrefix"))[1]);
                        }else {
                            etClaimCode.setText(claimDetails.getString("ClaimCode").split(claimDetails.getString("ClaimPrefix"))[1]);
                            disableView(etClaimPrefix);
                        }

                        if (sqlHandler.getProgamCode(claimDetails.getString("Program")).equals("PAL")){
                            llFagepFields.setVisibility(View.VISIBLE);
                            etTestNumber.setText(claimDetails.getString("TestNumber"));
                            switch (claimDetails.getString("Tdr")){
                                case "true":
                                    rgTdr.check(R.id.rbPositive);
                                    break;
                                case "false":
                                    rgTdr.check(R.id.rbNegative);
                                    break;
                                default:
                                    rgTdr.clearCheck();
                            }
                        }

                        switch (claimDetails.getString("VisitType")) {
                            case "E":
                                etVisitType.setText(getResources().getString(R.string.Emergency));
                                etVisitType.setTag("E");
                                break;
                            case "R":
                                etVisitType.setText(getResources().getString(R.string.Referral));
                                etVisitType.setTag("R");
                                break;
                            case "O":
                                etVisitType.setText(getResources().getString(R.string.Other));
                                etVisitType.setTag("O");
                                break;
                            default:
                                etVisitType.setText("");
                                etVisitType.setTag("");
                        }

                        lvItemList.clear();
                        if (claimObject.has("items")) {
                            JSONArray items = claimObject.getJSONArray("items");
                            for (int i = 0; i < items.length(); i++) {
                                HashMap<String, String> item = new HashMap<>();
                                JSONObject itemJson = items.getJSONObject(i);

                                item.put("Name", sqlHandler.getReferenceName(itemJson.getString("ItemCode")));
                                item.put("Code", itemJson.getString("ItemCode"));
                                item.put("Price", itemJson.getString("ItemPrice"));
                                item.put("Quantity", itemJson.getString("ItemQuantity"));

                                lvItemList.add(item);
                            }
                        }
                        tvItemTotal.setText(String.valueOf(lvItemList.size()));

                        lvServiceList.clear();
                        if (claimObject.has("services")) {
                            JSONArray services = claimObject.getJSONArray("services");
                            for (int i = 0; i < services.length(); i++) {
                                HashMap<String, String> service = new HashMap<>();
                                JSONObject serviceJson = services.getJSONObject(i);

                                service.put("Name", sqlHandler.getReferenceName(serviceJson.getString("ServiceCode")));
                                service.put("Code", serviceJson.getString("ServiceCode"));
                                service.put("Price", serviceJson.getString("ServicePrice"));
                                service.put("Quantity", serviceJson.getString("ServiceQuantity"));
                                service.put("PackageType", serviceJson.getString("ServicePackageType"));
                                if(!serviceJson.getString("ServicePackageType").equals("S")){
                                    service.put("SubServicesItems", serviceJson.getString("SubServicesItems"));
                                }

                                lvServiceList.add(service);
                            }
                        }
                        tvServiceTotal.setText(String.valueOf(lvServiceList.size()));

                        TotalItemService = lvItemList.size() + lvServiceList.size();
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, String.format("Error while parsing claim (%s)", claimUUID));
                    }
                });
            }
        }).start();
    }

    private int getTotalItem() {
        return lvItemList.size();
    }

    private int getTotalService() {
        return lvServiceList.size();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SCAN_QR_CODE:
                if (resultCode == RESULT_OK) {
                    String CHFID = data.getStringExtra("SCAN_RESULT");
                    etInsureeNumber.setText(CHFID);
                }
                break;
        }
    }

    private ValidationResult validateDataForSubmission() {
        LocalClaimCodeValidationResult localClaimCodeValidationResult = checkLocalClaimCodeUniqueness();

        if (etHealthFacility.getText().length() == 0) {
            return invalidValidationResult(etHealthFacility, getResources().getString(R.string.MissingHealthFacility), localClaimCodeValidationResult);
        }

        if (sqlHandler.getAdjustability("ClaimAdministrator").equals("M") && etClaimAdmin.getText().length() == 0) {
            return invalidValidationResult(etClaimAdmin, getResources().getString(R.string.MissingClaimAdmin), localClaimCodeValidationResult);
        }

        if (etClaimCode.getText().length() == 0) {
            return invalidValidationResult(etClaimCode, getResources().getString(R.string.MissingClaimCode), localClaimCodeValidationResult);
        }

        if (etInsureeNumber.getText().length() == 0) {
            return invalidValidationResult(etInsureeNumber, getResources().getString(R.string.MissingCHFID), localClaimCodeValidationResult);
        }

        /*if (!etProgram.getText().toString().equals("VIH")) {
            boolean isNumeric = StringUtils.isNumeric(etInsureeNumber.getText().toString());
            if(!isNumeric){
                showValidationDialog(etInsureeNumber, getResources().getString(R.string.MissingLengthCHFID));
                return false;
            }
        }*/

        if (!isValidInsureeNumber()) {
            return invalidValidationResult(etInsureeNumber, getResources().getString(R.string.InvalidCHFID), localClaimCodeValidationResult);
        }

        if (etStartDate.getText().length() == 0) {
            return invalidValidationResult(etStartDate, getResources().getString(R.string.MissingStartDate), localClaimCodeValidationResult);
        }

        if (etEndDate.getText().length() == 0) {
            return invalidValidationResult(etEndDate, getResources().getString(R.string.MissingEndDate), localClaimCodeValidationResult);
        }

        try {
            String StartDate = etStartDate.getText().toString();
            String EndDate = etEndDate.getText().toString();

            Date Current_date = new Date();
            Date Start_date = DateUtils.dateFromString(StartDate);
            Date End_date = DateUtils.dateFromString(EndDate);

            if (End_date.after(Current_date)) {
                return invalidValidationResult(etEndDate, getResources().getString(R.string.AfterCurrentDate), localClaimCodeValidationResult);
            }

            if (Start_date.after(End_date)) {
                return invalidValidationResult(etEndDate, getResources().getString(R.string.BiggerDate), localClaimCodeValidationResult);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while parsing dates", e);
        }

        if (etDiagnosis.getText().length() == 0) {
            return invalidValidationResult(etDiagnosis, getResources().getString(R.string.MissingDisease), localClaimCodeValidationResult);
        }

        if (etProgram.getText().length() == 0) {
            return invalidValidationResult(etProgram, getResources().getString(R.string.MissingProgram), localClaimCodeValidationResult);
        }

        if(etClaimPrefix.getText().length() == 0){
            return invalidValidationResult(etClaimPrefix, getResources().getString(R.string.MissingChequeNumber), localClaimCodeValidationResult);
        }

        if(etClaimCode.getText().length() > 7){
            return invalidValidationResult(etClaimPrefix, getResources().getString(R.string.InvalidClaimCode), localClaimCodeValidationResult);
        }

        if (!localClaimCodeValidationResult.valid) {
            return invalidValidationResult(etClaimCode, localClaimCodeValidationResult.errorMessage, localClaimCodeValidationResult);
        }

//        if (rgVisitType.getCheckedRadioButtonId() == -1) {
//            showValidationDialog(rgVisitType, getResources().getString(R.string.MissingVisitType));
//            return false;
//        }

        if(etVisitType.getText().toString().isEmpty()){
            return invalidValidationResult(rgVisitType, getResources().getString(R.string.MissingVisitType), localClaimCodeValidationResult);
        }

        if (Float.parseFloat(tvItemTotal.getText().toString()) + Float.parseFloat(tvServiceTotal.getText().toString()) == 0) {
            return invalidValidationResult(tvItemTotal, getResources().getString(R.string.MissingClaim), localClaimCodeValidationResult);
        }

        if(prefixProgramCode.equals("PAL")){
            if(etTestNumber.getText().length() == 0){
                return invalidValidationResult(etClaimPrefix, getResources().getString(R.string.MissingTestNumber), localClaimCodeValidationResult);
            }
            if(rgTdr.getCheckedRadioButtonId() == -1){
                return invalidValidationResult(etClaimPrefix, getResources().getString(R.string.MissingTdr), localClaimCodeValidationResult);
            }
        }
        return new ValidationResult(true, null, null, localClaimCodeValidationResult);
    }

    private boolean isValidInsureeNumber() {
        Escape escape = new Escape();
        return escape.CheckCHFID(etInsureeNumber.getText().toString());
    }

    protected void showValidationDialog(View view, String msg) {
        runOnUiThread(() -> showDialog(msg, (dialog, which) -> {
            if (view instanceof EditText) {
                EditText editText = (EditText) view;
                editText.requestFocus();
            }
        }));
    }

    protected void confirmNewDialog(String msg) {
        runOnUiThread(() -> showDialog(msg, (dialog, which) -> ClearForm(), (dialog, which) -> dialog.dismiss()));
    }

    private boolean saveClaim() {
        Intent intent = getIntent();
        String claimUUID;
        if (intent.hasExtra(EXTRA_CLAIM_UUID)) {
            claimUUID = intent.getStringExtra(EXTRA_CLAIM_UUID);
        } else {
            claimUUID = UUID.randomUUID().toString();
        }

        String claimDate = DateUtils.toDateString(new Date());

        //int SelectedId;
        //SelectedId = rgVisitType.getCheckedRadioButtonId();
        //RadioButton selectedTypeButton;
        //selectedTypeButton = findViewById(SelectedId);

        String tdr = "";
        String testNumber = "";
        if (prefixProgramCode.equals("PAL")) {
            int SelectedTdrId;
            SelectedTdrId = rgTdr.getCheckedRadioButtonId();
            RadioButton selectedTgrButton;
            selectedTgrButton = findViewById(SelectedTdrId);
            tdr = selectedTgrButton.getTag().toString();
            testNumber = etTestNumber.getText().toString();
        }

        ContentValues claimCV = new ContentValues();

        claimCV.put("ClaimUUID", claimUUID);
        claimCV.put("ClaimDate", claimDate);
        claimCV.put("HFCode", etHealthFacility.getText().toString());
        claimCV.put("ClaimAdmin", etClaimAdmin.getText().toString());
        claimCV.put("ClaimCode", etClaimPrefix.getText().toString() + etClaimCode.getText().toString());
        claimCV.put("GuaranteeNumber", etGuaranteeNo.getText().toString());
        claimCV.put("InsureeNumber", etInsureeNumber.getText().toString());
        claimCV.put("StartDate", etStartDate.getText().toString());
        claimCV.put("EndDate", etEndDate.getText().toString());
        claimCV.put("Program", etProgram.getText().toString());
        claimCV.put("ICDCode", etDiagnosis.getText().toString());
        claimCV.put("Comment", "");
        claimCV.put("Total", "");
        claimCV.put("ICDCode1", etDiagnosis1.getText().toString());
        claimCV.put("ICDCode2", etDiagnosis2.getText().toString());
        claimCV.put("ICDCode3", etDiagnosis3.getText().toString());
        claimCV.put("ICDCode4", etDiagnosis4.getText().toString());
        claimCV.put("VisitType", etVisitType.getTag().toString());
        claimCV.put("TestNumber", testNumber);
        claimCV.put("Tdr", tdr);
        claimCV.put("ClaimPrefix", etClaimPrefix.getText().toString());

        ArrayList<ContentValues> claimItemCVs = new ArrayList<>(lvItemList.size());
        for (int i = 0; i < lvItemList.size(); i++) {
            ContentValues claimItemCV = new ContentValues();

            claimItemCV.put("ClaimUUID", claimUUID);
            claimItemCV.put("ItemCode", lvItemList.get(i).get("Code"));
            claimItemCV.put("ItemPrice", lvItemList.get(i).get("Price"));
            claimItemCV.put("ItemQuantity", lvItemList.get(i).get("Quantity"));

            claimItemCVs.add(claimItemCV);
        }

        ArrayList<ContentValues> claimServiceCVs = new ArrayList<>(lvServiceList.size());
        for (int i = 0; i < lvServiceList.size(); i++) {
            ContentValues claimServiceCV = new ContentValues();

            claimServiceCV.put("ClaimUUID", claimUUID);
            claimServiceCV.put("ServiceCode", lvServiceList.get(i).get("Code"));
            claimServiceCV.put("ServicePrice", lvServiceList.get(i).get("Price"));
            claimServiceCV.put("ServiceQuantity", lvServiceList.get(i).get("Quantity"));
            claimServiceCV.put("ServicePackageType",lvServiceList.get(i).get("PackageType"));
            if (!lvServiceList.get(i).get("PackageType").equals("S")) {
                claimServiceCV.put("SubServicesItems", lvServiceList.get(i).get("SubServicesItems"));
            }

            claimServiceCVs.add(claimServiceCV);
        }
        sqlHandler.saveClaim(claimCV, claimItemCVs, claimServiceCVs);
        return true;
    }

    private LocalClaimCodeValidationResult checkLocalClaimCodeUniqueness() {
        if (getIntent().hasExtra(EXTRA_CLAIM_UUID)) {
            return new LocalClaimCodeValidationResult(true, null);
        }
        String finalCode = etClaimPrefix.getText().toString() + etClaimCode.getText().toString();
        if (finalCode.trim().isEmpty()) {
            return new LocalClaimCodeValidationResult(true, null);
        }

        boolean exists = sqlHandler.existsClaimCode(finalCode);
        if (exists) {
            String errorMessage = getResources().getString(R.string.ClaimNumberExist);
            return new LocalClaimCodeValidationResult(false, errorMessage);
        }
        return new LocalClaimCodeValidationResult(true, null);
    }

    private void renderLocalClaimCodeError(@NonNull LocalClaimCodeValidationResult result) {
        if (ettClaimCode != null) {
            if (result.valid) {
                ettClaimCode.setError(null);
                ettClaimCode.setErrorEnabled(false);
            } else {
                ettClaimCode.setErrorEnabled(true);
                ettClaimCode.setError(result.errorMessage);
            }
            return;
        }
        if (result.valid) {
            etClaimCode.setError(null);
        } else {
            etClaimCode.setError(result.errorMessage);
        }
    }

    private boolean validateLocalClaimCodeUniqueness() {
        LocalClaimCodeValidationResult result = checkLocalClaimCodeUniqueness();
        renderLocalClaimCodeError(result);
        return result.valid;
    }

    private ValidationResult invalidValidationResult(@NonNull View errorView, @NonNull String errorMessage, @NonNull LocalClaimCodeValidationResult localClaimCodeValidationResult) {
        return new ValidationResult(false, errorView, errorMessage, localClaimCodeValidationResult);
    }

    private void renderValidationResult(@NonNull ValidationResult result) {
        renderLocalClaimCodeError(result.localClaimCodeValidationResult);
        if (result.valid) {
            return;
        }
        showDialog(result.errorMessage, (dialog, which) -> {
            if (result.errorView instanceof EditText) {
                EditText editText = (EditText) result.errorView;
                editText.requestFocus();
            }
        });
    }

}
