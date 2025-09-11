package org.openimis.imisclaims;

import org.openimis.imisclaims.domain.entity.FamilyMember;
import org.openimis.imisclaims.network.GetFamilyMembersGraphQLRequest;
import org.openimis.imisclaims.tools.Log;

import java.util.List;

public class FetchFamilyMembers {
    private static final String LOG_TAG = "FETCH_FAMILY_MEMBERS";

    public FetchFamilyMembers() {
    }

    public List<FamilyMember> execute(String familyUuid) throws Exception {
        Log.d(LOG_TAG, "Fetching family members for familyUuid: " + familyUuid);
        
        GetFamilyMembersGraphQLRequest request = new GetFamilyMembersGraphQLRequest();
        List<FamilyMember> familyMembers = request.get(familyUuid);
        
        Log.d(LOG_TAG, "Found " + familyMembers.size() + " family members");
        return familyMembers;
    }
}