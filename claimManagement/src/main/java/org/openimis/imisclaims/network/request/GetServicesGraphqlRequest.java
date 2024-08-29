package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetServicesQuery;

import java.util.List;

public class GetServicesGraphqlRequest extends BaseGraphQLRequest {

    @NonNull
    @WorkerThread
    public GetServicesQuery.MedicalServices get(@NonNull int page, @NonNull String hfId) throws Exception {
        return makeSynchronous(new GetServicesQuery(Input.fromNullable(page), Input.fromNullable(hfId))).getData().medicalServices();
    }
}
