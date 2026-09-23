package org.openimis.imisclaims.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.apollographql.apollo.exception.ApolloCanceledException;

import org.junit.Test;
import org.openimis.imisclaims.network.exception.UnexpectedResponseException;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

public class NetworkUtilsTest {

    @Test
    public void socketFailuresAreReportedAsConnectionErrors() {
        assertTrue(NetworkUtils.isConnectionError(new SocketTimeoutException("Read timed out")));
        assertTrue(NetworkUtils.isConnectionError(new ConnectException("Connection refused")));
        assertTrue(NetworkUtils.isConnectionError(new UnknownHostException("No address associated with hostname")));
        assertTrue(NetworkUtils.isConnectionError(new SocketException("Software caused connection abort")));
        assertTrue(NetworkUtils.isConnectionError(new InterruptedIOException("Socket closed")));
    }

    @Test
    public void unfinishedGraphQlCallIsReportedAsConnectionError() {
        assertTrue(NetworkUtils.isConnectionError(new TimeoutException("Call couldn't finish within 60000ms")));
    }

    @Test
    public void portalPageInsteadOfJsonIsReportedAsConnectionError() {
        assertTrue(NetworkUtils.isConnectionError(new UnexpectedResponseException("Answer is not a valid JSON body", null)));
    }

    @Test
    public void wrappedConnectionErrorIsDetected() {
        assertTrue(NetworkUtils.isConnectionError(new RuntimeException("Uploading claim failed", new SocketTimeoutException("Read timed out"))));
    }

    @Test
    public void cancelledRequestIsAConnectionErrorOnlyWhenTheConnectionWasLost() {
        ApolloCanceledException canceled = new ApolloCanceledException();
        assertTrue(NetworkUtils.isConnectionError(canceled, true));
        assertFalse(NetworkUtils.isConnectionError(canceled, false));
    }

    @Test
    public void connectionErrorStaysDetectedWhileTheConnectionIsLost() {
        assertTrue(NetworkUtils.isConnectionError(new SocketTimeoutException("Read timed out"), true));
        assertTrue(NetworkUtils.isConnectionError(new SocketTimeoutException("Read timed out"), false));
    }

    @Test
    public void serverFailuresAreNotReportedAsConnectionErrors() {
        assertFalse(NetworkUtils.isConnectionError(null));
        assertFalse(NetworkUtils.isConnectionError(new RuntimeException("HTTP 500 - Internal Server Error")));
        assertFalse(NetworkUtils.isConnectionError(new IllegalArgumentException("Query is unsupported")));
        assertFalse(NetworkUtils.isConnectionError(new RuntimeException("HTTP 500 - Internal Server Error"), true));
    }
}
