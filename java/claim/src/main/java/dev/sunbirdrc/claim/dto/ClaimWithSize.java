package dev.sunbirdrc.claim.dto;

import dev.sunbirdrc.claim.entity.Claim;
import lombok.Data;

import java.util.List;

@Data
public class ClaimWithSize {

    List<Claim> claimList;
    Integer totalClaim=0;
    Integer totalRejected=0;
    Integer totalOpen=0;
    Integer totalApproved=0;

}
