package org.openimis.imisclaims.domain.entity;

import java.util.List;

public class Family {
    private String uuid;
    private String confirmationNo;
    private String address;
    private String parentUuid;
    private String headInsureeUuid;
    private String headInsureeChfId;
    private String headInsureeName;
    private String location;
    private List<FamilyMember> members;
    
    public Family() {
    }
    
    public Family(String uuid, String confirmationNo, String parentUuid, String headInsureeUuid) {
        this.uuid = uuid;
        this.confirmationNo = confirmationNo;
        this.parentUuid = parentUuid;
        this.headInsureeUuid = headInsureeUuid;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getConfirmationNo() {
        return confirmationNo;
    }
    
    public void setConfirmationNo(String confirmationNo) {
        this.confirmationNo = confirmationNo;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getParentUuid() {
        return parentUuid;
    }
    
    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }
    
    public String getHeadInsureeUuid() {
        return headInsureeUuid;
    }
    
    public void setHeadInsureeUuid(String headInsureeUuid) {
        this.headInsureeUuid = headInsureeUuid;
    }
    
    public String getHeadInsureeChfId() {
        return headInsureeChfId;
    }
    
    public void setHeadInsureeChfId(String headInsureeChfId) {
        this.headInsureeChfId = headInsureeChfId;
    }
    
    public String getHeadInsureeName() {
        return headInsureeName;
    }
    
    public void setHeadInsureeName(String headInsureeName) {
        this.headInsureeName = headInsureeName;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public List<FamilyMember> getMembers() {
        return members;
    }
    
    public void setMembers(List<FamilyMember> members) {
        this.members = members;
    }
    
    /**
     * Vérifie si cette famille a un parent (donc c'est une sous-famille)
     */
    public boolean hasParent() {
        return parentUuid != null && !parentUuid.isEmpty();
    }
    
    /**
     * Vérifie si cette famille est une famille principale (sans parent)
     */
    public boolean isMainFamily() {
        return !hasParent();
    }
}