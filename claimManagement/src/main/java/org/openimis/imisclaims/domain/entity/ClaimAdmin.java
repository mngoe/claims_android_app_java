package org.openimis.imisclaims.domain.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ClaimAdmin implements Parcelable {

    @NonNull
    private final String id;
    @NonNull
    private final String lastName;
    @NonNull
    private final String otherNames;
    @NonNull
    private final String claimAdminCode;
    @NonNull
    private final String hfCode;
    @NonNull
    private final String hfId;
    @Nullable
    private final List<String> programs;

    public ClaimAdmin(
            @NonNull String id,
            @NonNull String lastName,
            @NonNull String otherNames,
            @NonNull String claimAdminCode,
            @NonNull String healthFacilityCode,
            @NonNull String hfId,
            @Nullable List<String> programs
    ){
        this.id = id;
        this.lastName = lastName;
        this.otherNames = otherNames;
        this.claimAdminCode = claimAdminCode;
        this.hfCode = healthFacilityCode;
        this.hfId = hfId;
        this.programs = programs;
    }

    protected ClaimAdmin(Parcel in) {
        id = in.readString();
        lastName = in.readString();
        otherNames = in.readString();
        claimAdminCode = in.readString();
        hfCode = in.readString();
        hfId = in.readString();
        programs = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(lastName);
        dest.writeString(otherNames);
        dest.writeString(claimAdminCode);
        dest.writeString(hfCode);
        dest.writeString(hfId);
        dest.writeStringList(programs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public String getId() { return id; }

    @NonNull
    public String getDisplayName() {
        return lastName + " " + otherNames;
    }

    @NonNull
    public String getLastName() {
        return lastName;
    }

    @NonNull
    public String getOtherNames() {
        return otherNames;
    }

    @NonNull
    public String getClaimAdminCode() {
        return claimAdminCode;
    }

    @NonNull
    public String getHealthFacilityCode() {
        return hfCode;
    }

    @NonNull
    public String getHfId(){return hfId;}

    @Nullable
    public List<String> getPrograms(){
        return programs;
    }

    public static final Creator<ClaimAdmin> CREATOR = new Creator<>() {
        @Override
        public ClaimAdmin createFromParcel(Parcel in) {
            return new ClaimAdmin(in);
        }

        @Override
        public ClaimAdmin[] newArray(int size) {
            return new ClaimAdmin[size];
        }
    };
}
