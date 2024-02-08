package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, String> {
    List<Claim> findByConditionsIn(List<String> conditions);
    List<Claim> findByAttestorEntityIn(List<String> entities);
    List<Claim> findByAttestorEntity(String entity);

    List<Claim> findByAttestorEntityContainingAndEntityContaining(String attestorEntity, String entity);
    List<Claim> findByRequestorName(String requestorName);

    List<Claim> findByEntityId(String entityId);

    @Query("select c from Claim c where c.createdAt >= :startDate and c.createdAt <= :endDate and c.entity = :entity")
    List<Claim> findByCreatedAtWithEntity(Date startDate, Date endDate, String entity);

    @Query("select c from Claim c where c.createdAt >= :startDate and c.createdAt <= :endDate")
    List<Claim> findByCreatedAtRange(Date startDate, Date endDate);

    List<Claim> findByEntity(String entityName);
}
