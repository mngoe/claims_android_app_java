package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.ChequeImportLineQuery;
import org.openimis.imisclaims.GetProgramsQuery;
import org.openimis.imisclaims.domain.entity.ChequeImport;
import org.openimis.imisclaims.domain.entity.Program;
import org.openimis.imisclaims.network.request.ChequeImportLineGraphQLRequest;
import org.openimis.imisclaims.network.request.GetProgramsGraphQLRequest;
import org.openimis.imisclaims.network.util.Mapper;

import java.util.List;
import java.util.Objects;

public class FetchChequeNumber {

    @NonNull
    private final ChequeImportLineGraphQLRequest request;

    public FetchChequeNumber() {
        this(new ChequeImportLineGraphQLRequest());
    }

    public FetchChequeNumber(
            @NonNull ChequeImportLineGraphQLRequest request
    ) {
        this.request = request;
    }

    @WorkerThread
    @NonNull
    public List<ChequeImport> execute(@NonNull String code) throws Exception {
        return Mapper.map(request.get(code), dto -> {
            ChequeImportLineQuery.Node node = Objects.requireNonNull(dto.node());
            return new ChequeImport(
                    /* code = */ node.chequeImportLineCode(),
                    /* status = */ node.chequeImportLineStatus()
            );
        });
    }
}
