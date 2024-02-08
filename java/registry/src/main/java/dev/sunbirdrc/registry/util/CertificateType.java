package dev.sunbirdrc.registry.util;

public enum CertificateType {
    PHCER("Doctor/ Pharmacist Registration Certificate"),
    NDCER("No Dues/ Objection Certificate"),
    DIPCR("Diploma Certificate"),
    DGMST("Degree/ Diploma Marksheet"),
    DGCER("Degree/ Diploma Certificate");

    private final String description;

    CertificateType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

