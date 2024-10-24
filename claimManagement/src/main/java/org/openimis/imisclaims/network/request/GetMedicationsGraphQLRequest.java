package org.openimis.imisclaims.network.request;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetMedicationsQuery;

import java.util.List;
public class GetMedicationsGraphQLRequest extends  BaseGraphQLRequest{
    @NonNull
    @WorkerThread
    public List<GetMedicationsQuery.Edge> get() throws Exception {
        return makeSynchronous(new GetMedicationsQuery()).getData().medicalItems().edges();
    }
}
