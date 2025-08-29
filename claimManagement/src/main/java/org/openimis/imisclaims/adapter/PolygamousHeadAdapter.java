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

public class PolygamousHeadAdapter extends RecyclerView.Adapter<PolygamousHeadAdapter.PolygamousHeadViewHolder> {
    
    private List<FamilyMember> polygamousHeads;
    private Context context;
    private SimpleDateFormat dateFormat;
    private OnPolygamousHeadClickListener clickListener;
    
    public interface OnPolygamousHeadClickListener {
        void onPolygamousHeadClick(FamilyMember polygamousHead);
    }
    
    public PolygamousHeadAdapter(Context context, List<FamilyMember> polygamousHeads, OnPolygamousHeadClickListener clickListener) {
        this.context = context;
        this.polygamousHeads = polygamousHeads;
        this.clickListener = clickListener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public PolygamousHeadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_polygamous_head, parent, false);
        return new PolygamousHeadViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PolygamousHeadViewHolder holder, int position) {
        FamilyMember polygamousHead = polygamousHeads.get(position);
        
        // Définir le nom complet
        String fullName = polygamousHead.getFullName().trim();
        if (fullName.isEmpty()) {
            fullName = "Nom non disponible";
        }
        holder.tvPolygamousHeadName.setText(fullName);
        
        // Définir le genre
        String gender = polygamousHead.getGender();
        if (gender != null) {
            holder.tvPolygamousHeadGender.setText(gender.equals("M") ? "Masculin" : "Féminin");
        } else {
            holder.tvPolygamousHeadGender.setText("Non spécifié");
        }
        
        // Définir le code d'immatriculation
        String chfId = polygamousHead.getChfId();
        if (chfId != null && !chfId.isEmpty()) {
            holder.tvPolygamousHeadCode.setText(chfId);
        } else {
            holder.tvPolygamousHeadCode.setText("Non disponible");
        }
        
        // Définir la date de naissance
        if (polygamousHead.getDateOfBirth() != null) {
            holder.tvPolygamousHeadDOB.setText(dateFormat.format(polygamousHead.getDateOfBirth()));
        } else {
            holder.tvPolygamousHeadDOB.setText("Non disponible");
        }
        
        // Définir les polices actives
        String activePolicies = polygamousHead.getActivePoliciesString();
        holder.tvPolygamousHeadPolicies.setText(activePolicies);
        
        // Définir la photo
        if (polygamousHead.getPhoto() != null && polygamousHead.getPhoto().length > 0) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(polygamousHead.getPhoto(), 0, polygamousHead.getPhoto().length);
                if (bitmap != null) {
                    holder.ivPolygamousHeadPhoto.setImageBitmap(bitmap);
                } else {
                    holder.ivPolygamousHeadPhoto.setImageResource(R.drawable.noimage);
                }
            } catch (Exception e) {
                holder.ivPolygamousHeadPhoto.setImageResource(R.drawable.noimage);
            }
        } else {
            holder.ivPolygamousHeadPhoto.setImageResource(R.drawable.noimage);
        }
        
        // Gérer le clic sur l'élément
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPolygamousHeadClick(polygamousHead);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return polygamousHeads != null ? polygamousHeads.size() : 0;
    }
    
    public void updatePolygamousHeads(List<FamilyMember> newPolygamousHeads) {
        this.polygamousHeads = newPolygamousHeads;
        notifyDataSetChanged();
    }
    
    public static class PolygamousHeadViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPolygamousHeadPhoto;
        TextView tvPolygamousHeadName;
        TextView tvPolygamousHeadGender;
        TextView tvPolygamousHeadCode;
        TextView tvPolygamousHeadDOB;
        TextView tvPolygamousHeadPolicies;
        
        public PolygamousHeadViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPolygamousHeadPhoto = itemView.findViewById(R.id.ivPolygamousHeadPhoto);
            tvPolygamousHeadName = itemView.findViewById(R.id.tvPolygamousHeadName);
            tvPolygamousHeadGender = itemView.findViewById(R.id.tvPolygamousHeadGender);
            tvPolygamousHeadCode = itemView.findViewById(R.id.tvPolygamousHeadCode);
            tvPolygamousHeadDOB = itemView.findViewById(R.id.tvPolygamousHeadDOB);
            tvPolygamousHeadPolicies = itemView.findViewById(R.id.tvPolygamousHeadPolicies);
        }
    }
}