package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetDiagnosisQuery;

public class GetDiagnosesGraphQLRequest extends BaseGraphQLRequest {

    @NonNull
    @WorkerThread
    public GetDiagnosisQuery.Diagnoses get(@NonNull int page) throws Exception {
        return makeSynchronous(new GetDiagnosisQuery(Input.fromNullable(page))).getData().diagnoses();
    }
}
