package org.openimis.imisclaims.network;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.domain.entity.PolygamousSubFamily;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.domain.entity.Family;
import org.openimis.imisclaims.network.request.BaseGraphQLRequest;
import org.openimis.imisclaims.SQLHandler;

import java.util.List;
import java.util.ArrayList;

public class GetPolygamousSubFamiliesGraphQLRequest extends BaseGraphQLRequest {
    
    private static final String LOG_TAG = "GetPolygamousSubFamilies";
    private SQLHandler sqlHandler;
    private GetFamilyTypesGraphQLRequest familyTypesRequest;
    private GetFamilyWithTypeGraphQLRequest familyWithTypeRequest;
    
    public GetPolygamousSubFamiliesGraphQLRequest(SQLHandler sqlHandler) {
        this.sqlHandler = sqlHandler;
        this.familyTypesRequest = new GetFamilyTypesGraphQLRequest(sqlHandler);
        this.familyWithTypeRequest = new GetFamilyWithTypeGraphQLRequest();
    }
    
    @NonNull
    @WorkerThread
    public List<PolygamousSubFamily> get(String familyUuid) throws Exception {
        // Retrieve polygamous sub-families
        
        try {
            // STEP 1: First check the family type
            // Family type detection
            String familyType = detectFamilyType(familyUuid);
            
            String familyTypeName = familyTypesRequest.getFamilyTypeName(familyType);
            
            if (!"P".equals(familyType)) {
                // Famille monogame - aucune sous-famille
                return new ArrayList<>();
            }
            
            // Polygamous family - retrieve sub-families
            
            // STEP 2: Retrieve sub-families with new pagination logic

            

            
            GetFamilyMembersGraphQLRequest familyMembersRequest = new GetFamilyMembersGraphQLRequest(sqlHandler);
            
            List<PolygamousSubFamily> subFamilies = familyMembersRequest.getPolygamousSubFamilies(familyUuid);
            

            

            
            return subFamilies;
            
        } catch (Exception e) {
            throw e;
        }
    }
    
    /**
     * Convertit les familles avec parent_Uuid en sous-familles polygames
     * Cette approche est basée sur la structure hiérarchique des familles
     */
    private List<PolygamousSubFamily> convertFamiliesToPolygamousSubFamilies(List<Family> families, String parentFamilyUuid) throws Exception {
        Log.d(LOG_TAG, "--- DÉBUT CONVERSION DES FAMILLES ---");
        List<PolygamousSubFamily> subFamilies = new ArrayList<>();
        
        for (int i = 0; i < families.size(); i++) {
            Family family = families.get(i);
            Log.d(LOG_TAG, "Conversion famille " + (i+1) + "/" + families.size() + ": " + family.getUuid());
            
            try {
                PolygamousSubFamily subFamily = new PolygamousSubFamily();
                
                // Utiliser les informations du chef de la sous-famille
                Log.d(LOG_TAG, "  - UUID chef: " + family.getHeadInsureeUuid());
                Log.d(LOG_TAG, "  - CHFID chef: " + family.getHeadInsureeChfId());
                Log.d(LOG_TAG, "  - Nom chef: " + family.getHeadInsureeName());
                
                subFamily.setUuid(family.getHeadInsureeUuid());
                subFamily.setChfId(family.getHeadInsureeChfId());
                subFamily.setFamilyUuid(parentFamilyUuid);
                subFamily.setRelationship("SubFamilyHead");
                
                // Parser le nom complet
                String fullName = family.getHeadInsureeName();
                if (fullName != null && !fullName.isEmpty()) {
                    String[] nameParts = fullName.trim().split(" ", 2);
                    if (nameParts.length > 0) {
                        subFamily.setLastName(nameParts[0]);
                        if (nameParts.length > 1) {
                            subFamily.setOtherNames(nameParts[1]);
                        }
                    }
                }
                
                // Retrieve members of this sub-family
                Log.d(LOG_TAG, "  - Récupération des membres pour famille: " + family.getUuid());
                GetFamilyMembersGraphQLRequest membersRequest = new GetFamilyMembersGraphQLRequest();
                List<FamilyMember> members = membersRequest.get(family.getUuid());
                
                Log.d(LOG_TAG, "  - Membres trouvés: " + (members != null ? members.size() : "null"));
                
                if (members != null && !members.isEmpty()) {
                    // Update head information with complete data
                    boolean headFound = false;
                    for (FamilyMember member : members) {
                        Log.d(LOG_TAG, "    * Membre: " + member.getFullName() + " (" + member.getRelationship() + ")");
                        if ("Head".equalsIgnoreCase(member.getRelationship())) {
                            Log.d(LOG_TAG, "    ✓ CHEF TROUVÉ: " + member.getFullName());
                            subFamily.setLastName(member.getLastName());
                            subFamily.setOtherNames(member.getOtherNames());
                            subFamily.setGender(member.getGender());
                            subFamily.setGenderCode(member.getGenderCode());
                            subFamily.setDob(member.getDob());
                            subFamily.setPhotoId(member.getPhotoId());
                            subFamily.setPhotoData(member.getPhotoData());
                            headFound = true;
                            break;
                        }
                    }
                    
                    if (!headFound) {
                        Log.w(LOG_TAG, "    ⚠ AUCUN CHEF trouvé dans les membres!");
                    }
                    
                    subFamily.setMembers(members);
                }
                
                subFamilies.add(subFamily);
                
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error converting family: " + family.getUuid(), e);
                // Continue with other families even if one fails
            }
        }
        return subFamilies;
    }
    
    /**
     * Détecte le type de famille en utilisant uniquement l'API GraphQL
     * (évite les erreurs de base de données locale qui ne contient pas tblFamilies)
     */
    public String detectFamilyType(String familyUuid) throws Exception {
        Log.d(LOG_TAG, "=== DÉTECTION TYPE DE FAMILLE (SERVEUR GRAPHQL) ===");
        Log.d(LOG_TAG, "UUID famille: " + familyUuid);
        
        // SPECIAL TEST for problematic head 391284976466
        if ("391284976466".equals(familyUuid)) {
            Log.e(LOG_TAG, "🚨 DÉTECTION POUR CHEF PROBLÉMATIQUE: 391284976466");
        }
        
        try {
            Log.i(LOG_TAG, "🌐 STRATÉGIE PRINCIPALE: Récupération directe depuis le serveur");
            Log.i(LOG_TAG, "📋 ÉTAPES:");
            Log.i(LOG_TAG, "  1. Requête GetFamilyWithType vers le serveur");
            Log.i(LOG_TAG, "  2. Récupération du type de famille officiel");
            Log.i(LOG_TAG, "  3. Fallback: Détection via sous-familles si nécessaire");
            
            // STEP 1: Try to retrieve type directly from server
            String serverFamilyType = null;
            try {
                serverFamilyType = familyWithTypeRequest.getFamilyTypeFromServer(familyUuid);
                if (serverFamilyType != null) {
                    return serverFamilyType;
                }
            } catch (Exception serverError) {
                // Fallback en cas d'erreur serveur
            }
            
            // STEP 2: Fallback - Detection via existence of sub-families
            
            GetFamiliesGraphQLRequest familiesRequest = new GetFamiliesGraphQLRequest();
            List<Family> families = familiesRequest.getFamiliesWithParent(familyUuid);
            
            if (families != null && !families.isEmpty()) {
                return "P"; // Polygame
            } else {
                return "M"; // Monogame
            }
            
        } catch (Exception e) {
            return "M"; // By default, consider as monogamous
        }
    }
    
    /**
     * Vérifie si une famille est polygame
     */
    public boolean isPolygamousFamily(String familyUuid) throws Exception {
        String familyType = detectFamilyType(familyUuid);
        return "P".equals(familyType);
    }
}