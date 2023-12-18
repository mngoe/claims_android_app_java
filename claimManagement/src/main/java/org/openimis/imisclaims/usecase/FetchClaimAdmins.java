package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetClaimAdminsQuery;
import org.openimis.imisclaims.GetControlsQuery;
import org.openimis.imisclaims.domain.entity.ClaimAdmin;
import org.openimis.imisclaims.network.dto.IdentifierDto;
import org.openimis.imisclaims.network.request.GetPractitionersGraphQLRequest;
import org.openimis.imisclaims.network.request.GetPractitionersRequest;
import org.openimis.imisclaims.network.util.Mapper;
import org.openimis.imisclaims.network.util.PaginatedResponseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FetchClaimAdmins {

    @NonNull
    private final GetPractitionersGraphQLRequest request;

    public FetchClaimAdmins() {
        this(new GetPractitionersGraphQLRequest());
    }

    public FetchClaimAdmins(
            @NonNull GetPractitionersGraphQLRequest request
    ) {
        this.request = request;
    }

    @WorkerThread
    @NonNull
    public List<ClaimAdmin> execute() throws Exception {
        return Mapper.map(
                request.get().edges(),
                dto -> {
                    GetClaimAdminsQuery.Node node = Objects.requireNonNull(dto.node());
                    return new ClaimAdmin(
                            /* lastName = */ node.lastName(),
                            /* otherNames = */ node.otherNames(),
                            /* claimAdminCode = */ node.code(),
                            /* healthFacilityCode = */ node.healthFacility() != null ? node.healthFacility().code() : null
                    );
                }

        );
    }
}
