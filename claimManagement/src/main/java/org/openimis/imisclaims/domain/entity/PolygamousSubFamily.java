package org.openimis.imisclaims.domain.entity;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.io.Serializable;
import java.util.List;
import org.openimis.imisclaims.domain.entity.Family;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.util.FamilyTypeConstants;
import org.openimis.imisclaims.SQLHandler;

public class PolygamousSubFamily implements Serializable {
    private static final long serialVersionUID = 1L;
    private String uuid;
    private String chfId;
    private String lastName;
    private String otherNames;
    private String gender;
    private String genderCode;
    private String dob;
    private String photoId;
    private String photoData;
    private String relationship;
    private String familyUuid;
    private String parentUuid; // UUID du chef de famille polygame principal
    private List<FamilyMember> members;
    
    public PolygamousSubFamily() {
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getChfId() {
        return chfId;
    }
    
    public void setChfId(String chfId) {
        this.chfId = chfId;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getOtherNames() {
        return otherNames;
    }
    
    public void setOtherNames(String otherNames) {
        this.otherNames = otherNames;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getGenderCode() {
        return genderCode;
    }
    
    public void setGenderCode(String genderCode) {
        this.genderCode = genderCode;
    }
    
    public String getDob() {
        return dob;
    }
    
    public void setDob(String dob) {
        this.dob = dob;
    }
    
    public String getPhotoId() {
        return photoId;
    }
    
    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }
    
    public String getPhotoData() {
        return photoData;
    }
    
    public void setPhotoData(String photoData) {
        this.photoData = photoData;
    }
    
    public String getPhoto() {
        return photoData;
    }
    
    public void setPhoto(String photo) {
        this.photoData = photo;
    }
    
    public String getRelationship() {
        return relationship;
    }
    
    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
    
    public String getFamilyUuid() {
        return familyUuid;
    }
    
    public void setFamilyUuid(String familyUuid) {
        this.familyUuid = familyUuid;
    }
    
    public String getParentUuid() {
        return parentUuid;
    }
    
    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }
    
    public List<FamilyMember> getMembers() {
        return members;
    }
    
    public void setMembers(List<FamilyMember> members) {
        this.members = members;
    }
    
    public String getFullName() {
        return (otherNames != null ? otherNames : "") + " " + (lastName != null ? lastName : "");
    }
    
    public boolean isPolygamousHead() {
        // Un chef de ménage polygame principal est identifié par sa relation "Head" et l'absence de parentUuid
        // Les chefs de sous-familles ont la relation "SubFamilyHead" et un parentUuid défini
        return "Head".equalsIgnoreCase(relationship) && (parentUuid == null || parentUuid.isEmpty());
    }
    
    public boolean isSubFamilyHead() {
        // Un chef de sous-famille est identifié par sa relation "SubFamilyHead" et un parentUuid défini
        return "SubFamilyHead".equalsIgnoreCase(relationship) && parentUuid != null && !parentUuid.isEmpty();
    }
    
    /**
     * Vérifie si un assuré fait partie d'une famille polygame
     * La détection se base uniquement sur le familyType
     * 
     * @param insuree L'assuré à vérifier
     * @param insureeFamily La famille directe de l'assuré
     * @return true si l'assuré fait partie d'une famille polygame
     */
    public static boolean isInsureeInPolygamousFamily(FamilyMember insuree, Family insureeFamily) {
        if (insuree == null || insureeFamily == null) {
            return false;
        }

        // Détection basée uniquement sur le familyType
        return FamilyTypeConstants.isPolygamyFamilyType(insureeFamily.getFamilyType());
    }
    
    /**
     * Version simplifiée pour vérifier si cet objet PolygamousSubFamily représente
     * un membre d'une famille polygame
     * 
     * @return true si fait partie d'une famille polygame
     */
    public boolean isPartOfPolygamousFamily() {
        // Si c'est un chef polygame principal (pas de parent)
        if (isPolygamousHead()) {
            return true;
        }
        
        // Si c'est un chef de sous-famille (a un parent)
        if (isSubFamilyHead() && parentUuid != null && !parentUuid.isEmpty()) {
            return true;
        }
        
        return false;
    }
    

    
    /**
     * Vérifie si une famille fait partie d'une structure polygame
     * Une famille fait partie d'une structure polygame si :
     * 1. Elle est elle-même polygame
     * 2. Elle est une sous-famille d'une famille polygame
     * 
     * @param family La famille à vérifier
     * @param parentFamily La famille parent (peut être null si c'est une famille principale)
     * @return true si la famille fait partie d'une structure polygame
     */
    public static boolean isPartOfPolygamousFamily(Family family, Family parentFamily) {
        if (family == null) {
            return false;
        }

        // Vérification directe : si la famille est polygame
        if (FamilyTypeConstants.isPolygamyFamilyType(family.getFamilyType())) {
            return true;
        }

        // Vérification indirecte : si c'est une sous-famille d'une famille polygame
        if (parentFamily != null && FamilyTypeConstants.isPolygamyFamilyType(parentFamily.getFamilyType())) {
            return true;
        }

        return false;
    }
    
    /**
     * Vérifie si un UUID correspond à un chef de famille polygame
     * 
     * @param insureeUuid UUID de l'assuré
     * @param familyHeadUuid UUID du chef de famille
     * @return true si l'assuré est le chef de famille
     */
    public static boolean isInsureeHeadOfFamily(String insureeUuid, String familyHeadUuid) {
        return insureeUuid != null && !insureeUuid.isEmpty() &&
               familyHeadUuid != null && !familyHeadUuid.isEmpty() &&
               insureeUuid.equals(familyHeadUuid);
    }
    
    /**
     * Vérifie si cette sous-famille a un contrat d'assurance actif
     * 
     * @param context Contexte Android pour accéder à la base de données
     * @return true si la sous-famille a un contrat actif, false sinon
     */
    public boolean hasActiveInsuranceContract(Context context) {
        if (chfId == null || chfId.trim().isEmpty()) {
            return false;
        }
        
        SQLHandler sqlHandler = new SQLHandler(context);
        // S'assurer que les tables sont créées
        sqlHandler.createTables();
        SQLiteDatabase db = sqlHandler.getReadableDatabase();
        
        // Vérifier si la table existe
        Cursor tableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tblPolicyInquiry'", null);
        if (tableCheck.getCount() == 0) {
            Log.e("PolygamousSubFamily", "Table tblPolicyInquiry n'existe pas!");
            tableCheck.close();
            return false;
        }
        tableCheck.close();
        Log.d("PolygamousSubFamily", "Table tblPolicyInquiry trouvée, exécution de la requête");
        
        String[] columns = {"Status", "ExpiryDate"};
        String[] selectionArgs = {chfId.trim()};
        
        Cursor cursor = null;
        try {
            cursor = db.query("tblPolicyInquiry", columns, "Trim(InsureeNumber)=?", selectionArgs, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String status = cursor.getString(cursor.getColumnIndex("Status"));
                    String expiryDate = cursor.getString(cursor.getColumnIndex("ExpiryDate"));
                    
                    // Vérifier si le contrat est actif
                    if ("ACTIVE".equals(status)) {
                        return true;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            // Log l'erreur mais ne pas faire planter l'application
            android.util.Log.e("PolygamousSubFamily", "Erreur lors de la vérification du contrat d'assurance", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        
        return false;
    }
}
