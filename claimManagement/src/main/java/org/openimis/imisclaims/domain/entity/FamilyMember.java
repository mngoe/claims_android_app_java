package org.openimis.imisclaims.domain.entity;

import java.io.Serializable;

public class FamilyMember implements Serializable {
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
    private String parentUuid;
    
    public FamilyMember() {
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
    
    public String getFullName() {
        return (otherNames != null ? otherNames : "") + " " + (lastName != null ? lastName : "");
    }
}
