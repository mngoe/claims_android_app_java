package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.internal.QueryDocumentMinifier;

import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openimis.imisclaims.BuildConfig;
import org.openimis.imisclaims.Global;
import org.openimis.imisclaims.Token;
import org.openimis.imisclaims.domain.entity.Prescripteur;
import org.openimis.imisclaims.tools.Log;

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Request helper to fetch prescribers using the GraphQL endpoint.
 * Inspired from CreateClaimGraphQLRequest sample.
 */
public class GetPrescriberGraphQLRequest extends BaseGraphQLRequest {

    private static final String URI = BuildConfig.API_BASE_URL + "api/graphql";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final org.apache.commons.logging.Log log = LogFactory.getLog(GetPrescriberGraphQLRequest.class);
    private final Token token = Global.getGlobal().getJWTToken();

    public GetPrescriberGraphQLRequest() {
    }

    /**
     * Fetch prescribers using the PrescriberPicker GraphQL query.
     * This method runs network IO and must be called from a background thread (annotated @WorkerThread).
     * @param hfUuid health facility uuid
     * @param str search string (nullable)
     * @param first max number of results to return
     * @return list of Prescripteur (may be empty)
     * @throws Exception on network / parsing errors
     */
    @WorkerThread
    @NonNull
    public List<Prescripteur> fetchPrescribers(@NonNull String hfUuid, @Nullable String str, int first) throws Exception {
        // Build GraphQL query string
        String safeStr = (str == null) ? "" : escapeForGraphQL(str);
        String rawQuery = "query PrescriberPicker { prescribers(first: " + first + ", str: \"" + safeStr + "\", hf: \"" + hfUuid + "\") { edges { node { uuid code nin lastName otherNames } } } }";

        String QUERY_DOCUMENT = QueryDocumentMinifier.minify(rawQuery);

        JSONObject json = new JSONObject();
        json.put("query", QUERY_DOCUMENT);

        // Trust-all SSL (pattern copied from example). Use with caution in production.
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException { }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException { }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        OkHttpClient httpClient = builder.build();

        RequestBody body = RequestBody.create(json.toString(), JSON);

        Request request = new Request.Builder()
                .url(URI)
                .addHeader("Authorization", "bearer " + token.getTokenText().trim())
                .post(body)
                .build();

        Response response = httpClient.newCall(request).execute();
        int responseCode = response.code();
        Log.i("HTTP_POST", URI + " - " + responseCode);

        String responsePhrase = response.body() != null ? response.body().string() : "";
        Log.i("RESPONSE", String.format("response: %d %s", responseCode, responsePhrase));

        List<Prescripteur> list = new ArrayList<>();

        try {
            JSONObject root = new JSONObject(responsePhrase);
            JSONObject data = root.optJSONObject("data");
            if (data != null) {
                JSONObject prescribers = data.optJSONObject("prescribers");
                if (prescribers != null) {
                    JSONArray edges = prescribers.optJSONArray("edges");
                    if (edges != null) {
                        for (int i = 0; i < edges.length(); i++) {
                            JSONObject edge = edges.optJSONObject(i);
                            if (edge == null) continue;
                            JSONObject node = edge.optJSONObject("node");
                            if (node == null) continue;

                            // Debug: log the raw node JSON to ensure fields are present as expected
                            Log.d("GetPrescriberGraphQLRequest", "Prescriber node JSON: " + node.toString());

                            String uuid = null;
                            if (node.has("uuid") && !node.isNull("uuid")) {
                                uuid = node.optString("uuid");
                            }
                            String code = node.optString("code");
                            String nin = node.optString("nin");
                            String lastName = node.optString("lastName");
                            String otherNames = node.optString("otherNames");

                            Log.d("GetPrescriberGraphQLRequest", "Parsed prescriber: uuid='" + uuid + "', code='" + code + "', lastName='" + lastName + "'");

                            Prescripteur p = new Prescripteur();
                            p.setUuid(uuid);
                            p.setCode(code);
                            p.setNin(nin);
                            p.setLastName(lastName);
                            p.setOtherNames(otherNames);

                            Log.d("IOIO", p.getUuid());

                            list.add(p);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("GetPrescriberGraphQLRequest", "Error parsing prescribers response", e);
            throw e;
        }

        return list;
    }

    private static String escapeForGraphQL(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
