package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.BuildConfig;
import org.openimis.imisclaims.GetDiagnosisQuery;

public class GetDiagnosesGraphQLRequest extends BaseGraphQLRequest {

    private static final String URI = BuildConfig.MASTER_DATA_URL + "api/graphql";

    @NonNull
    @WorkerThread
    public GetDiagnosisQuery.Diagnoses get(@NonNull int page) throws Exception {
        return makeSynchronous(new GetDiagnosisQuery(Input.fromNullable(page)), URI).getData().diagnoses();
    }
}
