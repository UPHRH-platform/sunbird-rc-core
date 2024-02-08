package dev.sunbirdrc.claim.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "osid")
    String osid;
    @Column(name = "name")
    String name;
    @Column(name = "finalYearRollNo")
    String finalYearRollNo;
    @Column(name = "paymentAmount")
    String paymentAmount;
     @Column(name = "paymentStatus")
    String paymentStatus;
    @Column(name = "transactionId")
    String transactionId;
    @Column(name = "dateOfPayment")
    String dateOfPayment;

}
