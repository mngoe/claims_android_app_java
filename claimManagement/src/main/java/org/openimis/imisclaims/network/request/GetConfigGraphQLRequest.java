package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.BuildConfig;
import org.openimis.imisclaims.GetConfigsQuery;

import java.util.List;

public class GetConfigGraphQLRequest extends BaseGraphQLRequest {

    private static final String uri = BuildConfig.MASTER_DATA_URL + "api/graphql";

    @NonNull
    @WorkerThread
    public List<GetConfigsQuery.ModuleConfiguration> get() throws Exception {
        return makeSynchronous(new GetConfigsQuery(Input.fromNullable("fe")), uri).getData().moduleConfigurations();
    }
}