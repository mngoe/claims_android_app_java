package org.openimis.imisclaims;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class AddServices extends ImisActivity {
    ListView lvServices;
    TextView tvCode, tvName;
    LinearLayout llSService;
    LinearLayout.LayoutParams layoutParams;
    EditText etSQuantity, etSAmount, etSName, etsSQuantity;
    Button btnAdd;
    AutoCompleteTextView etServices;
    int Pos;
    HashMap<String, String> oService;
    SimpleAdapter alAdapter;
    SimpleAdapter ssAdapterServices;
    SimpleAdapter ssAdapterItems;

    public static ArrayList<HashMap<String, String>> lvSServiceList;
    public static ArrayList<HashMap<String, String>> lvSItemList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addservices);

        if (actionBar != null) {
            actionBar.setTitle(getResources().getString(R.string.app_name_claim));
        }

        lvSServiceList = new ArrayList<>();
        lvSItemList = new ArrayList<>();

        lvServices = findViewById(R.id.lvServices);
        tvCode = findViewById(R.id.tvCode);
        tvName = findViewById(R.id.tvName);
        etSQuantity = findViewById(R.id.etSQuantity);
        etSQuantity.setKeyListener(null);
        etSAmount = findViewById(R.id.etSAmount);
        etSAmount.setKeyListener(null);
        etSName = findViewById(R.id.etSName);
        etSName.setKeyListener(null);
        etServices = findViewById(R.id.etService);
        llSService = findViewById(R.id.llSService);
        layoutParams = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT, 200);

        ServiceAdapter serviceAdapter = new ServiceAdapter(this, sqlHandler);

        etServices.setAdapter(serviceAdapter);
        etServices.setThreshold(1);
        etServices.setOnItemClickListener((parent, view, position, l) -> {
                if (position >= 0) {

                    Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                    final int itemColumnIndex = cursor.getColumnIndexOrThrow("Code");
                    final int descColumnIndex = cursor.getColumnIndexOrThrow("Name");
                    String Code = cursor.getString(itemColumnIndex);
                    String Name = cursor.getString(descColumnIndex);
                    String packageType = sqlHandler.getPackageType(Code);
                    String id = sqlHandler.getId(Code);

                    oService = new HashMap<>();
                    oService.put("Code", Code);
                    oService.put("Name", Name);
                    oService.put("PackageType",packageType);

                    etSQuantity.setText("1");
                    etSAmount.setText(sqlHandler.getServicePrice(Code));
                    etSName.setText(sqlHandler.getServiceName(Code));

                    if (!packageType.equals("S")) {

                        try {

                            JSONArray subServicesIds = sqlHandler.getSubServicesId(id);
                            JSONArray subServiceArr = new JSONArray();
                            for(int i = 0; i < subServicesIds.length(); i++){
                                JSONObject objService = sqlHandler.getService(subServicesIds.getJSONObject(i).getString("ServiceId"));
                                subServiceArr.put(objService);
                            }

                            JSONArray subItemIds = sqlHandler.getSubItemsId(id);
                            JSONArray subItemArr = new JSONArray();
                            for(int i = 0; i < subItemIds.length(); i++){
                                JSONObject objItem = sqlHandler.getItem(subItemIds.getJSONObject(i).getString("ItemId"));
                                subItemArr.put(objItem);
                            }

                            Log.e("subServices", subServiceArr.toString());
                            Log.e("subItems", subItemArr.toString());

                            for (int i = 0; i < subServiceArr.length(); i++) {
                                JSONObject obj = subServiceArr.getJSONObject(i);

                                HashMap<String, String> sService = new HashMap<>();
                                sService.put("Code", obj.getString("Code"));
                                sService.put("Name", obj.getString("Name"));
                                sService.put("Price", obj.getString("Price"));
                                sService.put("Quantity", "0");

                                lvSServiceList.add(sService);

                            }



                            for (int i = 0; i < subItemArr.length(); i++) {
                                JSONObject obj = subItemArr.getJSONObject(i);

                                HashMap<String, String> sItem = new HashMap<>();
                                sItem.put("Code", obj.getString("Code"));
                                sItem.put("Name", obj.getString("Name"));
                                sItem.put("Price", obj.getString("Price"));
                                sItem.put("Quantity", "0");

                                lvSItemList.add(sItem);

                            }


                            ssAdapterServices = new SimpleAdapter(AddServices.this, lvSServiceList, R.layout.lv_sservice,
                                    new String[]{"Code", "Name", "Price", "Quantity"},
                                    new int[]{R.id.tvLvCode, R.id.tvLvName, R.id.tvLvPrice, R.id.tvLvQuantity});


                            ssAdapterItems = new SimpleAdapter(AddServices.this, lvSItemList, R.layout.lv_sservice,
                                    new String[]{"Code", "Name", "Price", "Quantity"},
                                    new int[]{R.id.tvLvCode, R.id.tvLvName, R.id.tvLvPrice, R.id.tvLvQuantity});

                            TextView textServices = new TextView(AddServices.this);
                            textServices.setText("Sub-Services");
                            textServices.setPadding(0,0,0,10);
                            textServices.setTextSize(18);

                            TextView textItems = new TextView(AddServices.this);
                            textItems.setText("Sub-Items");
                            textItems.setPadding(0,20,0,10);
                            textItems.setTextSize(18);

                            ListView listServices = new ListView(AddServices.this);
                            if(lvSServiceList.size() > 2){
                                listServices.setLayoutParams(layoutParams);
                            }
                            listServices.setAdapter(ssAdapterServices);

                            ListView listItems = new ListView(AddServices.this);
                            if(lvSItemList.size() > 2){
                                listItems.setLayoutParams(layoutParams);
                            }
                            listItems.setAdapter(ssAdapterItems);

                            llSService.addView(textServices);
                            llSService.addView(listServices);
                            llSService.addView(textItems);
                            llSService.addView(listItems);

                            //etsSQuantity = findViewById(R.id.tvLvQuantity);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else {
                        llSService.removeAllViews();
                    }
                }

        });

        etServices.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnAdd.setEnabled(s != null && s.toString().trim().length() != 0
                        && etSQuantity.getText().toString().trim().length() != 0
                        && etSAmount.getText().toString().trim().length() != 0);

                llSService.removeAllViews();
                lvSServiceList.clear();
                lvSItemList.clear();

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnAdd.setEnabled(s != null && s.toString().trim().length() != 0
                        && etServices.getText().toString().trim().length() != 0
                        && etSAmount.getText().toString().trim().length() != 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnAdd.setEnabled(s != null && s.toString().trim().length() != 0
                        && etSQuantity.getText().toString().trim().length() != 0
                        && etServices.getText().toString().trim().length() != 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        alAdapter = new SimpleAdapter(AddServices.this, ClaimActivity.lvServiceList, R.layout.lvitem,
                new String[]{"Code", "Name", "Price", "Quantity"},
                new int[]{R.id.tvLvCode, R.id.tvLvName, R.id.tvLvPrice, R.id.tvLvQuantity});

        lvServices.setAdapter(alAdapter);

        btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setEnabled(false);
        btnAdd.setOnClickListener(v -> {
            try {

                if (oService == null) return;

                llSService.removeAllViews();

                String Amount, Quantity;

                HashMap<String, String> lvService = new HashMap<>();
                lvService.put("Code", oService.get("Code"));
                lvService.put("Name", oService.get("Name"));
                lvService.put("PackageType", oService.get("PackageType"));

                if(lvSServiceList.size() != 0){

                    float amount = Float.valueOf(etSAmount.getText().toString());
                    JSONArray sServices = new JSONArray();
                    JSONArray sItems = new JSONArray();


                    for (int i=0; i < lvSServiceList.size();i++){

                        JSONObject sService = new JSONObject();
                        sService.put("Code",lvSServiceList.get(i).get("Code"));
                        sService.put("Price",lvSServiceList.get(i).get("Price"));
                        //sService.put("Quantity",etsSQuantity.getText().toString());

                        sServices.put(sService);

                        //amount = amount + (Float.valueOf(lvSServiceList.get(i).get("Price")) * Float.valueOf(etsSQuantity.getText().toString()) );

                    }

                    for (int i=0; i < lvSItemList.size();i++){

                        JSONObject sItem = new JSONObject();
                        sItem.put("Code",lvSItemList.get(i).get("Code"));
                        sItem.put("Price",lvSItemList.get(i).get("Price"));
                        //sItem.put("Quantity",etsSQuantity.getText().toString());

                        sItems.put(sItem);

                        //amount = amount + (Float.valueOf(lvSItemList.get(i).get("Price")) * Float.valueOf(etsSQuantity.getText().toString()));

                    }
                    //Float price = Float.valueOf(etSAmount.getText().toString()) + (qty*price);
                    //Amount =
                    lvService.put("Price",String.valueOf(amount));
                    lvService.put("SubServices",sServices.toString());
                    lvService.put("SubItems",sItems.toString());

                }else{
                    Amount = etSAmount.getText().toString();
                    lvService.put("Price", Amount);
                }
                if (etSQuantity.getText().toString().length() == 0) Quantity = "1";
                else Quantity = etSQuantity.getText().toString();
                lvService.put("Quantity", Quantity);
                ClaimActivity.lvServiceList.add(lvService);

                alAdapter.notifyDataSetChanged();

                etServices.setText("");
                etSAmount.setText("");
                etSQuantity.setText("");
                etSName.setText("");

            } catch (Exception e) {
                Log.d("AddLvError", e.getMessage());
            }
        });

        lvServices.setOnItemLongClickListener((parent, view, position, id) -> {
            try {
                Pos = position;
                HideAllDeleteButtons();

                Button d = view.findViewById(R.id.btnDelete);
                d.setVisibility(View.VISIBLE);

                d.setOnClickListener(v -> {
                    ClaimActivity.lvServiceList.remove(Pos);
                    HideAllDeleteButtons();
                    alAdapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.d("ErrorOnLongClick", e.getMessage());
            }
            return true;
        });


    }

    private void HideAllDeleteButtons() {
        for (int i = 0; i <= lvServices.getLastVisiblePosition(); i++) {
            Button Delete = lvServices.getChildAt(i).findViewById(R.id.btnDelete);
            Delete.setVisibility(View.GONE);
        }
    }
}
