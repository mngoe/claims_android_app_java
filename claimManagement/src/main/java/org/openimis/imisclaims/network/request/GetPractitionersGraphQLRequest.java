package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetClaimAdminsQuery;

import java.util.List;

public class GetPractitionersGraphQLRequest extends  BaseGraphQLRequest{

    @NonNull
    @WorkerThread
    public List<GetClaimAdminsQuery.Edge> get() throws Exception {
        return makeSynchronous(new GetClaimAdminsQuery()).getData().claimAdmins().edges();
    }
}
