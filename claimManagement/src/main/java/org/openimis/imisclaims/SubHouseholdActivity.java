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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.openimis.imisclaims.adapter.FamilyMemberAdapter;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.domain.entity.PolygamousSubFamily;
import org.openimis.imisclaims.domain.entity.Policy;
import org.openimis.imisclaims.network.GetFamilyMembersGraphQLRequest;
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

    public static final String EXTRA_CHF_ID = "extra_chf_id";
    public static final String EXTRA_FAMILY_UUID = "extra_family_uuid";
    
    // Database helper
    private SQLHandler sqlHandler;
    private SQLiteDatabase db;

    private ImageView ivSubHeadPhoto;
    private TextView tvSubHeadLastName;
    private TextView tvSubHeadFirstName;
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
        tvSubHeadLastName = findViewById(R.id.tvSubHeadLastName);
        tvSubHeadFirstName = findViewById(R.id.tvSubHeadFirstName);
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
        

    }

    private void setupRecyclerView() {
        rvSubHouseholdMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new FamilyMemberAdapter(this, new ArrayList<>());
        rvSubHouseholdMembers.setAdapter(memberAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
    

    private void loadDataFromIntent() {
        try {
            // Initialize database helper
            sqlHandler = new SQLHandler(this);
            sqlHandler.createTables();
            db = sqlHandler.getReadableDatabase();
            
            // Check if Intent contains the required extras
            if (getIntent() == null) {
                Log.e(LOG_TAG, "L'intent est null");
                showErrorAndFinish("Erreur: Données manquantes");
                return;
            }
            
            String chfId = getIntent().getStringExtra(EXTRA_CHF_ID);
            Log.d(LOG_TAG, "🔍 CHFID récupéré de l'Intent: '" + chfId + "'");
            
            // Log des données supplémentaires de l'Intent
            String lastName = getIntent().getStringExtra("EXTRA_LAST_NAME");
            String otherNames = getIntent().getStringExtra("EXTRA_OTHER_NAMES");
            String gender = getIntent().getStringExtra("EXTRA_GENDER");
            String dob = getIntent().getStringExtra("EXTRA_DOB");
            String familyUuid = getIntent().getStringExtra(EXTRA_FAMILY_UUID);
            
            Log.d(LOG_TAG, "📋 Données Intent - LastName: '" + lastName + "', OtherNames: '" + otherNames + "'");
            Log.d(LOG_TAG, "📋 Données Intent - Gender: '" + gender + "', DOB: '" + dob + "'");
            Log.d(LOG_TAG, "📋 Données Intent - FamilyUuid: '" + familyUuid + "'");
            
            if (chfId == null || chfId.trim().isEmpty()) {
                Log.e(LOG_TAG, "ID CHF manquant ou vide");
                showErrorAndFinish("Erreur: Identifiant du chef de sous-famille manquant");
                return;
            }
            
            // Récupération des données du chef de famille depuis la base de données
            Log.d(LOG_TAG, "🔄 Tentative de récupération depuis la base de données...");
            PolygamousSubFamily subHead = fetchSubHeadFromDatabase(chfId);
            if (subHead == null) {
                Log.e(LOG_TAG, "❌ Impossible de trouver le chef de famille avec l'ID CHF: " + chfId);
                Log.d(LOG_TAG, "🔄 Tentative de création depuis l'Intent comme solution de secours...");
                subHead = createSubHeadFromIntent(chfId);
                if (subHead == null) {
                    showErrorAndFinish("Erreur: Impossible de trouver les détails du chef de sous-famille");
                    return;
                }
            }
            
            // Log des données récupérées
            Log.d(LOG_TAG, "✅ Chef de sous-famille récupéré:");
            Log.d(LOG_TAG, "   - CHFID: '" + subHead.getChfId() + "'");
            Log.d(LOG_TAG, "   - LastName: '" + subHead.getLastName() + "'");
            Log.d(LOG_TAG, "   - OtherNames: '" + subHead.getOtherNames() + "'");
            Log.d(LOG_TAG, "   - Gender: '" + subHead.getGender() + "'");
            Log.d(LOG_TAG, "   - DOB: '" + subHead.getDob() + "'");
            Log.d(LOG_TAG, "   - FamilyUuid: '" + subHead.getFamilyUuid() + "'");
            
            // Fetch family members
            List<FamilyMember> familyMembers = fetchFamilyMembers(familyUuid != null ? familyUuid : subHead.getFamilyUuid());
            
            // Load data if everything is valid
            displaySubHeadInfo(subHead);
            displayPolicyInfo(subHead);
            loadSubHouseholdMembers(subHead, new ArrayList<>(familyMembers));
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur inattendue dans loadDataFromIntent", e);
            showErrorAndFinish("Erreur inattendue lors du chargement des données");
        }
    }
    
    private boolean doesTableExist(SQLiteDatabase db, String tableName) {
        if (db == null || !db.isOpen()) {
            return false;
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", 
                              new String[]{tableName});
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la vérification de l'existence de la table " + tableName, e);
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private PolygamousSubFamily fetchSubHeadFromDatabase(String chfId) {
        if (db == null || !db.isOpen()) {
            Log.e(LOG_TAG, "La base de données n'est pas ouverte");
            return null;
        }

        // Vérifier d'abord si la table tblPolicyInquiry existe
        if (!doesTableExist(db, "tblPolicyInquiry")) {
            Log.w(LOG_TAG, "La table tblPolicyInquiry n'existe pas, tentative avec tblInsuree");
            return fetchFromTblInsuree(chfId);
        }

        Cursor cursor = null;
        try {
            cursor = db.query(
                "tblPolicyInquiry",
                new String[]{"InsureeNumber", "InsureeName", "Gender", "DOB", "Photo"},
                "TRIM(InsureeNumber) = ?",
                new String[]{chfId},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                return createSubHeadFromCursor(cursor);
            }
            
            // Si non trouvé dans tblPolicyInquiry, essayer tblInsuree
            Log.d(LOG_TAG, "Aucun résultat dans tblPolicyInquiry, tentative avec tblInsuree");
            return fetchFromTblInsuree(chfId);
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la récupération du chef de famille depuis tblPolicyInquiry", e);
            // En cas d'erreur, essayer avec tblInsuree
            return fetchFromTblInsuree(chfId);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    private PolygamousSubFamily fetchFromTblInsuree(String chfId) {
        if (!doesTableExist(db, "tblInsuree")) {
            Log.e(LOG_TAG, "La table tblInsuree n'existe pas non plus");
            // Créer un objet basique avec les données disponibles de l'Intent
            return createSubHeadFromIntent(chfId);
        }
        
        Cursor cursor = null;
        try {
            cursor = db.query(
                "tblInsuree",
                new String[]{"CHFID", "LastName", "OtherNames", "Gender", "DOB", "PhotoPath"},
                "TRIM(CHFID) = ?",
                new String[]{chfId},
                null, null, null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                return createSubHeadFromTblInsuree(cursor);
            }
            
            Log.e(LOG_TAG, "Aucun résultat dans tblInsuree pour l'ID CHF: " + chfId);
            // Créer un objet depuis l'Intent comme solution de secours
            return createSubHeadFromIntent(chfId);
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la récupération depuis tblInsuree", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    private PolygamousSubFamily createSubHeadFromIntent(String chfId) {
        try {
            PolygamousSubFamily subHead = new PolygamousSubFamily();
            subHead.setChfId(chfId);
            
            // Récupérer les données supplémentaires de l'Intent si disponibles
            String lastName = getIntent().getStringExtra("EXTRA_LAST_NAME");
            String otherNames = getIntent().getStringExtra("EXTRA_OTHER_NAMES");
            String gender = getIntent().getStringExtra("EXTRA_GENDER");
            String dob = getIntent().getStringExtra("EXTRA_DOB");
            String familyUuid = getIntent().getStringExtra(EXTRA_FAMILY_UUID);
            
            // CORRECTION CRITIQUE: Définir le FamilyUuid pour permettre le filtrage correct
            if (familyUuid != null && !familyUuid.isEmpty()) {
                subHead.setFamilyUuid(familyUuid);
                Log.d(LOG_TAG, "FamilyUuid défini depuis l'Intent: " + familyUuid);
            } else {
                Log.w(LOG_TAG, "FamilyUuid manquant dans l'Intent - le filtrage utilisera LastName");
            }
            
            // Utiliser les vraies données de l'Intent
            if (lastName != null && !lastName.isEmpty()) {
                subHead.setLastName(lastName);
                Log.d(LOG_TAG, "✅ LastName récupéré de l'Intent: '" + lastName + "'");
            } else {
                subHead.setLastName("Chef de sous-famille"); // Valeur par défaut
                Log.w(LOG_TAG, "⚠️ LastName manquant dans l'Intent, utilisation de la valeur par défaut");
            }
            
            if (otherNames != null && !otherNames.isEmpty()) {
                subHead.setOtherNames(otherNames);
                Log.d(LOG_TAG, "✅ OtherNames récupéré de l'Intent: '" + otherNames + "'");
            } else {
                Log.w(LOG_TAG, "⚠️ OtherNames manquant dans l'Intent");
            }
            
            if (gender != null && !gender.isEmpty()) {
                subHead.setGender(gender);
                Log.d(LOG_TAG, "✅ Gender récupéré de l'Intent: '" + gender + "'");
            } else {
                Log.w(LOG_TAG, "⚠️ Gender manquant dans l'Intent");
            }
            
            if (dob != null && !dob.isEmpty()) {
                subHead.setDob(dob);
                Log.d(LOG_TAG, "✅ DOB récupéré de l'Intent: '" + dob + "'");
            } else {
                Log.w(LOG_TAG, "⚠️ DOB manquant dans l'Intent");
            }
            
            if (gender != null && !gender.isEmpty()) {
                subHead.setGender(gender);
            }
            
            if (dob != null && !dob.isEmpty()) {
                subHead.setDob(dob);
            }
            
            Log.d(LOG_TAG, "Objet PolygamousSubFamily créé depuis l'Intent avec CHFID: " + chfId);
            return subHead;
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la création de l'objet depuis l'Intent", e);
            // Créer un objet minimal en cas d'erreur
            PolygamousSubFamily subHead = new PolygamousSubFamily();
            subHead.setChfId(chfId);
            subHead.setLastName("Chef de sous-famille");
            
            // Même en cas d'erreur, essayer de définir le FamilyUuid
            String familyUuid = getIntent().getStringExtra(EXTRA_FAMILY_UUID);
            if (familyUuid != null && !familyUuid.isEmpty()) {
                subHead.setFamilyUuid(familyUuid);
            }
            
            return subHead;
        }
    }
    
    private PolygamousSubFamily createSubHeadFromCursor(Cursor cursor) {
        try {
            PolygamousSubFamily subHead = new PolygamousSubFamily();
            subHead.setChfId(cursor.getString(cursor.getColumnIndexOrThrow("InsureeNumber")));
            
            // Traiter le nom complet depuis InsureeName
            String fullName = cursor.getString(cursor.getColumnIndexOrThrow("InsureeName"));
            if (fullName != null && !fullName.trim().isEmpty()) {
                // Séparer le nom complet en prénom et nom de famille
                String[] nameParts = fullName.trim().split("\\s+", 2);
                if (nameParts.length >= 2) {
                    subHead.setOtherNames(nameParts[0]); // Premier mot = prénom
                    subHead.setLastName(nameParts[1]);   // Reste = nom de famille
                } else {
                    subHead.setOtherNames(fullName);     // Si un seul mot, considérer comme prénom
                    subHead.setLastName("");
                }
                Log.d(LOG_TAG, "Nom traité - Prénom: '" + subHead.getOtherNames() + "', Nom: '" + subHead.getLastName() + "'");
            } else {
                subHead.setOtherNames("N/A");
                subHead.setLastName("N/A");
                Log.w(LOG_TAG, "InsureeName vide ou null");
            }
            
            // CORRECTION: Définir le FamilyUuid depuis l'Intent
            String familyUuid = getIntent().getStringExtra(EXTRA_FAMILY_UUID);
            if (familyUuid != null && !familyUuid.isEmpty()) {
                subHead.setFamilyUuid(familyUuid);
                Log.d(LOG_TAG, "FamilyUuid défini depuis l'Intent (tblPolicyInquiry): " + familyUuid);
            }
            
            // Gestion des champs optionnels
            int genderIndex = cursor.getColumnIndex("Gender");
            if (genderIndex != -1) {
                subHead.setGender(cursor.getString(genderIndex));
            }
            
            int dobIndex = cursor.getColumnIndex("DOB");
            if (dobIndex != -1) {
                subHead.setDob(cursor.getString(dobIndex));
            }
            
            // Gestion de la photo si disponible
            int photoIndex = cursor.getColumnIndex("Photo");
            if (photoIndex != -1 && !cursor.isNull(photoIndex)) {
                byte[] photoBlob = cursor.getBlob(photoIndex);
                if (photoBlob != null) {
                    subHead.setPhotoData(android.util.Base64.encodeToString(photoBlob, android.util.Base64.DEFAULT));
                }
            }
            
            return subHead;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la création de l'objet PolygamousSubFamily", e);
            return null;
        }
    }
    
    private PolygamousSubFamily createSubHeadFromTblInsuree(Cursor cursor) {
        try {
            PolygamousSubFamily subHead = new PolygamousSubFamily();
            subHead.setChfId(cursor.getString(cursor.getColumnIndexOrThrow("CHFID")));
            
            // CORRECTION: Définir le FamilyUuid depuis l'Intent
            String familyUuid = getIntent().getStringExtra(EXTRA_FAMILY_UUID);
            if (familyUuid != null && !familyUuid.isEmpty()) {
                subHead.setFamilyUuid(familyUuid);
                Log.d(LOG_TAG, "FamilyUuid défini depuis l'Intent (tblInsuree): " + familyUuid);
            }
            
            // Définir LastName et OtherNames séparément
            int lastNameIndex = cursor.getColumnIndex("LastName");
            if (lastNameIndex != -1) {
                String lastName = cursor.getString(lastNameIndex);
                subHead.setLastName(lastName != null && !lastName.trim().isEmpty() ? lastName.trim() : "N/A");
                Log.d(LOG_TAG, "Nom de famille récupéré: '" + subHead.getLastName() + "'");
            } else {
                subHead.setLastName("N/A");
                Log.w(LOG_TAG, "Colonne LastName non trouvée");
            }
            
            int otherNamesIndex = cursor.getColumnIndex("OtherNames");
            if (otherNamesIndex != -1) {
                String otherNames = cursor.getString(otherNamesIndex);
                subHead.setOtherNames(otherNames != null && !otherNames.trim().isEmpty() ? otherNames.trim() : "N/A");
                Log.d(LOG_TAG, "Prénom récupéré: '" + subHead.getOtherNames() + "'");
            } else {
                subHead.setOtherNames("N/A");
                Log.w(LOG_TAG, "Colonne OtherNames non trouvée");
            }
            
            int genderIndex = cursor.getColumnIndex("Gender");
            if (genderIndex != -1) {
                subHead.setGender(cursor.getString(genderIndex));
            }
            
            int dobIndex = cursor.getColumnIndex("DOB");
            if (dobIndex != -1) {
                subHead.setDob(cursor.getString(dobIndex));
            }
            
            // Gestion de la photo si disponible
            int photoIndex = cursor.getColumnIndex("PhotoPath");
            if (photoIndex != -1 && !cursor.isNull(photoIndex)) {
                String photoPath = cursor.getString(photoIndex);
                if (photoPath != null && !photoPath.isEmpty()) {
                    // Ici, vous pourriez charger l'image depuis le chemin si nécessaire
                    // Pour l'instant, on stocke juste le chemin
                    subHead.setPhotoData(photoPath);
                }
            }
            
            return subHead;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la création de l'objet PolygamousSubFamily depuis tblInsuree", e);
            return null;
        }
    }
    

    private List<FamilyMember> fetchFamilyMembers(String familyUuid) {
        Log.d(LOG_TAG, "=== DÉBUT RÉCUPÉRATION MEMBRES FAMILLE ===");
        Log.d(LOG_TAG, "FamilyUUID demandé: " + familyUuid);
        
        List<FamilyMember> members = new ArrayList<>();
        if (familyUuid == null) {
            Log.w(LOG_TAG, "FamilyUUID est null, impossible de récupérer les membres de la famille");
            return members;
        }

        try {
            // Récupérer les membres depuis le serveur via GraphQL
            Log.d(LOG_TAG, "Tentative de récupération depuis le serveur GraphQL...");
            
            GetFamilyMembersGraphQLRequest request = new GetFamilyMembersGraphQLRequest();
            Log.d(LOG_TAG, "Appel de request.get() avec familyUuid: " + familyUuid);
            members = request.get(familyUuid);
            
            Log.d(LOG_TAG, "Retour de request.get(): " + (members != null ? members.size() + " membres" : "NULL"));
            
            if (members != null && !members.isEmpty()) {
                Log.d(LOG_TAG, "✓ SUCCÈS - Récupéré " + members.size() + " membres depuis le serveur");
                // Log détaillé des membres récupérés
                for (int i = 0; i < members.size(); i++) {
                    FamilyMember member = members.get(i);
                    Log.d(LOG_TAG, "Membre " + (i+1) + ": " + member.getLastName() + " " + member.getOtherNames() + 
                          " (CHFID: " + member.getChfId() + ", FamilyUUID: " + member.getFamilyUuid() + ")");
                }
                return members;
            } else {
                Log.w(LOG_TAG, "⚠ ÉCHEC SERVEUR - Aucun membre trouvé, tentative de récupération locale");
                return fetchFamilyMembersFromLocalDatabase(familyUuid);
            }
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "❌ ERREUR SERVEUR - Exception lors de la récupération: " + e.getMessage(), e);
            Log.d(LOG_TAG, "Tentative de récupération depuis la base de données locale...");
            // Fallback vers la base de données locale
            List<FamilyMember> localMembers = fetchFamilyMembersFromLocalDatabase(familyUuid);
            Log.d(LOG_TAG, "=== FIN RÉCUPÉRATION MEMBRES FAMILLE (via locale) - Total: " + 
                  (localMembers != null ? localMembers.size() : 0) + " ===");
            return localMembers;
        }
    }
    
    private List<FamilyMember> fetchFamilyMembersFromLocalDatabase(String familyUuid) {
        List<FamilyMember> members = new ArrayList<>();
        SQLHandler sqlHandler = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            Log.d(LOG_TAG, "Tentative de récupération depuis la base de données locale");
            sqlHandler = new SQLHandler(this);
            db = sqlHandler.getReadableDatabase();
            
            if (db == null || !db.isOpen()) {
                Log.e(LOG_TAG, "Base de données locale non disponible");
                return createMembersFromIntent();
            }
            
            // Vérifier si la table tblInsuree existe
            if (doesTableExist(db, "tblInsuree")) {
                // Récupérer les membres depuis tblInsuree
                cursor = db.query(
                    "tblInsuree",
                    new String[]{"CHFID", "LastName", "OtherNames", "Gender", "DOB", "PhotoPath"},
                    "FamilyUUID = ? OR CHFID LIKE ?",
                    new String[]{familyUuid, "%" + familyUuid + "%"},
                    null, null, "LastName ASC"
                );
                
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        FamilyMember member = createFamilyMemberFromCursor(cursor);
                        if (member != null) {
                            members.add(member);
                        }
                    } while (cursor.moveToNext());
                }
                
                Log.d(LOG_TAG, "Récupéré " + members.size() + " membres depuis la base locale");
            } else {
                Log.w(LOG_TAG, "Table tblInsuree non disponible, création de membres depuis l'Intent");
                return createMembersFromIntent();
            }
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la récupération depuis la base locale", e);
            return createMembersFromIntent();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (sqlHandler != null) {
                sqlHandler.close();
            }
        }
        
        return members;
    }
    
    private List<FamilyMember> createMembersFromIntent() {
        List<FamilyMember> members = new ArrayList<>();
        
        try {
            // Créer des membres fictifs ou récupérer depuis l'Intent si disponible
            String[] memberNames = getIntent().getStringArrayExtra("EXTRA_MEMBER_NAMES");
            String[] memberChfIds = getIntent().getStringArrayExtra("EXTRA_MEMBER_CHFIDS");
            String[] memberGenders = getIntent().getStringArrayExtra("EXTRA_MEMBER_GENDERS");
            
            if (memberNames != null && memberChfIds != null && memberNames.length == memberChfIds.length) {
                for (int i = 0; i < memberNames.length; i++) {
                    FamilyMember member = new FamilyMember();
                    member.setChfId(memberChfIds[i]);
                    member.setLastName(memberNames[i]);
                    
                    if (memberGenders != null && i < memberGenders.length) {
                        member.setGender(memberGenders[i]);
                    }
                    
                    members.add(member);
                }
            } else {
                // Créer un membre exemple si aucune donnée n'est disponible
                Log.d(LOG_TAG, "Aucune donnée de membre dans l'Intent, création d'exemples");
            }
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la création des membres depuis l'Intent", e);
        }
        
        return members;
    }
    
    private FamilyMember createFamilyMemberFromCursor(Cursor cursor) {
        try {
            FamilyMember member = new FamilyMember();
            
            // CHFID
            int chfIdIndex = cursor.getColumnIndex("CHFID");
            if (chfIdIndex != -1) {
                member.setChfId(cursor.getString(chfIdIndex));
            }
            
            // Nom de famille
            int lastNameIndex = cursor.getColumnIndex("LastName");
            if (lastNameIndex != -1) {
                member.setLastName(cursor.getString(lastNameIndex));
            }
            
            // Autres noms
            int otherNamesIndex = cursor.getColumnIndex("OtherNames");
            if (otherNamesIndex != -1) {
                member.setOtherNames(cursor.getString(otherNamesIndex));
            }
            
            // Genre
            int genderIndex = cursor.getColumnIndex("Gender");
            if (genderIndex != -1) {
                member.setGender(cursor.getString(genderIndex));
            }
            
            // Date de naissance
            int dobIndex = cursor.getColumnIndex("DOB");
            if (dobIndex != -1) {
                member.setDob(cursor.getString(dobIndex));
            }
            
            // Photo (si disponible)
            int photoIndex = cursor.getColumnIndex("PhotoPath");
            if (photoIndex != -1 && !cursor.isNull(photoIndex)) {
                String photoPath = cursor.getString(photoIndex);
                if (photoPath != null && !photoPath.isEmpty()) {
                    member.setPhotoData(photoPath);
                }
            }
            
            return member;
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la création du FamilyMember depuis le curseur", e);
            return null;
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void displaySubHeadInfo(PolygamousSubFamily subHead) {
        Log.d(LOG_TAG, "👤 === AFFICHAGE INFORMATIONS CHEF DE SOUS-FAMILLE ===");
        
        if (subHead == null) {
            Log.e(LOG_TAG, "❌ Objet subHead est null - impossible d'afficher les informations");
            return;
        }
        
        Log.d(LOG_TAG, "CHFID du chef: " + subHead.getChfId());
        Log.d(LOG_TAG, "FamilyUuid: " + subHead.getFamilyUuid());

        // Nom de famille (LastName)
        String lastName = subHead.getLastName() != null ? subHead.getLastName() : "N/A";
        tvSubHeadLastName.setText(lastName);
        Log.d(LOG_TAG, "Nom de famille affiché: " + lastName);
        
        // Prénom (OtherNames)
        String firstName = subHead.getOtherNames() != null ? subHead.getOtherNames() : "N/A";
        tvSubHeadFirstName.setText(firstName);
        Log.d(LOG_TAG, "Prénom affiché: " + firstName);

        // CHFID
        String chfIdDisplay = subHead.getChfId() != null ? subHead.getChfId() : "N/A";
        tvSubHeadChfId.setText(chfIdDisplay);
        Log.d(LOG_TAG, "CHFID affiché: " + chfIdDisplay);

        // Genre
        String gender = subHead.getGender();
        String genderDisplay;
        if ("M".equals(gender)) {
            genderDisplay = "Masculin";
        } else if ("F".equals(gender)) {
            genderDisplay = "Féminin";
        } else {
            genderDisplay = gender != null ? gender : "N/A";
        }
        tvSubHeadGender.setText(genderDisplay);
        Log.d(LOG_TAG, "Genre affiché: " + genderDisplay + " (original: " + gender + ")");

        // Date de naissance - formatage au format JJ/MM/AAAA
        String dobDisplay = "N/A";
        if (subHead.getDob() != null && !subHead.getDob().isEmpty()) {
            try {
                dobDisplay = DateUtils.formatExpiryDateString(subHead.getDob());
                Log.d(LOG_TAG, "Date formatée de '" + subHead.getDob() + "' vers '" + dobDisplay + "'");
            } catch (Exception e) {
                Log.w(LOG_TAG, "Impossible de formater la date: " + subHead.getDob() + ", utilisation de la valeur originale");
                dobDisplay = subHead.getDob();
            }
        }
        tvSubHeadDob.setText(dobDisplay);
        Log.d(LOG_TAG, "Date de naissance affichée: " + dobDisplay);

        // Photo
        Log.d(LOG_TAG, "Chargement de la photo du chef...");
        loadSubHeadPhoto(subHead);
        
        // Statut du contrat d'assurance
        Log.d(LOG_TAG, "Affichage du statut d'assurance...");
        displayInsuranceStatus(subHead);
        
        Log.d(LOG_TAG, "✅ Informations du chef affichées avec succès");
    }
    
    private void displayInsuranceStatus(PolygamousSubFamily subHead) {
        String detailedStatus = getDetailedInsuranceStatus(subHead);
        boolean hasActiveContract = isInsuranceStatusActive(detailedStatus);
        

        
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

            
            // Use FetchInsureeInquire to retrieve data from the server
            FetchInsureeInquire fetchInsureeInquire = new FetchInsureeInquire();
            Insuree insuree = fetchInsureeInquire.execute(subHead.getChfId());
            
            // Retrieve the most recent policy (active or most recent)
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
                return statusText;
            } else {
                return "Aucune police d'assurance";
            }
            
        } catch (Exception e) {
            // In case of server error, try to retrieve from local database as fallback
            return getLocalInsuranceStatus(subHead);
        }
    }
    
    private String getStatusText(Policy.Status status) {
        switch (status) {
            case ACTIVE: return "Actif";
            case INACTIF: return "Inactif";
            case SUSPENDED: return "Suspendu";
            case EXPIRED: return "Expiré";
            case READY: return "Prêt";
            default: return "Inconnu";
        }
    }
    
    private String getLocalInsuranceStatus(PolygamousSubFamily subHead) {
        try {
            SQLHandler sqlHandler = new SQLHandler(this);
            sqlHandler.createTables();
            SQLiteDatabase db = sqlHandler.getReadableDatabase();
            
            // Check if table exists
            Cursor tableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tblPolicyInquiry'", null);
            
            if (tableCheck.getCount() == 0) {
                tableCheck.close();
                return "Table tblPolicyInquiry manquante";
            }
            tableCheck.close();
            
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
            return "Erreur de vérification";
        }
    }

    private void loadSubHeadPhoto(PolygamousSubFamily subHead) {
        if (ivSubHeadPhoto == null) {
            Log.w(LOG_TAG, "Photo view not found");
            return;
        }
        
        // Check if subHead and photo are valid
        if (subHead == null) {
            Log.w(LOG_TAG, "SubHead is null, using default avatar");
            ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }
        
        String photoData = subHead.getPhoto();
        if (photoData == null || photoData.trim().isEmpty()) {
            Log.d(LOG_TAG, "No photo data available, using default avatar");
            ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }
        
        try {
            // Clean base64 data (remove spaces and unwanted characters)
            photoData = photoData.trim().replaceAll("\\s+", "");
            
            // Check if data appears to be valid base64
            if (photoData.length() % 4 != 0) {
                Log.w(LOG_TAG, "Invalid base64 data length, using default avatar");
                ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
                return;
            }
            
            // Decode base64 data
            byte[] decodedString = android.util.Base64.decode(photoData, android.util.Base64.DEFAULT);
            
            if (decodedString == null || decodedString.length == 0) {
                Log.w(LOG_TAG, "Decoded data is empty, using default avatar");
                ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
                return;
            }
            
            // Create the bitmap
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            
            if (decodedByte == null) {
                Log.w(LOG_TAG, "Failed to decode bitmap from data, using default avatar");
                ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
                return;
            }
            
            // Check bitmap size to avoid OutOfMemoryError
            int width = decodedByte.getWidth();
            int height = decodedByte.getHeight();
            
            if (width > 1000 || height > 1000) {
                Log.w(LOG_TAG, "Bitmap too large (" + width + "x" + height + "), scaling down");
                // Redimensionner le bitmap si trop grand
                int maxSize = 500;
                float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
                int newWidth = Math.round(width * ratio);
                int newHeight = Math.round(height * ratio);
                
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(decodedByte, newWidth, newHeight, true);
                if (scaledBitmap != decodedByte) {
                    decodedByte.recycle(); // Free memory of original bitmap
                }
                decodedByte = scaledBitmap;
            }
            
            ivSubHeadPhoto.setImageBitmap(decodedByte);
            Log.d(LOG_TAG, "Photo loaded successfully");
            
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Invalid base64 data for photo", e);
            ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "Out of memory while decoding photo", e);
            ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
            // Forcer le garbage collection
            System.gc();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unexpected error while loading photo", e);
            ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    private void loadSubHouseholdMembers(PolygamousSubFamily subHead, ArrayList<FamilyMember> allMembers) {
        Log.d(LOG_TAG, "=== DÉBUT CHARGEMENT MEMBRES SOUS-FAMILLE ===");
        Log.d(LOG_TAG, "Sous-famille: " + subHead.getLastName() + " (UUID: " + subHead.getFamilyUuid() + ")");
        Log.d(LOG_TAG, "Chef CHFID: " + subHead.getChfId());
        
        List<FamilyMember> subHouseholdMembers = subHead.getMembers();
        
        if (subHouseholdMembers != null && !subHouseholdMembers.isEmpty()) {
            Log.d(LOG_TAG, "Utilisation des membres pré-chargés: " + subHouseholdMembers.size());
            displayMembers(subHouseholdMembers);
        } else {
            if (allMembers == null || allMembers.isEmpty()) {
                Log.w(LOG_TAG, "Aucun membre disponible pour le filtrage");
                showNoMembersMessage();
                return;
            }

            Log.d(LOG_TAG, "Filtrage depuis allMembers (total: " + allMembers.size() + ")");
            
            // Filter members that belong to this sub-family
            List<FamilyMember> filteredMembers = new ArrayList<>();
            int filteredCount = 0;
            for (FamilyMember member : allMembers) {
                Log.d(LOG_TAG, "--- Examen membre: " + member.getLastName() + " " + member.getOtherNames() + " (CHFID: " + member.getChfId() + ")");
                Log.d(LOG_TAG, "FamilyUUID du membre: " + member.getFamilyUuid());
                
                boolean shouldInclude = false;
                
                // Use family information from server if available
                if (member.getFamilyUuid() != null && subHead.getFamilyUuid() != null) {
                    // Filter by familyUuid for more accurate sub-family detection
                    if (member.getFamilyUuid().equals(subHead.getFamilyUuid()) &&
                        !member.getChfId().equals(subHead.getChfId())) { // Exclude the head himself
                        filteredMembers.add(member);
                        filteredCount++;
                        shouldInclude = true;
                        Log.d(LOG_TAG, "Filtrage par FamilyUUID: INCLUS");
                    } else if (member.getChfId().equals(subHead.getChfId())) {
                        Log.d(LOG_TAG, "Membre EXCLU (chef de sous-famille)");
                    } else {
                        Log.d(LOG_TAG, "Filtrage par FamilyUUID: EXCLU (UUID différent)");
                    }
                } else {
                    // Fallback to lastname filtering if family info not available
                    if (member.getLastName() != null && 
                        member.getLastName().equals(subHead.getLastName()) &&
                        !member.getChfId().equals(subHead.getChfId())) { // Exclude the head himself
                        filteredMembers.add(member);
                        filteredCount++;
                        shouldInclude = true;
                        Log.d(LOG_TAG, "Filtrage par LastName (fallback): INCLUS");
                    } else if (member.getChfId().equals(subHead.getChfId())) {
                        Log.d(LOG_TAG, "Membre EXCLU (chef de sous-famille)");
                    } else {
                        Log.d(LOG_TAG, "Filtrage par LastName (fallback): EXCLU");
                    }
                }
                
                if (shouldInclude) {
                    Log.d(LOG_TAG, "Membre AJOUTÉ à la sous-famille");
                } else {
                    Log.d(LOG_TAG, "Membre EXCLU (critères non remplis)");
                }
            }
            
            Log.d(LOG_TAG, "Résultat filtrage: " + filteredCount + " membres trouvés");

            if (filteredMembers.isEmpty()) {
                Log.d(LOG_TAG, "Aucun membre filtré, affichage du message 'pas de membres'");
                showNoMembersMessage();
            } else {
                Log.d(LOG_TAG, "Affichage de " + filteredMembers.size() + " membres filtrés");
                displayMembers(filteredMembers);
            }
        }
        
        Log.d(LOG_TAG, "=== FIN CHARGEMENT MEMBRES SOUS-FAMILLE ===");
    }

    private void showNoMembersMessage() {
        Log.d(LOG_TAG, "🚫 === AFFICHAGE MESSAGE AUCUN MEMBRE ===");
        rvSubHouseholdMembers.setVisibility(View.GONE);
        tvNoMembers.setVisibility(View.VISIBLE);
        tvNoMembers.setText("Aucun membre dans cette sous-famille");
        Log.d(LOG_TAG, "RecyclerView masqué, message 'Aucun membre' affiché");
    }

    private void displayMembers(List<FamilyMember> members) {
        Log.d(LOG_TAG, "👥 === AFFICHAGE DES MEMBRES ===");
        Log.d(LOG_TAG, "Nombre de membres à afficher: " + (members != null ? members.size() : "null"));
        
        rvSubHouseholdMembers.setVisibility(View.VISIBLE);
        tvNoMembers.setVisibility(View.GONE);
        Log.d(LOG_TAG, "RecyclerView visible, message 'Aucun membre' masqué");
        
        Log.d(LOG_TAG, "Appel de memberAdapter.updateData()...");
        memberAdapter.updateData(members);
        Log.d(LOG_TAG, "memberAdapter.updateData() terminé");
    }

    private void displayPolicyInfo(PolygamousSubFamily subHead) {
        SQLHandler sqlHandler = null;
        SQLiteDatabase db = null;
        Cursor tableCheck = null;
        Cursor cursor = null;
        Cursor parentCursor = null;
        
        try {
            sqlHandler = new SQLHandler(this);
            
            if (sqlHandler == null) {
                Log.e(LOG_TAG, "SQLHandler is null");
                return;
            }
            
            // Ensure tables are created
            sqlHandler.createTables();
            db = sqlHandler.getReadableDatabase();
            
            if (db == null) {
                Log.e(LOG_TAG, "Database is null");
                return;
            }
            
            if (subHead == null || subHead.getChfId() == null || subHead.getChfId().trim().isEmpty()) {
                Log.e(LOG_TAG, "Invalid subHead or CHFID for policy query");
                return;
            }
        
            // Check if table exists
            tableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tblPolicyInquiry'", null);
            if (tableCheck == null || tableCheck.getCount() == 0) {
                Log.w(LOG_TAG, "Table tblPolicyInquiry does not exist");
                return;
            }
            
            String query = "SELECT * FROM tblPolicyInquiry WHERE InsureeNumber = ?";
            cursor = db.rawQuery(query, new String[]{subHead.getChfId()});
            
            // If no result with sub-family CHFID, try with main family head
            if (cursor != null && cursor.getCount() == 0 && subHead.getParentUuid() != null) {
                cursor.close();
                cursor = null;
                
                // Log: Cannot retrieve parent CHFID as tblInsuree table does not exist
                Log.w(LOG_TAG, "Cannot retrieve parent CHFID: tblInsuree table does not exist");
                // Skip parent query since table doesn't exist
                parentCursor = null;
                
                if (parentCursor != null && parentCursor.moveToFirst()) {
                    String parentChfId = parentCursor.getString(0);
                    
                    String parentPolicyQuery = "SELECT * FROM tblPolicyInquiry WHERE InsureeNumber = ?";
                    cursor = db.rawQuery(parentPolicyQuery, new String[]{parentChfId});
                }
            }
            
            ArrayList<Map<String, String>> PolicyList = new ArrayList<>();
            
            while (cursor.moveToNext()) {
                // Direct data retrieval from cursor
                String policyId = cursor.getString(cursor.getColumnIndexOrThrow("PolicyId"));
                String productName = cursor.getString(cursor.getColumnIndexOrThrow("ProductName"));
                String expiryDateStr = cursor.getString(cursor.getColumnIndexOrThrow("ExpiryDate"));
                String policyStatus = cursor.getString(cursor.getColumnIndexOrThrow("PolicyStatus"));
                
                // Retrieve deduction and ceiling data
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
            Log.e(LOG_TAG, "Error in displayPolicyInfo", e);
            if (cvPolicyInfo != null) {
                cvPolicyInfo.setVisibility(View.GONE);
            }
            if (llListView != null) {
                llListView.setVisibility(View.GONE);
            }
        } finally {
            // Properly close all resources
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error closing cursor", e);
                }
            }
            if (parentCursor != null) {
                try {
                    parentCursor.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error closing parentCursor", e);
                }
            }
            if (tableCheck != null) {
                try {
                    tableCheck.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error closing tableCheck", e);
                }
            }
            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error closing database", e);
                }
            }
            if (sqlHandler != null) {
                try {
                    sqlHandler.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error closing sqlHandler", e);
                }
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
