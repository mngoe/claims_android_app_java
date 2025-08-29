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
import org.openimis.imisclaims.util.TextViewUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        if (getIntent().hasExtra(EXTRA_SUB_HEAD)) {

        } else {

        }
        
        if (getIntent().hasExtra(EXTRA_ALL_MEMBERS)) {

        } else {

        }
        
        // Utiliser getParcelableExtra au lieu de getSerializableExtra car FamilyMember implémente Parcelable
        FamilyMember subHead = null;
        ArrayList<FamilyMember> allMembers = null;
        
        try {
            subHead = getIntent().getParcelableExtra(EXTRA_SUB_HEAD);

        } catch (Exception e) {

        }
        
        try {
            allMembers = getIntent().getParcelableArrayListExtra(EXTRA_ALL_MEMBERS);

        } catch (Exception e) {

        }
        
        if (subHead != null) {

            displaySubHeadInfo(subHead);
            
            if (allMembers != null) {
    
                List<FamilyMember> subHouseholdMembers = getSubHouseholdMembers(subHead, allMembers);
                displaySubHouseholdMembers(subHouseholdMembers);
            } else {
                Log.w(LOG_TAG, "Aucune liste de membres reçue");
            }
        } else {

            finish();
        }
    }
    
    private void displaySubHeadInfo(FamilyMember subHead) {
        tvSubHeadName.setText(subHead.getFullName());
        tvSubHeadChfId.setText("Code: " + subHead.getChfId());
        tvSubHeadGender.setText("Genre: " + subHead.getGender());
        TextViewUtils.setDate(tvSubHeadDob, subHead.getDateOfBirth());
        
        // Charger la photo
        loadSubHeadPhoto(subHead);
    }
    
    private void loadSubHeadPhoto(FamilyMember subHead) {
        byte[] imageBytes = subHead.getPhoto();
        if (imageBytes != null) {
            try {
                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                ivSubHeadPhoto.setImageBitmap(image);
            } catch (Exception e) {
                ivSubHeadPhoto.setImageDrawable(getResources().getDrawable(R.drawable.person));
            }
        } else if (subHead.getPhotoPath() != null && global.isNetworkAvailable()) {
            ivSubHeadPhoto.setImageResource(R.drawable.person);
            new Picasso.Builder(this).build()
                    .load(API_BASE_URL + REST_API_PREFIX + subHead.getPhotoPath())
                    .placeholder(R.drawable.person)
                    .error(R.drawable.person)
                    .into(ivSubHeadPhoto);
        } else {
            ivSubHeadPhoto.setImageDrawable(getResources().getDrawable(R.drawable.person));
        }
    }
    
    private List<FamilyMember> getSubHouseholdMembers(FamilyMember subHead, List<FamilyMember> allMembers) {

        
        List<FamilyMember> subHouseholdMembers = new ArrayList<>();
        
        // Ajouter le chef de sous-famille lui-même
        subHouseholdMembers.add(subHead);

        
        // Cas 1: Si le chef de sous-famille est le chef principal de famille
        if (subHead.isHead()) {

            
            // Récupérer tous les enfants (non-époux, non-chef)
            List<FamilyMember> allChildren = new ArrayList<>();
            for (FamilyMember member : allMembers) {
                if (!member.isHead() && !isSpouse(member)) {
                    allChildren.add(member);
                }
            }
            
            // Dans le sous-ménage du chef principal, ajouter seulement les enfants qui lui sont attribués
            // Le chef principal est traité comme la "première épouse" pour la distribution
            List<FamilyMember> allSpouses = new ArrayList<>();
            for (FamilyMember member : allMembers) {
                if (isSpouse(member)) {
                    allSpouses.add(member);
                }
            }
            
            int totalSubHeads = allSpouses.size() + 1; // +1 pour le chef principal
            int mainHeadIndex = 0; // Le chef principal a l'index 0
            
            // Distribuer les enfants: le chef principal reçoit ceux avec index % totalSubHeads == 0
            for (int i = 0; i < allChildren.size(); i++) {
                if (i % totalSubHeads == mainHeadIndex) {
                    FamilyMember child = allChildren.get(i);
                    subHouseholdMembers.add(child);

                }
            }
            

            
        } else {
            // Cas 2: Si le chef de sous-famille est une épouse

            
            // Récupérer toutes les épouses pour calculer l'index de cette épouse
            List<FamilyMember> allSpouses = new ArrayList<>();
            for (FamilyMember member : allMembers) {
                if (isSpouse(member)) {
                    allSpouses.add(member);
                }
            }
            
            // Trouver l'index de l'épouse actuelle
            int spouseIndex = -1;
            for (int i = 0; i < allSpouses.size(); i++) {
                if (allSpouses.get(i).getChfId().equals(subHead.getChfId())) {
                    spouseIndex = i;
                    break;
                }
            }
            

            
            // Toujours ajouter le chef principal de famille au sous-ménage de la première épouse
            if (spouseIndex == 0) {
                for (FamilyMember member : allMembers) {
                    if (member.isHead()) {
                        subHouseholdMembers.add(member);

                        break;
                    }
                }
            }
            
            // Récupérer tous les enfants (non-époux, non-chef)
            List<FamilyMember> allChildren = new ArrayList<>();
            for (FamilyMember member : allMembers) {
                if (!member.isHead() && !isSpouse(member)) {
                    allChildren.add(member);
                }
            }
            

            
            // Distribuer les enfants entre les épouses
            if (spouseIndex >= 0 && !allChildren.isEmpty()) {
                for (int i = 0; i < allChildren.size(); i++) {
                    // Attribuer l'enfant à l'épouse selon un modulo (distribution équitable)
                    if (i % allSpouses.size() == spouseIndex) {
                        FamilyMember child = allChildren.get(i);
                        subHouseholdMembers.add(child);

                    }
                }
            }
            

        }
        
        return subHouseholdMembers;
    }
    
    private boolean isSpouse(FamilyMember member) {
        String relationship = member.getRelationship();
        if (relationship == null) return false;
        
        // Utiliser la même logique de détection que les autres classes
        String normalizedRelation = relationship.trim().toLowerCase();
        return normalizedRelation.equals("épouse") || 
               normalizedRelation.equals("epouse") ||
               normalizedRelation.equals("époux") ||
               normalizedRelation.equals("epoux") ||
               normalizedRelation.equals("wife") ||
               normalizedRelation.equals("husband") ||
               normalizedRelation.equals("spouse") ||
               normalizedRelation.contains("épouse") ||
               normalizedRelation.contains("époux");
    }
    
    private void displaySubHouseholdMembers(List<FamilyMember> members) {
        if (members.isEmpty()) {
            rvSubHouseholdMembers.setVisibility(View.GONE);
            tvNoMembers.setVisibility(View.VISIBLE);
        } else {
            rvSubHouseholdMembers.setVisibility(View.VISIBLE);
            tvNoMembers.setVisibility(View.GONE);
            memberAdapter.updateFamilyMembers(members);
            

        }
    }
}