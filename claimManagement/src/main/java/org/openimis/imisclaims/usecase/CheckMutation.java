package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openimis.imisclaims.CheckMutationQuery;
import org.openimis.imisclaims.network.request.CheckMutationGraphQLRequest;
import org.openimis.imisclaims.tools.Log;

import java.util.concurrent.TimeoutException;

import io.sentry.Sentry;
import io.sentry.SentryLevel;

public class CheckMutation {

    private static final String TRACE_TAG = "SYNC_TRACE";
    private static final long DEFAULT_TIMEOUT = 600_000L;
    private static final long DEFAULT_DELAY = 1500L;
    private static final int STATUS_RECEIVED = 0;
    private static final int STATUS_ERROR = 1;

    private final long timeOutMs;
    private final long delayMs;

    @NonNull
    private final CheckMutationGraphQLRequest request;

    public CheckMutation() {
        this(DEFAULT_TIMEOUT, DEFAULT_DELAY);
    }

    public CheckMutation(long timeOutMs, long delayMs) {
        this(timeOutMs, delayMs, new CheckMutationGraphQLRequest());
    }

    public CheckMutation(long timeOutMs, long delayMs, @NonNull CheckMutationGraphQLRequest request) {
        this.timeOutMs = timeOutMs;
        this.delayMs = delayMs;
        this.request = request;
    }

    @WorkerThread
    public Integer execute(@NonNull String uuid, @NonNull String message) throws Exception {

        long start = System.currentTimeMillis();
        String thread = Thread.currentThread().getName() + ":" + Thread.currentThread().getId();

        String startMsg = String.format(
                "SYNC_TRACE session=- event=CHECKMUTATION_START ts=%d thread=%s cmid=%s",
                start, thread, uuid
        );

        Log.i(TRACE_TAG, startMsg);

        Sentry.configureScope(scope -> {
            scope.setTag("sync_event", "CHECKMUTATION_START");
            scope.setExtra("cmid", uuid);
            scope.setExtra("thread", thread);
            scope.setExtra("timestamp", String.valueOf(start));
            scope.setExtra("timeoutMs", String.valueOf(timeOutMs));
        });

        Sentry.captureMessage(startMsg, SentryLevel.INFO);

        CheckMutationQuery.Node node = null;
        Integer status = null;

        try {

            do {
                if (node != null) {
                    Thread.sleep(delayMs);
                }

                node = request.execute(uuid);
                status = node.status();

                long now = System.currentTimeMillis();

                Integer finalStatus = status;
                Sentry.configureScope(scope -> {
                    scope.setTag("sync_event", "CHECKMUTATION_POLL");
                    scope.setExtra("cmid", uuid);
                    scope.setExtra("status", String.valueOf(finalStatus));
                    scope.setExtra("elapsedMs", String.valueOf(now - start));
                });

                if (now >= start + timeOutMs) {
                    throw new TimeoutException(
                            "Could not retrieve the mutation status of '" + uuid + "' within " + timeOutMs + "ms"
                    );
                }

            } while (status == null || status == STATUS_RECEIVED);

            long end = System.currentTimeMillis();

            String doneMsg = String.format(
                    "SYNC_TRACE session=- event=CHECKMUTATION_DONE ts=%d thread=%s cmid=%s status=%s durationMs=%d",
                    end, thread, uuid, String.valueOf(status), end - start
            );

            Log.i(TRACE_TAG, doneMsg);

            Integer finalStatus1 = status;
            Sentry.configureScope(scope -> {
                scope.setTag("sync_event", "CHECKMUTATION_DONE");
                scope.setExtra("cmid", uuid);
                scope.setExtra("status", String.valueOf(finalStatus1));
                scope.setExtra("durationMs", String.valueOf(end - start));
            });

            Sentry.captureMessage(doneMsg, SentryLevel.INFO);

            if (status == STATUS_ERROR) {
                String errorDetail = getErrorDetail(node.error());

                IllegalStateException ex =
                        new IllegalStateException(message + ":\n" + errorDetail);

                Sentry.captureException(ex);
                throw ex;
            }

            return status;

        } catch (Exception e) {

            long errorTs = System.currentTimeMillis();

            String errorMsg = String.format(
                    "SYNC_TRACE session=- event=CHECKMUTATION_EXCEPTION ts=%d thread=%s cmid=%s error=%s",
                    errorTs, thread, uuid, e.getMessage()
            );

            Log.e(TRACE_TAG, errorMsg);

            Sentry.configureScope(scope -> {
                scope.setTag("sync_event", "CHECKMUTATION_EXCEPTION");
                scope.setExtra("cmid", uuid);
                scope.setExtra("thread", thread);
                scope.setExtra("timestamp", String.valueOf(errorTs));
            });

            Sentry.captureException(e);
            throw e;
        }
    }

    private String getErrorDetail(String error) {
        try {
            JSONArray array = new JSONArray(error);
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);

                if (builder.length() != 0) {
                    builder.append("\n");
                }

                builder.append(" - ");
                builder.append(object.getString("detail"));
            }

            if (builder.length() != 0) {
                return builder.toString();
            }

        } catch (Exception ignored) {
            //
        }

        return error;
    }
}