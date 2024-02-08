package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.entity.Payment;
import dev.sunbirdrc.claim.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    private PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> getAllPayment() {
        return paymentRepository.findAll();
    }


    public Payment createPayment(Payment payment) {
        return paymentRepository.save(payment);
    }


}
