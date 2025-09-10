package org.openimis.imisclaims.util;

import org.openimis.imisclaims.SQLHandler;
import org.openimis.imisclaims.tools.Log;

/**
 * Utilitaire pour initialiser les types de famille dans la base de données locale
 * si ils ne sont pas encore synchronisés depuis le serveur
 */
public class FamilyTypeInitializer {
    private static final String LOG_TAG = "FAMILY_TYPE_INIT";
    
    /**
     * Initialise les types de famille par défaut si la table est vide
     */
    public static void initializeDefaultFamilyTypes(SQLHandler sqlHandler) {
        // Initialisation des types de famille
        
        try {
            // Vérifier si des types de famille existent déjà
            String existingPolygamous = sqlHandler.getReferenceName("P");
            String existingMonogamous = sqlHandler.getReferenceName("M");
            
            if (existingPolygamous == null || existingPolygamous.isEmpty()) {
                sqlHandler.InsertReferences("P", "Polygame", "F", "");
            }
            
            if (existingMonogamous == null || existingMonogamous.isEmpty()) {
                sqlHandler.InsertReferences("M", "Monogame", "F", "");
            }
            

            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error initializing family types: " + e.getMessage());
        }
        

    }
}