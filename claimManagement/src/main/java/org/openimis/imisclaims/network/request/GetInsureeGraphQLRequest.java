package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Input;

import org.openimis.imisclaims.GetInsureeInquireQuery;
import org.openimis.imisclaims.GetInsureeQuery;
import org.openimis.imisclaims.network.exception.HttpException;

import java.net.HttpURLConnection;
import java.util.List;


public class GetInsureeGraphQLRequest extends BaseGraphQLRequest {

    @NonNull
    @WorkerThread
    public GetInsureeQuery.Node get(
            @NonNull String chfId
    ) throws Exception {
        List<GetInsureeQuery.Edge> edges = makeSynchronous(new GetInsureeQuery(
                Input.fromNullable(chfId)
        )).getData().insurees().edges();
        if (edges.isEmpty()) {
            throw new HttpException(
                    /* code = */ HttpURLConnection.HTTP_NOT_FOUND,
                    /* message = */ "Insuree with id '" + chfId + "' was not found",
                    /* body = */ null,
                    /* cause = */ null
            );
        }
        return edges.get(0).node();
    }
}
