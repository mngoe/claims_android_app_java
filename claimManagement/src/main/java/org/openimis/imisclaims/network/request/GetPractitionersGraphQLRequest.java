package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetClaimAdminsQuery;

public class GetPractitionersGraphQLRequest extends BaseGraphQLRequest {

    private static final String URI = "https://csureport.minsante.cm/api/graphql";

    @NonNull
    @WorkerThread
    public GetClaimAdminsQuery.ClaimAdmins get(@NonNull int page) throws Exception {
        return makeSynchronous(new GetClaimAdminsQuery(Input.fromNullable(page)), URI).getData().claimAdmins();
    }
}
