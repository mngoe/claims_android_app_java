package org.openimis.imisclaims.domain.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class HealthFacility implements Parcelable {

    @NonNull
    private final String id;

    @Nullable
    private final List<String> hfPrograms;

    public HealthFacility (
            @NonNull String id,
            @Nullable List<String> hfPrograms
    ){
        this.id = id;
        this.hfPrograms = hfPrograms;
    }

    protected HealthFacility (Parcel in){
        id = in.readString();
        hfPrograms = in.createStringArrayList();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags){
        dest.writeString(id);
        dest.writeStringList(hfPrograms);
    }

    @NonNull
    public String getId (){
        return id;
    }

    @Nullable
    public List<String> getHfPrograms(){
        return hfPrograms;
    }

    public static final Creator<HealthFacility> CREATOR = new Creator<>() {
        @Override
        public HealthFacility createFromParcel(Parcel in) {
            return new HealthFacility(in);
        }

        @Override
        public HealthFacility[] newArray(int size) {
            return new HealthFacility[size];
        }
    };
}
