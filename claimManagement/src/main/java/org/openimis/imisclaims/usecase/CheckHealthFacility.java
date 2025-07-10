package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetFosaQuery;
import org.openimis.imisclaims.network.request.GetFosaGraphQLRequest;

import java.util.List;

public class CheckHealthFacility {

    @NonNull
    private final GetFosaGraphQLRequest request;

    public CheckHealthFacility(){this(new GetFosaGraphQLRequest());}

    public CheckHealthFacility(@NonNull GetFosaGraphQLRequest request){
        this.request = request;
    }

    @WorkerThread
    @NonNull
    public boolean execute(String HfCode) throws Exception {
        List<GetFosaQuery.Edge> response = request.get(HfCode);
        return !response.isEmpty();
    }
}
