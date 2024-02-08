
package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.entity.Document;
import dev.sunbirdrc.claim.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/digilicker")
public class DocumentController {
    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<Document> saveDocument(@RequestBody Document document) {
        Document savedDocument = documentService.saveDocument(document);
        return new ResponseEntity<>(savedDocument, HttpStatus.CREATED);
    }

    @GetMapping("/osid/{osid}")
    public ResponseEntity<String> getDocumentByOsid(@PathVariable String osid) {
        Document document = documentService.getDocumentByOsid(osid);
        if (document != null) {
            return new ResponseEntity<>(document.getDocUri(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("", HttpStatus.OK);
        }
    }

    @GetMapping("/uri/{uri}")
    public ResponseEntity<String> getDocumentByUri(@PathVariable String uri) {
        Document document = documentService.getDocumentByUri(uri);
        if (document != null) {
            return new ResponseEntity<>(document.getOsid(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("OSID not available", HttpStatus.NOT_FOUND);
        }
    }
}

