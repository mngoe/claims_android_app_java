package org.openimis.imisclaims.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.openimis.imisclaims.R;
import org.openimis.imisclaims.domain.entity.FamilyMember;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.FamilyMemberViewHolder> {
    
    private List<FamilyMember> familyMembers;
    private Context context;
    private SimpleDateFormat dateFormat;
    private OnFamilyMemberClickListener clickListener;
    
    public interface OnFamilyMemberClickListener {
        void onFamilyMemberClick(FamilyMember familyMember);
    }
    
    public FamilyMemberAdapter(Context context, List<FamilyMember> familyMembers) {
        this.context = context;
        this.familyMembers = familyMembers;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }
    
    public void setOnFamilyMemberClickListener(OnFamilyMemberClickListener listener) {
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public FamilyMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_family_member, parent, false);
        return new FamilyMemberViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FamilyMemberViewHolder holder, int position) {
        FamilyMember member = familyMembers.get(position);
        
        // Afficher le nom complet
        holder.tvName.setText(member.getLastName() != null ? member.getLastName() : "");
        holder.tvFirstName.setText(member.getFirstName() != null ? member.getFirstName() : "");
        
        // Afficher le genre
        holder.tvGender.setText(member.getGender() != null ? member.getGender() : "");
        
        // Afficher le code d'immatriculation (CHFID)
        holder.tvChfId.setText(member.getChfId() != null ? member.getChfId() : "");
        
        // Afficher la date de naissance
        if (member.getDateOfBirth() != null) {
            holder.tvDateOfBirth.setText(dateFormat.format(member.getDateOfBirth()));
        } else {
            holder.tvDateOfBirth.setText("");
        }
        
        // Afficher les polices actives
        holder.tvActivePolicies.setText(member.getActivePoliciesString());
        
        // Afficher la photo
        if (member.getPhoto() != null && member.getPhoto().length > 0) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(member.getPhoto(), 0, member.getPhoto().length);
                if (bitmap != null) {
                    holder.ivPhoto.setImageBitmap(bitmap);
                } else {
                    holder.ivPhoto.setImageResource(R.drawable.enquire);
                }
            } catch (Exception e) {
                holder.ivPhoto.setImageResource(R.drawable.enquire);
            }
        } else {
            holder.ivPhoto.setImageResource(R.drawable.enquire);
        }
        
        // Gérer le clic sur l'élément (pour les chefs de sous-familles)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onFamilyMemberClick(member);
            }
        });
    }
    
    private boolean isPolygamousHead(FamilyMember member) {
        String relationship = member.getRelationship();
        if (relationship == null) return false;
        
        // Normaliser la relation (enlever espaces, mettre en minuscules)
        String normalizedRelation = relationship.trim().toLowerCase();
        
        // Vérifier différentes variantes possibles
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
    
    @Override
    public int getItemCount() {
        return familyMembers != null ? familyMembers.size() : 0;
    }
    
    public void updateFamilyMembers(List<FamilyMember> newFamilyMembers) {
        this.familyMembers = newFamilyMembers;
        notifyDataSetChanged();
    }
    
    public static class FamilyMemberViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName;
        TextView tvFirstName;
        TextView tvGender;
        TextView tvChfId;
        TextView tvDateOfBirth;
        TextView tvActivePolicies;
        
        public FamilyMemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_member_photo);
            tvName = itemView.findViewById(R.id.tv_member_name);
            tvFirstName = itemView.findViewById(R.id.tv_member_first_name);
            tvGender = itemView.findViewById(R.id.tv_member_gender);
            tvChfId = itemView.findViewById(R.id.tv_member_chfid);
            tvDateOfBirth = itemView.findViewById(R.id.tv_member_dob);
            tvActivePolicies = itemView.findViewById(R.id.tv_member_policies);
        }
    }
}