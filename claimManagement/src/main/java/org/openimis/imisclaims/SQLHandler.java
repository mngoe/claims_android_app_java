package org.openimis.imisclaims;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SQLHandler extends SQLiteOpenHelper {
    public static final String DB_NAME_MAPPING = Global.getGlobal().getSubdirectory("Databases") + "/" + "Mapping.db3";
    public static final String DB_NAME_DATA = Global.getGlobal().getSubdirectory("Databases") + "/" + "ImisData.db3";
    private static final String CreateTableMapping = "CREATE TABLE IF NOT EXISTS tblMapping(Code text,Name text,Type text);";
    private static final String CreateTableControls = "CREATE TABLE IF NOT EXISTS tblControls(FieldName text, Adjustibility text);";
    private static final String CreateTableClaimAdmins = "CREATE TABLE IF NOT EXISTS tblClaimAdmins(Code text, Name text);";
    private static final String CreateTableInsureeNumbers = "CREATE TABLE IF NOT EXISTS tblInsureeNumbers(Number text, Statut text);";
    private static final String CreateTableReferences = "CREATE TABLE IF NOT EXISTS tblReferences(Code text, Name text, Type text, Price text);";
    private static final String CreateTableServices = "CREATE TABLE IF NOT EXISTS tblServices(Code text, Name text, Type text, Price text, PackageType text);";
    //private static final String CreateTableDateUpdates = "CREATE TABLE tblDateUpdates(Id INTEGER PRIMARY KEY AUTOINCREMENT, last_update_date text);";

    Global global;
    SQLiteDatabase db;
    SQLiteDatabase dbMapping;

    public SQLHandler(Context context) {
        super(context, DB_NAME_MAPPING, null, 3);
        global = (Global) context.getApplicationContext();
        createOrOpenDatabases();
    }

    public void createOrOpenDatabases() {
        if (!checkDatabase()) {
            db = SQLiteDatabase.openOrCreateDatabase(DB_NAME_DATA, null);
        }
        if (!checkMapping()) {
            dbMapping = SQLiteDatabase.openOrCreateDatabase(DB_NAME_MAPPING, null);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public Cursor getMapping(String Type) {
        try {
            db.execSQL("ATTACH DATABASE '" + DB_NAME_MAPPING + "' AS dbMapping1");
            Cursor c = db.rawQuery("select I.code,I.name,M.Type AS isMapped FROM tblReferences I LEFT OUTER JOIN dbMapping1.tblMapping M ON I.Code = M.Code WHERE I.Type =?", new String[]{Type});
            return c;
        } catch (SQLException e) {
            Log.d("ErrorOnFetchingData", e.getMessage());
            return null;
        }
    }

    public String getPrice(String code, String type) {
        String price = "0";
        try (Cursor c = db.query("tblReferences", new String[]{"Price"}, "LOWER(Code) = LOWER(?) AND LOWER(Type) = LOWER(?)", new String[]{code, type}, null, null, null, "1")) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                String result = c.getString(0);
                if (!TextUtils.isEmpty(result)) {
                    price = result;
                }
            }
        } catch (SQLException e) {
            Log.d("ErrorOnFetchingData", String.format("Error while getting price of %s", code), e);
        }
        return price;
    }

    public String getPriceService(String code) {
        String price = "0";
        try (Cursor c = db.query("tblServices", new String[]{"Price"}, "LOWER(Code) = LOWER(?)", new String[]{code}, null, null, null, "1")) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                String result = c.getString(0);
                if (!TextUtils.isEmpty(result)) {
                    price = result;
                }
            }
        } catch (SQLException e) {
            Log.d("ErrorOnFetchingData", String.format("Error while getting price of %s", code), e);
        }
        return price;
    }

    public String getNameService(String code) {
        String name = "";
        try (Cursor c = db.query("tblServices", new String[]{"Name"}, "LOWER(Code) = LOWER(?)", new String[]{code}, null, null, null, "1")) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                String result = c.getString(0);
                if (!TextUtils.isEmpty(result)) {
                    name = result;
                }
            }
        } catch (SQLException e) {
            Log.d("ErrorOnFetchingData", String.format("Error while getting price of %s", code), e);
        }
        return name;
    }

    public String getPackageType(String code) {
        String name = "";
        try (Cursor c = db.query("tblServices", new String[]{"PackageType"}, "LOWER(Code) = LOWER(?)", new String[]{code}, null, null, null, "1")) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                String result = c.getString(0);
                if (!TextUtils.isEmpty(result)) {
                    name = result;
                }
            }
        } catch (SQLException e) {
            Log.d("ErrorOnFetchingData", String.format("Error while getting price of %s", code), e);
        }
        return name;
    }

    public String getItemPrice(String code) {
        return getPrice(code, "I");
    }

    public String getServicePrice(String code) {
        return getPriceService(code);
    }

    public String getServiceName(String code) {
        return getNameService(code);
    }

    public boolean InsertMapping(String Code, String Name, String Type) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("Code", Code);
            cv.put("Name", Name);
            cv.put("Type", Type);

            dbMapping.insert("tblMapping", null, cv);
        } catch (SQLiteFullException e) {
            return false;
        }
        return true;
    }

    public void InsertReferences(String Code, String Name, String Type, String Price) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("Code", Code);
            cv.put("Name", Name);
            cv.put("Type", Type);
            cv.put("Price", Price);

            db.insert("tblReferences", null, cv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void InsertService(String Code, String Name, String Type, String Price, String PackageType) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("Code", Code);
            cv.put("Name", Name);
            cv.put("Type", Type);
            cv.put("Price", Price);
            cv.put("PackageType", PackageType);

            db.insert("tblServices", null, cv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void InsertControls(String FieldName, String Adjustibility) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("FieldName", FieldName);
            cv.put("Adjustibility", Adjustibility);
            db.insert("tblControls", null, cv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void InsertClaimAdmins(String Code, String Name) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("Code", Code);
            cv.put("Name", Name);
            db.insert("tblClaimAdmins", null, cv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ClearMapping(String Type) {
        dbMapping.delete("tblMapping", "Type = ?", new String[]{Type});
    }

    public void ClearReferencesSI() {
        db.delete("tblReferences", "Type != ?", new String[]{"D"});
    }

    public void ClearAll(String tblName) {
        try {
            db.delete(tblName, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Cursor SearchDisease(String InputText) {
        //Cursor c = db.rawQuery("SELECT Code as _id,Code, Name,Code + ' ' + Name AS Disease FROM tblReferences WHERE Type = 'D' AND (Code LIKE '%"+ InputText +"%' OR Name LIKE '%"+ InputText +"%')",null);
        Cursor c = db.rawQuery("SELECT Code as _id,Code, Name FROM tblReferences WHERE Type = 'D' AND (Code LIKE '%" + InputText + "%' OR Name LIKE '%" + InputText + "%')", null);
        if (c != null) {
            c.moveToFirst();
        }

        return c;
    }

    public String getDiseaseCode(String disease) {
        String code = "";
        try {
            String table = "tblReferences";
            String[] columns = {"Code"};
            String selection = "Type='D' and Name=?";
            String[] selectionArgs = {disease};
            String limit = "1";
            Cursor c = db.query(table, columns, selection, selectionArgs, null, null, null, limit);
            if (c.getCount() == 1) {
                c.moveToFirst();
                code = c.getString(c.getColumnIndexOrThrow("Code"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public Cursor filterItemsServices(String nameFilter, String typeFilter) {
        String wildcardNameFilter = "%" + nameFilter + "%";
        Cursor c = dbMapping.query("tblMapping",
                new String[]{"Code AS _id", "Code", "Name"},
                "type = ? AND (Code LIKE ? OR Name LIKE ?)",
                new String[]{typeFilter, wildcardNameFilter, wildcardNameFilter},
                null,
                null,
                null);

        if (c != null) {
            c.moveToFirst();
        }

        return c;
    }

    public Cursor searchItems(String filter) {
        return filterItemsServices(filter, "I");
    }

    public Cursor searchServices(String filter) {
        return filterItemsServices(filter, "S");
    }

    public String getAdjustibility(String FieldName) {
        String adjustibility = "M";
        Cursor cursor = null;
        try {
            String query = "SELECT Adjustibility FROM tblControls WHERE FieldName = '" + FieldName + "'";
            cursor = db.rawQuery(query, null);
            // looping through all rows
            if (cursor.moveToFirst()) {
                do {
                    adjustibility = cursor.getString(0);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            return adjustibility;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return adjustibility;
    }

    public boolean checkIfAny(String table) {
        boolean tableExists = false;
        boolean any = false;
        try {
            Cursor c;
            c = db.query(true, "sqlite_master", new String[]{"tbl_name"}, "tbl_name = ?", new String[]{table},
                    null, null, null, "1");

            tableExists = c.getCount() > 0;
            c.close();

            if (tableExists) {
                c = db.query(table, null, null, null, null, null, null, "1");
                any = c.getCount() > 0;
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return any;
        }
        return any;
    }

    public String getClaimAdmin(String Code) {
        String Name = "";
        try {
            String query = "SELECT Name FROM tblClaimAdmins WHERE upper(Code) like '" + Code.toUpperCase() + "'";
            Cursor cursor1 = db.rawQuery(query, null);
            // looping through all rows
            if (cursor1.moveToFirst()) {
                do {
                    Name = cursor1.getString(0);
                } while (cursor1.moveToNext());
            }
        } catch (Exception e) {
            return Name;
        }

        return Name;
    }

    public JSONArray getServices() {
        String nullOverride="";
        JSONArray resultSet = new JSONArray();
        try {
            String query = "SELECT Name FROM tblServices ";
            Cursor cursor1 = db.rawQuery(query, null);
            cursor1.moveToFirst();
            // looping through all rows
            while (!cursor1.isAfterLast()) {
                int totalColumns = cursor1.getColumnCount();
                JSONObject rowObject = new JSONObject();
                for (int i = 0; i < totalColumns; i++) {
                    try {
                        if (cursor1.getString(i) != null)
                            rowObject.put(cursor1.getColumnName(i), cursor1.getString(i));
                        else
                            rowObject.put(cursor1.getColumnName(i), nullOverride);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("Tag Name", e.getMessage());
                    }
                }
                resultSet.put(rowObject);
                cursor1.moveToNext();
            }
            cursor1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultSet;
    }

    public void createTables() {
        try {
            db.execSQL(CreateTableControls);
            db.execSQL(CreateTableReferences);
            db.execSQL(CreateTableClaimAdmins);
            db.execSQL(CreateTableInsureeNumbers);
            db.execSQL(CreateTableServices);
            dbMapping.execSQL(CreateTableMapping);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeDatabases() {
        db.close();
        dbMapping.close();
    }

    public boolean checkDatabase() {
        return db != null && db.isOpen();
    }

    public boolean checkMapping() {
        return dbMapping != null && dbMapping.isOpen();
    }

    //ajouter un numéro d'assuré
    public void InsertInsureeNumber(String Numero, String Statut) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("Number", Numero);
            cv.put("Statut", Statut);
            db.insert("tblInsureeNumbers", null, cv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //cette fonction va retourner le statut d'un numéro d'assuré qu'on aura entrer
    public String getStatutInsureeNumber(String Code) {
        String Statut = "";
        try {
            String query = "SELECT Statut FROM tblInsureeNumbers WHERE upper(Number) like '" + Code.toUpperCase() + "'";
            Cursor cursor1 = db.rawQuery(query, null);
            // looping through all rows
            if (cursor1.moveToFirst()) {
                do {
                    Statut = cursor1.getString(0);
                } while (cursor1.moveToNext());
            }
        } catch (Exception e) {
            return Statut;
        }

        return Statut;
    }
}
