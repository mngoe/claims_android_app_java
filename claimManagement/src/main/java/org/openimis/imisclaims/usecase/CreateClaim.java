package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.SQLHandler;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.network.exception.HttpException;
import org.openimis.imisclaims.network.request.CreateClaimGraphQLRequest;

import java.net.HttpURLConnection;
import java.util.List;

public class CreateClaim {

    @NonNull
    private final CreateClaimGraphQLRequest createClaimGraphQLRequest;

    public CreateClaim() {
        this(new CreateClaimGraphQLRequest());
    }

    public CreateClaim(
            @NonNull CreateClaimGraphQLRequest createPolicyGraphQLRequest
    ) {
        this.createClaimGraphQLRequest = createPolicyGraphQLRequest;
    }

    @WorkerThread
    public void execute(List<Claim> claims, int adminId, int hfId) throws Exception {
        for (Claim claim : claims) {
            int insureeId = 0;

            createClaimGraphQLRequest.create(claim,hfId ,adminId, insureeId);
        }
    }
}
