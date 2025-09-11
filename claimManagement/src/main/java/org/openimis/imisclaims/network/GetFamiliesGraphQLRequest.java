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
            Log.d(LOG_TAG, "=== DÉBUT REQUÊTE FAMILLES AVEC PARENT ===");
            Log.d(LOG_TAG, "Parent UUID recherché: " + parentUuid);
            
            // TEST SPÉCIAL pour le chef problématique 391284976466
            if ("391284976466".equals(parentUuid)) {
                Log.e(LOG_TAG, "🚨 RECHERCHE SOUS-FAMILLES POUR CHEF PROBLÉMATIQUE: 391284976466");
                Log.e(LOG_TAG, "🚨 Cette requête devrait retourner 2 sous-familles selon le web");
            }
            
            // Utiliser la vraie requête GraphQL
            Log.d(LOG_TAG, "Création de GetFamiliesWithParentQuery avec parent_Uuid=" + parentUuid);
            GetFamiliesWithParentQuery query = new GetFamiliesWithParentQuery(parentUuid);
            Log.d(LOG_TAG, "Exécution de la requête GraphQL...");
            
            GetFamiliesWithParentQuery.Data response = makeSynchronous(query).getData();
            
            Log.d(LOG_TAG, "Réponse reçue: " + (response != null ? "Données disponibles" : "NULL"));
            
            if (response == null) {
                Log.w(LOG_TAG, "RÉPONSE NULL - Aucune donnée retournée par la requête");
                return new ArrayList<>();
            }
            
            if (response.families() == null) {
                Log.w(LOG_TAG, "FAMILIES NULL - Le champ families est null dans la réponse");
                return new ArrayList<>();
            }
            
            Log.d(LOG_TAG, "Objet families trouvé, vérification des edges...");
            
            if (response.families().edges() == null) {
                Log.w(LOG_TAG, "EDGES NULL - Aucun edge dans families");
                return new ArrayList<>();
            }
            
            Log.i(LOG_TAG, "✓ EDGES TROUVÉS: " + response.families().edges().size() + " éléments");
            
            // TEST SPÉCIAL pour le chef problématique 391284976466
            if ("391284976466".equals(parentUuid)) {
                Log.e(LOG_TAG, "🚨 CHEF 391284976466 - Edges trouvés: " + response.families().edges().size() + " (devrait être 2)");
                if (response.families().edges().size() == 0) {
                    Log.e(LOG_TAG, "🚨 ❌ PROBLÈME: Aucune sous-famille trouvée pour le chef polygame 391284976466!");
                    Log.e(LOG_TAG, "🚨 Vérifiez que les sous-familles ont parent_Uuid = 391284976466 dans la base de données");
                }
            }
            
            List<Family> familyList = new ArrayList<>();
            
            Log.d(LOG_TAG, "--- DÉBUT PARSING DES FAMILLES ---");
            
            for (int i = 0; i < response.families().edges().size(); i++) {
                GetFamiliesWithParentQuery.Edge edge = response.families().edges().get(i);
                Log.d(LOG_TAG, "Traitement edge " + (i+1) + "/" + response.families().edges().size());
                
                if (edge == null) {
                    Log.w(LOG_TAG, "  Edge " + (i+1) + " est NULL, ignoré");
                    continue;
                }
                
                if (edge.node() == null) {
                    Log.w(LOG_TAG, "  Node de l'edge " + (i+1) + " est NULL, ignoré");
                    continue;
                }
                
                GetFamiliesWithParentQuery.Node node = edge.node();
                Log.d(LOG_TAG, "  Node " + (i+1) + " trouvé, UUID: " + node.uuid());
                
                Family family = new Family();
                
                // Informations de base
                family.setUuid(node.uuid());
                family.setConfirmationNo(node.confirmationNo());
                family.setAddress(node.address());
                
                Log.d(LOG_TAG, "    - UUID: " + node.uuid());
                Log.d(LOG_TAG, "    - Confirmation: " + node.confirmationNo());
                Log.d(LOG_TAG, "    - Adresse: " + node.address());
                
                // Parent information
                if (node.parent() != null) {
                    family.setParentUuid(node.parent().uuid());
                    Log.d(LOG_TAG, "    - Parent UUID: " + node.parent().uuid());
                    
                    // Log family type information
                    if (node.parent().familyType() != null && node.parent().familyType().code() != null) {
                        String familyTypeCode = node.parent().familyType().code();
                        Log.i(LOG_TAG, "    - Type de famille parent: " + familyTypeCode);
                        
                        // Log if polygamous
                        if ("P".equals(familyTypeCode)) {
                            Log.i(LOG_TAG, "    ✓ FAMILLE POLYGAME DÉTECTÉE - Parent: " + node.parent().uuid());
                        } else {
                            Log.d(LOG_TAG, "    - Famille monogame - Parent: " + node.parent().uuid());
                        }
                    } else {
                        Log.w(LOG_TAG, "    - Type de famille parent non disponible");
                    }
                    
                    // Log parent head insuree
                    if (node.parent().headInsuree() != null) {
                        Log.d(LOG_TAG, "    - Chef de famille parent UUID: " + node.parent().headInsuree().uuid());
                    }
                } else {
                    Log.w(LOG_TAG, "    - PARENT NULL pour cette famille");
                }
                
                // Head insuree information
                if (node.headInsuree() != null) {
                    family.setHeadInsureeUuid(node.headInsuree().uuid());
                    family.setHeadInsureeChfId(node.headInsuree().chfId());
                    String fullName = (node.headInsuree().otherNames() + " " + 
                                     node.headInsuree().lastName()).trim();
                    family.setHeadInsureeName(fullName);
                    
                    Log.d(LOG_TAG, "    - Chef UUID: " + node.headInsuree().uuid());
                    Log.d(LOG_TAG, "    - Chef CHFID: " + node.headInsuree().chfId());
                    Log.d(LOG_TAG, "    - Chef Nom: " + fullName);
                } else {
                    Log.w(LOG_TAG, "    - HEAD INSUREE NULL pour cette famille");
                }
                
                familyList.add(family);
                Log.i(LOG_TAG, "  ✓ Famille " + (i+1) + " ajoutée: " + family.getHeadInsureeName());
            }
            
            Log.i(LOG_TAG, "--- FIN PARSING: " + familyList.size() + " familles créées ---");
            Log.i(LOG_TAG, "=== FIN REQUÊTE FAMILLES AVEC PARENT ===");
            return familyList;
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "ERREUR lors de la récupération des familles avec parent UUID: " + parentUuid, e);
            Log.e(LOG_TAG, "Type d'erreur: " + e.getClass().getSimpleName());
            Log.e(LOG_TAG, "Message: " + e.getMessage());
            throw e;
        }
    }
}