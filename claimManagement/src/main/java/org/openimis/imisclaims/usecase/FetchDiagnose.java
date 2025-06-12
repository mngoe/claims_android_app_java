package org.openimis.imisclaims.usecase;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import org.openimis.imisclaims.GetDiagnoseIdQuery;
import org.openimis.imisclaims.network.request.GetDiagnosesGraphQLRequest;
import org.openimis.imisclaims.util.IdUtils;

public class FetchDiagnose {

    @NonNull
    private final GetDiagnosesGraphQLRequest request;

    public FetchDiagnose() {
        this(new GetDiagnosesGraphQLRequest());
    }

    public FetchDiagnose(@NonNull GetDiagnosesGraphQLRequest request) {
        this.request = request;
    }

    @NonNull
    @WorkerThread
    public int execute(@NonNull String code) throws Exception {
        GetDiagnoseIdQuery.Node node = request.get(code);
        return IdUtils.getIdFromGraphQLString(node.id());
    }
}
