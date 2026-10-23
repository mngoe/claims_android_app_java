package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloParseException;

import org.openimis.imisclaims.BuildConfig;
import org.openimis.imisclaims.network.apollo.DateCustomTypeAdapter;
import org.openimis.imisclaims.network.apollo.DateTimeCustomTypeAdapter;
import org.openimis.imisclaims.network.apollo.DecimalCustomTypeAdapter;
import org.openimis.imisclaims.network.exception.HttpException;
import org.openimis.imisclaims.network.exception.UnexpectedResponseException;
import org.openimis.imisclaims.network.util.OkHttpUtils;
import org.openimis.imisclaims.type.CustomType;

import java.net.HttpURLConnection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class BaseGraphQLRequest {

    /**
     * Safety net for calls that never complete, kept from the previous behaviour. The connection
     * timeouts of OkHttpUtils are deliberately long and ConnectionMonitor cancels the requests as
     * soon as the connection of the user breaks, this cap only makes sure a call cannot wait forever.
     */
    private static final long TIME_OUT_IN_MS = 600_000;
    private static final String URI = BuildConfig.API_BASE_URL + "api/graphql";

    private static final ApolloClient apolloClient = ApolloClient.builder()
            .okHttpClient(OkHttpUtils.getDefaultOkHttpClient())
            .serverUrl(URI)
            .addCustomTypeAdapter(CustomType.DATE, new DateCustomTypeAdapter())
            .addCustomTypeAdapter(CustomType.DATETIME, new DateTimeCustomTypeAdapter())
            .addCustomTypeAdapter(CustomType.DECIMAL, new DecimalCustomTypeAdapter())
            .build();

    @NonNull
    @WorkerThread
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T extends Operation.Data> Response<T> makeSynchronous(Query<T, ?, ?> query) throws Exception {
        ApolloCall call = apolloClient.query(query);
        return await(call);
    }

    @NonNull
    @WorkerThread
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T extends Operation.Data> Response<T> makeSynchronous(Query<T, ?, ?> query, String uri) throws Exception {
        ApolloClient client = ApolloClient.builder()
                .okHttpClient(OkHttpUtils.getDefaultOkHttpClient())
                .serverUrl(uri)
                .addCustomTypeAdapter(CustomType.DATE, new DateCustomTypeAdapter())
                .addCustomTypeAdapter(CustomType.DATETIME, new DateTimeCustomTypeAdapter())
                .addCustomTypeAdapter(CustomType.DECIMAL, new DecimalCustomTypeAdapter())
                .build();

        ApolloCall call = client.query(query);
        return await(call);
    }

    @NonNull
    @WorkerThread
    protected <T extends Operation.Data> Response<T> makeSynchronous(Operation<T, ?, ?> query) throws Exception {
        Response<T> response = await(toCall(query));
        if (response.hasErrors()) {
            String details = response.getErrors().get(0).getMessage();
            if (details.equals("User not authorized for this operation")) {
                throw new HttpException(
                        HttpURLConnection.HTTP_UNAUTHORIZED,
                        details,
                        null,
                        null
                );
            }
            throw new RuntimeException(response.toString());
        }
        return response;
    }

    @NonNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Operation.Data> ApolloCall<T> toCall(@NonNull Operation<T, ?, ?> query) {
        if (query instanceof Query) {
            return apolloClient.query((Query) query);
        }
        if (query instanceof Mutation) {
            return apolloClient.mutate((Mutation) query);
        }
        throw new IllegalArgumentException("Query is unsupported");
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private static <T extends Operation.Data> Response<T> await(@NonNull ApolloCall<T> call) throws Exception {
        Semaphore semaphore = new Semaphore(0);
        final Response<T>[] responses = new Response[1];
        final ApolloException[] failures = new ApolloException[1];

        call.enqueue(new ApolloCall.Callback<T>() {
            @Override
            public void onResponse(@NonNull Response<T> response) {
                responses[0] = response;
                semaphore.release();
            }

            @Override
            public void onFailure(@NonNull ApolloException e) {
                failures[0] = e;
                semaphore.release();
            }
        });

        if (!semaphore.tryAcquire(TIME_OUT_IN_MS, TimeUnit.MILLISECONDS)) {
            // Do not let the request run in the background, the user is about to retry.
            call.cancel();
            throw new TimeoutException("Call couldn't finish within " + TIME_OUT_IN_MS + "ms");
        }
        if (failures[0] != null) {
            if (failures[0] instanceof ApolloParseException) {
                // The answer was not the expected GraphQL payload: on a connection without data the
                // operator answers with a portal page, which is reported as a connection problem.
                throw new UnexpectedResponseException("Answer is not a valid GraphQL body", failures[0]);
            }
            throw failures[0];
        }
        return responses[0];
    }
}
