package dev.sunbirdrc.registry.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Document {
    private String osid;
    private String docUri;

    public Document(){}
    public Document(String osid, String docUri) {
        this.docUri=docUri;
        this.osid=osid;
    }
}
