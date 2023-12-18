package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Response;

import org.openimis.imisclaims.CreateClaimMutation;
import org.openimis.imisclaims.GetClaimsQuery;
import org.openimis.imisclaims.GetServicesQuery;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.domain.entity.SubServiceItem;
import org.openimis.imisclaims.network.util.Mapper;
import org.openimis.imisclaims.type.CreateClaimMutationInput;

public class CreateClaimGraphQLRequest extends  BaseGraphQLRequest{

    @WorkerThread
    @NonNull
    public CreateClaimMutation.Data create(
            @NonNull Claim claim,
            @NonNull int insureeId,
            @NonNull int adminId,
            @NonNull int hfId
            ) throws Exception {
        Response<CreateClaimMutation.Data> response = makeSynchronous(new CreateClaimMutation(
                CreateClaimMutationInput.builder()
                        .uuid(claim.getUuid())
                        .code(claim.getClaimNumber())
                        .insureeId(insureeId)
                        .adminId(adminId)
                        .dateFrom(claim.getVisitDateFrom())
                        .dateTo(claim.getVisitDateTo())
                        .icdId(Integer.valueOf(claim.getMainDg()))
                        .dateClaimed(claim.getDateClaimed())
                        .healthFacilityId(hfId)
                        .program(Integer.valueOf(claim.getClaimProgram()))
                        .visitType(claim.getVisitType())
                        .services(claim.getServices())
                        .items(claim.getMedications())
                        .build()
        ));
        return Objects.requireNonNull(response.getData());
    }

    private Claim.Service toService(@NonNull GetClaimsQuery.Service service) {
        return new Claim.Service(
                /* code = */ service.service().code(),
                /* name = */ service.service().name(),
                /* price = */ service.service().price(),
                /* currency = */ "XAF",
                /* quantityProvided = */ service.qtyProvided().toString(),
                /* quantityApproved = */ service.qtyApproved() != null ? service.qtyApproved().toString() : null,
                /* priceAdjusted = */ service.priceAdjusted() != null ? service.priceAdjusted().toString() : null,
                /* priceValuated = */ service.priceValuated() != null ? service.priceValuated().toString() : null,
                /* explanation = */ service.explanation(),
                /* justification = */ service.justification(),
                /* packageType = */ service.service().packagetype()
        );
    }
}
