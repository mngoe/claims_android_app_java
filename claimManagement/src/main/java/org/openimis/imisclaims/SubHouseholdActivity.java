package org.openimis.imisclaims;

import static org.openimis.imisclaims.BuildConfig.API_BASE_URL;
import static org.openimis.imisclaims.BuildConfig.REST_API_PREFIX;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.openimis.imisclaims.adapter.FamilyMemberAdapter;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.domain.entity.PolygamousSubFamily;
import org.openimis.imisclaims.domain.entity.Policy;
import org.openimis.imisclaims.util.TextViewUtils;
import org.openimis.imisclaims.util.DateUtils;
import org.openimis.imisclaims.domain.entity.Insuree;
import org.openimis.imisclaims.usecase.FetchInsureeInquire;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SubHouseholdActivity extends AppCompatActivity {
    private static final String LOG_TAG = "SubHouseholdActivity";

    public static final String EXTRA_SUB_HEAD = "extra_sub_head";
    public static final String EXTRA_ALL_MEMBERS = "extra_all_members";

    private ImageView ivSubHeadPhoto;
    private TextView tvSubHeadName;
    private TextView tvSubHeadChfId;
    private TextView tvSubHeadGender;
    private TextView tvSubHeadDob;
    private ImageView ivInsuranceStatus;
    private TextView tvInsuranceStatus;
    private CardView cvInsuranceStatus;
    private RecyclerView rvSubHouseholdMembers;
    private TextView tvNoMembers;
    private ListView listView1;
    private LinearLayout llListView;
    private androidx.cardview.widget.CardView cvPolicyInfo;

    private FamilyMemberAdapter memberAdapter;
    private Global global;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_household);

        global = (Global) getApplicationContext();

        initializeViews();
        setupRecyclerView();
        loadDataFromIntent();
    }

    private void initializeViews() {
        ivSubHeadPhoto = findViewById(R.id.ivSubHeadPhoto);
        tvSubHeadName = findViewById(R.id.tvSubHeadName);
        tvSubHeadChfId = findViewById(R.id.tvSubHeadChfId);
        tvSubHeadGender = findViewById(R.id.tvSubHeadGender);
        tvSubHeadDob = findViewById(R.id.tvSubHeadDob);
        ivInsuranceStatus = findViewById(R.id.ivInsuranceStatus);
        tvInsuranceStatus = findViewById(R.id.tvInsuranceStatus);
        cvInsuranceStatus = findViewById(R.id.cvInsuranceStatus);
        rvSubHouseholdMembers = findViewById(R.id.rvSubHouseholdMembers);
        tvNoMembers = findViewById(R.id.tvNoMembers);
        listView1 = findViewById(R.id.listView1);
        llListView = findViewById(R.id.llListView);
        cvPolicyInfo = findViewById(R.id.cvPolicyInfo);
        
        Log.d(LOG_TAG, "Vues initialisées pour SubHouseholdActivity");
    }

    private void setupRecyclerView() {
        rvSubHouseholdMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new FamilyMemberAdapter(this, new ArrayList<>());
        rvSubHouseholdMembers.setAdapter(memberAdapter);
    }

    private void loadDataFromIntent() {
        // Vérifier si l'Intent contient les extras
        if (getIntent().hasExtra(EXTRA_SUB_HEAD) && getIntent().hasExtra(EXTRA_ALL_MEMBERS)) {
            PolygamousSubFamily subHead = (PolygamousSubFamily) getIntent().getSerializableExtra(EXTRA_SUB_HEAD);
            ArrayList<FamilyMember> allMembers = (ArrayList<FamilyMember>) getIntent().getSerializableExtra(EXTRA_ALL_MEMBERS);

            if (subHead != null && subHead.getChfId() != null && subHead.getFullName() != null) {
                displaySubHeadInfo(subHead);
                displayPolicyInfo(subHead);
                loadSubHouseholdMembers(subHead, allMembers);
            } else {
                finish();
            }
        } else {
            Log.e(LOG_TAG, "Données manquantes dans l'Intent");
            finish();
        }
    }

    private void displaySubHeadInfo(PolygamousSubFamily subHead) {
        Log.d(LOG_TAG, "Affichage des informations du chef de sous-famille: " + subHead.getOtherNames() + " " + subHead.getLastName());

        // Nom complet
        String fullName = (subHead.getOtherNames() != null ? subHead.getOtherNames() + " " : "") + 
                         (subHead.getLastName() != null ? subHead.getLastName() : "");
        tvSubHeadName.setText(fullName.trim());

        // CHFID
        tvSubHeadChfId.setText(subHead.getChfId() != null ? subHead.getChfId() : "N/A");

        // Genre
        String gender = subHead.getGender();
        if ("M".equals(gender)) {
            tvSubHeadGender.setText("Masculin");
        } else if ("F".equals(gender)) {
            tvSubHeadGender.setText("Féminin");
        } else {
            tvSubHeadGender.setText(gender != null ? gender : "N/A");
        }

        // Date de naissance
        tvSubHeadDob.setText(subHead.getDob() != null ? subHead.getDob() : "N/A");

        // Photo
        loadSubHeadPhoto(subHead);
        
        // Statut du contrat d'assurance
        displayInsuranceStatus(subHead);
    }
    
    private void displayInsuranceStatus(PolygamousSubFamily subHead) {
        String detailedStatus = getDetailedInsuranceStatus(subHead);
        boolean hasActiveContract = isInsuranceStatusActive(detailedStatus);
        
        Log.d(LOG_TAG, "Statut du contrat d'assurance pour " + subHead.getChfId() + ": " + hasActiveContract + " - " + detailedStatus);
        
        if (hasActiveContract) {
            // Design pour contrat actif - Vert
            cvInsuranceStatus.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            ivInsuranceStatus.setImageResource(android.R.drawable.ic_dialog_info);
            ivInsuranceStatus.setColorFilter(getResources().getColor(android.R.color.white));
            tvInsuranceStatus.setText(detailedStatus.isEmpty() ? "Contrat d'assurance actif" : detailedStatus);
            tvInsuranceStatus.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            // Design pour contrat inactif - Rouge
            cvInsuranceStatus.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            ivInsuranceStatus.setImageResource(android.R.drawable.ic_dialog_alert);
            ivInsuranceStatus.setColorFilter(getResources().getColor(android.R.color.white));
            tvInsuranceStatus.setText(detailedStatus.isEmpty() ? "Aucun contrat d'assurance actif" : detailedStatus);
            tvInsuranceStatus.setTextColor(getResources().getColor(android.R.color.white));
        }
    }
    
    /**
     * Détermine si le statut d'assurance indique un contrat actif
     * @param statusText Le texte du statut retourné par getDetailedInsuranceStatus
     * @return true si le contrat est actif, false sinon
     */
    private boolean isInsuranceStatusActive(String statusText) {
        if (statusText == null || statusText.trim().isEmpty()) {
            return false;
        }
        
        String status = statusText.toLowerCase().trim();
        return status.contains("actif") || status.contains("active") || status.contains("valide");
    }
    
    /**
     * Récupère le statut détaillé d'assurance du chef de sous-famille
     * @param subHead Le chef de sous-famille
     * @return Le statut détaillé avec informations sur la police
     */
    private String getDetailedInsuranceStatus(PolygamousSubFamily subHead) {
        if (subHead.getChfId() == null || subHead.getChfId().trim().isEmpty()) {
            return "CHFID manquant";
        }
        
        try {
            Log.d(LOG_TAG, "Récupération du statut d'assurance depuis le serveur pour: " + subHead.getChfId());
            
            // Utiliser FetchInsureeInquire pour récupérer les données depuis le serveur
            FetchInsureeInquire fetchInsureeInquire = new FetchInsureeInquire();
            Insuree insuree = fetchInsureeInquire.execute(subHead.getChfId());
            
            // Récupérer la police la plus récente (active ou la plus récente)
            Policy activePolicy = null;
            Policy mostRecentPolicy = null;
            
            for (Policy policy : insuree.getPolicies()) {
                if (policy.getStatus() == Policy.Status.ACTIVE) {
                    activePolicy = policy;
                    break;
                }
                if (mostRecentPolicy == null || 
                    (policy.getExpiryDate() != null && mostRecentPolicy.getExpiryDate() != null &&
                     policy.getExpiryDate().compareTo(mostRecentPolicy.getExpiryDate()) > 0)) {
                    mostRecentPolicy = policy;
                }
            }
            
            Policy policyToShow = activePolicy != null ? activePolicy : mostRecentPolicy;
            
            if (policyToShow != null) {
                String statusText = getStatusText(policyToShow.getStatus());
                if (policyToShow.getExpiryDate() != null) {
                    statusText += " (expire: " + DateUtils.toExpiryDateString(policyToShow.getExpiryDate()) + ")";
                }
                if (policyToShow.getName() != null && !policyToShow.getName().isEmpty()) {
                    statusText += " - " + policyToShow.getName();
                }
                Log.d(LOG_TAG, "Statut d'assurance récupéré depuis le serveur: " + statusText);
                return statusText;
            } else {
                Log.d(LOG_TAG, "Aucune police trouvée pour " + subHead.getChfId());
                return "Aucune police d'assurance";
            }
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la récupération du statut d'assurance depuis le serveur", e);
            // En cas d'erreur serveur, essayer de récupérer depuis la base locale comme fallback
            return getLocalInsuranceStatus(subHead);
        }
    }
    
    private String getStatusText(Policy.Status status) {
        switch (status) {
            case ACTIVE: return "Actif";
            case IDLE: return "Inactif";
            case SUSPENDED: return "Suspendu";
            case EXPIRED: return "Expiré";
            case READY: return "Prêt";
            default: return "Inconnu";
        }
    }
    
    private String getLocalInsuranceStatus(PolygamousSubFamily subHead) {
        try {
            SQLHandler sqlHandler = new SQLHandler(this);
            Log.d(LOG_TAG, "Création des tables de la base de données...");
            sqlHandler.createTables();
            SQLiteDatabase db = sqlHandler.getReadableDatabase();
            
            // Vérifier si la table existe
            Cursor tableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tblPolicyInquiry'", null);
            Log.d(LOG_TAG, "Vérification de l'existence de tblPolicyInquiry: " + tableCheck.getCount() + " résultat(s)");
            
            if (tableCheck.getCount() == 0) {
                tableCheck.close();
                return "Table tblPolicyInquiry manquante";
            }
            tableCheck.close();
            Log.d(LOG_TAG, "Table tblPolicyInquiry trouvée, recherche des données...");
            
            String query = "SELECT Status, ExpiryDate, ProductName FROM tblPolicyInquiry WHERE InsureeNumber = ? ORDER BY ExpiryDate DESC LIMIT 1";
            Cursor cursor = db.rawQuery(query, new String[]{subHead.getChfId()});
            
            String statusText = "";
            if (cursor.moveToFirst()) {
                String policyStatus = cursor.getString(cursor.getColumnIndexOrThrow("Status"));
                String expiryDate = cursor.getString(cursor.getColumnIndexOrThrow("ExpiryDate"));
                String productName = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                
                if (policyStatus != null) {
                    statusText = policyStatus;
                    if (expiryDate != null && !expiryDate.isEmpty()) {
                        statusText += " (expire: " + DateUtils.formatExpiryDateString(expiryDate) + ")";
                    }
                    if (productName != null && !productName.isEmpty()) {
                        statusText += " - " + productName;
                    }
                }
            } else {
                statusText = "Aucune police trouvée";
            }
            
            cursor.close();
            db.close();
            sqlHandler.close();
            
            return statusText;
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la récupération du statut détaillé: " + e.getMessage());
            return "Erreur de vérification";
        }
    }

    private void loadSubHeadPhoto(PolygamousSubFamily subHead) {
        if (subHead.getPhoto() != null && !subHead.getPhoto().isEmpty()) {
            try {
                // Décoder la photo depuis base64
                byte[] decodedString = android.util.Base64.decode(subHead.getPhoto(), android.util.Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivSubHeadPhoto.setImageBitmap(decodedByte);
            } catch (Exception e) {
                ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
            }
        } else {
            // Photo par défaut
            ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    private void loadSubHouseholdMembers(PolygamousSubFamily subHead, ArrayList<FamilyMember> allMembers) {
        List<FamilyMember> subHouseholdMembers = subHead.getMembers();
        Log.d(LOG_TAG, "Chargement des membres pour " + subHead.getChfId() + ": " + 
               (subHouseholdMembers != null ? subHouseholdMembers.size() + " membres directs" : "aucun membre direct"));
        
        if (subHouseholdMembers != null && !subHouseholdMembers.isEmpty()) {
            Log.d(LOG_TAG, "Affichage de " + subHouseholdMembers.size() + " membres directs");
            displayMembers(subHouseholdMembers);
        } else {
            if (allMembers == null || allMembers.isEmpty()) {
                showNoMembersMessage();
                return;
            }

            // Filtrer les membres qui appartiennent à cette sous-famille
            List<FamilyMember> filteredMembers = new ArrayList<>();
            for (FamilyMember member : allMembers) {
                if (member.getLastName() != null && 
                    member.getLastName().equals(subHead.getLastName()) &&
                    !member.getChfId().equals(subHead.getChfId())) { // Exclure le chef lui-même
                    filteredMembers.add(member);
                }
            }

            if (filteredMembers.isEmpty()) {
                showNoMembersMessage();
            } else {
                displayMembers(filteredMembers);
            }
        }
    }

    private void showNoMembersMessage() {
        rvSubHouseholdMembers.setVisibility(View.GONE);
        tvNoMembers.setVisibility(View.VISIBLE);
        tvNoMembers.setText("Aucun membre dans cette sous-famille");
    }

    private void displayMembers(List<FamilyMember> members) {
        Log.d(LOG_TAG, "displayMembers appelée avec " + (members != null ? members.size() : 0) + " membres");
        
        rvSubHouseholdMembers.setVisibility(View.VISIBLE);
        tvNoMembers.setVisibility(View.GONE);
        
        Log.d(LOG_TAG, "Mise à jour de l'adapter avec " + (members != null ? members.size() : 0) + " membres");
        memberAdapter.updateData(members);
    }

    private void displayPolicyInfo(PolygamousSubFamily subHead) {
        Log.d(LOG_TAG, "Affichage des informations de police pour CHFID: " + subHead.getChfId());
        
        try {
            SQLHandler sqlHandler = new SQLHandler(this);
        // S'assurer que les tables sont créées
        sqlHandler.createTables();
        SQLiteDatabase db = sqlHandler.getReadableDatabase();
        
        // Vérifier si la table existe
        Cursor tableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tblPolicyInquiry'", null);
        if (tableCheck.getCount() == 0) {
            Log.e(LOG_TAG, "Table tblPolicyInquiry n'existe pas!");
            tableCheck.close();
            return;
        }
        tableCheck.close();
        Log.d(LOG_TAG, "Table tblPolicyInquiry trouvée, exécution de la requête");
            
            String query = "SELECT * FROM tblPolicyInquiry WHERE InsureeNumber = ?";
            Log.d(LOG_TAG, "Recherche des polices pour CHFID: " + subHead.getChfId());
            Cursor cursor = db.rawQuery(query, new String[]{subHead.getChfId()});
            
            Log.d(LOG_TAG, "Nombre de résultats trouvés pour sous-famille: " + cursor.getCount());
            
            // Si aucun résultat avec le CHFID de la sous-famille, essayer avec le chef de famille principal
            if (cursor.getCount() == 0 && subHead.getParentUuid() != null) {
                cursor.close();
                
                // Récupérer le CHFID du chef de famille principal
                String parentQuery = "SELECT CHFID FROM tblInsuree WHERE InsureeUUID = ?";
                Cursor parentCursor = db.rawQuery(parentQuery, new String[]{subHead.getParentUuid()});
                
                if (parentCursor.moveToFirst()) {
                    String parentChfId = parentCursor.getString(0);
                    Log.d(LOG_TAG, "Recherche des polices pour le chef principal CHFID: " + parentChfId);
                    parentCursor.close();
                    
                    String parentPolicyQuery = "SELECT * FROM tblPolicyInquiry WHERE InsureeNumber = ?";
                    cursor = db.rawQuery(parentPolicyQuery, new String[]{parentChfId});
                    Log.d(LOG_TAG, "Nombre de résultats trouvés pour chef principal: " + cursor.getCount());
                } else {
                    parentCursor.close();
                }
            }
            
            ArrayList<Map<String, String>> PolicyList = new ArrayList<>();
            
            while (cursor.moveToNext()) {
                // Récupération directe des données depuis le curseur
                String policyId = cursor.getString(cursor.getColumnIndexOrThrow("PolicyId"));
                String productName = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                String expiryDateStr = cursor.getString(cursor.getColumnIndexOrThrow("ExpiryDate"));
                String policyStatus = cursor.getString(cursor.getColumnIndexOrThrow("PolicyStatus"));
                
                // Récupération des données de déduction et plafond
                String ded1 = cursor.getString(cursor.getColumnIndexOrThrow("DedType1"));
                String ded2 = cursor.getString(cursor.getColumnIndexOrThrow("DedType2"));
                String ceiling1 = cursor.getString(cursor.getColumnIndexOrThrow("CeilingType1"));
                String ceiling2 = cursor.getString(cursor.getColumnIndexOrThrow("CeilingType2"));
                
                String ded = "";
                String ceiling = "";
                
                if (ded1 != null || ded2 != null) {
                    ded = "Deduction: " + (ded1 != null ? ded1 : "") + (ded2 != null ? ded2 : "");
                }
                if (ceiling1 != null || ceiling2 != null) {
                    ceiling = "Ceiling: " + (ceiling1 != null ? ceiling1 : "") + (ceiling2 != null ? ceiling2 : "");
                }
                
                Map<String, String> policyMap = new HashMap<>();
                
                Log.d(LOG_TAG, "Processing policy: " + productName + " (" + policyId + ")");
                
                String heading1;
                if (expiryDateStr != null && !expiryDateStr.isEmpty()) {
                    String formattedExpiryDate = DateUtils.formatExpiryDateString(expiryDateStr);
                    heading1 = formattedExpiryDate + " " + (policyStatus != null ? policyStatus : "");
                } else {
                    heading1 = policyStatus != null ? policyStatus : "";
                }
                
                policyMap.put("Heading", policyId != null ? policyId : "");
                policyMap.put("Heading1", heading1);
                policyMap.put("SubItem1", productName != null ? productName : "");
                policyMap.put("SubItem2", ded);
                policyMap.put("SubItem3", ceiling);
                
                // Ajout des autres informations si disponibles
                policyMap.put("SubItem4", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("TotalAdmissionsLeft")), R.string.totalAdmissionsLeft));
                policyMap.put("SubItem5", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("TotalVisitsLeft")), R.string.totalVisitsLeft));
                policyMap.put("SubItem6", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("TotalConsultationsLeft")), R.string.totalConsultationsLeft));
                policyMap.put("SubItem7", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("TotalSurgeriesLeft")), R.string.totalSurgeriesLeft));
                policyMap.put("SubItem8", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("TotalDeliveriesLeft")), R.string.totalDeliveriesLeft));
                policyMap.put("SubItem9", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("TotalAntenatalLeft")), R.string.totalAntenatalLeft));
                policyMap.put("SubItem10", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("ConsultationAmountLeft")), R.string.consultationAmountLeft));
                policyMap.put("SubItem11", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("SurgeryAmountLeft")), R.string.surgeryAmountLeft));
                policyMap.put("SubItem12", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("HospitalizationAmountLeft")), R.string.hospitalizationAmountLeft));
                policyMap.put("SubItem13", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("AntenatalAmountLeft")), R.string.antenatalAmountLeft));
                policyMap.put("SubItem14", buildEnquireValue(cursor.getString(cursor.getColumnIndexOrThrow("DeliveryAmountLeft")), R.string.deliveryAmountLeft));
                
                PolicyList.add(policyMap);
                Log.d(LOG_TAG, "Added policy to list. Total policies: " + PolicyList.size());
            }
            
            cursor.close();
            db.close();
            sqlHandler.close();
            
            Log.d(LOG_TAG, "Initializing views - listView1: " + (listView1 != null ? "OK" : "NULL") + ", cvPolicyInfo: " + (cvPolicyInfo != null ? "OK" : "NULL"));
            

             
             Log.d(LOG_TAG, "Nombre de politiques trouvées: " + PolicyList.size());
             if (!PolicyList.isEmpty()) {
                 Log.d(LOG_TAG, "Configuration de l'affichage des politiques d'assurance");
                 
                 if (listView1 != null && cvPolicyInfo != null && llListView != null) {
                     // Configuration de l'adaptateur
                     ListAdapter adapter = new SimpleAdapter(SubHouseholdActivity.this,
                             PolicyList, R.layout.policylist,
                             new String[]{"Heading", "Heading1", "SubItem1", "SubItem2", "SubItem3", "SubItem4", "SubItem5", "SubItem6", "SubItem7", "SubItem8", "SubItem9", "SubItem10", "SubItem11", "SubItem12", "SubItem13", "SubItem14"},
                             new int[]{R.id.tvHeading, R.id.tvHeading1, R.id.tvSubItem1, R.id.tvSubItem2, R.id.tvSubItem3, R.id.tvSubItem4, R.id.tvSubItem5, R.id.tvSubItem6, R.id.tvSubItem7, R.id.tvSubItem8, R.id.tvSubItem9, R.id.tvSubItem10, R.id.tvSubItem11, R.id.tvSubItem12, R.id.tvSubItem13, R.id.tvSubItem14}
                     );
                     listView1.setAdapter(adapter);
                     
                     // Affichage des sections des polices (comme dans EnquireActivity)
                     llListView.setVisibility(View.VISIBLE);
                     cvPolicyInfo.setVisibility(View.VISIBLE);
                     Log.d(LOG_TAG, "Sections des politiques affichées avec " + PolicyList.size() + " éléments");
                 } else {
                     Log.e(LOG_TAG, "Erreur: Vues non initialisées - listView1: " + (listView1 != null) + ", cvPolicyInfo: " + (cvPolicyInfo != null) + ", llListView: " + (llListView != null));
                 }
             } else {
                 Log.d(LOG_TAG, "No policy records found, hiding policy sections");
                 if (cvPolicyInfo != null) {
                     cvPolicyInfo.setVisibility(View.GONE);
                 }
                 if (llListView != null) {
                     llListView.setVisibility(View.GONE);
                 }
             }
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la récupération des informations de police: " + e.getMessage());
            if (cvPolicyInfo != null) {
                cvPolicyInfo.setVisibility(View.GONE);
            }
            if (llListView != null) {
                llListView.setVisibility(View.GONE);
            }
        }
    }
    
    private String buildEnquireValue(String value, int stringResourceId) {
        if (value != null && !value.trim().isEmpty() && !value.equals("null")) {
            return getString(stringResourceId) + ": " + value;
        }
        return "";
    }
}