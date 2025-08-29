package org.openimis.imisclaims;

import static org.openimis.imisclaims.BuildConfig.API_BASE_URL;
import static org.openimis.imisclaims.BuildConfig.REST_API_PREFIX;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ImageButton;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import androidx.cardview.widget.CardView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.openimis.imisclaims.adapter.FamilyMemberAdapter;
import org.openimis.imisclaims.adapter.PolygamousHeadAdapter;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.domain.entity.Insuree;
import org.openimis.imisclaims.domain.entity.Policy;
import org.openimis.imisclaims.network.exception.HttpException;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.usecase.FetchInsureeInquire;
import org.openimis.imisclaims.util.DateUtils;
import org.openimis.imisclaims.util.TextViewUtils;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EnquireActivity extends ImisActivity {
    private static final String LOG_TAG = "ENQUIRE";
    private static final int REQUEST_QR_SCAN_CODE = 1;
    TextInputEditText etCHFID;
    TextView tvCHFID, tvName, tvGender, tvDOB;
    MaterialButton btnGo, btnScan;
    ListView lv;
    ImageView iv;
    LinearLayout llHeadInfo;
    CardView llListView;
    RecyclerView rvFamilyMembers;
    FamilyMemberAdapter familyMemberAdapter;
    CardView cvPolygamousSection;
    RecyclerView rvPolygamousHeads;
    PolygamousHeadAdapter polygamousHeadAdapter;
    private List<FamilyMember> currentFamilyMembers = new ArrayList<>();
    ProgressDialog pd;

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
        llHeadInfo = findViewById(R.id.llHeadInfo);
        rvFamilyMembers = findViewById(R.id.rvFamilyMembers);
        llListView = findViewById(R.id.llListView);
        
        // Configuration du RecyclerView
        rvFamilyMembers.setLayoutManager(new LinearLayoutManager(this));
        familyMemberAdapter = new FamilyMemberAdapter(this, new ArrayList<>());
        familyMemberAdapter.setOnFamilyMemberClickListener(this::onFamilyMemberClick);
        rvFamilyMembers.setAdapter(familyMemberAdapter);
        
        // Initialiser les éléments pour la section polygame
        cvPolygamousSection = findViewById(R.id.cvPolygamousSection);
        rvPolygamousHeads = findViewById(R.id.rvPolygamousHeads);
        rvPolygamousHeads.setLayoutManager(new LinearLayoutManager(this));
        polygamousHeadAdapter = new PolygamousHeadAdapter(this, new ArrayList<>(), this::onPolygamousHeadClick);
        rvPolygamousHeads.setAdapter(polygamousHeadAdapter);

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

            // Ne pas appeler ClearForm() pour les CHFIDs de test
                String chfid = etCHFID.getText().toString().trim();
                {
                    ClearForm();
                }
            
            Escape escape = new Escape();
            if (!escape.CheckCHFID(etCHFID.getText().toString())) {
                ShowDialog(tvCHFID, getResources().getString(R.string.MissingCHFID));
                return;
            }



            pd = ProgressDialog.show(EnquireActivity.this, "", getResources().getString(R.string.GetingInsuuree));
            new Thread(() -> {
                getInsureeInfo();
                pd.dismiss();
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
                    pd.dismiss();
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
                        pd.dismiss();
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
    private List<FamilyMember> getFamilyMembersFromDb(String headChfid) {
        try {
            SQLiteDatabase db = openOrCreateDatabase(SQLHandler.DB_NAME_DATA, SQLiteDatabase.OPEN_READONLY, null);
            
            // Rechercher d'abord le chef de famille pour obtenir le FamilyId
            String[] headColumns = {"FamilyId"};
            String[] headSelectionArgs = {headChfid};
            Cursor headCursor = db.query("tblPolicyInquiry", headColumns, "Trim(CHFID)=?", headSelectionArgs, null, null, null);
            
            String familyId = null;
            if (headCursor.moveToFirst()) {
                familyId = headCursor.getString(headCursor.getColumnIndex("FamilyId"));
            }
            headCursor.close();
            
            if (familyId == null) {
                // Si pas de FamilyId trouvé, retourner juste le membre recherché
                return getSingleMemberFromDb(headChfid);
            }
            
            // Rechercher tous les membres de la famille
            String[] columns = {"CHFID", "Photo", "InsureeName", "DOB", "Gender", "ProductCode", "ProductName", "ExpiryDate", "Status", "DedType", "Ded1", "Ded2", "Ceiling1", "Ceiling2", "Relationship"};
            String[] selectionArgs = {familyId};
            Cursor c = db.query("tblPolicyInquiry", columns, "FamilyId=?", selectionArgs, null, null, "CHFID");
            
            Map<String, FamilyMember> membersMap = new HashMap<>();
            
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                String chfid = c.getString(c.getColumnIndex("CHFID"));
                
                FamilyMember member = membersMap.get(chfid);
                if (member == null) {
                    String name = c.getString(c.getColumnIndex("InsureeName"));
                    String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
                    String firstName = nameParts.length > 0 ? nameParts[0] : "";
                    String lastName = nameParts.length > 1 ? nameParts[1] : "";
                    
                    String dateOfBirthString = c.getString(c.getColumnIndex("DOB"));
                    Date dateOfBirth = null;
                    if (dateOfBirthString != null) {
                        dateOfBirth = DateUtils.dateFromString(dateOfBirthString);
                    }
                    
                    String gender = c.getString(c.getColumnIndex("Gender"));
                    byte[] photo = c.getBlob(c.getColumnIndex("Photo"));
                    String relationship = c.getString(c.getColumnIndex("Relationship"));
                    boolean isHead = chfid.equals(headChfid);
                    
                    member = new FamilyMember(chfid, lastName, firstName, gender, dateOfBirth, photo, null, new ArrayList<>(), relationship, isHead);
                    membersMap.put(chfid, member);
                }
                
                // Ajouter la police à ce membre
                String expiryDate = c.getString(c.getColumnIndex("ExpiryDate"));
                String status = c.getString(c.getColumnIndex("Status"));
                String deductibleType = c.getString(c.getColumnIndex("DedType"));
                String deductibleIp = c.getString(c.getColumnIndex("Ded1"));
                String deductibleOp = c.getString(c.getColumnIndex("Ded2"));
                String ceilingIp = c.getString(c.getColumnIndex("Ceiling1"));
                String ceilingOp = c.getString(c.getColumnIndex("Ceiling2"));
                
                Policy policy = new Policy(
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
                );
                
                member.getActivePolicies().add(policy);
            }
            
            c.close();
            db.close();
            
            List<FamilyMember> familyMembers = new ArrayList<>(membersMap.values());
            // Trier pour mettre le chef de famille en premier
            familyMembers.sort((m1, m2) -> Boolean.compare(m2.isHead(), m1.isHead()));
            

            
            return familyMembers;
        } catch (Exception e) {
            return null;
        }
    }
    
    @SuppressLint({"WrongConstant", "Range"})
    @Nullable
    private List<FamilyMember> getSingleMemberFromDb(String chfid) {
        try {
            SQLiteDatabase db = openOrCreateDatabase(SQLHandler.DB_NAME_DATA, SQLiteDatabase.OPEN_READONLY, null);
            String[] columns = {"CHFID", "Photo", "InsureeName", "DOB", "Gender", "ProductCode", "ProductName", "ExpiryDate", "Status", "DedType", "Ded1", "Ded2", "Ceiling1", "Ceiling2"};
            String[] selectionArgs = {chfid};
            Cursor c = db.query("tblPolicyInquiry", columns, "Trim(CHFID)=?", selectionArgs, null, null, null);
            
            if (!c.moveToFirst()) {
                c.close();
                db.close();
                return null;
            }
            
            String name = c.getString(c.getColumnIndex("InsureeName"));
            String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            String dateOfBirthString = c.getString(c.getColumnIndex("DOB"));
            Date dateOfBirth = null;
            if (dateOfBirthString != null) {
                dateOfBirth = DateUtils.dateFromString(dateOfBirthString);
            }
            
            String gender = c.getString(c.getColumnIndex("Gender"));
            byte[] photo = c.getBlob(c.getColumnIndex("Photo"));
            
            List<Policy> policies = new ArrayList<>();
            do {
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
            } while (c.moveToNext());
            
            c.close();
            db.close();
            
            FamilyMember member = new FamilyMember(chfid, lastName, firstName, gender, dateOfBirth, photo, null, policies, "Chef de famille", true);
            List<FamilyMember> members = new ArrayList<>();
            members.add(member);
            return members;
        } catch (Exception e) {
            return null;
        }
    }

    @WorkerThread
    private void getInsureeInfo() {
        String chfid = etCHFID.getText().toString();

        
        // Ne pas appeler ClearForm() pour les tests car ils gèrent leur propre affichage
        {
            runOnUiThread(this::ClearForm);
        }
        
        if (global.isNetworkAvailable()) {

            try {
                // Utiliser la nouvelle méthode pour récupérer tous les membres de famille
                List<FamilyMember> familyMembers = new FetchInsureeInquire().executeFamilyMembers(chfid);

                runOnUiThread(() -> renderFamilyResult(familyMembers));
            } catch (HttpException e) {
                if (e.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    runOnUiThread(() -> showDialog(getResources().getString(R.string.RecordNotFound)));
                } else {
                    runOnUiThread(() -> showDialog(e.getMessage()));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showDialog(getResources().getString(R.string.UnknownError)));
            }
        } else {

            List<FamilyMember> familyMembers = getFamilyMembersFromDb(chfid);
            if (familyMembers == null) {
                Log.w(LOG_TAG, "Aucune donnée trouvée dans la base de données locale pour CHFID: " + chfid);
            } else {

            }
            runOnUiThread(() -> renderFamilyResult(familyMembers));
        }
    }

    public void renderFamilyResult(@Nullable List<FamilyMember> familyMembers) {
        if (familyMembers == null || familyMembers.isEmpty()) {
            showDialog(getResources().getString(R.string.RecordNotFound));
            return;
        }
        
        // CORRECTION: Déduplicquer les membres dès le début pour éviter les doublons dans l'affichage
        Map<String, FamilyMember> uniqueMembers = new LinkedHashMap<>();
        for (FamilyMember member : familyMembers) {
            String chfid = member.getChfId();
            if (chfid != null && !uniqueMembers.containsKey(chfid)) {
                uniqueMembers.put(chfid, member);
                
            } else {
                
            }
        }
        
        List<FamilyMember> deduplicatedFamilyMembers = new ArrayList<>(uniqueMembers.values());
        
        
        // Stocker les membres de famille dédupliqués pour utilisation ultérieure
        currentFamilyMembers = new ArrayList<>(deduplicatedFamilyMembers);

        // Afficher les informations du chef de famille
        FamilyMember headOfFamily = deduplicatedFamilyMembers.stream()
                .filter(FamilyMember::isHead)
                .findFirst()
                .orElse(deduplicatedFamilyMembers.get(0));

        if (!etCHFID.getText().toString().trim().equals(headOfFamily.getChfId()))
            return;

        llHeadInfo.setVisibility(View.VISIBLE);
        tvCHFID.setText(headOfFamily.getChfId());
        tvName.setText(headOfFamily.getFullName());
        TextViewUtils.setDate(tvDOB, headOfFamily.getDateOfBirth());
        tvGender.setText(headOfFamily.getGender());

        byte[] imageBytes = headOfFamily.getPhoto();
        if (imageBytes != null) {
            try {
                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                iv.setImageBitmap(image);
            } catch (Exception e) {
                iv.setImageDrawable(getResources().getDrawable(R.drawable.person));
            }
        } else if (headOfFamily.getPhotoPath() != null && global.isNetworkAvailable()) {
            iv.setImageResource(R.drawable.person);
            new Picasso.Builder(this).build()
                    .load(API_BASE_URL + REST_API_PREFIX + headOfFamily.getPhotoPath())
                    .placeholder(R.drawable.person)
                    .error(R.drawable.person)
                    .into(iv);
        } else {
            iv.setImageDrawable(getResources().getDrawable(R.drawable.person));
        }

        // Détecter la polygamie
        List<FamilyMember> polygamousHeads = detectPolygamousHeads(deduplicatedFamilyMembers);
        

        
        if (!polygamousHeads.isEmpty()) {
            // Si polygame : afficher UNIQUEMENT les chefs de sous-familles (épouses), PAS le chef principal
            cvPolygamousSection.setVisibility(View.VISIBLE);
            polygamousHeadAdapter.updatePolygamousHeads(polygamousHeads);
            rvFamilyMembers.setVisibility(View.GONE);
            familyMemberAdapter.updateFamilyMembers(new ArrayList<>()); // Vider l'adapter
        } else {
            // Si non polygame : afficher TOUS les membres de la famille

            cvPolygamousSection.setVisibility(View.GONE);
            rvFamilyMembers.setVisibility(View.VISIBLE);
            familyMemberAdapter.updateFamilyMembers(deduplicatedFamilyMembers);
        }
        

        
        llListView.setVisibility(View.VISIBLE);
        
        etCHFID.setText("");
    }
    
    private List<FamilyMember> detectPolygamousHeads(List<FamilyMember> familyMembers) {
        // Déduplicquer les membres basés sur leur CHFID
        Map<String, FamilyMember> uniqueMembers = new LinkedHashMap<>();
        for (FamilyMember member : familyMembers) {
            String chfid = member.getChfId();
            if (chfid != null && !uniqueMembers.containsKey(chfid)) {
                uniqueMembers.put(chfid, member);
            }
        }
        
        List<FamilyMember> deduplicatedMembers = new ArrayList<>(uniqueMembers.values());
        
        // Rechercher les épouses/époux avec différentes variantes possibles
        List<FamilyMember> spouses = deduplicatedMembers.stream()
                .filter(member -> {
                    String relationship = member.getRelationship();
                    String gender = member.getGender();
                    boolean isHead = member.isHead();
                    
                    // Si c'est le chef de famille, ce n'est pas une épouse
                    if (isHead) {
                        return false;
                    }
                    
                    if (relationship == null) {
                        relationship = "";
                    }
                    
                    // Normaliser la relation (enlever espaces, mettre en minuscules)
                    String normalizedRelation = relationship.trim().toLowerCase();
                    
                    // Vérifier différentes variantes possibles d'épouses
                    boolean isSpouse = normalizedRelation.equals("épouse") || 
                                     normalizedRelation.equals("epouse") ||
                                     normalizedRelation.equals("époux") ||
                                     normalizedRelation.equals("epoux") ||
                                     normalizedRelation.equals("wife") ||
                                     normalizedRelation.equals("husband") ||
                                     normalizedRelation.equals("spouse") ||
                                     normalizedRelation.contains("épouse") ||
                                     normalizedRelation.contains("époux") ||
                                     normalizedRelation.contains("wife") ||
                                     normalizedRelation.contains("husband");
                    
                    // Détecter les femmes adultes avec relation "Membre" ou vide
                    // comme épouses potentielles dans les familles polygames
                    if (!isSpouse && !isHead) {
                        // Vérifier différents formats de genre féminin
                        boolean isFemale = "F".equals(gender) || "Feminine".equals(gender) || "Female".equals(gender) || "f".equals(gender);
                        if (isFemale && (normalizedRelation.equals("membre") || normalizedRelation.isEmpty())) {
                            isSpouse = true;
                        }
                    }
                    
                    return isSpouse;
                })
                .collect(java.util.stream.Collectors.toList());
        
        // Une famille est polygame s'il y a AU MOINS DEUX épouses/époux détectées
        // Une famille avec une seule épouse est considérée comme monogame
        if (spouses.size() >= 2) {
            // Grouper les épouses par ParentId pour créer des sous-familles distinctes
            List<FamilyMember> subFamilyHeads = groupSpousesByParentId(spouses);
            
            return subFamilyHeads;
        } else {
            return new ArrayList<>(); // Retourner liste vide pour famille monogame
        }
    }
    
    /**
     * Groupe les épouses par ParentId pour créer des sous-familles distinctes.
     * Si des épouses ont le même ParentId, elles forment une sous-famille.
     * Chaque groupe de ParentId unique devient une sous-famille avec un chef représentatif.
     */
    private List<FamilyMember> groupSpousesByParentId(List<FamilyMember> spouses) {
        // Grouper les épouses par ParentId
        Map<String, List<FamilyMember>> spousesByParentId = new HashMap<>();
        
        for (FamilyMember spouse : spouses) {
            // Pour l'instant, utiliser le CHFID comme ParentId temporaire
            // Dans une vraie implémentation, il faudrait récupérer le ParentId de la base de données
            String parentId = getParentIdForSpouse(spouse);
            
            if (!spousesByParentId.containsKey(parentId)) {
                spousesByParentId.put(parentId, new ArrayList<>());
            }
            spousesByParentId.get(parentId).add(spouse);
        }
        
        // Créer une liste de chefs de sous-famille (un représentant par ParentId)
        List<FamilyMember> subFamilyHeads = new ArrayList<>();
        
        for (Map.Entry<String, List<FamilyMember>> entry : spousesByParentId.entrySet()) {
            String parentId = entry.getKey();
            List<FamilyMember> spousesInGroup = entry.getValue();
            
            // Prendre la première épouse du groupe comme chef de sous-famille représentatif
            FamilyMember representativeSpouse = spousesInGroup.get(0);
            subFamilyHeads.add(representativeSpouse);
        }
        
        return subFamilyHeads;
    }
    
    /**
     * Récupère le ParentId pour une épouse donnée.
     * Pour l'instant, retourne un ParentId simulé basé sur le nom de famille.
     * Dans une vraie implémentation, ceci devrait interroger la base de données.
     */
    private String getParentIdForSpouse(FamilyMember spouse) {
        // Simulation: utiliser le nom de famille comme ParentId
        // Les épouses avec le même nom de famille auront le même ParentId
        String lastName = spouse.getLastName();
        if (lastName != null && !lastName.isEmpty()) {
            return "PARENT_" + lastName.toUpperCase();
        }
        // Fallback: utiliser le CHFID comme ParentId unique
        return "PARENT_" + spouse.getChfId();
    }
    
    private void onPolygamousHeadClick(FamilyMember polygamousHead) {
        if (polygamousHead == null || currentFamilyMembers == null) {
            return;
        }
        
        // Lancer l'activité pour afficher les membres du sous-ménage
        Intent intent = new Intent(this, SubHouseholdActivity.class);
        intent.putExtra(SubHouseholdActivity.EXTRA_SUB_HEAD, polygamousHead);
        intent.putExtra(SubHouseholdActivity.EXTRA_ALL_MEMBERS, new ArrayList<>(currentFamilyMembers));
        startActivity(intent);
    }
    


    
    private void onFamilyMemberClick(FamilyMember familyMember) {
        // Vérifier si la famille est polygame
        List<FamilyMember> polygamousHeads = detectPolygamousHeads(currentFamilyMembers);
        boolean isPolygamousFamily = !polygamousHeads.isEmpty();
        
        if (isPolygamousFamily) {
            // Dans une famille polygame, permettre de cliquer sur le chef principal ou les épouses
            boolean isSubHouseholdHead = false;
            
            // Vérifier si c'est le chef principal
            if (familyMember.isHead()) {
                isSubHouseholdHead = true;
            } else {
                // Vérifier si c'est une épouse/époux
                String relationship = familyMember.getRelationship();
                if (relationship != null) {
                    String normalizedRelation = relationship.trim().toLowerCase();
                    isSubHouseholdHead = normalizedRelation.equals("épouse") || 
                                        normalizedRelation.equals("epouse") ||
                                        normalizedRelation.equals("époux") ||
                                        normalizedRelation.equals("epoux") ||
                                        normalizedRelation.equals("wife") ||
                                        normalizedRelation.equals("husband") ||
                                        normalizedRelation.equals("spouse") ||
                                        normalizedRelation.contains("épouse") ||
                                        normalizedRelation.contains("époux");
                }
            }
            
            if (isSubHouseholdHead) {
                Intent intent = new Intent(this, SubHouseholdActivity.class);
                intent.putExtra(SubHouseholdActivity.EXTRA_SUB_HEAD, familyMember);
                intent.putExtra(SubHouseholdActivity.EXTRA_ALL_MEMBERS, new ArrayList<>(currentFamilyMembers));
                startActivity(intent);
            }
        }
    }
    
    public void renderResult(@Nullable Insuree insuree) {
        if (insuree == null) {
            showDialog(getResources().getString(R.string.RecordNotFound));
            return;
        }

        llListView.setVisibility(View.VISIBLE);

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
            } catch (Exception e) {
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
                    DateUtils.toDateString(policy.getExpiryDate()) : null;
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
    }

    protected String buildEnquireValue(@Nullable Number value, @StringRes int labelId) {
        if (value == null) {
            return "";
        } else {
            String label = getResources().getString(labelId);
            return label + ": " + value;
        }
    }

    private void ClearForm() {
        tvCHFID.setText(getResources().getString(R.string.CHFID));
        tvName.setText(getResources().getString(R.string.InsureeName));
        tvDOB.setText(getResources().getString(R.string.DOB));
        tvGender.setText(getResources().getString(R.string.Gender));
        iv.setImageResource(R.drawable.noimage);
        llListView.setVisibility(View.INVISIBLE);
        llHeadInfo.setVisibility(View.INVISIBLE);
        llListView.setVisibility(View.INVISIBLE);
        rvFamilyMembers.setVisibility(View.INVISIBLE);
        cvPolygamousSection.setVisibility(View.GONE);
        lv.setAdapter(null);
        familyMemberAdapter.updateFamilyMembers(new ArrayList<>());
        polygamousHeadAdapter.updatePolygamousHeads(new ArrayList<>());
    }
}
