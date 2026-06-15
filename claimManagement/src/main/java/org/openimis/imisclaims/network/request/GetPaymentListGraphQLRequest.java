package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.BuildConfig;
import org.openimis.imisclaims.GetPaymentListQuery;

public class GetPaymentListGraphQLRequest extends BaseGraphQLRequest {

    private static final String URI = BuildConfig.MASTER_DATA_URL + "api/graphql";

    @NonNull
    @WorkerThread
    public GetPaymentListQuery.Node get(
            @NonNull String claimAdministratorCode
    ) throws Exception {
        return makeSynchronous(new GetPaymentListQuery(
                Input.optional(claimAdministratorCode)
        ), URI).getData().claimAdmins().edges().get(0).node();
    }
}
