package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetInsureeIdQuery;
import org.openimis.imisclaims.network.request.GetInsureeIdGraphQLRequest;
import org.openimis.imisclaims.util.IdUtils;

public class FetchInsuree {

    @NonNull
    private final GetInsureeIdGraphQLRequest request;

    public FetchInsuree() {
        this(new GetInsureeIdGraphQLRequest());
    }

    public FetchInsuree(@NonNull GetInsureeIdGraphQLRequest request) {
        this.request = request;
    }

    @NonNull
    @WorkerThread
    public int execute(@NonNull String chfId) throws Exception {
        GetInsureeIdQuery.Node node = request.get(chfId);
        return IdUtils.getIdFromGraphQLString(node.id());
    }
}