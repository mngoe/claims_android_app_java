package org.openimis.imisclaims;

import static org.openimis.imisclaims.BuildConfig.API_BASE_URL;
import static org.openimis.imisclaims.BuildConfig.REST_API_PREFIX;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.openimis.imisclaims.adapter.FamilyMemberAdapter;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.domain.entity.PolygamousSubFamily;
import org.openimis.imisclaims.util.TextViewUtils;

import java.util.ArrayList;
import java.util.List;

public class SubHouseholdActivity extends AppCompatActivity {
    private static final String LOG_TAG = "SubHouseholdActivity";

    public static final String EXTRA_SUB_HEAD = "extra_sub_head";
    public static final String EXTRA_ALL_MEMBERS = "extra_all_members";

    private ImageView ivSubHeadPhoto;
    private TextView tvSubHeadName;
    private TextView tvSubHeadChfId;
    private TextView tvSubHeadGender;
    private TextView tvSubHeadDob;
    private RecyclerView rvSubHouseholdMembers;
    private TextView tvNoMembers;

    private FamilyMemberAdapter memberAdapter;
    private Global global;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_household);

        global = (Global) getApplicationContext();

        initializeViews();
        setupRecyclerView();
        loadDataFromIntent();
    }

    private void initializeViews() {
        ivSubHeadPhoto = findViewById(R.id.ivSubHeadPhoto);
        tvSubHeadName = findViewById(R.id.tvSubHeadName);
        tvSubHeadChfId = findViewById(R.id.tvSubHeadChfId);
        tvSubHeadGender = findViewById(R.id.tvSubHeadGender);
        tvSubHeadDob = findViewById(R.id.tvSubHeadDob);
        rvSubHouseholdMembers = findViewById(R.id.rvSubHouseholdMembers);
        tvNoMembers = findViewById(R.id.tvNoMembers);
    }

    private void setupRecyclerView() {
        rvSubHouseholdMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new FamilyMemberAdapter(this, new ArrayList<>());
        rvSubHouseholdMembers.setAdapter(memberAdapter);
    }

    private void loadDataFromIntent() {
        // Vérifier si l'Intent contient les extras
        if (getIntent().hasExtra(EXTRA_SUB_HEAD) && getIntent().hasExtra(EXTRA_ALL_MEMBERS)) {
            PolygamousSubFamily subHead = (PolygamousSubFamily) getIntent().getSerializableExtra(EXTRA_SUB_HEAD);
            ArrayList<FamilyMember> allMembers = (ArrayList<FamilyMember>) getIntent().getSerializableExtra(EXTRA_ALL_MEMBERS);

            if (subHead != null && subHead.getChfId() != null && subHead.getFullName() != null) {
                displaySubHeadInfo(subHead);
                loadSubHouseholdMembers(subHead, allMembers);
            } else {
                finish();
            }
        } else {
            Log.e(LOG_TAG, "Données manquantes dans l'Intent");
            finish();
        }
    }

    private void displaySubHeadInfo(PolygamousSubFamily subHead) {
        Log.d(LOG_TAG, "Affichage des informations du chef de sous-famille: " + subHead.getOtherNames() + " " + subHead.getLastName());

        // Nom complet
        String fullName = (subHead.getOtherNames() != null ? subHead.getOtherNames() + " " : "") + 
                         (subHead.getLastName() != null ? subHead.getLastName() : "");
        tvSubHeadName.setText(fullName.trim());

        // CHFID
        tvSubHeadChfId.setText(subHead.getChfId() != null ? subHead.getChfId() : "N/A");

        // Genre
        String gender = subHead.getGender();
        if ("M".equals(gender)) {
            tvSubHeadGender.setText("Masculin");
        } else if ("F".equals(gender)) {
            tvSubHeadGender.setText("Féminin");
        } else {
            tvSubHeadGender.setText(gender != null ? gender : "N/A");
        }

        // Date de naissance
        tvSubHeadDob.setText(subHead.getDob() != null ? subHead.getDob() : "N/A");

        // Photo
        loadSubHeadPhoto(subHead);
    }

    private void loadSubHeadPhoto(PolygamousSubFamily subHead) {
        if (subHead.getPhoto() != null && !subHead.getPhoto().isEmpty()) {
            try {
                // Décoder la photo depuis base64
                byte[] decodedString = android.util.Base64.decode(subHead.getPhoto(), android.util.Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivSubHeadPhoto.setImageBitmap(decodedByte);
            } catch (Exception e) {
                ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
            }
        } else {
            // Photo par défaut
            ivSubHeadPhoto.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    private void loadSubHouseholdMembers(PolygamousSubFamily subHead, ArrayList<FamilyMember> allMembers) {
        List<FamilyMember> subHouseholdMembers = subHead.getMembers();
        
        if (subHouseholdMembers != null && !subHouseholdMembers.isEmpty()) {
            displayMembers(subHouseholdMembers);
        } else {
            if (allMembers == null || allMembers.isEmpty()) {
                showNoMembersMessage();
                return;
            }

            // Filtrer les membres qui appartiennent à cette sous-famille
            List<FamilyMember> filteredMembers = new ArrayList<>();
            for (FamilyMember member : allMembers) {
                if (member.getLastName() != null && 
                    member.getLastName().equals(subHead.getLastName()) &&
                    !member.getChfId().equals(subHead.getChfId())) { // Exclure le chef lui-même
                    filteredMembers.add(member);
                }
            }

            if (filteredMembers.isEmpty()) {
                showNoMembersMessage();
            } else {
                displayMembers(filteredMembers);
            }
        }
    }

    private void showNoMembersMessage() {
        rvSubHouseholdMembers.setVisibility(View.GONE);
        tvNoMembers.setVisibility(View.VISIBLE);
        tvNoMembers.setText("Aucun membre dans cette sous-famille");
    }

    private void displayMembers(List<FamilyMember> members) {
        rvSubHouseholdMembers.setVisibility(View.VISIBLE);
        tvNoMembers.setVisibility(View.GONE);
        
        memberAdapter.updateData(members);
    }
}