package org.openimis.imisclaims.domain.entity;

import androidx.annotation.NonNull;

public class Program {

    @NonNull
    private final String idProgram;
    @NonNull
    private final String code;
    @NonNull
    private final String nameProgram;

    public Program(
            @NonNull String idProgram,
            @NonNull String code,
            @NonNull String nameProgram
    ) {
        this.idProgram = idProgram;
        this.code = code;
        this.nameProgram = nameProgram;
    }

    @NonNull
    public String getIdProgram() {
        return idProgram;
    }

    @NonNull
    public String getCode() {
        return code;
    }

    @NonNull
    public String getNameProgram() {
        return nameProgram;
    }
}
