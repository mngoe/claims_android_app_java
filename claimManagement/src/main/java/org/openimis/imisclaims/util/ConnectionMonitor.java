package org.openimis.imisclaims.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.openimis.imisclaims.network.util.OkHttpUtils;
import org.openimis.imisclaims.tools.Log;

/**
 * Watches the connection of the device and cancels the requests in flight as soon as a working
 * connection is lost (mobile data exhausted, link dropped, ...).
 *
 * The network timeouts of the app are deliberately generous because a slow transfer is normal on the
 * Cameroonian mobile networks, so they must not be shortened to report a broken connection. This
 * monitor is what makes a request fail when the connection of the user breaks: the request is
 * cancelled instead of being left waiting for its timeout, and the app can tell the user about the
 * connection problem (see {@link NetworkUtils}).
 */
public final class ConnectionMonitor {

    private static final String LOG_TAG = "CONNECTIONMONITOR";

    /**
     * A connection that comes back within this delay is a transport switch (wifi to mobile data for
     * instance), not a connection the user lost.
     */
    private static final long LOST_CONNECTION_GRACE_PERIOD_MS = 5_000L;

    private static volatile ConnectionMonitor instance;

    @NonNull
    private final ConnectivityManager connectivityManager;
    @NonNull
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Whether Android currently validated a network, meaning the device can really reach the
     * internet. Only a loss of such a connection is reported, so a device whose network is never
     * validated (which happens on some networks) keeps its previous behaviour.
     */
    private volatile boolean usableConnection;

    private volatile boolean connectionLost;

    private final Runnable cancelRequests = () -> {
        if (usableConnection) {
            return;
        }
        connectionLost = true;
        Log.i(LOG_TAG, "the connection was lost, cancelling the requests in flight");
        OkHttpUtils.getDefaultOkHttpClient().dispatcher().cancelAll();
    };

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            onConnectivityChanged();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
            onConnectivityChanged();
        }

        @Override
        public void onLost(@NonNull Network network) {
            onConnectivityChanged();
        }

        @Override
        public void onUnavailable() {
            onConnectivityChanged();
        }
    };

    private ConnectionMonitor(@NonNull Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        usableConnection = hasUsableConnection();
    }

    /**
     * Starts watching the connection of the device. Called once, when the application starts.
     *
     * @param context any context of the application
     * @return the monitor watching the connection of the device
     */
    @NonNull
    public static ConnectionMonitor install(@NonNull Context context) {
        ConnectionMonitor monitor = instance;
        if (monitor == null) {
            synchronized (ConnectionMonitor.class) {
                monitor = instance;
                if (monitor == null) {
                    monitor = new ConnectionMonitor(context.getApplicationContext());
                    monitor.register();
                    instance = monitor;
                }
            }
        }
        return monitor;
    }

    /**
     * @return {@code true} when the connection of the user was lost, which means the requests that
     * fail can be reported to the user as a connection problem.
     */
    public static boolean isConnectionLost() {
        ConnectionMonitor monitor = instance;
        return monitor != null && monitor.connectionLost;
    }

    private void register() {
        try {
            connectivityManager.registerNetworkCallback(
                    new NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build(),
                    networkCallback
            );
        } catch (Exception e) {
            Log.e(LOG_TAG, "the connection of the device cannot be watched", e);
        }
    }

    private void onConnectivityChanged() {
        boolean usable = hasUsableConnection();
        usableConnection = usable;
        handler.removeCallbacks(cancelRequests);
        if (usable) {
            connectionLost = false;
            return;
        }
        // Give the device the time to switch to another network before failing the requests.
        handler.postDelayed(cancelRequests, LOST_CONNECTION_GRACE_PERIOD_MS);
    }

    /**
     * @return {@code true} when Android validated a network, which means the device can really reach
     * the internet. A network whose mobile data are exhausted, or a captive portal, is not
     * validated. Every network is checked since the requests follow the routing of the device.
     */
    private boolean hasUsableConnection() {
        try {
            if (isUsable(connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork()))) {
                return true;
            }
            for (Network network : connectivityManager.getAllNetworks()) {
                if (isUsable(connectivityManager.getNetworkCapabilities(network))) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "the connection of the device cannot be read", e);
        }
        return false;
    }

    static boolean isUsable(@Nullable NetworkCapabilities capabilities) {
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
