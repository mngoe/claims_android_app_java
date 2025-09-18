package org.openimis.imisclaims.network;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetFamilyWithTypeQuery;
import org.openimis.imisclaims.network.request.BaseGraphQLRequest;
import org.openimis.imisclaims.network.exception.HttpException;

import java.net.HttpURLConnection;

public class GetFamilyWithTypeGraphQLRequest extends BaseGraphQLRequest {
    private static final String LOG_TAG = "GetFamilyWithType";
    
    public GetFamilyWithTypeGraphQLRequest() {
        // Default constructor
    }
    
    /**
     * Récupère les informations complètes d'une famille avec son type depuis le serveur GraphQL
     * @param familyUuid UUID de la famille
     * @return Le type de famille ("P" pour Polygame, "M" pour Monogame, etc.) ou null si non trouvé
     */
    @NonNull
    @WorkerThread
    public String getFamilyTypeFromServer(String familyUuid) throws Exception {
        try {
            // Retrieve family type from server
            
            // Execute GraphQL query
            GetFamilyWithTypeQuery.Data response = makeSynchronous(
                new GetFamilyWithTypeQuery(familyUuid)
            ).getData();
            
            if (response == null || response.families() == null || response.families().edges().isEmpty()) {
                return null;
            }
            
            // Retrieve first family (there should only be one with this UUID)
            GetFamilyWithTypeQuery.Edge firstEdge = response.families().edges().get(0);
            if (firstEdge.node() == null) {
                return null;
            }
            
            GetFamilyWithTypeQuery.Node familyNode = firstEdge.node();
            
            // Retrieve family type
            String familyTypeCode = null;
            if (familyNode.familyType() != null && familyNode.familyType().code() != null) {
                familyTypeCode = familyNode.familyType().code();
            }
            return familyTypeCode;
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error retrieving family type", e);
            throw e;
        }
    }
    
    /**
     * Vérifie si une famille est polygame en récupérant son type depuis le serveur
     * @param familyUuid UUID de la famille
     * @return true si la famille est polygame, false sinon
     */
    @WorkerThread
    public boolean isPolygamousFamily(String familyUuid) {
        try {
            String familyType = getFamilyTypeFromServer(familyUuid);
            boolean isPolygamous = "P".equals(familyType);
            

            
            return isPolygamous;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error checking polygamy", e);
            return false; // By default, consider as monogamous in case of error
        }
    }
}
