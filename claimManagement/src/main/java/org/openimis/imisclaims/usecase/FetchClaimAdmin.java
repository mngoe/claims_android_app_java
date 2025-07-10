package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetAdminIdQuery;
import org.openimis.imisclaims.network.request.GetAdminIdGraphQLRequest;
import org.openimis.imisclaims.util.IdUtils;

public class FetchClaimAdmin {

    @NonNull
    private final GetAdminIdGraphQLRequest request;

    public FetchClaimAdmin() {
        this(new GetAdminIdGraphQLRequest());
    }

    public FetchClaimAdmin(@NonNull GetAdminIdGraphQLRequest request) {
        this.request = request;
    }

    @NonNull
    @WorkerThread
    public int execute(@NonNull String claimAdminCode) throws Exception {
        GetAdminIdQuery.Node node = request.get(claimAdminCode);
        return IdUtils.getIdFromGraphQLString(node.id());
    }
}
