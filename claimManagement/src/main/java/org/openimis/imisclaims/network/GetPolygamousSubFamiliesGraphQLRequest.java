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
        // Récupération des sous-familles polygames
        
        try {
            // ÉTAPE 1: Vérifier d'abord le type de famille
            // Détection du type de famille
            String familyType = detectFamilyType(familyUuid);
            
            String familyTypeName = familyTypesRequest.getFamilyTypeName(familyType);
            
            if (!"P".equals(familyType)) {
                // Famille monogame - aucune sous-famille
                return new ArrayList<>();
            }
            
            // Famille polygame - récupération des sous-familles
            
            // ÉTAPE 2: Récupérer les chefs de sous-familles qui ont le même parent
            // Récupération des sous-familles avec parent_Uuid
            GetFamiliesGraphQLRequest familiesRequest = new GetFamiliesGraphQLRequest();
            
            List<Family> subFamilies = familiesRequest.getFamiliesWithParent(familyUuid);
            
            if (subFamilies == null || subFamilies.isEmpty()) {
                // Aucune sous-famille trouvée
                return new ArrayList<>();
            }
            
            // Conversion des familles en sous-familles polygames
            
            // Traitement de la sous-famille
            
            List<PolygamousSubFamily> result = convertFamiliesToPolygamousSubFamilies(subFamilies, familyUuid);
            // Fin de la récupération
            return result;
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error retrieving sub-families for UUID: " + familyUuid, e);
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
                
                // Récupérer les membres de cette sous-famille
                Log.d(LOG_TAG, "  - Récupération des membres pour famille: " + family.getUuid());
                GetFamilyMembersGraphQLRequest membersRequest = new GetFamilyMembersGraphQLRequest();
                List<FamilyMember> members = membersRequest.get(family.getUuid());
                
                Log.d(LOG_TAG, "  - Membres trouvés: " + (members != null ? members.size() : "null"));
                
                if (members != null && !members.isEmpty()) {
                    // Mettre à jour les informations du chef avec les données complètes
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
                // Continue avec les autres familles même si une échoue
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
        
        // TEST SPÉCIAL pour le chef problématique 391284976466
        if ("391284976466".equals(familyUuid)) {
            Log.e(LOG_TAG, "🚨 DÉTECTION POUR CHEF PROBLÉMATIQUE: 391284976466");
        }
        
        try {
            Log.i(LOG_TAG, "🌐 STRATÉGIE PRINCIPALE: Récupération directe depuis le serveur");
            Log.i(LOG_TAG, "📋 ÉTAPES:");
            Log.i(LOG_TAG, "  1. Requête GetFamilyWithType vers le serveur");
            Log.i(LOG_TAG, "  2. Récupération du type de famille officiel");
            Log.i(LOG_TAG, "  3. Fallback: Détection via sous-familles si nécessaire");
            
            // ÉTAPE 1: Essayer de récupérer le type directement depuis le serveur
            String serverFamilyType = null;
            try {
                serverFamilyType = familyWithTypeRequest.getFamilyTypeFromServer(familyUuid);
                if (serverFamilyType != null) {
                    return serverFamilyType;
                }
            } catch (Exception serverError) {
                // Fallback en cas d'erreur serveur
            }
            
            // ÉTAPE 2: Fallback - Détection via l'existence de sous-familles
            
            GetFamiliesGraphQLRequest familiesRequest = new GetFamiliesGraphQLRequest();
            List<Family> families = familiesRequest.getFamiliesWithParent(familyUuid);
            
            if (families != null && !families.isEmpty()) {
                return "P"; // Polygame
            } else {
                return "M"; // Monogame
            }
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error detecting family type: " + familyUuid, e);
            return "M"; // Par défaut, considérer comme monogame
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