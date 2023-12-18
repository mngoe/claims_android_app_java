package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetClaimAdminsQuery;

public class GetPractitionersGraphQLRequest extends  BaseGraphQLRequest{

    @NonNull
    @WorkerThread
    public GetClaimAdminsQuery.ClaimAdmins get() throws Exception {
        return makeSynchronous(new GetClaimAdminsQuery()).getData().claimAdmins();
    }
}
