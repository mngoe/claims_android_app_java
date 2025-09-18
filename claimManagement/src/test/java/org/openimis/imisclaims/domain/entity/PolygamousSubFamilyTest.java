package org.openimis.imisclaims.domain.entity;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import org.openimis.imisclaims.util.FamilyTypeConstants;

/**
 * Tests unitaires pour la logique de détection de famille polygame
 * Équivalent des tests JavaScript analysés
 */
public class PolygamousSubFamilyTest {
    
    private PolygamousSubFamily polygamousHead;
    private PolygamousSubFamily subFamilyHead;
    
    @Before
    public void setUp() {
        // Configuration d'un chef polygame principal
        polygamousHead = new PolygamousSubFamily();
        polygamousHead.setUuid("head-uuid-123");
        polygamousHead.setRelationship("Head");
        polygamousHead.setParentUuid(null); // Pas de parent pour le chef principal
        
        // Configuration d'un chef de sous-famille
        subFamilyHead = new PolygamousSubFamily();
        subFamilyHead.setUuid("sub-head-uuid-456");
        subFamilyHead.setRelationship("SubFamilyHead");
        subFamilyHead.setParentUuid("head-uuid-123"); // Référence au chef principal
    }
    
    @Test
    public void testIsPolygamousHead() {
        assertTrue("Le chef polygame principal doit être identifié correctement", 
                  polygamousHead.isPolygamousHead());
        
        assertFalse("Le chef de sous-famille ne doit pas être identifié comme chef principal", 
                   subFamilyHead.isPolygamousHead());
    }
    
    @Test
    public void testIsSubFamilyHead() {
        assertTrue("Le chef de sous-famille doit être identifié correctement", 
                  subFamilyHead.isSubFamilyHead());
        
        assertFalse("Le chef polygame principal ne doit pas être identifié comme chef de sous-famille", 
                   polygamousHead.isSubFamilyHead());
    }
    

    
    @Test
    public void testIsInsureeInPolygamousFamily_DirectPolygamy() {
        // Arrange
        FamilyMember insuree = new FamilyMember();
        insuree.setUuid("insuree-uuid-1");
        
        Family polygamousFamily = new Family();
        polygamousFamily.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_POLYGAMY_CODE);
        
        // Act
        boolean result = PolygamousSubFamily.isInsureeInPolygamousFamily(insuree, polygamousFamily);
        
        // Assert
        assertTrue("L'assuré devrait être détecté comme faisant partie d'une famille polygame directement", result);
    }
    
    @Test
    public void testIsInsureeInPolygamousFamily_IndirectPolygamy() {
        FamilyMember insuree = new FamilyMember();
        insuree.setUuid("insuree-123");
        
        Family currentFamily = new Family();
        currentFamily.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_NORMAL_CODE);
        
        Family parentFamily = new Family();
        parentFamily.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_POLYGAMY_CODE);
        parentFamily.setHeadInsureeUuid("insuree-123"); // L'assuré est chef de la famille parent
        
        // Test: famille normale - détection basée uniquement sur familyType
        boolean result = PolygamousSubFamily.isInsureeInPolygamousFamily(insuree, currentFamily);
        assertFalse("L'assuré ne devrait pas être dans une famille polygame car sa famille est normale", result);
        
        // Test: famille polygame - détection basée uniquement sur familyType
        currentFamily.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_POLYGAMY_CODE);
        result = PolygamousSubFamily.isInsureeInPolygamousFamily(insuree, currentFamily);
        assertTrue("L'assuré devrait être dans une famille polygame car sa famille est polygame", result);
    }
    
    @Test
    public void testIsInsureeHeadOfFamily() {
        // Test avec UUID correspondant
        boolean result = PolygamousSubFamily.isInsureeHeadOfFamily("insuree-123", "insuree-123");
        assertTrue("L'assuré devrait être chef de famille", result);
        
        // Test avec UUID différent
        result = PolygamousSubFamily.isInsureeHeadOfFamily("insuree-123", "autre-insuree");
        assertFalse("L'assuré ne devrait pas être chef de famille", result);
        
        // Test avec UUID null
        result = PolygamousSubFamily.isInsureeHeadOfFamily("insuree-123", null);
        assertFalse("L'assuré ne devrait pas être chef de famille avec UUID null", result);
        
        // Test avec insuree UUID null
        result = PolygamousSubFamily.isInsureeHeadOfFamily(null, "insuree-123");
        assertFalse("L'assuré null ne devrait pas être chef de famille", result);
    }
    
    @Test
    public void testIsPartOfPolygamousFamily() {
        // Arrange
        FamilyMember insuree = new FamilyMember();
        insuree.setUuid("insuree-uuid-1");
        
        Family normalFamily = new Family();
        normalFamily.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_NORMAL_CODE);
        
        Family polygamousParent = new Family();
        polygamousParent.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_POLYGAMY_CODE);
        polygamousParent.setHeadInsureeUuid("insuree-uuid-1"); // L'assuré est chef de famille du parent
        
        // Act - détection basée uniquement sur le familyType de la famille directe
        boolean result = PolygamousSubFamily.isInsureeInPolygamousFamily(insuree, normalFamily);
        
        // Assert
        assertFalse("L'assuré ne devrait pas être détecté comme polygame car sa famille directe est normale", result);
    }
    
    @Test
    public void testIsInsureeInPolygamousFamily_NullInsuree() {
        // Arrange
        Family family = new Family();
        family.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_POLYGAMY_CODE);
        
        // Act
        boolean result = PolygamousSubFamily.isInsureeInPolygamousFamily(null, family);
        
        // Assert
        assertFalse("Un assuré null ne devrait pas être détecté comme polygame", result);
    }
    
    @Test
    public void testIsInsureeInPolygamousFamily_NotHeadOfPolygamousParent() {
        // Arrange - L'assuré n'est PAS chef de famille du parent polygame
        FamilyMember insuree = new FamilyMember();
        insuree.setUuid("insuree-uuid-1");
        
        Family normalFamily = new Family();
        normalFamily.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_NORMAL_CODE);
        
        Family polygamousParent = new Family();
        polygamousParent.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_POLYGAMY_CODE);
        polygamousParent.setHeadInsureeUuid("different-head-uuid"); // Différent de l'assuré
        
        // Act - détection basée uniquement sur le familyType de la famille directe
        boolean result = PolygamousSubFamily.isInsureeInPolygamousFamily(insuree, normalFamily);
        
        // Assert
        assertFalse("L'assuré ne devrait pas être détecté comme polygame car sa famille directe est normale", result);
    }
    
    @Test
    public void testIsInsureeInPolygamousFamily_DirectPolygamyOverridesParent() {
        // Arrange - Famille directe polygame, peu importe le parent
        FamilyMember insuree = new FamilyMember();
        insuree.setUuid("insuree-uuid-1");
        
        Family polygamousFamily = new Family();
        polygamousFamily.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_POLYGAMY_CODE);
        
        Family normalParent = new Family();
        normalParent.setFamilyType(FamilyTypeConstants.FAMILY_TYPE_NORMAL_CODE);
        
        // Act - détection basée uniquement sur le familyType de la famille directe
        boolean result = PolygamousSubFamily.isInsureeInPolygamousFamily(insuree, polygamousFamily);
        
        // Assert
        assertTrue("L'assuré devrait être détecté comme polygame si sa famille directe est polygame", result);
    }
    
    @Test
    public void testFamilyTypeConstants() {
        assertEquals("Le code de famille polygame doit être 'P'", 
                    "P", FamilyTypeConstants.FAMILY_TYPE_POLYGAMY_CODE);
        
        assertTrue("La méthode isPolygamyFamilyType doit identifier correctement le code 'P'", 
                  FamilyTypeConstants.isPolygamyFamilyType("P"));
        
        assertFalse("La méthode isPolygamyFamilyType ne doit pas identifier 'H' comme polygame", 
                   FamilyTypeConstants.isPolygamyFamilyType("H"));
    }
}