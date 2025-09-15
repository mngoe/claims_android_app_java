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
import java.util.HashSet;
import java.util.Set;

public class GetFamilyMembersGraphQLRequest extends BaseGraphQLRequest {
    
    private SQLHandler sqlHandler;
    private GetFamilyWithTypeGraphQLRequest familyWithTypeRequest;
    
    public GetFamilyMembersGraphQLRequest() {
        // Default constructor
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
            // Retrieve family members
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
                    
                    // Map photo data
                    if (node.photo() != null) {
                        member.setPhotoId(node.photo().id());
                        member.setPhotoData(node.photo().photo());
                    }
                    
                    // Note: Les informations de famille (familyUuid, parentUuid) sont disponibles
                    // but are not stored in FamilyMember for now
                    // They will be used for polygamous household detection
                    
                    familyMembers.add(member);
                }
            }
            return familyMembers;
        } catch (Exception e) {

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

        
        try {
            // Use new GraphQL query to retrieve type from server

            
            String familyType = familyWithTypeRequest.getFamilyTypeFromServer(familyUuid);
            
            if (familyType != null) {

                
                // Optionnel: Synchroniser avec la base locale si SQLHandler est disponible
                if (sqlHandler != null) {
                    try {
                        // Check if type exists in local references
                        String refQuery = "SELECT Name FROM tblReferences WHERE Code = ? AND Type = 'F'";
                        org.json.JSONArray refResult = sqlHandler.getQueryResultAsJsonArray(refQuery, new String[]{familyType});
                        
                        if (refResult == null || refResult.length() == 0) {

                            String typeName = "P".equals(familyType) ? "Polygame" : "Monogame";
                            sqlHandler.InsertReferences(familyType, typeName, "F", "");
                        }
                    } catch (Exception syncError) {

                    }
                }
                
                return familyType;
            } else {

                return null;
            }
            
        } catch (Exception e) {

            
            throw e; // Re-throw exception so caller can handle it
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

            
            // STEP 1: Retrieve family information to get parent ID
            GetFamilyWithTypeQuery.Data familyResponse = makeSynchronous(
                new GetFamilyWithTypeQuery(familyUuid)
            ).getData();
            
            if (familyResponse == null || familyResponse.families() == null || familyResponse.families().edges().isEmpty()) {

                return new ArrayList<>();
            }
            
            GetFamilyWithTypeQuery.Node familyNode = familyResponse.families().edges().get(0).node();
            
            // First check family type
            String familyType = null;
            if (familyNode.familyType() != null && familyNode.familyType().code() != null) {
                familyType = familyNode.familyType().code();
            }
            

            
            // Si ce n'est pas une famille polygame, retourner une liste vide
            if (!"P".equals(familyType)) {

                return new ArrayList<>();
            }
            
            // Determine UUID and ID to use for sub-family search
            String searchUuid;
            String searchId;
            
            if (familyNode.parent() != null) {
                // This family has a parent, so we search for other sub-families of the same parent
                searchUuid = familyNode.parent().uuid();
                searchId = familyNode.parent().id();

            } else {
                // Cette famille n'a pas de parent et est polygame, donc elle est la famille principale polygame
                // On utilise son propre UUID/ID pour chercher ses sous-familles
                searchUuid = familyNode.uuid();
                searchId = familyNode.id();

            }
            

            
            // STEP 2: Retrieve sub-family heads directly from server
            GetSubFamilyHeadsQuery.Data response = makeSynchronous(
                new GetSubFamilyHeadsQuery(Input.fromNullable(searchUuid), Input.fromNullable(searchId), Input.fromNullable(100), Input.fromNullable(null), Input.fromNullable(true))
            ).getData();
            

            
            if (response == null || response.families() == null) {

                return new ArrayList<>();
            }
            
            List<PolygamousSubFamily> subFamilies = new ArrayList<>();
            Set<String> seenUuids = new HashSet<>();
            Set<String> seenChfIds = new HashSet<>();
            
            // Note: We only retrieve sub-family heads, not the main head

            
            // Add only sub-family heads with deduplication
            for (GetSubFamilyHeadsQuery.Edge edge : response.families().edges()) {
                if (edge.node() != null && edge.node().headInsuree() != null) {
                    GetSubFamilyHeadsQuery.Node subFamilyNode = edge.node();
                    GetSubFamilyHeadsQuery.HeadInsuree1 headInsuree = subFamilyNode.headInsuree();
                    
                    // Deduplication check
                    String uuid = headInsuree.uuid();
                    String chfId = headInsuree.chfId();
                    
                    boolean isDuplicateByUuid = uuid != null && seenUuids.contains(uuid);
                    boolean isDuplicateByChfId = chfId != null && seenChfIds.contains(chfId);
                    
                    if (isDuplicateByUuid || isDuplicateByChfId) {
                        continue; // Ignorer ce doublon
                    }
                    
                    // Marquer comme vu
                    if (uuid != null) {
                        seenUuids.add(uuid);
                    }
                    if (chfId != null) {
                        seenChfIds.add(chfId);
                    }
                    
                    PolygamousSubFamily subFamily = new PolygamousSubFamily();

                    
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
                    
                    // Retrieve members of this sub-family
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

                        } else {
                            subFamily.setMembers(new ArrayList<>());

                        }
                    } catch (Exception memberException) {

                        subFamily.setMembers(new ArrayList<>());
                    }
                    
                    subFamilies.add(subFamily);
                }
            }
            

            return subFamilies;
            
        } catch (Exception e) {

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

            
            // Use GetFamilyWithType query to retrieve family information
            GetFamilyWithTypeQuery.Data response = makeSynchronous(
                new GetFamilyWithTypeQuery(familyUuid)
            ).getData();
            
            if (response == null || response.families() == null || response.families().edges().isEmpty()) {

                return null;
            }
            
            GetFamilyWithTypeQuery.Node familyNode = response.families().edges().get(0).node();
            
            // Check if family has a parent
            if (familyNode.parent() == null) {

                return null;
            }
            
            GetFamilyWithTypeQuery.Parent parentNode = familyNode.parent();
            
            // Create parent Family object
            org.openimis.imisclaims.domain.entity.Family parentFamily = new org.openimis.imisclaims.domain.entity.Family();
            parentFamily.setUuid(parentNode.uuid());
            
            // Type de famille du parent
            if (parentNode.familyType() != null && parentNode.familyType().code() != null) {
                parentFamily.setFamilyType(parentNode.familyType().code());
            } else {
                
            }
            
            // Retrieve parent family head information (HeadInsuree)
            if (parentNode.headInsuree() != null) {
                GetFamilyWithTypeQuery.HeadInsuree1 parentHead = parentNode.headInsuree();
                
                // CORRECTION: Set headInsureeChfId for identity comparison
                parentFamily.setHeadInsureeChfId(parentHead.chfId());
                
                // Create Insuree object for parent family head
                org.openimis.imisclaims.domain.entity.Insuree parentHeadInsuree = new org.openimis.imisclaims.domain.entity.Insuree(
                    parentHead.chfId() != null ? parentHead.chfId() : "",
                    (parentHead.lastName() != null ? parentHead.lastName() : "") + 
                    (parentHead.otherNames() != null ? " " + parentHead.otherNames() : ""),
                    parentHead.dob() != null ? parentHead.dob() : new java.util.Date(),
                    parentHead.gender() != null ? parentHead.gender().code() : null,
                    parentHead.photo() != null ? parentHead.photo().photo() : null,
                    null, // photo bytes - not available in query
                    new java.util.ArrayList<>(), // policies vides
                    parentNode.uuid() // familyUuid
                );
                
                parentFamily.setHeadInsuree(parentHeadInsuree);
                
            } else {
                
            }
            
            return parentFamily;
            
        } catch (Exception e) {

            return null;
        }
    }
}