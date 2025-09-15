package org.openimis.imisclaims.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.openimis.imisclaims.R;
import org.openimis.imisclaims.domain.entity.PolygamousSubFamily;
import org.openimis.imisclaims.util.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PolygamousSubFamilyAdapter extends RecyclerView.Adapter<PolygamousSubFamilyAdapter.ViewHolder> {
    private static final String LOG_TAG = "PolygamousSubFamilyAdapter";
    
    private Context context;
    private List<PolygamousSubFamily> subFamilies;
    private LayoutInflater inflater;
    private OnSubFamilyClickListener clickListener;
    
    public interface OnSubFamilyClickListener {
        void onSubFamilyClick(PolygamousSubFamily subFamily);
    }
    
    public PolygamousSubFamilyAdapter(Context context, List<PolygamousSubFamily> subFamilies) {
        this.context = context;
        this.subFamilies = subFamilies;
        this.inflater = LayoutInflater.from(context);
    }
    
    public void setOnSubFamilyClickListener(OnSubFamilyClickListener listener) {
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.polygamous_sub_family_item, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (subFamilies == null || position < 0 || position >= subFamilies.size()) {
                Log.e(LOG_TAG, "Index invalide: position=" + position + ", size=" + (subFamilies != null ? subFamilies.size() : "null"));
                return;
            }
            
            PolygamousSubFamily subFamily = subFamilies.get(position);
            if (subFamily == null) {
                Log.e(LOG_TAG, "SubFamily null à la position: " + position);
                return;
            }
            
            bindSubFamily(holder, subFamily);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors du binding à la position " + position, e);
        }
    }
    
    @Override
    public int getItemCount() {
        return subFamilies != null ? subFamilies.size() : 0;
    }
    
    private void bindSubFamily(ViewHolder holder, PolygamousSubFamily subFamily) {
        try {
            Log.d(LOG_TAG, "=== BINDING SUB-FAMILY DATA ===");
            Log.d(LOG_TAG, "UUID: " + subFamily.getUuid());
            Log.d(LOG_TAG, "LastName: '" + subFamily.getLastName() + "'");
            Log.d(LOG_TAG, "OtherNames: '" + subFamily.getOtherNames() + "'");
            Log.d(LOG_TAG, "CHFID: '" + subFamily.getChfId() + "'");
            Log.d(LOG_TAG, "Gender: '" + subFamily.getGender() + "'");
            Log.d(LOG_TAG, "DOB: '" + subFamily.getDob() + "'");
            Log.d(LOG_TAG, "PhotoData: " + (subFamily.getPhotoData() != null ? "Present (" + subFamily.getPhotoData().length() + " chars)" : "null"));
        
        // Nom complet avec indication du type de chef
        String fullName = subFamily.getFullName().trim();
        if (fullName.isEmpty()) {
            fullName = context.getString(R.string.unknown_name);
        }
        
        // Ajouter une indication visuelle dans le nom
        if (subFamily.isPolygamousHead()) {
            fullName = "👑 " + fullName + " (Chef Principal)";
            holder.tvName.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        } else if (subFamily.isSubFamilyHead()) {
            fullName = "👥 " + fullName + " (Chef Sous-Famille)";
            holder.tvName.setTextColor(context.getResources().getColor(android.R.color.black));
        } else {
            holder.tvName.setTextColor(context.getResources().getColor(android.R.color.black));
        }
        
        holder.tvName.setText(fullName);
        Log.d(LOG_TAG, "FullName set to: '" + fullName + "'");
        
        // CHFID
        String chfid = subFamily.getChfId();
        if (chfid != null && !chfid.isEmpty()) {
            holder.tvCHFID.setText(context.getString(R.string.chfid_format, chfid));
            Log.d(LOG_TAG, "CHFID set to: '" + chfid + "'");
        } else {
            holder.tvCHFID.setText(context.getString(R.string.chfid_not_available));
            Log.d(LOG_TAG, "CHFID set to N/A (original was: '" + chfid + "')");
        }
        
        // Genre
        String gender = subFamily.getGender();
        if (gender != null && !gender.isEmpty()) {
            holder.tvGender.setText(gender);
            Log.d(LOG_TAG, "Gender set to: '" + gender + "'");
        } else {
            holder.tvGender.setText(context.getString(R.string.unknown_gender));
            Log.d(LOG_TAG, "Gender set to N/A (original was: '" + gender + "')");
        }
        
        // Date de naissance
        String dob = subFamily.getDob();
        if (dob != null && !dob.isEmpty()) {
            try {
                // Essayer de formater la date si possible
                try {
                    Date dobDate = DateUtils.dateFromString(dob);
                    holder.tvDOB.setText(DateUtils.toDateString(dobDate));
                    Log.d(LOG_TAG, "DOB formatted to: '" + DateUtils.toDateString(dobDate) + "'");
                } catch (Exception e) {
                    holder.tvDOB.setText(dob); // Fallback to original string
                    Log.d(LOG_TAG, "DOB set to original: '" + dob + "' (format failed)");
                }
            } catch (Exception e) {
                holder.tvDOB.setText(dob);
                Log.d(LOG_TAG, "DOB set to original: '" + dob + "' (exception)");
            }
        } else {
            holder.tvDOB.setText(context.getString(R.string.unknown_dob));
            Log.d(LOG_TAG, "DOB set to N/A (original was: '" + dob + "')");
        }
        
        // Nombre de membres et type de chef
        int membersCount = subFamily.getMembers() != null ? subFamily.getMembers().size() : 0;
        String membersText;
        
        // Distinguer le chef polygame principal des chefs de sous-familles
        if (subFamily.isPolygamousHead()) {
            // Chef polygame principal
            membersText = "👑 Chef Principal - " + context.getResources().getQuantityString(
                R.plurals.sub_family_members_count, membersCount, membersCount);
            holder.tvMembersCount.setTextColor(context.getResources().getColor(R.color.colorPrimary));
            Log.d(LOG_TAG, "Chef polygame principal identifié: " + subFamily.getFullName());
        } else if (subFamily.isSubFamilyHead()) {
            // Chef de sous-famille
            membersText = "👥 Chef Sous-Famille - " + context.getResources().getQuantityString(
                R.plurals.sub_family_members_count, membersCount, membersCount);
            holder.tvMembersCount.setTextColor(context.getResources().getColor(R.color.colorAccent));
            Log.d(LOG_TAG, "Chef de sous-famille identifié: " + subFamily.getFullName());
        } else {
            // Default case (should not happen)
            membersText = context.getResources().getQuantityString(
                R.plurals.sub_family_members_count, membersCount, membersCount);
            holder.tvMembersCount.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            Log.w(LOG_TAG, "Type de chef non identifié pour: " + subFamily.getFullName() + 
                  ", relationship: " + subFamily.getRelationship() + 
                  ", parentUuid: " + subFamily.getParentUuid());
        }
        
        holder.tvMembersCount.setText(membersText);
        
        // Photo
        loadPhoto(holder.ivPhoto, subFamily);
        
        // Gestion du clic
        holder.itemView.setOnClickListener(v -> {
            try {
                if (clickListener != null && subFamily != null) {
                    Log.d(LOG_TAG, "Clic sur sous-famille: " + subFamily.getFullName());
                    clickListener.onSubFamilyClick(subFamily);
                } else {
                    Log.w(LOG_TAG, "Clic ignoré - clickListener: " + (clickListener != null) + ", subFamily: " + (subFamily != null));
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Erreur lors du clic sur sous-famille", e);
            }
        });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors du binding de la sous-famille", e);
        }
    }
    
    private void loadPhoto(ImageView imageView, PolygamousSubFamily subFamily) {
        Log.d(LOG_TAG, "=== LOADING PHOTO ===");
        String photoData = subFamily.getPhotoData();
        Log.d(LOG_TAG, "PhotoData: " + (photoData != null ? "Present (" + photoData.length() + " chars)" : "null"));
        
        try {
            if (photoData != null && !photoData.isEmpty()) {
                Log.d(LOG_TAG, "Attempting to decode Base64 photo...");
                // Decode Base64 photo
                byte[] decodedBytes = Base64.decode(photoData, Base64.DEFAULT);
                Log.d(LOG_TAG, "Decoded bytes length: " + decodedBytes.length);
                
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    Log.d(LOG_TAG, "✓ Photo successfully decoded and set (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
                    imageView.setImageBitmap(bitmap);
                    return;
                } else {
                    Log.w(LOG_TAG, "⚠ Bitmap decode returned null");
                }
            } else {
                Log.d(LOG_TAG, "No photo data available");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "❌ Error loading photo for sub-family head", e);
        }
        
        // Default photo
        Log.d(LOG_TAG, "Using default placeholder image");
        imageView.setImageResource(R.drawable.ic_person_placeholder);
    }
    
    public void updateData(List<PolygamousSubFamily> newSubFamilies) {
        try {
            Log.d(LOG_TAG, "Mise à jour des données - anciennes: " + (this.subFamilies != null ? this.subFamilies.size() : "null") + ", nouvelles: " + (newSubFamilies != null ? newSubFamilies.size() : "null"));
            
            if (newSubFamilies == null) {
                this.subFamilies = new ArrayList<>();
            } else {
                // Deduplication based on UUID and CHFID to avoid duplicates
                this.subFamilies = removeDuplicates(newSubFamilies);
            }
            
            notifyDataSetChanged();
            Log.d(LOG_TAG, "Données mises à jour avec succès - après déduplication: " + this.subFamilies.size());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Erreur lors de la mise à jour des données", e);
        }
    }
    
    /**
     * Supprime les doublons de la liste des sous-familles polygames
     * basé sur l'UUID et le CHFID pour éviter l'affichage de chefs en double
     */
    private List<PolygamousSubFamily> removeDuplicates(List<PolygamousSubFamily> subFamilies) {
        List<PolygamousSubFamily> uniqueSubFamilies = new ArrayList<>();
        Set<String> seenUuids = new HashSet<>();
        Set<String> seenChfIds = new HashSet<>();
        
        for (PolygamousSubFamily subFamily : subFamilies) {
            if (subFamily == null) {
                continue;
            }
            
            String uuid = subFamily.getUuid();
            String chfId = subFamily.getChfId();
            
            // Check duplicates based on UUID or CHFID
            boolean isDuplicateByUuid = uuid != null && seenUuids.contains(uuid);
            boolean isDuplicateByChfId = chfId != null && seenChfIds.contains(chfId);
            
            if (!isDuplicateByUuid && !isDuplicateByChfId) {
                uniqueSubFamilies.add(subFamily);
                if (uuid != null) {
                    seenUuids.add(uuid);
                }
                if (chfId != null) {
                    seenChfIds.add(chfId);
                }
            } else {
                Log.w(LOG_TAG, "Doublon détecté et supprimé - UUID: " + uuid + ", CHFID: " + chfId);
            }
        }
        
        int duplicatesRemoved = subFamilies.size() - uniqueSubFamilies.size();
        if (duplicatesRemoved > 0) {
            Log.i(LOG_TAG, "Déduplication terminée - " + duplicatesRemoved + " doublon(s) supprimé(s)");
        }
        
        return uniqueSubFamilies;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName;
        TextView tvCHFID;
        TextView tvGender;
        TextView tvDOB;
        TextView tvMembersCount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivSubFamilyHeadPhoto);
            tvName = itemView.findViewById(R.id.tvSubFamilyHeadName);
            tvCHFID = itemView.findViewById(R.id.tvSubFamilyHeadCHFID);
            tvGender = itemView.findViewById(R.id.tvSubFamilyHeadGender);
            tvDOB = itemView.findViewById(R.id.tvSubFamilyHeadDOB);
            tvMembersCount = itemView.findViewById(R.id.tvSubFamilyMembersCount);
        }
    }
}