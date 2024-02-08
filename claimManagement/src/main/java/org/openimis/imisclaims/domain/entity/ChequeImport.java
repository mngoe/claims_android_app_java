package org.openimis.imisclaims.domain.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ChequeImport implements Parcelable {

    @NonNull
    private final String code;
    @NonNull
    private final String status;

    public ChequeImport (@NonNull String code, @NonNull String status){
        this.code = code;
        this.status = status;
    }

    protected ChequeImport(Parcel in) {
        code = in.readString();
        status = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(code);
        dest.writeString(status);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public String getCode() {
        return code;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public static final Parcelable.Creator<ChequeImport> CREATOR = new Parcelable.Creator<>() {
        @Override
        public ChequeImport createFromParcel(Parcel in) {
            return new ChequeImport(in);
        }

        @Override
        public ChequeImport[] newArray(int size) {
            return new ChequeImport[size];
        }
    };
}
