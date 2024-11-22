package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetMedicationsQuery;

public class GetMedicationsGraphQLRequest extends BaseGraphQLRequest{

    private static final String URI = "https://csureport.minsante.cm/api/graphql";

    @NonNull
    @WorkerThread
    public GetMedicationsQuery.MedicalItems get(@NonNull int page) throws Exception {
        return makeSynchronous(new GetMedicationsQuery(Input.fromNullable(page)), URI).getData().medicalItems();
    }

}
