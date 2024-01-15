package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.ValidateClaimCodeQuery;

public class ValidateClaimCodeGraphQLRequest extends BaseGraphQLRequest{

    @NonNull
    @WorkerThread
    public Boolean get(@NonNull String claimCode) throws Exception{
        return makeSynchronous(new ValidateClaimCodeQuery(claimCode)).getData().validateClaimCode();
    }
}
