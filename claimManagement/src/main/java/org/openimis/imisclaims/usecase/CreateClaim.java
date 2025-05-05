package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.network.request.CreateClaimGraphQLRequest;

public class CreateClaim {

    @NonNull
    private final CreateClaimGraphQLRequest createClaimGraphQLRequest;
    @NonNull
    private final CheckMutation checkMutation;

    public CreateClaim() {
        this(new CheckMutation(), new CreateClaimGraphQLRequest());
    }

    public CreateClaim(
            @NonNull CheckMutation checkMutation,
            @NonNull CreateClaimGraphQLRequest createClaimGraphQLRequest
    ) {
        this.createClaimGraphQLRequest = createClaimGraphQLRequest;
        this.checkMutation = checkMutation;
    }

    @WorkerThread
    public Integer execute(
            Claim claim,
            int adminId,
            int hfId,
            int insureeId,
            int programId,
            int diagnosisId,
            String programCode
    ) throws Exception {
        return checkMutation.execute(
                createClaimGraphQLRequest.create(claim,hfId ,adminId, insureeId, programId, diagnosisId, programCode),
                "Error while creating policy for beneficiary '" + claim.getClaimNumber() + "'");
    }
}
