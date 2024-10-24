package org.openimis.imisclaims.usecase;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import org.apache.commons.codec.binary.Base64;
import org.openimis.imisclaims.GetMedicationsQuery;
import org.openimis.imisclaims.domain.entity.Medication;
import org.openimis.imisclaims.network.request.GetMedicationsGraphQLRequest;
import org.openimis.imisclaims.network.util.Mapper;
import java.util.List;
import java.util.Objects;
public class FetchMedications {
    @NonNull
    private final GetMedicationsGraphQLRequest request;
    public FetchMedications() {
        this(new GetMedicationsGraphQLRequest());
    }
    public FetchMedications(
            @NonNull GetMedicationsGraphQLRequest request
    ) {
        this.request = request;
    }
    @WorkerThread
    @NonNull
    public List<Medication> execute() throws Exception {
        return Mapper.map(request.get(), dto -> {
            GetMedicationsQuery.Node node = Objects.requireNonNull(dto.node());
            byte[] bytes = node.id().getBytes();
            String id = new String(Base64.decodeBase64(bytes)).split(":")[1];
            return new Medication(
                    /* id = */ id,
                    /* code = */ node.code(),
                    /* name = */ node.name(),
                    /* price = */ node.price(),
                    "$"
            );
        });
    }
}