package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.entity.Payment;
import dev.sunbirdrc.claim.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/Payment")
public class PaymentController {

    private PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/all")
    @CrossOrigin (origins = "*")
    public ResponseEntity<List<Payment>> getAllPayment() {
        List<Payment> Payment = paymentService.getAllPayment();
        return ResponseEntity.ok(Payment);
    }

     @PostMapping
     @CrossOrigin (origins = "*")
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        Payment savedCourse = paymentService.createPayment(payment);
        return ResponseEntity.ok(savedCourse);
    }
}
