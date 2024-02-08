package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Document findByOsid(String osid);

    Document findByDocUri(String docUri);
}