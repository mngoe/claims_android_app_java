package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.apache.commons.codec.binary.Base64;
import org.openimis.imisclaims.GetClaimsQuery;
import org.openimis.imisclaims.GetControlsQuery;
import org.openimis.imisclaims.GetServicesQuery;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.domain.entity.Control;
import org.openimis.imisclaims.domain.entity.Program;
import org.openimis.imisclaims.domain.entity.Service;
import org.openimis.imisclaims.domain.entity.SubServiceItem;
import org.openimis.imisclaims.network.request.GetServicesGraphqlRequest;
import org.openimis.imisclaims.network.util.Mapper;

import java.util.List;
import java.util.Objects;

public class FetchServices {

    @NonNull
    private final GetServicesGraphqlRequest request;

    public FetchServices() {
        this(new GetServicesGraphqlRequest());
    }

    public FetchServices(
            @NonNull GetServicesGraphqlRequest request
    ) {
        this.request = request;
    }

    @WorkerThread
    @NonNull
    public List<Service> execute() throws Exception {
        Mapper<GetServicesQuery.ServiceserviceSet, SubServiceItem> subServiceMapper = new Mapper<>(this::toSubService);
        Mapper<GetServicesQuery.ServicesLinked, SubServiceItem> subItemMapper = new Mapper<>(this::toSubItem);
        return Mapper.map(request.get().edges(), dto -> toService(dto,subServiceMapper,subItemMapper));
    }

    private Service toService(
            @NonNull GetServicesQuery.Edge dto,
            @NonNull Mapper<GetServicesQuery.ServiceserviceSet, SubServiceItem> subServiceMapper,
            @NonNull Mapper<GetServicesQuery.ServicesLinked, SubServiceItem> subItemMapper
    ){
        GetServicesQuery.Node node = Objects.requireNonNull(dto.node());
        byte[] bytes = node.id().getBytes();
        String id = new String(Base64.decodeBase64(bytes)).split(":")[1];
        return new Service(
                /* id = */ id,
                /* code = */ node.code(),
                /* name = */ node.name(),
                /* price = */ node.price(),
                "XAF",
                /* packageType = */ node.packagetype(),
                /* program = */ node.program() != null ? node.program().idProgram():null,
                /* subServices = */ subServiceMapper.map(node.serviceserviceSet()),
                /* subItems = */ subItemMapper.map(node.servicesLinked())
        );
    }

    private SubServiceItem toSubService(@NonNull GetServicesQuery.ServiceserviceSet service) {
        byte[] bytes = service.service().id().getBytes();
        String id = new String(Base64.decodeBase64(bytes)).split(":")[1];
        return new SubServiceItem(
                /* id = */ id,
                /* quantity = */ service.qtyProvided(),
                /* price = */ service.priceAsked()
        );
    }

    private SubServiceItem toSubItem(@NonNull GetServicesQuery.ServicesLinked item) {
        byte[] bytes = item.item().id().getBytes();
        String id = new String(Base64.decodeBase64(bytes)).split(":")[1];
        return new SubServiceItem(
                /* id = */ id,
                /* quantity = */ item.qtyProvided(),
                /* price = */ item.priceAsked()
        );
    }
}
