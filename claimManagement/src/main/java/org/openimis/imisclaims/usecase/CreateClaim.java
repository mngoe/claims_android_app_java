package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openimis.imisclaims.SQLHandler;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.domain.entity.Insuree;
import org.openimis.imisclaims.network.exception.HttpException;
import org.openimis.imisclaims.network.request.CreateClaimGraphQLRequest;
import org.openimis.imisclaims.tools.Log;

import java.net.HttpURLConnection;
import java.util.List;

import okhttp3.Response;

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
    public Response execute(Claim claim, int adminId, int hfId, int insureeId, int programId, int diagnosisId) throws Exception {
        return createClaimGraphQLRequest.create(claim,hfId ,adminId, insureeId, programId, diagnosisId);
    }
}
