package org.openimis.imisclaims;

import static org.openimis.imisclaims.EnquireActivity.LOG_TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.apache.commons.io.IOUtils;
import org.openimis.imisclaims.tools.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class AddAttachments extends ImisActivity {
    ListView lvAttachments;
    EditText etAttachTitle, etAttachName;
    Button btnAdd;

    int Pos;

    HashMap<String, String> oAttachment;
    SimpleAdapter alAdapter;
    private static final int REQUEST_PICK_ATTACH_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_attachments);

        if (actionBar != null) {
            actionBar.setTitle(getResources().getString(R.string.app_name_claim));
        }

        lvAttachments = findViewById(R.id.lvAttachments);
        etAttachName = findViewById(R.id.etAttachName);
        etAttachTitle = findViewById(R.id.etAttachTitle);
        btnAdd = findViewById(R.id.btnAdd);

        alAdapter = new SimpleAdapter(AddAttachments.this, ClaimActivity.lvAttachmentList, R.layout.lv_attachment,
                new String[]{"Title", "Name"},
                new int[]{R.id.tvLvTitle, R.id.tvLvAttachmentName});
        lvAttachments.setAdapter(alAdapter);

        if (isIntentReadonly()) {
            disableView(etAttachTitle);
            disableView(etAttachName);
            disableView(btnAdd);
        } else {

            etAttachName.setOnClickListener(view -> {
                //choose file
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("image/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, REQUEST_PICK_ATTACH_FILE);

            });

            etAttachTitle.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    btnAdd.setEnabled(s != null && s.toString().trim().length() != 0
                            && etAttachName.getText().toString().trim().length() != 0);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            etAttachName.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    btnAdd.setEnabled(s != null && s.toString().trim().length() != 0
                            && etAttachTitle.getText().toString().trim().length() != 0);
                }
            });

            lvAttachments.setAdapter(alAdapter);

            btnAdd.setEnabled(false);
            btnAdd.setOnClickListener(v -> {
                try {
                    if (oAttachment == null) return;

                    String Title, Name, File;

                    HashMap<String, String> lvAttachment = new HashMap<>();
                    lvAttachment.put("Title", etAttachTitle.getText().toString());
                    lvAttachment.put("Name", etAttachName.getText().toString());
                    lvAttachment.put("File", oAttachment.get("File"));
                    ClaimActivity.lvAttachmentList.add(lvAttachment);

                    alAdapter.notifyDataSetChanged();

                    etAttachTitle.setText("");
                    etAttachName.setText("");

                } catch (Exception e) {
                    Log.d("AddLvError", e.getMessage());
                }
            });

            lvAttachments.setOnItemLongClickListener((parent, view, position, id) -> {
                try {

                    Pos = position;
                    HideAllDeleteButtons();

                    Button d = view.findViewById(R.id.btnDeleteAttachment);
                    d.setVisibility(View.VISIBLE);

                    d.setOnClickListener(v -> {
                        ClaimActivity.lvAttachmentList.remove(Pos);
                        HideAllDeleteButtons();
//						alAdapter.notifyDataSetChanged();
                    });


                } catch (Exception e) {
                    Log.d("ErrorOnLongClick", e.getMessage());
                }
                return true;
            });
        }


    }

    private boolean isIntentReadonly() {
        Intent intent = getIntent();
        return intent.getBooleanExtra(ClaimActivity.EXTRA_READONLY, false);
    }

    private void HideAllDeleteButtons() {
        for (int i = 0; i <= lvAttachments.getLastVisiblePosition(); i++) {
            Button Delete = (Button) lvAttachments.getChildAt(i).findViewById(R.id.btnDeleteAttachment);
            Delete.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,@Nullable Intent data) {

        switch (requestCode) {
            case REQUEST_PICK_ATTACH_FILE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    Cursor cursor = getContentResolver()
                            .query(uri, null, null, null, null, null);

                    try {
                        if (cursor != null && cursor.moveToFirst()) {
                            @SuppressLint("Range") String displayName = cursor.getString(
                                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                            etAttachName.setText(displayName);

                            byte[] bytes = IOUtils.toByteArray(getContentResolver().openInputStream(uri));
                            oAttachment = new HashMap<>();
                            oAttachment.put("File", Base64.encodeToString(bytes, Base64.DEFAULT));
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        cursor.close();
                    }

                }
                break;
        }
    }
}