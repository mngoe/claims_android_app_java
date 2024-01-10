package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.apache.commons.codec.binary.Base64;
import org.openimis.imisclaims.GetClaimAdminsQuery;
import org.openimis.imisclaims.GetControlsQuery;
import org.openimis.imisclaims.GetProgramsQuery;
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
        List<ClaimAdmin> claimAdmins = new ArrayList<>();
        int page = 0;
        boolean hasNextPage;
        Mapper<GetClaimAdminsQuery.Edge2, String> programMapper = new Mapper<>(this::toProgram);
        do{
            GetClaimAdminsQuery.ClaimAdmins response = request.get(page);
            claimAdmins.addAll(
                    Mapper.map(
                            response.edges(),
                            dto -> {
                                GetClaimAdminsQuery.Node node = Objects.requireNonNull(dto.node());
                                byte[] bytes = node.id().getBytes();
                                String id = new String(Base64.decodeBase64(bytes)).split(":")[1];
                                String fosaId = "";
                                if(node.healthFacility() != null){
                                    byte[] fosaBytes = node.healthFacility().id().getBytes();
                                    fosaId = new String(Base64.decodeBase64(fosaBytes)).split(":")[1];
                                }

                                return new ClaimAdmin(
                                        /* id = */ id,
                                        /* lastName = */ node.lastName(),
                                        /* otherNames = */ node.otherNames(),
                                        /* claimAdminCode = */ node.code(),
                                        /* healthFacilityCode = */ node.healthFacility() != null ? node.healthFacility().code() : null,
                                        /* healthFacilityId = */ fosaId,
                                        /* programs = */ programMapper.map(
                                                node.userSet().edges().get(0).node().iUser().programSet().edges(),
                                        dt -> dt.node().idProgram()
                                )
                                );
                            }
                    )
            );
            hasNextPage = response.pageInfo().hasNextPage();
            page = page + 100;
        }while(hasNextPage);

        return claimAdmins;
    }

    private String toProgram (
            @NonNull GetClaimAdminsQuery.Edge2 dto
    ){
        return dto.node().idProgram();
    }
}
