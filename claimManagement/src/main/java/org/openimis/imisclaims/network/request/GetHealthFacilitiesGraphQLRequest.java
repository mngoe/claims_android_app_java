package org.openimis.imisclaims.network.request;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.apollographql.apollo.api.Input;
import org.openimis.imisclaims.GetHealthFacilityQuery;

public class GetHealthFacilitiesGraphQLRequest extends BaseGraphQLRequest{

    private static final String URI = "https://csureport.minsante.cm/api/graphql";

    @NonNull
    @WorkerThread
    public GetHealthFacilityQuery.HealthFacilities get(@NonNull int page) throws Exception {
        return makeSynchronous(new GetHealthFacilityQuery(Input.fromNullable(page)), URI).getData().healthFacilities();
    }
}