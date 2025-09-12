package org.openimis.imisclaims.network;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetFamiliesWithParentQuery;
import org.openimis.imisclaims.GetFamiliesWithParentIdQuery;
import org.openimis.imisclaims.GetSubFamilyHeadsQuery;
import org.openimis.imisclaims.GetFamilyMembersQuery;
import org.openimis.imisclaims.GetFamilyWithTypeQuery;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.domain.entity.PolygamousSubFamily;
import org.openimis.imisclaims.network.request.BaseGraphQLRequest;
import org.openimis.imisclaims.network.exception.HttpException;
import org.openimis.imisclaims.SQLHandler;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.ArrayList;

public class GetFamilyMembersGraphQLRequest extends BaseGraphQLRequest {
    
    private SQLHandler sqlHandler;
    private GetFamilyWithTypeGraphQLRequest familyWithTypeRequest;
    
    public GetFamilyMembersGraphQLRequest() {
        // Constructeur par défaut
        this.familyWithTypeRequest = new GetFamilyWithTypeGraphQLRequest();
    }
    
    public GetFamilyMembersGraphQLRequest(SQLHandler sqlHandler) {
        this.sqlHandler = sqlHandler;
        this.familyWithTypeRequest = new GetFamilyWithTypeGraphQLRequest();
    }
    
    @NonNull
    @WorkerThread
    public List<FamilyMember> get(String familyUuid) throws Exception {
        try {
            // Récupération des membres de la famille
            GetFamilyMembersQuery.Data response = makeSynchronous(
                new GetFamilyMembersQuery(familyUuid)
            ).getData();
            
            if (response == null || response.familyMembers() == null) {
                return new ArrayList<>();
            }
            
            List<FamilyMember> familyMembers = new ArrayList<>();
            
            for (GetFamilyMembersQuery.Edge edge : response.familyMembers().edges()) {
                if (edge.node() != null) {
                    GetFamilyMembersQuery.Node node = edge.node();
                    FamilyMember member = new FamilyMember();
                    
                    member.setUuid(node.uuid());
                    member.setChfId(node.chfId());
                    member.setOtherNames(node.otherNames());
                    member.setLastName(node.lastName());
                    member.setGender(node.gender() != null ? node.gender().gender() : "");
                    member.setGenderCode(node.gender() != null ? node.gender().code() : "");
                    member.setDob(node.dob() != null ? node.dob().toString() : "");
                    member.setRelationship(node.relationship() != null ? node.relationship().relation() : "");
                    
                    // Mapper les données de photo
                    if (node.photo() != null) {
                        member.setPhotoId(node.photo().id());
                        member.setPhotoData(node.photo().photo());
                    }
                    
                    // Note: Les informations de famille (familyUuid, parentUuid) sont disponibles
                    // mais ne sont pas stockées dans FamilyMember pour le moment
                    // Elles seront utilisées pour la détection des ménages polygames
                    
                    familyMembers.add(member);
                }
            }
            return familyMembers;
        } catch (Exception e) {
            Log.e("GetFamilyMembersGraphQLRequest", "Error retrieving family members", e);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Récupère le type de famille depuis le serveur via GraphQL
     * Cette méthode remplace l'ancienne approche qui tentait d'accéder à la base locale
     */
    @NonNull
    @WorkerThread
    public String getFamilyType(String familyUuid) throws Exception {
        Log.d("GetFamilyMembersGraphQLRequest", "=== RÉCUPÉRATION TYPE DE FAMILLE DEPUIS LE SERVEUR ===");
        Log.d("GetFamilyMembersGraphQLRequest", "UUID famille: " + familyUuid);
        
        try {
            // Utiliser la nouvelle requête GraphQL pour récupérer le type depuis le serveur
            Log.i("GetFamilyMembersGraphQLRequest", "🌐 Récupération du type de famille via API GraphQL...");
            
            String familyType = familyWithTypeRequest.getFamilyTypeFromServer(familyUuid);
            
            if (familyType != null) {
                Log.i("GetFamilyMembersGraphQLRequest", "✅ TYPE DE FAMILLE RÉCUPÉRÉ DEPUIS LE SERVEUR: " + familyType);
                
                // Optionnel: Synchroniser avec la base locale si SQLHandler est disponible
                if (sqlHandler != null) {
                    try {
                        // Vérifier si le type existe dans les références locales
                        String refQuery = "SELECT Name FROM tblReferences WHERE Code = ? AND Type = 'F'";
                        org.json.JSONArray refResult = sqlHandler.getQueryResultAsJsonArray(refQuery, new String[]{familyType});
                        
                        if (refResult == null || refResult.length() == 0) {
                            Log.d("GetFamilyMembersGraphQLRequest", "Synchronisation du type " + familyType + " avec la base locale");
                            String typeName = "P".equals(familyType) ? "Polygame" : "Monogame";
                            sqlHandler.InsertReferences(familyType, typeName, "F", "");
                        }
                    } catch (Exception syncError) {
                        Log.w("GetFamilyMembersGraphQLRequest", "Erreur lors de la synchronisation locale: " + syncError.getMessage());
                    }
                }
                
                return familyType;
            } else {
                Log.w("GetFamilyMembersGraphQLRequest", "⚠ Type de famille non trouvé sur le serveur pour: " + familyUuid);
                return null;
            }
            
        } catch (Exception e) {
            Log.e("GetFamilyMembersGraphQLRequest", "❌ ERREUR lors de la récupération du type depuis le serveur: " + familyUuid, e);
            
            // En cas d'erreur, essayer un diagnostic local pour information
            if (sqlHandler != null) {
                Log.w("GetFamilyMembersGraphQLRequest", "🔍 Diagnostic local en cas d'erreur serveur...");
                try {
                    String refQuery = "SELECT Code, Name FROM tblReferences WHERE Type = 'F'";
                    org.json.JSONArray refResult = sqlHandler.getQueryResultAsJsonArray(refQuery, null);
                    if (refResult != null && refResult.length() > 0) {
                        Log.d("GetFamilyMembersGraphQLRequest", "Types disponibles localement:");
                        for (int i = 0; i < refResult.length(); i++) {
                            org.json.JSONObject ref = refResult.getJSONObject(i);
                            Log.d("GetFamilyMembersGraphQLRequest", "  - " + ref.optString("Code") + ": " + ref.optString("Name"));
                        }
                    }
                } catch (Exception diagError) {
                    Log.w("GetFamilyMembersGraphQLRequest", "Erreur lors du diagnostic local: " + diagError.getMessage());
                }
            }
            
            throw e; // Relancer l'exception pour que l'appelant puisse la gérer
        }
    }
    
    /**
     * Récupère les sous-familles polygames avec leur parentUuid depuis le serveur
     * @param parentUuid UUID du chef de famille polygame principal
     * @return Liste des sous-familles avec parentUuid renseigné
     */
    @NonNull
    @WorkerThread
    public List<PolygamousSubFamily> getPolygamousSubFamilies(String familyUuid) throws Exception {
        try {
            Log.d("GetFamilyMembersGraphQLRequest", "Récupération des sous-familles pour famille UUID: " + familyUuid);
            
            // ÉTAPE 1: Récupérer les informations de la famille pour obtenir l'ID du parent
            GetFamilyWithTypeQuery.Data familyResponse = makeSynchronous(
                new GetFamilyWithTypeQuery(familyUuid)
            ).getData();
            
            if (familyResponse == null || familyResponse.families() == null || familyResponse.families().edges().isEmpty()) {
                Log.w("GetFamilyMembersGraphQLRequest", "Famille non trouvée: " + familyUuid);
                return new ArrayList<>();
            }
            
            GetFamilyWithTypeQuery.Node familyNode = familyResponse.families().edges().get(0).node();
            
            // Vérifier d'abord le type de famille
            String familyType = null;
            if (familyNode.familyType() != null && familyNode.familyType().code() != null) {
                familyType = familyNode.familyType().code();
            }
            
            Log.d("GetFamilyMembersGraphQLRequest", "Type de famille détecté: " + familyType);
            
            // Si ce n'est pas une famille polygame, retourner une liste vide
            if (!"P".equals(familyType)) {
                Log.i("GetFamilyMembersGraphQLRequest", "Famille de type " + familyType + " (non-polygame) - Aucune sous-famille à récupérer");
                return new ArrayList<>();
            }
            
            // Déterminer l'UUID et l'ID à utiliser pour la recherche des sous-familles
            String searchUuid;
            String searchId;
            
            if (familyNode.parent() != null) {
                // Cette famille a un parent, donc on cherche les autres sous-familles du même parent
                searchUuid = familyNode.parent().uuid();
                searchId = familyNode.parent().id();
                Log.i("GetFamilyMembersGraphQLRequest", "Famille polygame avec parent - Parent UUID: " + searchUuid + ", Parent ID: " + searchId);
            } else {
                // Cette famille n'a pas de parent et est polygame, donc elle est la famille principale polygame
                // On utilise son propre UUID/ID pour chercher ses sous-familles
                searchUuid = familyNode.uuid();
                searchId = familyNode.id();
                Log.i("GetFamilyMembersGraphQLRequest", "Famille principale polygame - Recherche des sous-familles avec UUID: " + searchUuid + ", ID: " + searchId);
            }
            
            // Log spécial pour le cas problématique
            if ("4a7b67f6-3138-4094-b2f0-6e6d747d0b93".equals(familyUuid) || "91dcada4-0721-4b38-af5f-c8a42d68eb26".equals(familyUuid)) {
                Log.e("GetFamilyMembersGraphQLRequest", "🚨 FAMILLE SPÉCIALE DÉTECTÉE!");
                Log.e("GetFamilyMembersGraphQLRequest", "🚨 Famille UUID: " + familyUuid);
                Log.e("GetFamilyMembersGraphQLRequest", "🚨 Search UUID: " + searchUuid);
                Log.e("GetFamilyMembersGraphQLRequest", "🚨 Search ID: " + searchId);
                Log.e("GetFamilyMembersGraphQLRequest", "🚨 A un parent: " + (familyNode.parent() != null));
                if (familyNode.headInsuree() != null) {
                    Log.e("GetFamilyMembersGraphQLRequest", "🚨 Chef de famille UUID: " + familyNode.headInsuree().uuid());
                    Log.e("GetFamilyMembersGraphQLRequest", "🚨 Chef de famille CHF ID: " + familyNode.headInsuree().chfId());
                }
            }
            
            // ÉTAPE 2: Récupérer directement les chefs de sous-familles depuis le serveur
            GetSubFamilyHeadsQuery.Data response = makeSynchronous(
                new GetSubFamilyHeadsQuery(Input.fromNullable(searchUuid), Input.fromNullable(searchId), Input.fromNullable(100), Input.fromNullable(null), Input.fromNullable(true))
            ).getData();
            
            // Log détaillé de la réponse
            if ("4a7b67f6-3138-4094-b2f0-6e6d747d0b93".equals(familyUuid)) {
                Log.e("GetFamilyMembersGraphQLRequest", "🚨 RÉPONSE REÇUE: " + (response != null ? "NON NULL" : "NULL"));
                if (response != null) {
                    Log.e("GetFamilyMembersGraphQLRequest", "🚨 response.families(): " + (response.families() != null ? "NON NULL" : "NULL"));
                    if (response.families() != null) {
                        Log.e("GetFamilyMembersGraphQLRequest", "🚨 response.families().edges(): " + (response.families().edges() != null ? response.families().edges().size() + " éléments" : "NULL"));
                        Log.e("GetFamilyMembersGraphQLRequest", "🚨 response.families().totalCount(): " + (response.families().totalCount() != null ? response.families().totalCount() : "NULL"));
                    }
                }
            }
            
            if (response == null || response.families() == null) {
                Log.w("GetFamilyMembersGraphQLRequest", "Aucun chef de sous-famille trouvé pour UUID: " + searchUuid + ", ID: " + searchId);
                return new ArrayList<>();
            }
            
            List<PolygamousSubFamily> subFamilies = new ArrayList<>();
            
            // Note: On récupère uniquement les chefs de sous-familles, pas le chef principal
            Log.d("GetFamilyMembersGraphQLRequest", "Récupération des chefs de sous-familles uniquement pour: " + searchUuid);
            
            // Ajouter uniquement les chefs de sous-familles
            for (GetSubFamilyHeadsQuery.Edge edge : response.families().edges()) {
                if (edge.node() != null && edge.node().headInsuree() != null) {
                    GetSubFamilyHeadsQuery.Node subFamilyNode = edge.node();
                    GetSubFamilyHeadsQuery.HeadInsuree1 headInsuree = subFamilyNode.headInsuree();
                    
                    PolygamousSubFamily subFamily = new PolygamousSubFamily();
                    Log.d("GetFamilyMembersGraphQLRequest", "Traitement du chef de sous-famille: " + headInsuree.chfId());
                    
                    // Informations du chef de sous-famille
                    subFamily.setUuid(headInsuree.uuid());
                    subFamily.setChfId(headInsuree.chfId());
                    subFamily.setLastName(headInsuree.lastName());
                    subFamily.setOtherNames(headInsuree.otherNames());
                    subFamily.setGender(headInsuree.gender() != null ? headInsuree.gender().code() : "");
                    subFamily.setDob(headInsuree.dob() != null ? headInsuree.dob().toString() : "");
                    subFamily.setRelationship("SubFamilyHead"); // Chef de sous-famille
                    
                    // UUID de la famille
                    subFamily.setFamilyUuid(subFamilyNode.uuid());
                    
                    // UUID du parent (chef de famille polygame principal)
                    subFamily.setParentUuid(searchUuid);
                    
                    // Photo du chef de sous-famille
                    if (headInsuree.photo() != null) {
                        subFamily.setPhotoId(headInsuree.photo().id());
                        subFamily.setPhotoData(headInsuree.photo().photo());
                    }
                    
                    // Récupérer les membres de cette sous-famille
                    try {
                        List<FamilyMember> subFamilyMembers = get(subFamilyNode.uuid());
                        if (subFamilyMembers != null && !subFamilyMembers.isEmpty()) {
                            // Exclure le chef de famille de la liste des membres
                            List<FamilyMember> membersWithoutHead = new ArrayList<>();
                            for (FamilyMember member : subFamilyMembers) {
                                if (!headInsuree.chfId().equals(member.getChfId())) {
                                    membersWithoutHead.add(member);
                                }
                            }
                            subFamily.setMembers(membersWithoutHead);
                            Log.d("GetFamilyMembersGraphQLRequest", "Assigné " + membersWithoutHead.size() + " membres à la sous-famille " + headInsuree.chfId());
                        } else {
                            subFamily.setMembers(new ArrayList<>());
                            Log.d("GetFamilyMembersGraphQLRequest", "Aucun membre trouvé pour la sous-famille " + headInsuree.chfId());
                        }
                    } catch (Exception memberException) {
                        Log.w("GetFamilyMembersGraphQLRequest", "Erreur lors de la récupération des membres pour " + headInsuree.chfId(), memberException);
                        subFamily.setMembers(new ArrayList<>());
                    }
                    
                    subFamilies.add(subFamily);
                }
            }
            
            Log.d("GetFamilyMembersGraphQLRequest", "Récupéré " + subFamilies.size() + " sous-familles polygames");
            return subFamilies;
            
        } catch (Exception e) {
            Log.e("GetFamilyMembersGraphQLRequest", "Error retrieving sub-families for family UUID: " + familyUuid, e);
            throw e;
        }
    }
    
    /**
     * Récupère les informations de la famille parent d'une famille donnée
     * @param familyUuid UUID de la famille dont on veut récupérer le parent
     * @return Family parent ou null si pas de parent
     */
    @WorkerThread
    public org.openimis.imisclaims.domain.entity.Family getParentFamily(String familyUuid) {
        try {
            Log.d("GetFamilyMembersGraphQLRequest", "🔍 Recherche du parent pour famille: " + familyUuid);
            
            // Utiliser la requête GetFamilyWithType pour récupérer les informations de la famille
            GetFamilyWithTypeQuery.Data response = makeSynchronous(
                new GetFamilyWithTypeQuery(familyUuid)
            ).getData();
            
            if (response == null || response.families() == null || response.families().edges().isEmpty()) {
                Log.d("GetFamilyMembersGraphQLRequest", "❌ Famille non trouvée: " + familyUuid);
                return null;
            }
            
            GetFamilyWithTypeQuery.Node familyNode = response.families().edges().get(0).node();
            
            // Vérifier si la famille a un parent
            if (familyNode.parent() == null) {
                Log.d("GetFamilyMembersGraphQLRequest", "👥 Pas de famille parent pour: " + familyUuid);
                return null;
            }
            
            GetFamilyWithTypeQuery.Parent parentNode = familyNode.parent();
            
            // Créer l'objet Family parent
            org.openimis.imisclaims.domain.entity.Family parentFamily = new org.openimis.imisclaims.domain.entity.Family();
            parentFamily.setUuid(parentNode.uuid());
            
            // Type de famille du parent
            if (parentNode.familyType() != null && parentNode.familyType().code() != null) {
                parentFamily.setFamilyType(parentNode.familyType().code());
                Log.d("GetFamilyMembersGraphQLRequest", "👑 Parent trouvé - UUID: " + parentNode.uuid() + ", Type: " + parentNode.familyType().code());
            } else {
                Log.w("GetFamilyMembersGraphQLRequest", "⚠️ Type de famille parent non disponible");
            }
            
            // Note: Le headInsuree du parent n'est pas disponible dans cette requête
            // Il faudrait faire une requête séparée pour récupérer cette information si nécessaire
            Log.d("GetFamilyMembersGraphQLRequest", "ℹ️ HeadInsuree du parent non récupéré dans cette requête");
            
            return parentFamily;
            
        } catch (Exception e) {
            Log.e("GetFamilyMembersGraphQLRequest", "❌ Erreur lors de la récupération du parent pour: " + familyUuid, e);
            return null;
        }
    }
}