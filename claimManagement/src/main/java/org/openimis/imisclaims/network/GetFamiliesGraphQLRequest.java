package org.openimis.imisclaims.network;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetFamiliesWithParentQuery;
import org.openimis.imisclaims.domain.entity.Family;
import org.openimis.imisclaims.network.request.BaseGraphQLRequest;
import org.openimis.imisclaims.network.exception.HttpException;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.ArrayList;

public class GetFamiliesGraphQLRequest extends BaseGraphQLRequest {
    private static final String LOG_TAG = "GetFamiliesGraphQLRequest";
    
    @NonNull
    @WorkerThread
    public List<Family> getFamiliesWithParent(String parentUuid) throws Exception {
        try {

            

            
            // Implement pagination to retrieve all sub-families
            List<Family> allFamilies = new ArrayList<>();
            String cursor = null;
            int pageSize = 50; // Increase page size for better efficiency
            boolean hasNextPage = true;
            int pageCount = 0;
            
            while (hasNextPage) {
                pageCount++;

                
                // Use real GraphQL query with pagination
                GetFamiliesWithParentQuery query = new GetFamiliesWithParentQuery(parentUuid, Input.fromNullable(pageSize), Input.fromNullable(cursor), Input.fromNullable(true));

                
                GetFamiliesWithParentQuery.Data response = makeSynchronous(query).getData();
            

                
                if (response == null) {
                    break;
                }
                
                if (response.families() == null) {
                    break;
                }
                

                
                if (response.families().edges() == null) {
                    break;
                }
                

                
                // Check pagination
                hasNextPage = response.families().pageInfo() != null && 
                             response.families().pageInfo().hasNextPage();
                             
                if (hasNextPage && response.families().pageInfo().endCursor() != null) {
                    cursor = response.families().pageInfo().endCursor();
                }
                

                
                List<Family> pageFamilies = new ArrayList<>();
                

            
            for (int i = 0; i < response.families().edges().size(); i++) {
                GetFamiliesWithParentQuery.Edge edge = response.families().edges().get(i);
                
                if (edge == null) {
                    continue;
                }
                
                if (edge.node() == null) {
                    continue;
                }
                
                GetFamiliesWithParentQuery.Node node = edge.node();
                
                Family family = new Family();
                
                // Informations de base
                family.setUuid(node.uuid());
                family.setConfirmationNo(node.confirmationNo());
                family.setAddress(node.address());
                
                // Parent information
                if (node.parent() != null) {
                    family.setParentUuid(node.parent().uuid());
                }
                
                // Head insuree information
                if (node.headInsuree() != null) {
                    family.setHeadInsureeUuid(node.headInsuree().uuid());
                    family.setHeadInsureeChfId(node.headInsuree().chfId());
                    String fullName = (node.headInsuree().otherNames() + " " + 
                                     node.headInsuree().lastName()).trim();
                    family.setHeadInsureeName(fullName);
                }
                
                pageFamilies.add(family);
            }
            
            // Add families from this page to total list
            allFamilies.addAll(pageFamilies);
            
            // Sortir de la boucle si pas de page suivante
            if (!hasNextPage) {
                break;
            }
        }
        

        
        return allFamilies;
            
        } catch (Exception e) {
            throw e;
        }
    }
}