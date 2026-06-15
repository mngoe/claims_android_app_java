package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.BuildConfig;
import org.openimis.imisclaims.GetControlsQuery;

import java.util.List;

public class GetControlsGraphQLRequest extends BaseGraphQLRequest {

    private static final String uri = BuildConfig.MASTER_DATA_URL + "api/graphql";

    @NonNull
    @WorkerThread
    public List<GetControlsQuery.Edge> get() throws Exception {
        return makeSynchronous(new GetControlsQuery(), uri).getData().control().edges();
    }
}
