package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.apollographql.apollo.api.internal.QueryDocumentMinifier;
import org.json.JSONObject;
import org.openimis.imisclaims.BuildConfig;
import org.openimis.imisclaims.Global;
import org.openimis.imisclaims.Token;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.domain.entity.SubServiceItem;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.util.DateUtils;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateClaimGraphQLRequest extends BaseGraphQLRequest{

    private static final String URI = BuildConfig.API_BASE_URL + "api/graphql";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    protected Global global;
    private final Token token = Global.getGlobal().getJWTToken();

    @WorkerThread
    @NonNull
    public Response create(
            @NonNull Claim claim,
            @NonNull int hfId,
            @NonNull int adminId,
            @NonNull int insureeId,
            @NonNull int programId
    ) throws Exception{

        JSONObject variables = new JSONObject();
        variables.put("code", claim.getClaimNumber());
        variables.put("insureeId", insureeId);
        variables.put("adminId", adminId);
        variables.put("dateFrom", DateUtils.toDateString(claim.getVisitDateFrom()));
        variables.put("dateTo", DateUtils.toDateString(claim.getVisitDateTo()));
        variables.put("icdId", 2500);
        variables.put("dateClaimed", DateUtils.toDateString(claim.getDateClaimed()));
        variables.put("healthFacilityId", hfId);
        variables.put("program", programId);
        variables.put("visitType", claim.getVisitType());

        String claimServices = "";
        if(claim.getServices().size() == 0){
            claimServices = "[]";
        }else{
            claimServices = "[";
            for(Claim.Service service: claim.getServices()){
                String subServices = "";
                String subItems = "";

                if(service.getSubServices().size() == 0){
                    subServices = "[]";
                }else{
                    subServices = "[";
                    for (SubServiceItem subService: service.getSubServices()){
                        String subObj = "{"
                                + " subServiceCode: \"" + subService.getCode() + "\""
                                + " qtyAsked: \"" + subService.getQty() + "\""
                                + " priceAsked: \"" + subService.getPrice() + "\""
                                + " qtyProvided: \"" + subService.getQty() + "\""
                                + "}";
                        subServices = subServices + subObj;
                    }
                    subServices = subServices + "]";
                }

                if(service.getSubItems().size() == 0){
                    subItems = "[]";
                }else{
                    subItems = "[";
                    for(SubServiceItem subItem: service.getSubItems()){
                        String subObj = "{"
                                + " subItemCode: \"" + subItem.getCode() + "\""
                                + " qtyAsked: \"" + subItem.getQty() + "\""
                                + " priceAsked: \"" + subItem.getPrice() + "\""
                                + " qtyProvided: \"" + subItem.getQty() + "\""
                                + "}";
                        subItems = subItems + subObj;
                    }
                    subItems = subItems + "]";
                }

                String obj = "{"
                        + " serviceId: " + Integer.valueOf(service.getId())
                        + " priceAsked: \"" + service.getPrice() + "\""
                        + " qtyProvided: \"" + service.getQuantity() + "\""
                        + " serviceserviceSet: " + subServices
                        + " serviceLinked: " + subItems
                        + " status: 1"
                        + "}";
                claimServices = claimServices + obj;
            }
            claimServices = claimServices + "]";
        }

        String claimItems = "";
        if(claim.getMedications().size() == 0){
            claimItems = "[]";
        }else{
            claimItems = "[";
            for(Claim.Medication item: claim.getMedications()){
                String obj = "{"
                        + " serviceId: " + Integer.valueOf(item.getId())
                        + " priceAsked: \"" + item.getPrice() + "\""
                        + " qtyProvided: \"" + item.getQuantity() + "\""
                        + " status: 1"
                        + "}";
                claimItems = claimItems + obj;
            }
            claimItems = claimItems + "]";
        }

        String QUERY_DOCUMENT = QueryDocumentMinifier.minify(
                "mutation {"
                        + "  createClaim(input: {"
                        + " code: \"" + claim.getClaimNumber() + "\""
                        + " insureeId: " + insureeId
                        + " adminId: " + adminId
                        + " dateFrom: \"" + DateUtils.toDateString(claim.getVisitDateFrom()) + "\""
                        + " dateTo: \"" + DateUtils.toDateString(claim.getVisitDateTo()) + "\""
                        + " icdId: " + 2500
                        + " dateClaimed: \"" + DateUtils.toDateString(claim.getDateClaimed()) + "\""
                        + " healthFacilityId: " + hfId
                        + " program: " + programId
                        + " visitType: \"" + claim.getVisitType() +"\""
                        + " services: " + claimServices
                        + " items: " + claimItems
                        + "}){"
                        + "    clientMutationId"
                        + "    internalId"
                        + "  }"
                        + "}"
        );

        JSONObject json = new JSONObject();
        json.put("query", QUERY_DOCUMENT);


        final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        OkHttpClient httpClient = builder.build();

        RequestBody formBody = new FormBody.Builder()
                .add("Create claim", QUERY_DOCUMENT)
                .build();

        RequestBody body = RequestBody.create(json.toString(), JSON);

        Request request = new Request.Builder()
                .url(URI)
                .addHeader("Authorization","bearer " + token.getTokenText().trim())
                .post(body)
                .build();


            Response response = httpClient.newCall(request).execute();
            int responseCode = response.code();

            Log.i("HTTP_POST", URI + " - " + responseCode);
            Log.i("Claim", QUERY_DOCUMENT);

            String responsePhrase = response.body().string();
            Log.i("RESPONSE", String.format("response: %d %s", responseCode, responsePhrase));

            return response;
    }
}