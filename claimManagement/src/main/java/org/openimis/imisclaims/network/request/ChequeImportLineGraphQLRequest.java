package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.ChequeImportLineQuery;

import java.util.List;

public class ChequeImportLineGraphQLRequest extends BaseGraphQLRequest {

    @NonNull
    @WorkerThread
    public List<ChequeImportLineQuery.Edge> get(@NonNull String code) throws Exception {
        return makeSynchronous(new ChequeImportLineQuery(Input.fromNullable(code))).getData().chequeimportline().edges();
    }
}
