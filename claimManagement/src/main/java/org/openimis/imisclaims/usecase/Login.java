package org.openimis.imisclaims.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.Global;
import org.openimis.imisclaims.network.dto.LoginDto;
import org.openimis.imisclaims.network.dto.TokenDto;
import org.openimis.imisclaims.network.request.LoginRequest;
import org.openimis.imisclaims.util.NetworkUtils;

import java.util.concurrent.TimeUnit;

public class Login {

    /** Outcome of a login attempt. */
    public enum Result {
        SUCCESS,
        INVALID_CREDENTIALS,
        CONNECTION_ERROR
    }

    @NonNull
    private final LoginRequest request;
    @NonNull
    private final Global global;

    public Login(
            @NonNull LoginRequest request,
            @NonNull Global global
    ) {
        this.request = request;
        this.global = global;
    }

    public Login() {
        this(new LoginRequest(), Global.getGlobal());
    }

    @WorkerThread
    @NonNull
    public Result execute(@NonNull String username, String password) {
        try {
            TokenDto token = request.post(new LoginDto(username.trim(), password));
            global.getJWTToken().saveTokenText(
                    token.getToken(),
                    TimeUnit.SECONDS.toMillis(token.getExpiresOn())
            );
            return Result.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return NetworkUtils.isConnectionError(e) ? Result.CONNECTION_ERROR : Result.INVALID_CREDENTIALS;
        }
    }
}
