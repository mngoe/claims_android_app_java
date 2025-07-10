package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetFosaQuery;

import java.util.List;

public class GetFosaGraphQLRequest extends BaseGraphQLRequest {

    @NonNull
    @WorkerThread
    public List<GetFosaQuery.Edge> get (@NonNull String HfCode) throws Exception {
        return makeSynchronous(new GetFosaQuery(Input.fromNullable(HfCode))).getData().healthFacilities().edges();
    }
}
