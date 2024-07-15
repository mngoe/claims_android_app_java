package org.openimis.imisclaims.usecase;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetInsureeInquireQuery;
import org.openimis.imisclaims.GetInsureeQuery;
import org.openimis.imisclaims.domain.entity.Insuree;
import org.openimis.imisclaims.domain.entity.Policy;
import org.openimis.imisclaims.network.request.GetInsureeGraphQLRequest;
import org.openimis.imisclaims.network.request.GetInsureeInquireGraphQLRequest;
import org.openimis.imisclaims.network.util.Mapper;

import java.util.List;
import java.util.Objects;

public class FetchInsuree {

    @NonNull
    private final GetInsureeGraphQLRequest request;

    public FetchInsuree() {
        this(new GetInsureeGraphQLRequest());
    }

    public FetchInsuree(@NonNull GetInsureeGraphQLRequest request) {
        this.request = request;
    }

    @NonNull
    @WorkerThread
    public String execute(@NonNull String chfId) throws Exception {
        GetInsureeQuery.Node node = request.get(chfId);
        byte[] bytes = node.id().getBytes();
        String id = new String(org.apache.commons.codec.binary.Base64.decodeBase64(bytes)).split(":")[1];
        return id;
    }
}
