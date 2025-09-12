package org.openimis.imisclaims.util;

import androidx.annotation.NonNull;

import org.openimis.imisclaims.AppInformation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final SimpleDateFormat DATE_FORMAT = AppInformation.DateTimeInfo.getDefaultDateFormatter();
    private static final SimpleDateFormat EXPIRY_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    private DateUtils() {
        throw new IllegalAccessError("This constructor is private");
    }

    @NonNull
    public static String toDateString(@NonNull Date date) {
        return DATE_FORMAT.format(date);
    }

    @NonNull
    public static Date dateFromString(@NonNull String date) throws ParseException {
        return DATE_FORMAT.parse(date);
    }

    @NonNull
    public static String toExpiryDateString(@NonNull Date date) {
        return EXPIRY_DATE_FORMAT.format(date);
    }

    @NonNull
    public static String formatExpiryDateString(@NonNull String dateString) {
        try {
            Date date = DATE_FORMAT.parse(dateString);
            return EXPIRY_DATE_FORMAT.format(date);
        } catch (ParseException e) {
            // Si le parsing échoue, retourner la chaîne originale
            return dateString;
        }
    }

}
