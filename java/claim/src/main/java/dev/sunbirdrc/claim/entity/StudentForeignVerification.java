package dev.sunbirdrc.claim.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentForeignVerification {
    private String name;
    private String registrationNumber;
    private String refNo;
    private String phoneNumber;
    private String osid;
    private String fathersName;
    private String email;
    private String date;
    private String council;
    private String candidatePic;
    private String trainingCenter;
    private String workPlace;
    private String validityOfRegistration;
    private String dob;
    private String paymentStatus;
    private String claimType;
    private String feeReciptNo;
    private String state;
    private String district;
    private String country;
    private String pincode;


}