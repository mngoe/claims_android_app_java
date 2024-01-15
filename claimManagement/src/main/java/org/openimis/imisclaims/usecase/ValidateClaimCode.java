package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.network.request.ValidateClaimCodeGraphQLRequest;

public class ValidateClaimCode {

    @NonNull
    private final ValidateClaimCodeGraphQLRequest request;

    public ValidateClaimCode(){
        this(new ValidateClaimCodeGraphQLRequest());
    }

    public ValidateClaimCode(@NonNull ValidateClaimCodeGraphQLRequest request){
        this.request = request;
    }

    @NonNull
    @WorkerThread
    public Boolean execute (@NonNull String claimCode) throws Exception{
        return request.get(claimCode);
    }
}
