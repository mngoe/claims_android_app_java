package org.openimis.imisclaims.domain.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FamilyMember implements Parcelable {
    private String chfId;
    private String lastName;
    private String firstName;
    private String gender;
    private Date dateOfBirth;
    private byte[] photo;
    private String photoPath;
    private List<Policy> activePolicies;
    private String relationship;
    private boolean isHead;

    public FamilyMember(String chfId, String lastName, String firstName, String gender, 
                       Date dateOfBirth, byte[] photo, String photoPath, 
                       List<Policy> activePolicies, String relationship, boolean isHead) {
        this.chfId = chfId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.photo = photo;
        this.photoPath = photoPath;
        this.activePolicies = activePolicies;
        this.relationship = relationship;
        this.isHead = isHead;
    }

    // Getters
    public String getChfId() {
        return chfId;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getGender() {
        return gender;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public List<Policy> getActivePolicies() {
        return activePolicies;
    }

    public String getRelationship() {
        return relationship;
    }

    public boolean isHead() {
        return isHead;
    }

    // Setters
    public void setChfId(String chfId) {
        this.chfId = chfId;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public void setActivePolicies(List<Policy> activePolicies) {
        this.activePolicies = activePolicies;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public void setHead(boolean head) {
        isHead = head;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getActivePoliciesString() {
        if (activePolicies == null || activePolicies.isEmpty()) {
            return "Aucune police active";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < activePolicies.size(); i++) {
            Policy policy = activePolicies.get(i);
            sb.append(policy.getName());
            if (i < activePolicies.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    // Parcelable implementation
    protected FamilyMember(Parcel in) {
        chfId = in.readString();
        lastName = in.readString();
        firstName = in.readString();
        gender = in.readString();
        long tmpDateOfBirth = in.readLong();
        dateOfBirth = tmpDateOfBirth != -1 ? new Date(tmpDateOfBirth) : null;
        photo = in.createByteArray();
        photoPath = in.readString();
        activePolicies = in.createTypedArrayList(Policy.CREATOR);
        relationship = in.readString();
        isHead = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chfId);
        dest.writeString(lastName);
        dest.writeString(firstName);
        dest.writeString(gender);
        dest.writeLong(dateOfBirth != null ? dateOfBirth.getTime() : -1);
        dest.writeByteArray(photo);
        dest.writeString(photoPath);
        dest.writeTypedList(activePolicies);
        dest.writeString(relationship);
        dest.writeByte((byte) (isHead ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FamilyMember> CREATOR = new Creator<FamilyMember>() {
        @Override
        public FamilyMember createFromParcel(Parcel in) {
            return new FamilyMember(in);
        }

        @Override
        public FamilyMember[] newArray(int size) {
            return new FamilyMember[size];
        }
    };
}