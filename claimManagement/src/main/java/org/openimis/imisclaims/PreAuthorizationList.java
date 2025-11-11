package org.openimis.imisclaims;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class PreAuthorizationList extends AppCompatActivity {

    private static final String LOG_TAG = "PreAuthList";
    private SQLHandler sqlHandler;
    private LinearLayout container; // garder une référence pour reload

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_authorized_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name_preauthorization));
        }

        sqlHandler = new SQLHandler(this);
        container = findViewById(R.id.container_claims);

        loadPreAuthorizations(container);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Efface le contenu avant de recharger (évite les doublons)
        if (container != null) {
            container.removeAllViews();
            loadPreAuthorizations(container);
        }
    }

    private void loadPreAuthorizations(LinearLayout container) {
        JSONArray preAuths = sqlHandler.getAllPreAuth();

        if (preAuths.length() == 0) {
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No data available");
            emptyMsg.setTextSize(16);
            emptyMsg.setTextColor(Color.BLACK);
            emptyMsg.setPadding(40, 40, 40, 40);
            container.addView(emptyMsg);
            return;
        }

        for (int i = 0; i < preAuths.length(); i++) {
            try {
                JSONObject item = preAuths.getJSONObject(i);
                JSONObject details = item.optJSONObject("details");
                if (details == null) continue;

                String code = details.optString("ClaimPreAuthorizationCode", "N/A");
                String insureeNumber = details.optString("CHFID", "N/A");
                String date = details.optString("DatePreAuthorization", "N/A");
                String claimUUID = details.optString("ClaimUUID", "");

                // --- Ligne principale ---
                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setPadding(30, 30, 30, 30);

                // Dégradé de fond
                int colorStart = Color.parseColor("#021533");
                int colorMiddle = Color.parseColor("#02031E");
                int colorEnd = Color.parseColor("#2F346D");
                GradientDrawable gradient = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{colorStart, colorMiddle, colorEnd}
                );
                rowLayout.setBackground(gradient);

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(20, 10, 20, 10);
                rowLayout.setLayoutParams(rowParams);
                rowLayout.setClickable(true);
                rowLayout.setFocusable(true);

                // --- Bloc gauche : Code + CHFID ---
                LinearLayout leftLayout = new LinearLayout(this);
                leftLayout.setOrientation(LinearLayout.VERTICAL);
                leftLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                ));

                TextView codeView = new TextView(this);
                codeView.setText(code);
                codeView.setTextSize(16);
                codeView.setTextColor(Color.WHITE);

                TextView insureeView = new TextView(this);
                insureeView.setText(insureeNumber);
                insureeView.setTextSize(16);
                insureeView.setTextColor(Color.WHITE);

                leftLayout.addView(codeView);
                leftLayout.addView(insureeView);

                // --- Bloc droite : Date ---
                TextView dateView = new TextView(this);
                dateView.setText(date);
                dateView.setTextSize(16);
                dateView.setTextColor(Color.WHITE);
                dateView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                // Ajouter au layout principal
                rowLayout.addView(leftLayout);
                rowLayout.addView(dateView);

                // --- Clic sur la ligne ---
                final String uuidForIntent = claimUUID;
                rowLayout.setOnClickListener(v -> {
                    try {
                        Intent intent = PreAuthorizationActivity.newIntent(
                                PreAuthorizationList.this,
                                uuidForIntent,
                                false
                        );
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Erreur ouverture PreAuthorizationActivity", e);
                    }
                });

                container.addView(rowLayout);

            } catch (Exception e) {
                Log.e(LOG_TAG, "Erreur lors de l'affichage d'une pré-autorisation", e);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem addItem = menu.add(Menu.NONE, 1, Menu.NONE, "");
        addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        TextView tv = new TextView(this);
        tv.setText("+");
        tv.setTextSize(34);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(20, 0, 20, 0);
        tv.setOnClickListener(v -> {
            Intent intent = new Intent(this, PreAuthorizationActivity.class);
            startActivity(intent);
        });

        addItem.setActionView(tv);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == 1) {
            Intent intent = new Intent(this, PreAuthorizationActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
