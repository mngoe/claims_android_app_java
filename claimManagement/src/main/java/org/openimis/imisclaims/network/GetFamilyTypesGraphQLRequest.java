package org.openimis.imisclaims.network;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetFamilyTypesQuery;
import org.openimis.imisclaims.SQLHandler;
import org.openimis.imisclaims.network.request.BaseGraphQLRequest;
import org.openimis.imisclaims.tools.Log;

public class GetFamilyTypesGraphQLRequest extends BaseGraphQLRequest {
    private static final String LOG_TAG = "GET_FAMILY_TYPES";
    private SQLHandler sqlHandler;

    public GetFamilyTypesGraphQLRequest(SQLHandler sqlHandler) {
        this.sqlHandler = sqlHandler;
    }

    @NonNull
    @WorkerThread
    public void getFamilyTypes() {
        // Retrieve family types from server
        
        try {
            // Retrieve family types from server via GraphQL
            
            GetFamilyTypesQuery.Data response = makeSynchronous(
                new GetFamilyTypesQuery()
            ).getData();
            
            if (response != null && response.familyTypes() != null) {
                
                // Synchronize with local database
                for (GetFamilyTypesQuery.FamilyType familyType : response.familyTypes()) {
                    String code = familyType.code();
                    String type = familyType.type();
                    
                    sqlHandler.InsertReferences(code, type, "F", "");
                }

            } else {
                // Fallback to default types
                sqlHandler.InsertReferences("P", "Polygame", "F", "");
                sqlHandler.InsertReferences("M", "Monogame", "F", "");
            }
            
        } catch (Exception e) {
            
            try {
                // Fallback to default types in case of error
                sqlHandler.InsertReferences("P", "Polygame", "F", "");
                sqlHandler.InsertReferences("M", "Monogame", "F", "");
            } catch (Exception fallbackError) {
            }
        }

    }

    /**
     * Récupère le nom d'un type de famille depuis la base locale
     */
    public String getFamilyTypeName(String code) {
        return sqlHandler.getReferenceName(code);
    }
}