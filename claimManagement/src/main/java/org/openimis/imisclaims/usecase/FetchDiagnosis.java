package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.apache.commons.codec.binary.Base64;
import org.openimis.imisclaims.GetDiagnosisQuery;
import org.openimis.imisclaims.domain.entity.Diagnosis;
import org.openimis.imisclaims.network.request.GetDiagnosesGraphQLRequest;
import org.openimis.imisclaims.network.util.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FetchDiagnosis {

    @NonNull
    private final GetDiagnosesGraphQLRequest request;

    public FetchDiagnosis() {
        this(new GetDiagnosesGraphQLRequest());
    }

    public FetchDiagnosis(
            @NonNull GetDiagnosesGraphQLRequest request
    ) {
        this.request = request;
    }

    @WorkerThread
    @NonNull
    public List<Diagnosis> execute() throws Exception {
        List<Diagnosis> diagnoses = new ArrayList<>();
        int page = 0;
        boolean hasNextPage;
        do{
            GetDiagnosisQuery.Diagnoses response = request.get(page);
            diagnoses.addAll(
                    Mapper.map(
                            response.edges(),
                            dto -> toDiagnosis(dto)
                    )
            );
            hasNextPage = response.pageInfo().hasNextPage();
            page = page + 100;
        }while (hasNextPage);

        return diagnoses;
    }

    private Diagnosis toDiagnosis (@NonNull GetDiagnosisQuery.Edge dto){
        GetDiagnosisQuery.Node node = Objects.requireNonNull(dto.node());
        byte[] bytes = node.id().getBytes();
        String id = new String(Base64.decodeBase64(bytes)).split(":")[1];
        return new Diagnosis(
                /* id = */ id,
                /* code = */ node.code(),
                /* name = */ node.name()
        );
    }
}
