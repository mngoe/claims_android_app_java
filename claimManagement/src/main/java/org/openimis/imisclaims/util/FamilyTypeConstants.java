package org.openimis.imisclaims.util;

/**
 * Constantes pour les types de famille
 * Équivalent des constantes JavaScript pour la détection de polygamie
 */
public class FamilyTypeConstants {
    
    /**
     * Code du type de famille polygame
     * Équivalent de FAMILY_TYPE_POLYGAMY_CODE en JavaScript
     */
    public static final String FAMILY_TYPE_POLYGAMY_CODE = "P";
    
    /**
     * Code du type de famille normale/monogame
     */
    public static final String FAMILY_TYPE_NORMAL_CODE = "H";
    
    /**
     * Autres codes de types de famille si nécessaire
     */
    public static final String FAMILY_TYPE_SINGLE_CODE = "S";
    
    // Empêcher l'instanciation de cette classe utilitaire
    private FamilyTypeConstants() {
        throw new UnsupportedOperationException("Cette classe ne doit pas être instanciée");
    }
    
    /**
     * Vérifie si un code de type de famille correspond à une famille polygame
     * 
     * @param familyTypeCode Le code à vérifier
     * @return true si c'est un type polygame
     */
    public static boolean isPolygamyFamilyType(String familyTypeCode) {
        return FAMILY_TYPE_POLYGAMY_CODE.equals(familyTypeCode);
    }
    
    /**
     * Vérifie si un code de type de famille correspond à une famille normale
     * 
     * @param familyTypeCode Le code à vérifier
     * @return true si c'est un type normal
     */
    public static boolean isNormalFamilyType(String familyTypeCode) {
        return FAMILY_TYPE_NORMAL_CODE.equals(familyTypeCode);
    }
}