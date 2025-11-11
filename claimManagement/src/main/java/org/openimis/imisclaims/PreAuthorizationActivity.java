package org.openimis.imisclaims;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.domain.entity.Prescripteur;
import org.openimis.imisclaims.network.request.GetPrescriberGraphQLRequest;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PreAuthorizationActivity extends ImisActivity {
    private static final String LOG_TAG = "PREAUTH";
    private static final int REQUEST_SCAN_QR_CODE = 1;

    final Calendar cal = Calendar.getInstance();

    public static ArrayList<HashMap<String, String>> lvItemList;
    public static ArrayList<HashMap<String, String>> lvServiceList;
    private static final String EXTRA_CLAIM_DATA = "claim";
    private static final String EXTRA_CLAIM_UUID = "claimUUID";
    public static final String EXTRA_READONLY = "readonly";

    public static Intent newIntent(@NonNull Context context, @NonNull Claim claim) {
        return new Intent(context, PreAuthorizationActivity.class).putExtra(EXTRA_CLAIM_DATA, claim);
    }

    public static Intent newIntent(@NonNull Context context, @NonNull String claimUUID, boolean readOnly) {
        return new Intent(context, PreAuthorizationActivity.class)
                .putExtra(EXTRA_CLAIM_UUID, claimUUID)
                .putExtra(EXTRA_READONLY, readOnly);
    }

    private int year, month, day;
    int TotalItemService;

    EditText etClaimCode, etHealthFacility, etInsureeNumber, etClaimAdmin, etGuaranteeNo, etReason, etDatePreAuth, etReferralCode;
    AutoCompleteTextView etDiagnosis, etDiagnosis1, etDiagnosis2, etDiagnosis3, etDiagnosis4, etReferalHF, etPrescriber;
    // Adapter and selected uuid for prescriber autocomplete
    private ArrayAdapter<Prescripteur> prescriberAdapter;
    private String selectedPrescriberUuid;
    TextView tvItemTotal, tvServiceTotal;
    Button btnPost, btnNew;
    RadioGroup rgVisitType;
    RadioButton rbEmergency, rbReferral, rbOther, rbDiseased;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preauthorized);
        actionBar.setTitle("PreAuthorization");

        if (!global.isNetworkAvailable()) {
            setTitle(getResources().getString(R.string.app_name_claims) + "-" + getResources().getString(R.string.OfflineMode));
            setTitleColor(getResources().getColor(R.color.Red));
        }

        lvItemList = new ArrayList<>();
        lvServiceList = new ArrayList<>();

        etDiagnosis = findViewById(R.id.etDiagnosis);
        btnNew = findViewById(R.id.btnNew);
        btnPost = findViewById(R.id.btnPost);
        etHealthFacility = findViewById(R.id.etHealthFacility);
        etClaimAdmin = findViewById(R.id.etClaimAdmin);
        etGuaranteeNo = findViewById(R.id.etGuaranteeNo);
        etReason = findViewById(R.id.etReason);
        etDatePreAuth = findViewById(R.id.etDatePreAuth);

        etDatePreAuth.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    PreAuthorizationActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        int minute = calendar.get(Calendar.MINUTE);

                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                PreAuthorizationActivity.this,
                                (timeView, hourOfDay, minuteOfHour) -> {
                                    String datetime = String.format("%02d/%02d/%04d %02d:%02d",
                                            dayOfMonth, (month1 + 1), year1, hourOfDay, minuteOfHour);
                                    etDatePreAuth.setText(datetime);
                                },
                                hour, minute, true
                        );
                        timePickerDialog.show();
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

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
        etReferalHF = findViewById(R.id.etReferalHF);
        etReferralCode = findViewById(R.id.etReferralCode);
        etPrescriber = findViewById(R.id.etPrescriber);

        tvItemTotal.setText("0");
        tvServiceTotal.setText("0");

        DiseaseAdapter adapter = new DiseaseAdapter(PreAuthorizationActivity.this, sqlHandler);
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

        etDiagnosis2.setVisibility(View.GONE);
        etDiagnosis3.setVisibility(View.GONE);
        etDiagnosis4.setVisibility(View.GONE);

        HFAdapter hfAdapter = new HFAdapter(PreAuthorizationActivity.this, sqlHandler);
        etReferalHF.setAdapter(hfAdapter);
        etReferalHF.setThreshold(1);
        etReferalHF.setOnItemClickListener(hfAdapter);

        etReferalHF.setVisibility(View.GONE);
        etReferralCode.setVisibility(View.GONE);

        rgVisitType.setOnCheckedChangeListener((radioGroup, i) -> {
            if(radioGroup.getCheckedRadioButtonId() == R.id.rbReferral){
                etDatePreAuth.setVisibility(View.GONE);
                etReferalHF.setVisibility(View.VISIBLE);
                etReferralCode.setVisibility(View.VISIBLE);
            } else if (radioGroup.getCheckedRadioButtonId() == R.id.rbEmergency) {
                etDatePreAuth.setVisibility(View.VISIBLE);
                etReferalHF.setVisibility(View.GONE);
                etReferralCode.setVisibility(View.GONE);
            } else{
                etDatePreAuth.setVisibility(View.GONE);
                etReferalHF.setVisibility(View.GONE);
                etReferralCode.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.ivAddItem).setOnClickListener(v -> addItem());
        findViewById(R.id.ivAddService).setOnClickListener(v -> addService());

        btnPost.setOnClickListener(v -> {
            progressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.Processing));
            runOnNewThread(
                    () -> isValidData() && saveClaim(),
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
                    () -> progressDialog.dismiss(),
                    500
            );
        });

        if (sqlHandler.getAdjustability("GuaranteeNo").equals("N")) {
            etGuaranteeNo.setVisibility(View.GONE);
        }
        if (sqlHandler.getAdjustability("Reason").equals("N")) {
            etReason.setVisibility(View.GONE);
        }
        if (sqlHandler.getAdjustability("ClaimAdministrator").equals("N")) {
            etClaimAdmin.setVisibility(View.GONE);
        }

        // hfCode and adminCode not editable
        disableView(etHealthFacility);
        disableView(etClaimAdmin);

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
                btnNew.setText("DELETE PREAUTH");
                btnNew.setOnClickListener(v -> confirmDelete());
            }
        } else {
            if (global.getOfficerCode() != null) {
                etClaimAdmin.setText(global.getOfficerCode());
                etHealthFacility.setText(global.getOfficerHealthFacility());
                setupPrescribersAdapter(sqlHandler.getHfUuid(global.getOfficerHealthFacility()));
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

    private void setupPrescribersAdapter(String HFUuid) {
        new Thread(() -> {
            try {
                GetPrescriberGraphQLRequest req = new GetPrescriberGraphQLRequest();
                List<Prescripteur> prescribers = req.fetchPrescribers(HFUuid, "", 100);

                runOnUiThread(() -> {
                    try {
                        prescriberAdapter = new ArrayAdapter<>(PreAuthorizationActivity.this, android.R.layout.simple_dropdown_item_1line, prescribers);
                        etPrescriber.setAdapter(prescriberAdapter);
                        etPrescriber.setThreshold(1);

                        etPrescriber.setOnItemClickListener((parent, view, position, id) -> {
                            Prescripteur picked = prescriberAdapter.getItem(position);
                            if (picked != null) {
                                etPrescriber.setText(picked.getNin());
                                selectedPrescriberUuid = picked.getUuid();
                            }
                        });

                        etPrescriber.addTextChangedListener(new TextWatcher() {
                            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                                selectedPrescriberUuid = null;
                            }
                            @Override public void afterTextChanged(Editable s) {}
                        });

                        if (prescriberAdapter.getCount() > 0) {
                            prescriberAdapter.notifyDataSetChanged();
                            etPrescriber.showDropDown();
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error updating prescribers UI", e);
                    }
                });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error fetching prescribers", e);
            }
        }).start();
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
        }).setCancelable(true);
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
        Intent addItemsIntent = new Intent(PreAuthorizationActivity.this, AddItemsPreAuth.class);
        addItemsIntent.putExtra(EXTRA_READONLY, isIntentReadonly());
        Cursor c = sqlHandler.getMapping("I");
        if(c != null && c.getCount() == 0){
            showDialog(getResources().getString(R.string.NoItemsPricelist));
        }else {
            PreAuthorizationActivity.this.startActivity(addItemsIntent);
        }
    }

    private  void addService() {
        Intent addServicesIntent = new Intent(this, AddServices.class);
        addServicesIntent.putExtra(EXTRA_READONLY, isIntentReadonly());
        Cursor c = sqlHandler.getMapping("S");
        if(c != null && c.getCount() == 0){
            showDialog(getResources().getString(R.string.NoServicesPricelist));
        }else {
            PreAuthorizationActivity.this.startActivity(addServicesIntent);
        }
    }

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
        etReason.setText("");
        etDatePreAuth.setText("");
        etInsureeNumber.setText("");
        etDiagnosis.setText("");
        lvItemList.clear();
        lvServiceList.clear();
        tvItemTotal.setText("0");
        tvServiceTotal.setText("0");
        TotalItemService = 0;
        etDiagnosis1.setText("");
        etDiagnosis2.setText("");
        etDiagnosis3.setText("");
        etDiagnosis4.setText("");
        etReferalHF.setText("");
        etReferralCode.setText("");
        rgVisitType.clearCheck();
        etClaimCode.requestFocus();
    }

    private void disableForm() {
        disableView(etClaimCode);
        disableView(etGuaranteeNo);
        disableView(etReason);
        disableView(etDatePreAuth);
        disableView(etInsureeNumber);
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
        disableView(etReferralCode);
        disableView(etReferalHF);
        disableView(rbDiseased);
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

        etReason.setText(claim.getInsuranceNumber());

        etInsureeNumber.setText(claim.getInsuranceNumber());
        if (Claim.Status.REJECTED != claim.getStatus()) {
            etInsureeNumber.setText("");
        }

        etDiagnosis.setText(sqlHandler.getDiseaseCode(claim.getMainDg()));
        etDiagnosis1.setText(sqlHandler.getDiseaseCode(claim.getSecDg1()));
        etDiagnosis2.setText(sqlHandler.getDiseaseCode(claim.getSecDg2()));
        etDiagnosis3.setText(sqlHandler.getDiseaseCode(claim.getSecDg3()));
        etDiagnosis4.setText(sqlHandler.getDiseaseCode(claim.getSecDg4()));

        switch (claim.getVisitType() != null ? claim.getVisitType() : "") {
            case "Emergency":
                rgVisitType.check(R.id.rbEmergency);
                break;
            case "Referral":
                rgVisitType.check(R.id.rbReferral);
                break;
            case "Other":
                rgVisitType.check(R.id.rbOther);
                break;
            default:
                rgVisitType.clearCheck();
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
                // Dialog UI sur le thread principal
                runOnUiThread(() -> showDialog(
                        getResources().getString(R.string.ClaimNotFound),
                        (dialog, which) -> finish()
                ));
            } else {
                runOnUiThread(() -> {
                    try {
                        JSONObject claimDetails = claimObject.getJSONObject("details");

                        etClaimCode.setText(claimDetails.optString("ClaimPreAuthorizationCode", ""));
                        if (etClaimAdmin.getVisibility() != View.GONE) {
                            etClaimAdmin.setText(claimDetails.optString("ClaimAdmin", ""));
                        }
                        etHealthFacility.setText(claimDetails.optString("HFCode", ""));

                        if (etGuaranteeNo.getVisibility() != View.GONE) {
                            etGuaranteeNo.setText(claimDetails.optString("GuaranteeNumber", ""));
                        }

                        etReason.setText(claimDetails.optString("RejectionPreAuthorizationReason", ""));
                        etDatePreAuth.setText(claimDetails.optString("DatePreAuthorization", ""));
                        etInsureeNumber.setText(claimDetails.optString("InsureeNumber", ""));

                        etDiagnosis.setText(claimDetails.optString("ICDCode", ""));
                        etDiagnosis1.setText(claimDetails.optString("ICDCode1", ""));
                        etDiagnosis2.setText(claimDetails.optString("ICDCode2", ""));
                        etDiagnosis3.setText(claimDetails.optString("ICDCode3", ""));
                        etDiagnosis4.setText(claimDetails.optString("ICDCode4", ""));
                        etReferalHF.setText(claimDetails.optString("ReferalHF", ""));
                        etReferralCode.setText(claimDetails.optString("ReferralCode", ""));
                        etPrescriber.setText(claimDetails.optString("PrescriberNIN", ""));

                        setupPrescribersAdapter(sqlHandler.getHfUuid(global.getOfficerHealthFacility()));

                        switch (claimDetails.optString("VisitType", "")) {
                            case "E":
                                rgVisitType.check(R.id.rbEmergency);
                                break;
                            case "R":
                                rgVisitType.check(R.id.rbReferral);
                                break;
                            case "O":
                                rgVisitType.check(R.id.rbOther);
                                break;
                            default:
                                rgVisitType.clearCheck();
                        }

                        if (rgVisitType.getCheckedRadioButtonId() == R.id.rbReferral) {
                            if (isIntentReadonly()) {
                                disableView(etReferalHF);
                                disableView(etReferralCode);
                            } else {
                                etReferralCode.setEnabled(true);
                                etReferalHF.setEnabled(true);
                            }
                        } else {
                            disableView(etReferalHF);
                            disableView(etReferralCode);
                        }

                        // Chargement des items
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

                        // Chargement des services
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
                                if (!serviceJson.getString("ServicePackageType").equals("S")) {
                                    service.put("SubServicesItems", serviceJson.getString("SubServicesItems"));
                                }

                                lvServiceList.add(service);
                            }
                        }
                        tvServiceTotal.setText(String.valueOf(lvServiceList.size()));

                        TotalItemService = lvItemList.size() + lvServiceList.size();

                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error while parsing claim (" + claimUUID + ")", e);
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

    private boolean isValidData() {

        if (etHealthFacility.getText().length() == 0) {
            showValidationDialog(etHealthFacility, getResources().getString(R.string.MissingHealthFacility));
            return false;
        }

        if (sqlHandler.getAdjustability("ClaimAdministrator").equals("M") && etClaimAdmin.getText().length() == 0) {
            showValidationDialog(etClaimAdmin, getResources().getString(R.string.MissingClaimAdmin));
            return false;
        }

        if (etClaimCode.getText().length() == 0) {
            showValidationDialog(etClaimCode, getResources().getString(R.string.MissingClaimCode));
            return false;
        }

        if (etInsureeNumber.getText().length() == 0) {
            showValidationDialog(etInsureeNumber, getResources().getString(R.string.MissingCHFID));
            return false;
        }

        if (!isValidInsureeNumber()) {
            showValidationDialog(etInsureeNumber, getResources().getString(R.string.InvalidCHFID));
            return false;
        }

        if (etDiagnosis.getText().length() == 0) {
            showValidationDialog(etDiagnosis, getResources().getString(R.string.MissingDisease));
            return false;
        }

        if (rgVisitType.getCheckedRadioButtonId() == -1) {
            showValidationDialog(rgVisitType, getResources().getString(R.string.MissingVisitType));
            return false;
        }

        if (Float.parseFloat(tvItemTotal.getText().toString()) + Float.parseFloat(tvServiceTotal.getText().toString()) == 0) {
            showValidationDialog(tvItemTotal, getResources().getString(R.string.MissingClaim));
            return false;
        }

        if(rgVisitType.getCheckedRadioButtonId() == R.id.rbReferral){
            if(etReferralCode.getText().length() == 0){
                showValidationDialog(etReferralCode, getResources().getString(R.string.MissingReferralCode));
                return false;
            }
        }

        if(!etReferalHF.getText().toString().isEmpty() && sqlHandler.getHfId(etReferalHF.getText().toString()).isEmpty()){
            showValidationDialog(rgVisitType, getResources().getString(R.string.InvalidReferalHf));
            return false;
        }

        return true;
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

        int SelectedId;
        SelectedId = rgVisitType.getCheckedRadioButtonId();
        RadioButton selectedTypeButton;
        selectedTypeButton = findViewById(SelectedId);
        String visitType = selectedTypeButton.getTag().toString();

        ContentValues claimCV = new ContentValues();

        claimCV.put("ClaimUUID", claimUUID);
        claimCV.put("ClaimDate", claimDate);
        claimCV.put("HFCode", etHealthFacility.getText().toString());
        claimCV.put("ClaimAdmin", etClaimAdmin.getText().toString());
        claimCV.put("ClaimPreAuthorizationCode", etClaimCode.getText().toString());
        claimCV.put("GuaranteeNumber", etGuaranteeNo.getText().toString());
        claimCV.put("RejectionPreAuthorizationReason", etReason.getText().toString());
        claimCV.put("DatePreAuthorization", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        claimCV.put("DatePreAuthorization", etDatePreAuth.getText().toString());
        claimCV.put("IsPreAuthorization", "1");
        claimCV.put("InsureeNumber", etInsureeNumber.getText().toString());
        claimCV.put("ICDCode", etDiagnosis.getText().toString());
        claimCV.put("Comment", "");
        claimCV.put("Total", "");
        claimCV.put("ICDCode1", etDiagnosis1.getText().toString());
        claimCV.put("ICDCode2", etDiagnosis2.getText().toString());
        claimCV.put("ICDCode3", etDiagnosis3.getText().toString());
        claimCV.put("ICDCode4", etDiagnosis4.getText().toString());
        claimCV.put("VisitType", visitType);
        claimCV.put("ReferalHF", etReferalHF.getText().toString());
        claimCV.put("ReferralCode", etReferralCode.getText().toString());
        claimCV.put("PrescriberNIN", etPrescriber.getText().toString());
        claimCV.put("PrescriberUuid", selectedPrescriberUuid);

        ArrayList<ContentValues> claimItemCVs = new ArrayList<>(lvItemList.size());
        for (int i = 0; i < lvItemList.size(); i++) {
            ContentValues claimItemCV = new ContentValues();
            String itemId = sqlHandler.getItemId(lvItemList.get(i).get("Code"));

            claimItemCV.put("ClaimUUID", claimUUID);
            claimItemCV.put("ItemId", itemId);
            claimItemCV.put("ItemCode", lvItemList.get(i).get("Code"));
            claimItemCV.put("ItemPrice", lvItemList.get(i).get("Price"));
            claimItemCV.put("ItemQuantity", lvItemList.get(i).get("Quantity"));

            claimItemCVs.add(claimItemCV);
        }

        ArrayList<ContentValues> claimServiceCVs = new ArrayList<>(lvServiceList.size());
        for (int i = 0; i < lvServiceList.size(); i++) {
            ContentValues claimServiceCV = new ContentValues();
            String serviceId = sqlHandler.getServiceId(lvServiceList.get(i).get("Code"));

            claimServiceCV.put("ClaimUUID", claimUUID);
            claimServiceCV.put("ServiceId", serviceId);
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
}
