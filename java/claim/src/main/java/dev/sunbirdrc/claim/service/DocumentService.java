package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.entity.Document;
import dev.sunbirdrc.claim.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {
    private DocumentRepository documentRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }

    public Document getDocumentByOsid(String osid) {
        return documentRepository.findByOsid(osid);
    }

    public Document getDocumentByUri(String uri) {
        return documentRepository.findByDocUri(uri);
    }

}
