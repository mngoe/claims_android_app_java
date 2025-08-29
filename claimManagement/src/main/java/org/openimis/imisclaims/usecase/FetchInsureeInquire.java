package org.openimis.imisclaims.usecase;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.openimis.imisclaims.GetInsureeInquireQuery;
import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.domain.entity.Insuree;
import org.openimis.imisclaims.domain.entity.Policy;
import org.openimis.imisclaims.network.request.GetInsureeInquireGraphQLRequest;
import org.openimis.imisclaims.network.util.Mapper;
import org.openimis.imisclaims.util.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FetchInsureeInquire {

    @NonNull
    private final GetInsureeInquireGraphQLRequest request;

    public FetchInsureeInquire() {
        this(new GetInsureeInquireGraphQLRequest());
    }

    public FetchInsureeInquire(@NonNull GetInsureeInquireGraphQLRequest request) {
        this.request = request;
    }

    @NonNull
    @WorkerThread
    public Insuree execute(@NonNull String chfId) throws Exception {
        GetInsureeInquireQuery.Node node = request.get(chfId);
        return new Insuree(
                /* chfId = */ Objects.requireNonNull(node.chfId()),
                /* name = */ node.lastName() + " " + node.otherNames(),
                /* dateOfBirth = */ node.dob(),
                /* gender = */ node.gender() != null ? node.gender().gender() : null,
                /* photoPath = */ getPhotoPath(node.photos()),
                /* photo = */ getPhotoBytes(node.photos()),
                /* policies = */ Mapper.map(node.insureePolicies().edges(), this::toPolicy)
        );
    }

    // Variable pour stocker le CHFID principal pour les corrections
    private String primaryChfId;

    @NonNull
    @WorkerThread
    public List<FamilyMember> executeFamilyMembers(@NonNull String chfId) throws Exception {
        GetInsureeInquireQuery.Node node = request.get(chfId);
        List<FamilyMember> familyMembers = new ArrayList<>();
        
        // Stocker le CHFID principal pour les corrections
        this.primaryChfId = chfId;
        
        // Vérifier si les données de famille sont disponibles
        if (node.family() != null && node.family().members() != null) {
            // Récupérer tous les membres de la famille depuis GraphQL
            for (GetInsureeInquireQuery.Edge1 memberEdge : node.family().members().edges()) {
                GetInsureeInquireQuery.Node1 memberNode = memberEdge.node();
                if (memberNode != null) {
                    FamilyMember familyMember = createFamilyMemberFromNode(memberNode);
                    familyMembers.add(familyMember);
                }
            }
        } else {
            // Fallback: créer un membre de famille à partir du nœud principal
            FamilyMember primaryMember = createFamilyMemberFromPrimaryNode(node);
            familyMembers.add(primaryMember);
        }
        
        // Trier pour mettre le chef de famille en premier
        familyMembers.sort((m1, m2) -> Boolean.compare(m2.isHead(), m1.isHead()));
        
        return familyMembers;
    }

    @NonNull
    private FamilyMember createFamilyMemberFromNode(@NonNull GetInsureeInquireQuery.Node1 memberNode) {
        String chfId = Objects.requireNonNull(memberNode.chfId());
        String lastName = memberNode.lastName() != null ? memberNode.lastName() : "";
        String otherNames = memberNode.otherNames() != null ? memberNode.otherNames() : "";
        String gender = memberNode.gender() != null ? memberNode.gender().gender() : null;
        Date dateOfBirth = memberNode.dob();
        boolean isHead = memberNode.head();
        String relationship = memberNode.relationship() != null ? memberNode.relationship().relation() : "Membre";
        
        // CORRECTION 1: Ajuster le statut de chef de famille
        // Seul le membre avec le CHFID principal devrait être chef
        if (this.primaryChfId != null && !chfId.equals(this.primaryChfId)) {
            isHead = false;
        } else if (this.primaryChfId != null && chfId.equals(this.primaryChfId)) {
            isHead = true;
        }
        
        // CORRECTION 2: Corriger les relations familiales
        relationship = correctRelationship(relationship, gender, isHead, chfId);
        
        byte[] photo = getPhotoBytes1(memberNode.photos());
        String photoPath = getPhotoPath1(memberNode.photos());
        
        return new FamilyMember(
                chfId,
                lastName,
                otherNames,
                gender,
                dateOfBirth,
                photo,
                photoPath,
                new ArrayList<>(), // Les politiques ne sont pas disponibles pour les membres individuels
                relationship,
                isHead
        );
    }
    
    /**
     * Corrige les relations familiales basées sur le genre et le statut de chef
     */
    @NonNull
    private String correctRelationship(@NonNull String originalRelationship, @Nullable String gender, boolean isHead, @NonNull String chfId) {
        // Si c'est le chef de famille, garder "Chef de famille"
        if (isHead && chfId.equals(this.primaryChfId)) {
            return "Chef de famille";
        }
        
        // Si la relation est "Membre" ou vide, essayer de la corriger
        if ("Membre".equals(originalRelationship) || originalRelationship.isEmpty()) {
            if ("F".equals(gender)) {
                // Femme non-chef = probablement épouse
                return "Épouse";
            } else if ("M".equals(gender)) {
                // Homme non-chef = probablement fils ou autre
                return "Fils";
            }
        }
        
        // Normaliser les relations existantes
        String normalized = normalizeRelationship(originalRelationship);
        return normalized.isEmpty() ? originalRelationship : normalized;
    }
    
    /**
     * Normalise les relations familiales pour une meilleure détection
     */
    @NonNull
    private String normalizeRelationship(@NonNull String relationship) {
        String lower = relationship.toLowerCase().trim();
        
        // Épouses
        if (lower.contains("épouse") || lower.contains("epouse") || 
            lower.contains("wife") || lower.contains("femme")) {
            return "Épouse";
        }
        
        // Maris
        if (lower.contains("époux") || lower.contains("epoux") || 
            lower.contains("husband") || lower.contains("mari")) {
            return "Époux";
        }
        
        // Enfants
        if (lower.contains("fils") || lower.contains("son")) {
            return "Fils";
        }
        if (lower.contains("fille") || lower.contains("daughter")) {
            return "Fille";
        }
        
        return "";
    }

    @NonNull
    private FamilyMember createFamilyMemberFromPrimaryNode(@NonNull GetInsureeInquireQuery.Node primaryNode) {
        String chfId = Objects.requireNonNull(primaryNode.chfId());
        String lastName = primaryNode.lastName() != null ? primaryNode.lastName() : "";
        String otherNames = primaryNode.otherNames() != null ? primaryNode.otherNames() : "";
        String gender = primaryNode.gender() != null ? primaryNode.gender().gender() : null;
        Date dateOfBirth = primaryNode.dob();
        
        byte[] photo = getPhotoBytes(primaryNode.photos());
        String photoPath = getPhotoPath(primaryNode.photos());
        List<Policy> policies = Mapper.map(primaryNode.insureePolicies().edges(), this::toPolicy);
        
        return new FamilyMember(
                chfId,
                lastName,
                otherNames,
                gender,
                dateOfBirth,
                photo,
                photoPath,
                policies,
                "Chef de famille",
                true
        );
    }

    @Nullable
    private String getPhotoPath(@NonNull List<GetInsureeInquireQuery.Photo> photos) {
        for (GetInsureeInquireQuery.Photo photo : photos) {
            String filename = photo.filename();
            if (filename != null) {
                String folder = photo.folder();
                if (folder != null) {
                    folder = folder.replace('\\', '/');
                    if (!folder.endsWith("/")) {
                        folder += "/";
                    }
                    return folder + filename;
                }
                return filename;
            }
        }

        return null;
    }

    @Nullable
    private byte[] getPhotoBytes(@NonNull List<GetInsureeInquireQuery.Photo> photos) {
        for (GetInsureeInquireQuery.Photo photo : photos) {
            String photoBase64 = photo.photo();
            if (photoBase64 != null) {
                return Base64.decode(photoBase64, Base64.DEFAULT);
            }
        }

        return null;
    }

    @Nullable
    private String getPhotoPath1(@NonNull List<GetInsureeInquireQuery.Photo1> photos) {
        for (GetInsureeInquireQuery.Photo1 photo : photos) {
            String filename = photo.filename();
            if (filename != null) {
                String folder = photo.folder();
                if (folder != null) {
                    folder = folder.replace('\\', '/');
                    if (!folder.endsWith("/")) {
                        folder += "/";
                    }
                    return folder + filename;
                }
                return filename;
            }
        }

        return null;
    }

    @Nullable
    private byte[] getPhotoBytes1(@NonNull List<GetInsureeInquireQuery.Photo1> photos) {
        for (GetInsureeInquireQuery.Photo1 photo : photos) {
            String photoBase64 = photo.photo();
            if (photoBase64 != null) {
                return Base64.decode(photoBase64, Base64.DEFAULT);
            }
        }

        return null;
    }

    @NonNull
    private Policy toPolicy(@NonNull GetInsureeInquireQuery.Edge2 edge) {
        GetInsureeInquireQuery.Policy policy = Objects.requireNonNull(edge.node()).policy();
        GetInsureeInquireQuery.Product product = policy.product();
        return new Policy(
                /* code = */ product.code(),
                /* name = */ product.name(),
                /* value = */ policy.value(),
                /* expiryDate = */ policy.expiryDate(),
                /* status = */ intAsStatus(policy.status()),
                /* deductibleType = */ product.deductible(),
                /* deductibleIp = */ product.deductibleIp(),
                /* deductibleOp = */ product.deductibleOp(),
                /* ceilingIp = */ product.ceilingIp(),
                /* ceilingOp = */ product.ceilingOp(),
                /* antenatalAmountLeft = */ product.maxAmountAntenatal(),
                /* consultationAmountLeft = */ product.maxAmountConsultation(),
                /* deliveryAmountLeft = */ product.maxAmountDelivery(),
                /* hospitalizationAmountLeft = */ null, // maxAmountHospitalization n'est pas disponible dans cette variante
                /* surgeryAmountLeft = */ product.maxAmountSurgery(),
                /* totalAdmissionsLeft = */ product.maxNoHospitalization(),
                /* totalAntenatalLeft = */ product.maxNoAntenatal(),
                /* totalConsultationsLeft = */ product.maxNoConsultation(),
                /* totalDeliveriesLeft = */ product.maxNoDelivery(),
                /* totalSurgeriesLeft = */ product.maxNoSurgery(),
                /* totalVisitsLeft = */ product.maxNoVisits()
        );
    }

    /**
     * <a href="https://github.com/openimis/database_ms_sqlserver/blob/main/sql/stored_procedures/uspAPIGetCoverage.sql#L48C3-L48C101">Values for the status in the stored procedure</a>
     */
    @NonNull
    private Policy.Status intAsStatus(@Nullable Integer integer) {
        if (integer == null) {
            return Policy.Status.EXPIRED;
        }
        switch (integer) {
            case 1: return Policy.Status.IDLE;
            case 2: return Policy.Status.ACTIVE;
            case 4: return Policy.Status.SUSPENDED;
            //case 8: return Policy.Status.EXPIRED; <-- Same as default
            case 16: return Policy.Status.READY;
            default:
                return Policy.Status.EXPIRED;
        }
    }
}
