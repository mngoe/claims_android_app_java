package org.openimis.imisclaims.util;

import androidx.annotation.Nullable;

import com.apollographql.apollo.exception.ApolloCanceledException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Helpers telling apart the failures caused by the device connection (no more mobile data, dropped
 * link, unreachable host, socket timeout) from the failures caused by the server.
 */
public final class NetworkUtils {

    private NetworkUtils() {
        throw new IllegalAccessError("This constructor is private");
    }

    /**
     * @param error the error thrown by a request while talking to the server, may be {@code null}
     * @return {@code true} when the user's connection is the cause of the failure. Socket errors,
     * connect errors, unknown hosts, TLS errors and response read timeouts all extend IOException,
     * and the requests cancelled by {@link ConnectionMonitor} when the connection breaks are
     * reported the same way.
     */
    public static boolean isConnectionError(@Nullable Throwable error) {
        return isConnectionError(error, ConnectionMonitor.isConnectionLost());
    }

    /**
     * @param error          the error thrown by a request while talking to the server, may be
     *                       {@code null}
     * @param connectionLost {@code true} when a working connection was lost, in which case a request
     *                       cancelled in the meantime is a connection error too
     * @return {@code true} when the user's connection is the cause of the failure
     */
    static boolean isConnectionError(@Nullable Throwable error, boolean connectionLost) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof IOException || current instanceof TimeoutException) {
                return true;
            }
            if (connectionLost && current instanceof ApolloCanceledException) {
                return true;
            }
            Throwable cause = current.getCause();
            current = cause == current ? null : cause;
        }
        return false;
    }
}
