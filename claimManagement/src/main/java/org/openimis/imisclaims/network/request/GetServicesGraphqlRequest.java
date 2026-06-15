package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.BuildConfig;
import org.openimis.imisclaims.GetServicesQuery;

public class GetServicesGraphqlRequest extends BaseGraphQLRequest {

    private static final String URI = BuildConfig.MASTER_DATA_URL + "api/graphql";

    @NonNull
    @WorkerThread
    public GetServicesQuery.MedicalServices get(@NonNull int page, @NonNull String hfId) throws Exception {
        return makeSynchronous(new GetServicesQuery(Input.fromNullable(page), Input.fromNullable(hfId)), URI).getData().medicalServices();
    }
}
