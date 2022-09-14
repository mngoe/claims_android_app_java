package org.openimis.imisclaims;

public class Escape {
    public boolean CheckCHFID(String InsureeNumber) {

        if (InsureeNumber.length() == 0){
            return false;
        }

        if (InsureeNumber.length() != 6){
            return false;
        }

        return true;
    }
}
