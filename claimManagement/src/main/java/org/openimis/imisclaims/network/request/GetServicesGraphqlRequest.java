package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetServicesQuery;

public class GetServicesGraphqlRequest extends BaseGraphQLRequest{

    @NonNull
    @WorkerThread
    public GetServicesQuery.MedicalServices get() throws Exception {
        return makeSynchronous(new GetServicesQuery()).getData().medicalServices();
    }
}
