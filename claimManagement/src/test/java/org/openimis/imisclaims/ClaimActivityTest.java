package org.openimis.imisclaims;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentValues;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
public class ClaimActivityTest {

    ClaimActivity activity;
    Global global;

    MockedConstruction<SQLHandler> mockedSqlHandler;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        JSONArray array = new JSONArray();

        mockedSqlHandler = mockConstruction(SQLHandler.class, (mock, context) -> {
            when(mock.getModuleConfig(anyString())).thenReturn(new JSONObject());
            when(mock.getAdjustability(anyString())).thenReturn("M");
        });

        global = spy(new Global());
        global.setOfficerCode("joseph");
        global.setOfficerHealthFacility("ES015");

        ActivityController<ClaimActivity> controller = Robolectric.buildActivity(ClaimActivity.class);
        activity = controller.get();

        // Inject mocks
        activity.global = global;
        activity.sqlHandler = mock(SQLHandler.class);

        doReturn(true).when(activity.global).isNetworkAvailable();
        when(activity.sqlHandler.getAdjustability(anyString())).thenReturn("M");
        when(activity.sqlHandler.getModuleConfig(anyString())).thenReturn(new JSONObject());
        when(activity.sqlHandler.getClaimAdminInfo(anyString(), eq("HFId"))).thenReturn("4");
        when(activity.sqlHandler.getHealthFacilityPrograms(anyString())).thenReturn("[]");
        when(activity.sqlHandler.getClaimAdminInfo(anyString(), eq("Programs"))).thenReturn("[]");

        controller.create().start().resume();

        // set mandatory fields
        activity.etHealthFacility.setText("ES015");
        activity.etClaimAdmin.setText("joseph");
        activity.etClaimCode.setText("CP23");
        activity.etProgram.setText("Chèque Santé");
        activity.etClaimPrefix.setText("664768");
        activity.etInsureeNumber.setText("12000825441284");
        activity.etStartDate.setText("2025-01-01");
        activity.etEndDate.setText("2025-01-02");
        activity.etDiagnosis.setText("A01");
        activity.etVisitType.setText("Other");
        activity.etVisitType.setTag("O");

        activity.tvItemTotal.setText("0");
        activity.tvServiceTotal.setText("1");


        ArrayList<HashMap<String, String>> fakeServices = new ArrayList<>();
        HashMap<String, String> svc1 = new HashMap<>();
        svc1.put("Code", "PCS4");
        svc1.put("Price", "0.0");
        svc1.put("Quantity", "1");
        svc1.put("PackageType", "P");
        svc1.put("SubServicesItems", "[]");
        fakeServices.add(svc1);
        ClaimActivity.lvServiceList = fakeServices;
    }

    @After
    public void tearDown() {
        if (mockedSqlHandler != null) {
            mockedSqlHandler.close();
        }
    }

    @Test
    public void isValidData_AllValid_ReturnsTrue() {
        boolean result = activity.isValidData();
        assertTrue(result);
    }

    @Test
    public void isValidData_MinInsureeNumber_ReturnsFalse() {
        activity.etInsureeNumber.setText("120008254");
        boolean result = activity.isValidData();
        assertFalse(result);
    }

    @Test
    public void isValidData_ExceedMaxInsureeNumber_ReturnsFalse() {
        activity.etInsureeNumber.setText("12000825478878888777868768687");
        boolean result = activity.isValidData();
        assertFalse(result);
    }

    @Test
    public void isValidData_MinChequeNumber_ReturnsFalse() {
        activity.etClaimPrefix.setText("3554");
        boolean result = activity.isValidData();
        assertFalse(result);
    }

    @Test
    public void saveClaim_ValidData_CallsSqlHandlerAndReturnsTrue() {
        doNothing().when(activity.sqlHandler)
                .saveClaim(any(ContentValues.class), anyList(), anyList());

        boolean result = activity.saveClaim();

        assertTrue(result);
        verify(activity.sqlHandler, times(1))
                .saveClaim(any(ContentValues.class), anyList(), anyList());
    }

}
