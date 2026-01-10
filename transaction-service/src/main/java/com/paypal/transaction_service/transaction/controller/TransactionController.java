package com.paypal.transaction_service.transaction.controller;

import com.paypal.transaction_service.transaction.Transaction;
import com.paypal.transaction_service.transaction.dto.TransactionRequest;
import com.paypal.transaction_service.transaction.dto.TransactionResponse;
import com.paypal.transaction_service.transaction.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@RequestBody TransactionRequest req) {
        Transaction resp = service.createTransaction(req);
        return ResponseEntity.ok(resp);    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> get(@PathVariable Long id) {
        return service.getById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(
            @PathVariable("userId") Long userId,
            HttpServletRequest request) {

        // Read JWT userId forwarded by gateway
        String tokenUserIdHeader = request.getHeader("X-User-Id");
        if (tokenUserIdHeader == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Missing X-User-Id header from gateway");
        }

        Long tokenUserId = Long.parseLong(tokenUserIdHeader);

        // Ensure user can only fetch their own transactions
        if (!userId.equals(tokenUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not authorized to view these transactions.");
        }

        List<Transaction> transactions = service.getTransactionsByUser(userId);

        return ResponseEntity.ok(transactions);
    }
}

