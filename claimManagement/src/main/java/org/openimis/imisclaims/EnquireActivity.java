package org.openimis.imisclaims;

import static org.openimis.imisclaims.BuildConfig.API_BASE_URL;
import static org.openimis.imisclaims.BuildConfig.REST_API_PREFIX;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.squareup.picasso.Picasso;

import org.openimis.imisclaims.adapter.FamilyMemberAdapter;
import org.openimis.imisclaims.adapter.PolygamousSubFamilyAdapter;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.domain.entity.Insuree;
import org.openimis.imisclaims.domain.entity.Policy;
import org.openimis.imisclaims.domain.entity.PolygamousSubFamily;
import org.openimis.imisclaims.network.GetFamilyMembersGraphQLRequest;
import org.openimis.imisclaims.network.GetPolygamousSubFamiliesGraphQLRequest;
import org.openimis.imisclaims.network.exception.HttpException;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.usecase.FetchInsureeInquire;
import org.openimis.imisclaims.util.FamilyTypeConstants;
import org.openimis.imisclaims.domain.entity.Family;
import org.openimis.imisclaims.util.DateUtils;
import org.openimis.imisclaims.util.TextViewUtils;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EnquireActivity extends ImisActivity {
    private static final String LOG_TAG = "ENQUIRE";
    private static final int REQUEST_QR_SCAN_CODE = 1;
    EditText etCHFID;
    TextView tvCHFID, tvName, tvGender, tvDOB;
    ImageButton btnGo, btnScan;
    ListView lv;
    RecyclerView listViewFamilyMembers;
    RecyclerView listViewPolygamousSubFamilies;
    LinearLayout llFamilyMembers;
    LinearLayout llPolygamousSubFamilies;
    ImageView iv;
    LinearLayout ll;
    ProgressDialog pd;
    FamilyMemberAdapter familyMemberAdapter;
    PolygamousSubFamilyAdapter polygamousSubFamilyAdapter;

    private boolean ZoomOut = false;
    private int orgHeight, orgWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_enquire);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getResources().getString(R.string.app_name_enquire));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        isSDCardAvailable();

        //Check if network available
        if (!global.isNetworkAvailable()) {
            setTitle(getResources().getString(R.string.app_name_claims) + "-" + getResources().getString(R.string.OfflineMode));
            setTitleColor(getResources().getColor(R.color.Red));
        }

        etCHFID = findViewById(R.id.etCHFID);
        tvCHFID = findViewById(R.id.tvCHFID);
        tvName = findViewById(R.id.tvName);
        tvDOB = findViewById(R.id.tvDOB);
        tvGender = findViewById(R.id.tvGender);
        iv = findViewById(R.id.imageView);
        btnGo = findViewById(R.id.btnGo);
        btnScan = findViewById(R.id.btnScan);
        lv = findViewById(R.id.listView1);
        listViewFamilyMembers = findViewById(R.id.listViewFamilyMembers);
        listViewFamilyMembers.setLayoutManager(new LinearLayoutManager(this));
        listViewPolygamousSubFamilies = findViewById(R.id.listViewPolygamousSubFamilies);
        listViewPolygamousSubFamilies.setLayoutManager(new LinearLayoutManager(this));
        llFamilyMembers = findViewById(R.id.llFamilyMembers);
        llPolygamousSubFamilies = findViewById(R.id.llPolygamousSubFamilies);
        ll = findViewById(R.id.llListView);

        lv.setNestedScrollingEnabled(true);

        iv.setOnClickListener(v -> {
            if (ZoomOut) {
                iv.setLayoutParams(new LinearLayout.LayoutParams(orgWidth, orgHeight));
                iv.setAdjustViewBounds(true);
                ZoomOut = false;
            } else {
                orgWidth = iv.getWidth();
                orgHeight = iv.getHeight();
                iv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                ZoomOut = true;
            }
        });

        btnGo.setOnClickListener(v -> {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            ClearForm();
            Escape escape = new Escape();
            if (!escape.CheckCHFID(etCHFID.getText().toString())) {
                ShowDialog(tvCHFID, getResources().getString(R.string.MissingCHFID));
                return;
            }

            pd = ProgressDialog.show(EnquireActivity.this, "", getResources().getString(R.string.GetingInsuuree));
            new Thread(() -> {
                getInsureeInfo();
                // ProgressDialog will be closed in renderResult() or in error methods
            }).start();
        });

        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, REQUEST_QR_SCAN_CODE);
            ClearForm();
        });

        etCHFID.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                ClearForm();
                Escape escape = new Escape();
                if (!escape.CheckCHFID(etCHFID.getText().toString())) return false;

                pd = ProgressDialog.show(EnquireActivity.this, "", getResources().getString(R.string.GetingInsuuree));
                new Thread(() -> {
                    getInsureeInfo();
                    // ProgressDialog will be closed in renderResult() or in error methods
                }).start();
            }
            return false;
        });

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_QR_SCAN_CODE:
                if (resultCode == RESULT_OK) {
                    String result = data.getStringExtra("SCAN_RESULT");
                    String CHFID = result.substring(result.indexOf(":")+2,result.indexOf("}")-1);
                    etCHFID.setText(CHFID);

                    Escape escape = new Escape();
                    if (!escape.CheckCHFID(etCHFID.getText().toString())) return;

                    pd = ProgressDialog.show(EnquireActivity.this, "", getResources().getString(R.string.GetingInsuuree));
                    new Thread(() -> {
                        getInsureeInfo();
                        // ProgressDialog will be closed in renderResult() or in error methods
                    }).start();
                }
                break;
        }
    }

    private void isSDCardAvailable() {
        String status = ((Global) getApplicationContext()).getSDCardStatus();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(status)) {
            //Toast.makeText(this, "SD Card is in read only mode.", Toast.LENGTH_LONG);
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.SDCardReadOnly))
                    .setCancelable(false)
                    .setPositiveButton("Force close", (dialog, which) -> finish()).show();

        } else if (!Environment.MEDIA_MOUNTED.equals(status)) {
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.SDCardMissing))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.ForceClose), (dialog, which) -> finish()).create().show();
        }
    }

    protected AlertDialog ShowDialog(final TextView tv, String msg) {
        return new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.Ok, (dialog, which) -> tv.requestFocus()).show();
    }

    @SuppressLint({"WrongConstant", "Range"})
    @Nullable
    private Insuree getDataFromDb(String chfid) {
        try {
            SQLiteDatabase db = openOrCreateDatabase(SQLHandler.DB_NAME_DATA, SQLiteDatabase.OPEN_READONLY, null);
            String[] columns = {"CHFID", "Photo", "InsureeName", "DOB", "Gender", "ProductCode", "ProductName", "ExpiryDate", "Status", "DedType", "Ded1", "Ded2", "Ceiling1", "Ceiling2"};
            String[] selectionArgs = {chfid};
            Cursor c = db.query("tblPolicyInquiry", columns, "Trim(InsureeNumber)=?", selectionArgs, null, null, null);
            String name = null;
            Date dateOfBirth = null;
            String gender = null;
            byte[] photo = null;
            List<Policy> policies = new ArrayList<>();
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                if (c.isFirst()) {
                    name = c.getString(c.getColumnIndex("InsureeName"));
                    String dateOfBirthString = c.getString(c.getColumnIndex("DOB"));
                    if (dateOfBirthString != null) {
                        dateOfBirth = DateUtils.dateFromString(dateOfBirthString);
                    }
                    gender = c.getString(c.getColumnIndex("Gender"));
                    photo = c.getBlob(c.getColumnIndex("Photo"));
                }
                String expiryDate = c.getString(c.getColumnIndex("ExpiryDate"));
                String status = c.getString(c.getColumnIndex("Status"));
                String deductibleType = c.getString(c.getColumnIndex("DedType"));
                String deductibleIp = c.getString(c.getColumnIndex("Ded1"));
                String deductibleOp = c.getString(c.getColumnIndex("Ded2"));
                String ceilingIp = c.getString(c.getColumnIndex("Ceiling1"));
                String ceilingOp = c.getString(c.getColumnIndex("Ceiling2"));
                policies.add(new Policy(
                        /* code = */ c.getString(c.getColumnIndex("ProductCode")),
                        /* name = */ c.getString(c.getColumnIndex("ProductName")),
                        /* value = */ null,
                        /* expiryDate = */ expiryDate != null ? DateUtils.dateFromString(expiryDate) : null,
                        /* status = */ status != null ? Policy.Status.valueOf(status) : null,
                        /* deductibleType = */ deductibleType != null ? Double.parseDouble(deductibleType) : null,
                        /* deductibleIp = */ deductibleIp != null ? Double.parseDouble(deductibleIp) : null,
                        /* deductibleOp = */ deductibleOp != null ? Double.parseDouble(deductibleOp) : null,
                        /* ceilingIp = */ ceilingIp != null ? Double.parseDouble(ceilingIp) : null,
                        /* ceilingOp = */ ceilingOp != null ? Double.parseDouble(ceilingOp) : null,
                        /* antenatalAmountLeft = */ null,
                        /* consultationAmountLeft = */ null,
                        /* deliveryAmountLeft = */ null,
                        /* hospitalizationAmountLeft = */ null,
                        /* surgeryAmountLeft = */ null,
                        /* totalAdmissionsLeft = */ null,
                        /* totalAntenatalLeft = */ null,
                        /* totalConsultationsLeft = */ null,
                        /* totalDeliveriesLeft = */ null,
                        /* totalSurgeriesLeft = */ null,
                        /* totalVisitsLeft = */ null
                ));
            }
            c.close();
            db.close();
            return new Insuree(
                    /* chfId = */ chfid,
                    /* name = */ Objects.requireNonNull(name),
                    /* dateOfBirth = */ Objects.requireNonNull(dateOfBirth),
                    /* gender = */ gender,
                    /* photoPath = */ null,
                    /* photo = */ photo,
                    /* policies = */ policies,
                    /* familyUuid = */ null // Pas d'UUID famille en mode hors ligne
            );
        } catch (Exception e) {

            return null;
        }

    }

    @WorkerThread
    private void getInsureeInfo() {
        runOnUiThread(this::ClearForm);
        String chfid = etCHFID.getText().toString();
        if (global.isNetworkAvailable()) {
            try {
                Insuree insuree = new FetchInsureeInquire().execute(chfid);
                runOnUiThread(() -> renderResult(insuree));
            } catch (HttpException e) {
                if (e.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        showDialog(getResources().getString(R.string.RecordNotFound));
                    });
                } else {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        showDialog(e.getMessage());
                    });
                }
            } catch (Exception e) {

                runOnUiThread(() -> {
                    pd.dismiss();
                    showDialog(getResources().getString(R.string.UnknownError));
                });
            }
        } else {
            //TODO: yet to be done
            runOnUiThread(() -> {
                renderResult(getDataFromDb(chfid));
            });
        }
    }

    public void renderResult(@Nullable Insuree insuree) {
        if (insuree == null) {
            pd.dismiss();
            showDialog(getResources().getString(R.string.RecordNotFound));
            return;
        }

        // FIRST STEP: Check family type (polygamous or monogamous)
        if (insuree.getFamilyUuid() != null && !insuree.getFamilyUuid().isEmpty()) {
            checkFamilyTypeAndRender(insuree);
        } else {
            // Pas d'UUID famille, afficher directement les informations de base
            renderInsureeBasicInfo(insuree);
        }
    }
    
    /**
     * Détermine si on doit afficher les sous-familles polygames ou les membres normaux
     * Implémentation basée sur la logique JavaScript conditionnelle :
     * 
     * JavaScript équivalent :
     * {
     *   shouldShowSubFamilies(insuree.family?.parent?.uuid || insuree.family?.uuid, familyUuid) ? (
     *     <SubFamiliesTable />
     *   ) : (
     *     <FamilyMembersTable />
     *   )
     * }
     * 
     * Logique conditionnelle intégrée :
     * 1. Si la famille actuelle est polygame → afficher sous-familles (SubFamiliesTable)
     * 2. Si l'assuré est lui-même un chef polygame → afficher ses sous-familles
     * 3. Si le parent est polygame ET l'assuré est chef du parent → afficher sous-familles du parent
     * 4. Sinon → afficher membres normaux (FamilyMembersTable)
     * 
     * @param insuree L'assuré principal
     * @param familyType Type de la famille actuelle
     * @param parentFamily Famille parent (peut être null)
     * @return true pour afficher SubFamiliesTable, false pour FamilyMembersTable
     */
    private boolean shouldShowPolygamousSubFamilies(Insuree insuree, String familyType, Family parentFamily) {
        // JavaScript equivalent: const parent = insuree?.family?.parent;
        Family parent = parentFamily;
        
        // JavaScript equivalent: isPolygamyFamilyType (condition for SubFamiliesTable)
        boolean isPolygamyFamilyType = FamilyTypeConstants.isPolygamyFamilyType(familyType);
        
        // Integrated conditional logic: check if insuree is himself a polygamous head
        // This corresponds to the shouldShowSubFamilies() condition in JavaScript
        boolean isInsureePolygamousHead = isInsureePolygamousHead(insuree);
        
        // JavaScript equivalent: const isParentPolygamy = parent?.familyType?.code === FAMILY_TYPE_POLYGAMY_CODE;
        boolean isParentPolygamy = false;
        if (parent != null && parent.getFamilyType() != null) {
            isParentPolygamy = FamilyTypeConstants.isPolygamyFamilyType(parent.getFamilyType());
        }
        
        // JavaScript equivalent: const isInsureeParentHead = insuree.uuid === parent.headInsuree.uuid;
        // Using chfId for identity comparison
        boolean isInsureeParentHead = false;
        if (insuree != null && insuree.getChfId() != null && 
            parent != null && parent.getHeadInsureeChfId() != null) {
            isInsureeParentHead = insuree.getChfId().equals(parent.getHeadInsureeChfId());
        }
        
        // Logique conditionnelle finale : true = SubFamiliesTable, false = FamilyMembersTable
        // Equivalent to: shouldShowSubFamilies() ? <SubFamiliesTable /> : <FamilyMembersTable />
        boolean shouldShowSubFamilies = isPolygamyFamilyType || isInsureePolygamousHead || (isParentPolygamy && isInsureeParentHead);
        
        return shouldShowSubFamilies;
    }
    
    /**
     * Vérifie si un assuré est un chef polygame en vérifiant s'il a des sous-familles
     * Cette méthode résout le problème où un chef polygame inclus dans une sous-famille
     * perdrait son statut de polygame
     * 
     * @param insuree L'assuré à vérifier
     * @return true si l'assuré a des sous-familles (donc est un chef polygame)
     */
    private boolean isInsureePolygamousHead(Insuree insuree) {
        try {
            String familyUuid = insuree.getFamilyUuid();
            if (familyUuid == null) {
                return false;
            }
            
            // Check if this insuree has sub-families
            GetPolygamousSubFamiliesGraphQLRequest polygamousRequest = new GetPolygamousSubFamiliesGraphQLRequest(sqlHandler);
            List<PolygamousSubFamily> subFamilies = polygamousRequest.get(familyUuid);
            
            boolean hasSubFamilies = subFamilies != null && !subFamilies.isEmpty();
            
            return hasSubFamilies;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère l'UUID de famille approprié pour l'affichage des sous-familles
     * Implémentation directe de la logique JavaScript :
     * insuree.family?.parent?.uuid || insuree.family?.uuid
     * 
     * Cette méthode implémente l'opérateur de coalescence nullish (??) du JavaScript
     * pour déterminer quel UUID de famille utiliser dans la logique conditionnelle.
     * 
     * @param insuree L'assuré principal
     * @param parentFamily La famille parent (peut être null)
     * @return L'UUID de la famille à utiliser (parent en priorité, sinon famille actuelle)
     */
    private String getFamilyUuidForSubFamilies(Insuree insuree, Family parentFamily) {
        // JavaScript equivalent: insuree.family?.parent?.uuid
        // First priority: Parent family UUID if it exists
        if (parentFamily != null && parentFamily.getUuid() != null && !parentFamily.getUuid().isEmpty()) {
            return parentFamily.getUuid();
        }
        
        // JavaScript equivalent: || insuree.family?.uuid
        // Second priority: Insuree's family UUID (fallback)
        String familyUuid = insuree != null ? insuree.getFamilyUuid() : null;
        
        return familyUuid;
    }

    /**
     * Détecte automatiquement si la famille est polygame ou monogame
     * et affiche le contenu approprié selon la logique conditionnelle intégrée
     * 
     * Implémentation de la logique JavaScript :
     * {
     *   shouldShowSubFamilies(insuree.family?.parent?.uuid || insuree.family?.uuid, familyUuid) ? (
     *     <SubFamiliesTable />  // renderPolygamousFamily()
     *   ) : (
     *     <FamilyMembersTable />  // renderMonogamousFamily()
     *   )
     * }
     */
    private void checkFamilyTypeAndRender(Insuree insuree) {
        String familyUuid = insuree.getFamilyUuid();
        
        if (!global.isNetworkAvailable()) {
            renderInsureeBasicInfo(insuree);
            return;
        }
        
        new Thread(() -> {
            try {
                // Step 1: Retrieve family and parent information
                GetFamilyMembersGraphQLRequest familyRequest = new GetFamilyMembersGraphQLRequest(sqlHandler);
                String familyType = familyRequest.getFamilyType(familyUuid);
                Family parentFamily = familyRequest.getParentFamily(familyUuid);
                
                // Step 2: Apply integrated conditional logic
                // Equivalent to: shouldShowSubFamilies() ? <SubFamiliesTable /> : <FamilyMembersTable />
                boolean shouldShowPolygamous = shouldShowPolygamousSubFamilies(insuree, familyType, parentFamily);
                
                if (shouldShowPolygamous) {
                    // BRANCHE: <SubFamiliesTable /> - Afficher les sous-familles polygames
                    
                    // Utiliser la logique JavaScript: insuree.family?.parent?.uuid || insuree.family?.uuid
                    String targetFamilyUuid = getFamilyUuidForSubFamilies(insuree, parentFamily);
                    
                    // Retrieve and display sub-family heads (SubFamiliesTable equivalent)
                    GetPolygamousSubFamiliesGraphQLRequest polygamousRequest = new GetPolygamousSubFamiliesGraphQLRequest(sqlHandler);
                    List<PolygamousSubFamily> polygamousSubFamilies = polygamousRequest.get(targetFamilyUuid);
                    
                    runOnUiThread(() -> {
                        if (polygamousSubFamilies != null && !polygamousSubFamilies.isEmpty()) {
                            renderPolygamousFamily(insuree, polygamousSubFamilies);
                        } else {
                            renderInsureeBasicInfo(insuree);
                        }
                    });
                } else {
                    // BRANCHE: <FamilyMembersTable /> - Afficher les membres de famille normaux
                    // Afficher les membres normaux
                    runOnUiThread(() -> {
                        renderMonogamousFamily(insuree);
                    });
                }
                
            } catch (Exception e) {
                runOnUiThread(() -> renderInsureeBasicInfo(insuree));
            }
        }).start();
    }
    
    /**
     * Affiche une famille polygame avec ses chefs de sous-familles
     * @param insuree L'assuré principal
     * @param polygamousSubFamilies Liste des chefs de sous-familles à afficher
     */
    private void renderPolygamousFamily(Insuree insuree, List<PolygamousSubFamily> polygamousSubFamilies) {
        renderInsureeBasicInfoForPolygamous(insuree);
        
        if (polygamousSubFamilyAdapter == null) {
            polygamousSubFamilyAdapter = new PolygamousSubFamilyAdapter(this, polygamousSubFamilies);
            polygamousSubFamilyAdapter.setOnSubFamilyClickListener(subFamily -> {
                showSubFamilyMembers(subFamily);
            });
            listViewPolygamousSubFamilies.setAdapter(polygamousSubFamilyAdapter);
        } else {
            polygamousSubFamilyAdapter.updateData(polygamousSubFamilies);
        }
        
        llPolygamousSubFamilies.setVisibility(View.VISIBLE);
        
        if (llFamilyMembers != null) {
            llFamilyMembers.setVisibility(View.GONE);
        }
        
        if (ll != null) {
            ll.setVisibility(View.GONE);
        }
        
        // Close loading indicator once all information is displayed
        pd.dismiss();
    }
    
    /**
     * Affiche une famille monogame avec ses membres normaux
     * @param insuree L'assuré principal
     */
    private void renderMonogamousFamily(Insuree insuree) {
        renderInsureeBasicInfo(insuree);
        loadFamilyMembers(insuree.getFamilyUuid());
        
        // Masquer la section des sous-familles polygames
        if (llPolygamousSubFamilies != null) {
            llPolygamousSubFamilies.setVisibility(View.GONE);
        }
    }
    
    private void renderInsureeBasicInfo(Insuree insuree) {
        ll.setVisibility(View.VISIBLE);

        if (!etCHFID.getText().toString().trim().equals(insuree.getChfId()))
            return;

        tvCHFID.setText(insuree.getChfId());
        tvName.setText(insuree.getName());
        TextViewUtils.setDate(tvDOB, insuree.getDateOfBirth());
        tvGender.setText(insuree.getGender());

        byte[] imageBytes = insuree.getPhoto();
        if (imageBytes != null) {
            try {
                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                iv.setImageBitmap(image);
            } catch (Throwable e) {
                Log.e(LOG_TAG, "Error while processing Base64 image", e);
                iv.setImageDrawable(getResources().getDrawable(R.drawable.person));
            }
        } else if (insuree.getPhotoPath() != null && global.isNetworkAvailable()) {
            iv.setImageResource(R.drawable.person);
            new Picasso.Builder(this).build()
                    .load(API_BASE_URL + REST_API_PREFIX + insuree.getPhotoPath())
                    .placeholder(R.drawable.person)
                    .error(R.drawable.person)
                    .into(iv);
        } else {
            iv.setImageDrawable(getResources().getDrawable(R.drawable.person));
        }

        ArrayList<Map<String, String>> PolicyList = new ArrayList<>();
        Collections.reverse(insuree.getPolicies());
        for (Policy policy : insuree.getPolicies()) {
            HashMap<String, String> policyMap = new HashMap<>();
            double iDedType = policy.getDeductibleType() != null ? policy.getDeductibleType() : 0;

            String Ded = "", Ded1 = "", Ded2 = "";
            String Ceiling = "", Ceiling1 = "", Ceiling2 = "";


            //Get the type

            if (iDedType == 1 | iDedType == 2 | iDedType == 3) {
                if (policy.getDeductibleIp() != null) {
                    Ded1 = String.valueOf(policy.getDeductibleIp());
                    Ded = "Deduction: " + Ded1;
                }
                if (policy.getCeilingIp() != null) {
                    Ceiling1 = String.valueOf(policy.getCeilingIp());
                    Ceiling = "Ceiling: " + Ceiling1;
                }
            } else if (iDedType == 1.1 | iDedType == 2.1 | iDedType == 3.1) {
                if (policy.getDeductibleIp() != null) {
                    Ded1 = " IP:" + policy.getDeductibleIp();
                }
                if (policy.getDeductibleOp() != null) {
                    Ded2 = " OP:" + policy.getDeductibleOp();
                }
                if (policy.getCeilingIp() != null) {
                    Ceiling1 = " IP:" + policy.getCeilingIp();
                }
                if (policy.getCeilingIp() != null) {
                    Ceiling2 = " OP:" + policy.getCeilingOp();
                }

                if (!(Ded1 + Ded2).equals("")) {
                    Ded = "Deduction: " + Ded1 + Ded2;
                }
                if (!(Ceiling1 + Ceiling2).equals("")) {
                    Ceiling = "Ceiling: " + Ceiling1 + Ceiling2;
                }
            }

            String expiryDate = policy.getExpiryDate() != null ?
                    DateUtils.toExpiryDateString(policy.getExpiryDate()) : null;
            String status = policy.getStatus().name();
            String heading1;
            if (expiryDate != null) {
                heading1 = expiryDate + " " + status;
            } else {
                heading1 = status;
            }
            policyMap.put("Heading", policy.getCode());
            policyMap.put("Heading1", heading1);
            policyMap.put("SubItem1", policy.getName());
            policyMap.put("SubItem2", Ded);
            policyMap.put("SubItem3", Ceiling);

            SQLHandler sqlHandler = new SQLHandler(this);
            if (!sqlHandler.getAdjustability("TotalAdmissionsLeft").equals("N")) {
                policyMap.put("SubItem4", buildEnquireValue(policy.getTotalAdmissionsLeft(), R.string.totalAdmissionsLeft));
            }
            if (!sqlHandler.getAdjustability("TotalVisitsLeft").equals("N")) {
                policyMap.put("SubItem5", buildEnquireValue(policy.getTotalVisitsLeft(), R.string.totalVisitsLeft));
            }
            if (!sqlHandler.getAdjustability("TotalConsultationsLeft").equals("N")) {
                policyMap.put("SubItem6", buildEnquireValue(policy.getTotalConsultationsLeft(), R.string.totalConsultationsLeft));
            }
            if (!sqlHandler.getAdjustability("TotalSurgeriesLeft").equals("N")) {
                policyMap.put("SubItem7", buildEnquireValue(policy.getTotalSurgeriesLeft(), R.string.totalSurgeriesLeft));
            }
            if (!sqlHandler.getAdjustability("TotalDelivieriesLeft").equals("N")) {
                policyMap.put("SubItem8", buildEnquireValue(policy.getTotalDeliveriesLeft(), R.string.totalDeliveriesLeft));
            }
            if (!sqlHandler.getAdjustability("TotalAntenatalLeft").equals("N")) {
                policyMap.put("SubItem9", buildEnquireValue(policy.getTotalAntenatalLeft(), R.string.totalAntenatalLeft));
            }
            if (!sqlHandler.getAdjustability("ConsultationAmountLeft").equals("N")) {
                policyMap.put("SubItem10", buildEnquireValue(policy.getConsultationAmountLeft(), R.string.consultationAmountLeft));
            }
            if (!sqlHandler.getAdjustability("AntenatalAmountLeft").equals("N")) {
                policyMap.put("SubItem13", buildEnquireValue(policy.getAntenatalAmountLeft(), R.string.antenatalAmountLeft));
            }
            if (!sqlHandler.getAdjustability("SurgeryAmountLeft").equals("N")) {
                policyMap.put("SubItem11", buildEnquireValue(policy.getSurgeryAmountLeft(), R.string.surgeryAmountLeft));
            }
            if (!sqlHandler.getAdjustability("HospitalizationAmountLeft").equals("N")) {
                policyMap.put("SubItem12", buildEnquireValue(policy.getHospitalizationAmountLeft(), R.string.hospitalizationAmountLeft));
            }
            if (!sqlHandler.getAdjustability("DeliveryAmountLeft").equals("N")) {
                policyMap.put("SubItem14", buildEnquireValue(policy.getDeliveryAmountLeft(), R.string.deliveryAmountLeft));
            }
            sqlHandler.close();

            PolicyList.add(policyMap);
            etCHFID.setText("");
            //break;
        }

        ListAdapter adapter = new SimpleAdapter(EnquireActivity.this,
                PolicyList, R.layout.policylist,
                new String[]{"Heading", "Heading1", "SubItem1", "SubItem2", "SubItem3", "SubItem4", "SubItem5", "SubItem6", "SubItem7", "SubItem8", "SubItem9", "SubItem10", "SubItem11", "SubItem12", "SubItem13", "SubItem14"},
                new int[]{R.id.tvHeading, R.id.tvHeading1, R.id.tvSubItem1, R.id.tvSubItem2, R.id.tvSubItem3, R.id.tvSubItem4, R.id.tvSubItem5, R.id.tvSubItem6, R.id.tvSubItem7, R.id.tvSubItem8, R.id.tvSubItem9, R.id.tvSubItem10, R.id.tvSubItem11, R.id.tvSubItem12, R.id.tvSubItem13, R.id.tvSubItem14}
        );

        lv.setAdapter(adapter);
        
        // Close loading indicator once all information is displayed
        pd.dismiss();
    }

    private void renderInsureeBasicInfoForPolygamous(Insuree insuree) {
        // Ne pas rendre llListView visible pour les familles polygames
        
        if (!etCHFID.getText().toString().trim().equals(insuree.getChfId()))
            return;

        tvCHFID.setText(insuree.getChfId());
        tvName.setText(insuree.getName());
        TextViewUtils.setDate(tvDOB, insuree.getDateOfBirth());
        tvGender.setText(insuree.getGender());

        byte[] imageBytes = insuree.getPhoto();
        if (imageBytes != null) {
            try {
                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                iv.setImageBitmap(image);
            } catch (Throwable e) {
                Log.e(LOG_TAG, "Error while processing Base64 image", e);
                iv.setImageDrawable(getResources().getDrawable(R.drawable.person));
            }
        } else if (insuree.getPhotoPath() != null && global.isNetworkAvailable()) {
            iv.setImageResource(R.drawable.person);
            new Picasso.Builder(this).build()
                    .load(API_BASE_URL + REST_API_PREFIX + insuree.getPhotoPath())
                    .placeholder(R.drawable.person)
                    .error(R.drawable.person)
                    .into(iv);
        } else {
            iv.setImageDrawable(getResources().getDrawable(R.drawable.person));
        }
        
        // Close loading indicator once all information is displayed
        pd.dismiss();
    }

    protected String buildEnquireValue(@Nullable Number value, @StringRes int labelId) {
        if (value == null) {
            return "";
        } else {
            String label = getResources().getString(labelId);
            return label + ": " + value;
        }
    }

    /**
     * Charge et affiche les membres d'une famille monogame
     * @param familyUuid UUID de la famille dont charger les membres
     */
    private void loadFamilyMembers(String familyUuid) {
        if (!global.isNetworkAvailable()) {
            if (llFamilyMembers != null) {
                llFamilyMembers.setVisibility(View.GONE);
            }
            return;
        }
        
        new Thread(() -> {
            try {
                GetFamilyMembersGraphQLRequest request = new GetFamilyMembersGraphQLRequest();
                List<FamilyMember> familyMembers = request.get(familyUuid);
                
                runOnUiThread(() -> {
                    if (familyMembers != null && !familyMembers.isEmpty()) {
                        if (familyMemberAdapter == null) {
                            familyMemberAdapter = new FamilyMemberAdapter(this, familyMembers);
                            listViewFamilyMembers.setAdapter(familyMemberAdapter);
                        } else {
                            familyMemberAdapter.updateData(familyMembers);
                        }
                        llFamilyMembers.setVisibility(View.VISIBLE);
                    } else {
                        llFamilyMembers.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (llFamilyMembers != null) {
                        llFamilyMembers.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private void showSubFamilyMembers(PolygamousSubFamily subFamily) {
        try {
            if (subFamily == null || subFamily.getChfId() == null) {
                Log.e(LOG_TAG, "Sous-famille ou ID CHF manquant");
                return;
            }
            
            Intent intent = new Intent(this, SubHouseholdActivity.class);
            // Transmission de toutes les informations disponibles du chef de sous-famille
            intent.putExtra(SubHouseholdActivity.EXTRA_CHF_ID, subFamily.getChfId());
            
            // Ajout des informations personnelles
            if (subFamily.getLastName() != null) {
                intent.putExtra("EXTRA_LAST_NAME", subFamily.getLastName());
            }
            if (subFamily.getOtherNames() != null) {
                intent.putExtra("EXTRA_OTHER_NAMES", subFamily.getOtherNames());
            }
            if (subFamily.getGender() != null) {
                intent.putExtra("EXTRA_GENDER", subFamily.getGender());
            }
            if (subFamily.getDob() != null) {
                intent.putExtra("EXTRA_DOB", subFamily.getDob());
            }
            if (subFamily.getPhoto() != null) {
                 intent.putExtra("EXTRA_PHOTO_PATH", subFamily.getPhoto());
             }
            
            // Si un UUID de famille est disponible, on l'ajoute aux extras
            if (subFamily.getFamilyUuid() != null) {
                intent.putExtra(SubHouseholdActivity.EXTRA_FAMILY_UUID, subFamily.getFamilyUuid());
            }
            
            Log.d(LOG_TAG, "📤 Transmission des données vers SubHouseholdActivity:");
            Log.d(LOG_TAG, "   - CHFID: '" + subFamily.getChfId() + "'");
            Log.d(LOG_TAG, "   - LastName: '" + subFamily.getLastName() + "'");
            Log.d(LOG_TAG, "   - OtherNames: '" + subFamily.getOtherNames() + "'");
            Log.d(LOG_TAG, "   - Gender: '" + subFamily.getGender() + "'");
            Log.d(LOG_TAG, "   - DOB: '" + subFamily.getDob() + "'");
            Log.d(LOG_TAG, "   - FamilyUuid: '" + subFamily.getFamilyUuid() + "'");
            
            startActivity(intent);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur dans showSubFamilyMembers", e);
            Toast.makeText(this, "Erreur lors de l'affichage des détails de la famille", Toast.LENGTH_SHORT).show();
        }
    }

    private PolygamousSubFamily createSafeCopy(PolygamousSubFamily original) {
        try {
            PolygamousSubFamily copy = new PolygamousSubFamily();
            copy.setUuid(original.getUuid());
            copy.setChfId(original.getChfId());
            copy.setLastName(original.getLastName());
            copy.setOtherNames(original.getOtherNames());
            copy.setGender(original.getGender());
            copy.setGenderCode(original.getGenderCode());
            copy.setDob(original.getDob());
            copy.setPhotoId(original.getPhotoId());
            copy.setPhotoData(original.getPhotoData());
            copy.setRelationship(original.getRelationship());
            copy.setFamilyUuid(original.getFamilyUuid());
            copy.setParentUuid(original.getParentUuid());
            
            if (original.getMembers() != null) {
                List<FamilyMember> membersCopy = new ArrayList<>();
                for (FamilyMember member : original.getMembers()) {
                    if (member != null) {
                        membersCopy.add(member);
                    }
                }
                copy.setMembers(membersCopy);
            }
            
            return copy;
        } catch (Exception e) {

            return original;
        }
    }

    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        // If no items, set height to 0
        if (listAdapter.getCount() == 0) {
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = 0;
            listView.setLayoutParams(params);
            return;
        }

        // Optimization: measure only first item and multiply by number of items
        // This avoids multiple getView() calls that create duplicates
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        View listItem = listAdapter.getView(0, null, listView);
        listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
        int itemHeight = listItem.getMeasuredHeight();
        
        // Calculate total height based on single item height
        int totalHeight = itemHeight * listAdapter.getCount();
        
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private void ClearForm() {
        tvCHFID.setText(getResources().getString(R.string.CHFID));
        tvName.setText(getResources().getString(R.string.InsureeName));
        tvDOB.setText(getResources().getString(R.string.DOB));
        tvGender.setText(getResources().getString(R.string.Gender));
        iv.setImageResource(R.drawable.noimage);
        ll.setVisibility(View.GONE);
        lv.setAdapter(null);
        
        // Clear family members and polygamous sub-families lists
        if (listViewFamilyMembers != null) {
            listViewFamilyMembers.setAdapter(null);
        }
        if (llFamilyMembers != null) {
            llFamilyMembers.setVisibility(View.GONE);
        }
        if (listViewPolygamousSubFamilies != null) {
            listViewPolygamousSubFamilies.setAdapter(null);
        }
        if (llPolygamousSubFamilies != null) {
            llPolygamousSubFamilies.setVisibility(View.GONE);
        }
        familyMemberAdapter = null;
        polygamousSubFamilyAdapter = null;
    }
}
