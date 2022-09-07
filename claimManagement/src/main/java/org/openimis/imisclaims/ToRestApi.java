package org.openimis.imisclaims;

import static org.openimis.imisclaims.BuildConfig.API_BASE_URL;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class ToRestApi {
    private final Token token;
    private final String uri;

    public ToRestApi() {
        token = Global.getGlobal().getJWTToken();
        uri = API_BASE_URL + "api/";
    }

    public HttpResponse getFromRestApi(String functionName, boolean addToken) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(uri + functionName);
        httpGet.setHeader("Content-Type", "application/json");
        if (addToken) {
            httpGet.setHeader("Authorization", "bearer " + token.getTokenText().trim());
        }

        try {
            HttpResponse response = httpClient.execute(httpGet);
            int responseCode = response.getStatusLine().getStatusCode();
            Log.i("HTTP_GET", uri + functionName + " - " + responseCode);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public HttpResponse getFromRestApiServices(String functionName, String api_version) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(uri + functionName);
        httpGet.setHeader("Content-Type", "application/json");
        httpGet.setHeader("api-version", api_version);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            int responseCode = response.getStatusLine().getStatusCode();
            Log.i("HTTP_GET", uri + functionName + " - " + responseCode);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public HttpResponse postToRestApi(Object object, String functionName, boolean addToken) {
        HttpClient httpClient = new DefaultHttpClient();

        HttpPost httpPost = new HttpPost(uri + functionName);
        httpPost.setHeader("Content-type", "application/json");
        if (addToken) {
            httpPost.setHeader("Authorization", "bearer " + token.getTokenText().trim());
        }

        try {
            StringEntity postingString = new StringEntity(object.toString());
            httpPost.setEntity(postingString);
            HttpResponse response = httpClient.execute(httpPost);
            int responseCode = response.getStatusLine().getStatusCode();
            Log.i("HTTP_POST", uri + functionName + " - " + responseCode);
            Log.i("Object", object.toString());
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public HttpResponse postToRestApi(Object object, String functionName) {
        return postToRestApi(object, functionName, false);
    }

    public HttpResponse postToRestApiToken(Object object, String functionName) {
        return postToRestApi(object, functionName, true);
    }

    public String getFromRestApi(String functionName) {
        HttpResponse response = getFromRestApi(functionName, false);
        return getContent(response);
    }

    public String getFromRestApiVersion(String functionName, String api_version) {
        HttpResponse response = getFromRestApiServices(functionName,api_version);
        return getContent(response);
    }

    public HttpResponse getFromRestApiToken(String functionName) {
        return getFromRestApi(functionName, true);

    }

    public String getContent(HttpResponse response) {
        try {
            HttpEntity respEntity = (response != null) ? response.getEntity() : null;
            return (respEntity != null) ? EntityUtils.toString(respEntity) : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
