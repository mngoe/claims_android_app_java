package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Response;

import org.openimis.imisclaims.CreateClaimMutation;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.type.CreateClaimMutationInput;

import java.util.Objects;

public class CreateClaimGraphQLRequest extends BaseGraphQLRequest{

    @WorkerThread
    @NonNull
    public CreateClaimMutation.Data create(
            @NonNull Claim claim,
            @NonNull int hfId,
            @NonNull int adminId,
            @NonNull int insureeId
    ) throws Exception{
        Response<CreateClaimMutation.Data> response = makeSynchronous(new CreateClaimMutation(
                CreateClaimMutationInput.builder()
                        .code(claim.getClaimNumber())
                        .visitType(claim.getVisitType())
                        .program(Integer.valueOf(claim.getClaimProgram()))
                        .dateClaimed(claim.getDateClaimed())
                        .healthFacilityId(hfId)
                        .icdId(Integer.valueOf(claim.getMainDg()))
                        .dateFrom(claim.getVisitDateFrom())
                        .dateTo(claim.getVisitDateTo())
                        .insureeId(insureeId)
                        .adminId(adminId)
                        .build()
        ));
        return Objects.requireNonNull(response.getData());
    }
}