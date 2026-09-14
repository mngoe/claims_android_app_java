package org.openimis.imisclaims.network.exception;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

/**
 * Thrown when a successful answer cannot be read as the expected JSON payload, which typically
 * happens when the mobile connection intercepts the request and answers with a portal page instead
 * of the requested data. Extending IOException makes it reported as a connection problem.
 */
public class UnexpectedResponseException extends IOException {

    public UnexpectedResponseException(@NonNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
