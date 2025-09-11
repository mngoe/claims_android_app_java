package org.openimis.imisclaims.network;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetFamilyMembersQuery;
import org.openimis.imisclaims.GetFamiliesWithParentQuery;
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
    public List<PolygamousSubFamily> getPolygamousSubFamilies(String parentUuid) throws Exception {
        try {
            Log.d("GetFamilyMembersGraphQLRequest", "Récupération des sous-familles pour parent UUID: " + parentUuid);
            
            GetFamiliesWithParentQuery.Data response = makeSynchronous(
                new GetFamiliesWithParentQuery(parentUuid)
            ).getData();
            
            if (response == null || response.families() == null) {
                return new ArrayList<>();
            }
            
            List<PolygamousSubFamily> subFamilies = new ArrayList<>();
            
            for (GetFamiliesWithParentQuery.Edge edge : response.families().edges()) {
                if (edge.node() != null && edge.node().headInsuree() != null) {
                    GetFamiliesWithParentQuery.Node familyNode = edge.node();
                    GetFamiliesWithParentQuery.HeadInsuree1 headInsuree = familyNode.headInsuree();
                    
                    PolygamousSubFamily subFamily = new PolygamousSubFamily();
                    
                    // Informations du chef de sous-famille
                    subFamily.setUuid(headInsuree.uuid());
                    subFamily.setChfId(headInsuree.chfId());
                    subFamily.setLastName(headInsuree.lastName());
                    subFamily.setOtherNames(headInsuree.otherNames());
                    subFamily.setGender(headInsuree.gender() != null ? headInsuree.gender().code() : "");
                    subFamily.setDob(headInsuree.dob() != null ? headInsuree.dob().toString() : "");
                    subFamily.setRelationship("Head"); // Chef de sous-famille
                    
                    // UUID de la famille
                    subFamily.setFamilyUuid(familyNode.uuid());
                    
                    // UUID du parent (chef de famille polygame principal)
                    if (familyNode.parent() != null) {
                        subFamily.setParentUuid(familyNode.parent().uuid());
                    }
                    
                    // Photo du chef de sous-famille
                    if (headInsuree.photo() != null) {
                        subFamily.setPhotoId(headInsuree.photo().id());
                        subFamily.setPhotoData(headInsuree.photo().photo());
                    }
                    
                    subFamilies.add(subFamily);
                }
            }
            
            Log.d("GetFamilyMembersGraphQLRequest", "Récupéré " + subFamilies.size() + " sous-familles polygames");
            return subFamilies;
            
        } catch (Exception e) {
            Log.e("GetFamilyMembersGraphQLRequest", "Erreur lors de la récupération des sous-familles", e);
            throw e;
        }
    }
}