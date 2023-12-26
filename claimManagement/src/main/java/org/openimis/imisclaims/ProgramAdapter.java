package org.openimis.imisclaims;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.openimis.imisclaims.tools.Log;

import java.util.ArrayList;
import java.util.List;

public class ProgramAdapter extends CursorAdapter implements AdapterView.OnItemClickListener {
    SQLHandler sqlHandler;
    SQLiteDatabase db;
    String hfPrograms;

    public ProgramAdapter(Context context, SQLHandler sqlHandler, String hfPrograms) {
        super(context, null, 0);
        this.sqlHandler = sqlHandler;
        this.hfPrograms = hfPrograms;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.program_list, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final int descColumnIndex = cursor.getColumnIndexOrThrow("Name");
        String Suggestion = cursor.getString(descColumnIndex);
        TextView text1 = view.findViewById(R.id.text2);
        text1.setText(Suggestion);

    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }
        List<String> hfProg = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(hfPrograms);
            for (int i = 0 ; i< array.length(); i++){
                hfProg.add(array.get(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return sqlHandler.SearchProgram((constraint != null ? constraint.toString() : ""), hfProg);
    }

    @Override
    public CharSequence convertToString(Cursor c) {
        return c.getString(c.getColumnIndexOrThrow("Name"));
    }


    @Override
    public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    }
}

