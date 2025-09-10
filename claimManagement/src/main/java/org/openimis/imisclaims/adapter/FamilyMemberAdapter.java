package org.openimis.imisclaims.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.TextView;

import org.openimis.imisclaims.R;
import org.openimis.imisclaims.domain.entity.FamilyMember;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.ViewHolder> {
    private static final String LOG_TAG = "FamilyMemberAdapter";
    private Context context;
    private List<FamilyMember> familyMembers;
    private LayoutInflater inflater;
    private SimpleDateFormat inputDateFormat;
    private SimpleDateFormat outputDateFormat;

    public FamilyMemberAdapter(Context context, List<FamilyMember> familyMembers) {
        this.context = context;
        this.familyMembers = familyMembers;
        this.inflater = LayoutInflater.from(context);
        this.outputDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        this.inputDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'XXX yyyy", Locale.ENGLISH);
        Log.d(LOG_TAG, "FamilyMemberAdapter created with " + (familyMembers != null ? familyMembers.size() : "null") + " members");
        if (familyMembers != null) {
            for (int i = 0; i < familyMembers.size(); i++) {
                FamilyMember member = familyMembers.get(i);
                Log.d(LOG_TAG, "Member " + i + ": " + member.getFullName() + ", CHFID: " + member.getChfId() + ", hasPhoto: " + (member.getPhotoData() != null && !member.getPhotoData().isEmpty()));
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = familyMembers != null ? familyMembers.size() : 0;
        Log.d(LOG_TAG, "getItemCount() returning: " + count);
        return count;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.family_member_item, parent, false);
        Log.d(LOG_TAG, "onCreateViewHolder called");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(LOG_TAG, "onBindViewHolder called for position: " + position);
        
        FamilyMember member = familyMembers.get(position);
        Log.d(LOG_TAG, "Displaying member at position " + position + ": " + member.getFullName());
        
        // Nom complet
        holder.tvMemberName.setText(member.getFullName());
        
        // CHFID
        if (member.getChfId() != null && !member.getChfId().isEmpty()) {
            holder.tvMemberChfId.setText("CHFID: " + member.getChfId());
        } else {
            holder.tvMemberChfId.setText("CHFID: N/A");
        }
        
        // Genre
        if (member.getGender() != null && !member.getGender().isEmpty()) {
            holder.tvMemberGender.setText(member.getGender());
        } else {
            holder.tvMemberGender.setText("N/A");
        }
        
        Log.d(LOG_TAG, "Basic data set for " + member.getFullName() + ": CHFID=" + member.getChfId() + ", Gender=" + member.getGender());
        
        // Date de naissance
        if (member.getDob() != null && !member.getDob().isEmpty()) {
            String formattedDate = formatDate(member.getDob());
            holder.tvMemberDob.setText(formattedDate);
            Log.d(LOG_TAG, "DOB formatted for " + member.getFullName() + ": " + formattedDate);
        } else {
            holder.tvMemberDob.setText("N/A");
            Log.d(LOG_TAG, "No DOB for " + member.getFullName());
        }
        
        // Photo
        if (member.getPhotoData() != null && !member.getPhotoData().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(member.getPhotoData(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    holder.ivMemberPhoto.setImageBitmap(decodedByte);
                    Log.d(LOG_TAG, "Photo set for " + member.getFullName());
                } else {
                    holder.ivMemberPhoto.setImageResource(R.drawable.ic_person_placeholder);
                    Log.w(LOG_TAG, "Failed to decode photo for " + member.getFullName());
                }
            } catch (Exception e) {
                holder.ivMemberPhoto.setImageResource(R.drawable.ic_person_placeholder);
                Log.e(LOG_TAG, "Error decoding photo for " + member.getFullName() + ": " + e.getMessage());
            }
        } else {
            holder.ivMemberPhoto.setImageResource(R.drawable.ic_person_placeholder);
            Log.d(LOG_TAG, "No photo data for " + member.getFullName() + ", using placeholder");
        }
        
        Log.d(LOG_TAG, "View setup complete for position " + position + ": " + member.getFullName());
    }

    public void updateData(List<FamilyMember> newFamilyMembers) {
        this.familyMembers = newFamilyMembers;
        if (newFamilyMembers != null) {
            for (int i = 0; i < newFamilyMembers.size(); i++) {
                FamilyMember member = newFamilyMembers.get(i);
                Log.d(LOG_TAG, "Updated Member " + i + ": " + member.getFullName() + ", CHFID: " + member.getChfId());
            }
        }
        notifyDataSetChanged();
        Log.d(LOG_TAG, "notifyDataSetChanged() called");
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "N/A";
        }

        // Liste des formats de date possibles
        SimpleDateFormat[] inputFormats = {
            new SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'XXX yyyy", Locale.ENGLISH), // Thu Apr 02 00:00:00 GMT+03:00 2009
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()), // 2009-04-02
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()), // 02/04/2009
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) // 02-04-2009
        };

        for (SimpleDateFormat format : inputFormats) {
            try {
                Date date = format.parse(dateString);
                if (date != null) {
                    return outputDateFormat.format(date);
                }
            } catch (ParseException e) {
                // Continue avec le format suivant
            }
        }

        // Si aucun format ne fonctionne, retourner la chaîne originale
        Log.w(LOG_TAG, "Unable to parse date: " + dateString);
        return dateString;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMemberPhoto;
        TextView tvMemberName;
        TextView tvMemberChfId;
        TextView tvMemberGender;
        TextView tvMemberDob;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberPhoto = itemView.findViewById(R.id.ivMemberPhoto);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberChfId = itemView.findViewById(R.id.tvMemberChfId);
            tvMemberGender = itemView.findViewById(R.id.tvMemberGender);
            tvMemberDob = itemView.findViewById(R.id.tvMemberDob);
        }
    }
}