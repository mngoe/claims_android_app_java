package org.openimis.imisclaims.domain.entity;

import androidx.annotation.NonNull;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class Prescripteur {

    private long id;

    @NonNull
    private String lastName;

    @NonNull
    private String otherNames;

    @NonNull
    private String nin;

    private String telephone;

    @NonNull
    private String structurePrincipale;

    private String specialite;

    private String statut;

    private List<String> fosaAutorisees;

    private Date dateEntree;

    private Date dateSortie;

    private String uuid;
    private String code;

    public Prescripteur() {
        this.fosaAutorisees = new ArrayList<>();
    }

    public Prescripteur(long id,
                        @NonNull String lastName,
                        @NonNull String otherNames,
                        @NonNull String nin,
                        String telephone,
                        @NonNull String structurePrincipale,
                        String specialite,
                        String statut,
                        List<String> fosaAutorisees,
                        Date dateEntree,
                        Date dateSortie) {
        this.id = id;
        this.lastName = lastName;
        this.otherNames = otherNames;
        this.nin = nin;
        this.telephone = telephone;
        this.structurePrincipale = structurePrincipale;
        this.specialite = specialite;
        this.statut = statut;
        this.fosaAutorisees = fosaAutorisees != null ? fosaAutorisees : new ArrayList<>();
        this.dateEntree = dateEntree;
        this.dateSortie = dateSortie;
    }

    // Getters et Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getLastName() {
        return lastName;
    }

    public void setLastName(@NonNull String lastName) {
        this.lastName = lastName;
    }

    @NonNull
    public String getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(@NonNull String otherNames) {
        this.otherNames = otherNames;
    }

    @NonNull
    public String getNin() {
        return nin;
    }

    public void setNin(@NonNull String nin) {
        this.nin = nin;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    @NonNull
    public String getStructurePrincipale() {
        return structurePrincipale;
    }

    public void setStructurePrincipale(@NonNull String structurePrincipale) {
        this.structurePrincipale = structurePrincipale;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public List<String> getFosaAutorisees() {
        return fosaAutorisees;
    }

    public void setFosaAutorisees(List<String> fosaAutorisees) {
        this.fosaAutorisees = fosaAutorisees;
    }

    public Date getDateEntree() {
        return dateEntree;
    }

    public void setDateEntree(Date dateEntree) {
        this.dateEntree = dateEntree;
    }

    public Date getDateSortie() {
        return dateSortie;
    }

    public void setDateSortie(Date dateSortie) {
        this.dateSortie = dateSortie;
    }

    public void addFosaAutorisee(String fosa) {
        if (!this.fosaAutorisees.contains(fosa)) {
            this.fosaAutorisees.add(fosa);
        }
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        String namePart = (lastName != null ? lastName : "") + (otherNames != null && !otherNames.isEmpty() ? " " + otherNames : "");
        String codePart = (code != null && !code.isEmpty()) ? (code + " - ") : "";
        return (nin != null && !nin.isEmpty() ? nin + " - " : "") + codePart + namePart;
    }
}