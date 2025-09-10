package org.openimis.imisclaims.network;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetFamilyMembersQuery;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.network.request.BaseGraphQLRequest;
import org.openimis.imisclaims.network.exception.HttpException;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.ArrayList;

public class GetFamilyMembersGraphQLRequest extends BaseGraphQLRequest {
    
    @NonNull
    @WorkerThread
    public List<FamilyMember> get(String familyUuid) throws Exception {
        try {
            Log.d("GetFamilyMembersGraphQLRequest", "Starting request for family UUID: " + familyUuid);
            // Utiliser la vraie requête GraphQL
            Log.d("GetFamilyMembersGraphQLRequest", "Executing GraphQL query with variables: familyUuid=" + familyUuid);
            GetFamilyMembersQuery.Data response = makeSynchronous(
                new GetFamilyMembersQuery(familyUuid)
            ).getData();
            Log.d("GetFamilyMembersGraphQLRequest", "Received response: " + (response != null ? "Data received" : "null"));
            
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
                    
                    familyMembers.add(member);
                }
            }
            
            Log.d("GetFamilyMembersGraphQLRequest", "Parsed " + familyMembers.size() + " family members");
            return familyMembers;
        } catch (Exception e) {
            Log.e("GetFamilyMembersGraphQLRequest", "Error getting family members", e);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}