package org.openimis.imisclaims.network.request;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.apollographql.apollo.api.Response;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openimis.imisclaims.CreateClaimMutation;
import org.openimis.imisclaims.GetServicesQuery;
import org.openimis.imisclaims.domain.entity.Claim;
import org.openimis.imisclaims.domain.entity.Medication;
import org.openimis.imisclaims.domain.entity.Service;
import org.openimis.imisclaims.domain.entity.SubServiceItem;
import org.openimis.imisclaims.network.util.Mapper;
import org.openimis.imisclaims.tools.Log;
import org.openimis.imisclaims.type.ClaimItemInputType;
import org.openimis.imisclaims.type.ClaimServiceInputType;
import org.openimis.imisclaims.type.CreateClaimMutationInput;
import org.openimis.imisclaims.type.SubItemInputType;
import org.openimis.imisclaims.type.SubServiceInputType;
import org.openimis.imisclaims.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CreateClaimGraphQLRequest extends BaseGraphQLRequest{

    @WorkerThread
    @NonNull
    public CreateClaimMutation.Data create(
            @NonNull JSONObject claim,
            @NonNull int hfId,
            @NonNull int adminId,
            @NonNull int insureeId,
            @NonNull int programId
    ) throws Exception{
        JSONArray arrayItems = claim.getJSONArray("items");
        JSONArray arrayServices = claim.getJSONArray("services");
        Response<CreateClaimMutation.Data> response = makeSynchronous(new CreateClaimMutation(
                CreateClaimMutationInput.builder()
                        .code(claim.getJSONObject("details").getString("ClaimCode"))
                        .visitType(claim.getJSONObject("details").getString("VisitType"))
                        .program(programId)
                        .dateClaimed(Objects.requireNonNull(JsonUtils.getDateOrDefault(claim.getJSONObject("details"), "ClaimDate")))
                        .healthFacilityId(hfId)
                        .icdId(3)
                        .dateFrom(Objects.requireNonNull(JsonUtils.getDateOrDefault(claim.getJSONObject("details"), "StartDate")))
                        .dateTo(Objects.requireNonNull(JsonUtils.getDateOrDefault(claim.getJSONObject("details"), "EndDate")))
                        .insureeId(insureeId)
                        .adminId(adminId)
                        .services(toService(arrayServices))
                        .items(toItem(arrayItems))
                        .build()
        ));
        return Objects.requireNonNull(response.getData());
    }

    private List<ClaimItemInputType> toItem (
            @NonNull JSONArray arrayItems
            ) throws JSONException {
        List<ClaimItemInputType> items = new ArrayList<>();
        for (int i=0 ; i < arrayItems.length() ; i++){
            JSONObject objItem = arrayItems.getJSONObject(i);
            items.add(
                    ClaimItemInputType.builder()
                            .itemId(Integer.valueOf(objItem.getString("ItemId")))
                            .priceAsked(Double.valueOf(objItem.getString("ItemPrice")))
                            .qtyProvided(Double.valueOf(objItem.getString("ItemQuantity")))
                            .status(1)
                            .build()
            );
        }
        return items;
    }

    private List<ClaimServiceInputType> toService (
            @NonNull JSONArray arrayServices
    ) throws JSONException {
        List<ClaimServiceInputType> services = new ArrayList<>();
        List<SubServiceInputType> emptyList = new ArrayList<>();
        List<SubItemInputType> emptyItemList = new ArrayList<>();
        for (int i=0 ; i < arrayServices.length() ; i++){
            JSONObject objService = arrayServices.getJSONObject(i);
            JSONArray arrSubServices = new JSONArray();
            JSONArray arrSubItems = new JSONArray();
            if(objService.has("SubServicesItems")){
                JSONArray arrSubServicesItems = objService.getJSONArray("SubServicesItems");
                for (int j=0; j< arrSubServicesItems.length();j++){
                    if(arrSubServicesItems.getJSONObject(j).getString("Type").equals("S")){
                        arrSubServices.put(arrSubServicesItems.getJSONObject(j));
                    }else if(arrSubServicesItems.getJSONObject(j).getString("Type").equals("I")){
                        arrSubItems.put(arrSubServicesItems.getJSONObject(j));
                    }
                }
            }

            services.add(
                    ClaimServiceInputType.builder()
                            .serviceId(Integer.valueOf(objService.getString("ServiceId")))
                            .priceAsked(Double.valueOf(objService.getString("ServicePrice")))
                            .qtyProvided(Double.valueOf(objService.getString("ServiceQuantity")))
                            .serviceserviceSet(arrSubServices.length() != 0 ? toSubService(arrSubServices) : emptyList)
                            .serviceLinked(arrSubItems.length() != 0 ? toSubItem(arrSubItems) : emptyItemList)
                            .status(1)
                            .build()
            );
        }
        return services;
    }

    private List<SubServiceInputType> toSubService(
            @NonNull JSONArray arraySubServices
    ) throws JSONException{
        List<SubServiceInputType> subServices = new ArrayList<>();
        for (int i=0; i < arraySubServices.length(); i++){
            subServices.add(
                    SubServiceInputType.builder()
                            .subServiceCode(arraySubServices.getJSONObject(i).getString("Code"))
                            .qtyAsked(arraySubServices.getJSONObject(i).getString("Quantity"))
                            .priceAsked(arraySubServices.getJSONObject(i).getString("Price"))
                            .build()
            );
        }
        return subServices;
    }

    private List<SubItemInputType> toSubItem(
            @NonNull JSONArray arraySubItems
    ) throws JSONException{
        List<SubItemInputType> subItems = new ArrayList<>();
        for( int i=0; i<arraySubItems.length(); i++){
            subItems.add(
                    SubItemInputType.builder()
                            .subItemCode(arraySubItems.getJSONObject(i).getString("Code"))
                            .qtyAsked(arraySubItems.getJSONObject(i).getString("Quantity"))
                            .priceAsked(arraySubItems.getJSONObject(i).getString("Price"))
                            .build()
            );
        }
        return subItems;
    }
}