package com.paypal.transaction_service.transaction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.transaction_service.transaction.Transaction;
import com.paypal.transaction_service.transaction.dto.TransactionRequest;
import com.paypal.transaction_service.transaction.dto.TransactionResponse;
import com.paypal.transaction_service.transaction.repo.TransactionRepository;
import com.paypal.transaction_service.kafka.TransactionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository repository;
    private final TransactionPublisher publisher;
    private final ObjectMapper objectMapper;
    @Autowired
    private RestTemplate restTemplate;
    public TransactionService(TransactionRepository repository, TransactionPublisher publisher, ObjectMapper objectMapper) {
        this.repository = repository;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Transaction createTransaction(TransactionRequest req) {

        System.out.println("🚀 Entered createTransaction()");
        Long senderId = req.getSenderAccountId();
        Long receiverId = req.getReceiverAccountId();
        Double amount = req.getAmount().doubleValue();
        Transaction t = new Transaction(req.getSenderAccountId(), req.getReceiverAccountId(), req.getAmount());
        req.setDescription("PENDING");
        Transaction savedTransaction = repository.save(t);
        System.out.println("📥 Transaction PENDING saved: " + savedTransaction);
        String walletServiceUrl = "http://localhost:8093/api/v1/wallets"; // wallet service base URL
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String holdReference = null;
        boolean captured = false; // whether capture (actual debit) completed

        try {
            // Step 1: Place hold on sender wallet
            String holdJson = String.format("{\"userId\": %d, \"currency\": \"INR\", \"amount\": %.2f}", senderId, amount);
            HttpEntity<String> holdEntity = new HttpEntity<>(holdJson, headers);
            ResponseEntity<String> holdResponse = restTemplate.postForEntity(walletServiceUrl + "/hold", holdEntity, String.class);

            if (!holdResponse.getStatusCode().is2xxSuccessful() || holdResponse.getBody() == null) {
                throw new RuntimeException("Failed to place hold: status=" + holdResponse.getStatusCode());
            }

            // Extract hold reference from response safely
            JsonNode holdNode = objectMapper.readTree(holdResponse.getBody());
            if (holdNode.get("holdReference") == null) {
                throw new RuntimeException("Hold response missing holdReference: " + holdResponse.getBody());
            }
            holdReference = holdNode.get("holdReference").asText();
            System.out.println("🛑 Hold placed: " + holdReference);

            try {
                ResponseEntity<String> receiverCheck = restTemplate.getForEntity(walletServiceUrl + "/" + receiverId, String.class);
                if (!receiverCheck.getStatusCode().is2xxSuccessful()) {
                    // release hold and fail the transaction
                    tryReleaseHold(walletServiceUrl, holdReference, headers);
                    System.out.println("🔄 Receiver wallet missing → hold released: " + holdReference);
                    savedTransaction.setStatus("FAILED");
                    savedTransaction = repository.save(savedTransaction);
                    System.out.println("❌ Transaction FAILED (receiver wallet missing): " + savedTransaction);
                    return savedTransaction;
                }
            } catch (HttpClientErrorException hx) {
                // receiver not found or other 4xx
                System.err.println("❌ Receiver wallet check failed: " + hx.getResponseBodyAsString());
                tryReleaseHold(walletServiceUrl, holdReference, headers);
                savedTransaction.setStatus("FAILED");
                savedTransaction = repository.save(savedTransaction);
                System.out.println("❌ Transaction FAILED (receiver check error): " + savedTransaction);
                return savedTransaction;
            }

            // Step 2: Capture hold → debit sender wallet
            String captureJson = String.format("{\"holdReference\": \"%s\"}", holdReference);
            HttpEntity<String> captureEntity = new HttpEntity<>(captureJson, headers);
            ResponseEntity<String> captureResponse = restTemplate.postForEntity(walletServiceUrl + "/capture", captureEntity, String.class);

            if (!captureResponse.getStatusCode().is2xxSuccessful()) {
                // If capture failed, release hold and fail
                System.err.println("❌ Capture failed: status=" + captureResponse.getStatusCode() + " body=" + captureResponse.getBody());
                tryReleaseHold(walletServiceUrl, holdReference, headers);
                savedTransaction.setStatus("FAILED");
                savedTransaction = repository.save(savedTransaction);
                System.out.println("❌ Transaction FAILED (capture failed): " + savedTransaction);
                return savedTransaction;
            }
            captured = true;
            System.out.println("💸 Hold captured → sender debited");

            // Step 3: Credit receiver wallet
            String creditJson = String.format("{\"userId\": %d, \"currency\": \"INR\", \"amount\": %.2f}", receiverId, amount);
            HttpEntity<String> creditEntity = new HttpEntity<>(creditJson, headers);
            try {
                ResponseEntity<String> creditResponse = restTemplate.postForEntity(walletServiceUrl + "/credit", creditEntity, String.class);
                if (!creditResponse.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Failed to credit receiver: status=" + creditResponse.getStatusCode());
                }
                System.out.println("💰 Receiver credited successfully");
            } catch (HttpClientErrorException creditEx) {
                // Credit failed AFTER capture — perform compensating refund to sender
                System.err.println("❌ Credit failed: " + creditEx.getResponseBodyAsString());

                // Attempt to refund sender
                try {
                    String refundJson = String.format("{\"userId\": %d, \"currency\": \"INR\", \"amount\": %.2f}", senderId, amount);
                    HttpEntity<String> refundEntity = new HttpEntity<>(refundJson, headers);
                    ResponseEntity<String> refundResponse = restTemplate.postForEntity(walletServiceUrl + "/credit", refundEntity, String.class);
                    if (refundResponse.getStatusCode().is2xxSuccessful()) {
                        System.out.println("🔁 Compensating refund to sender succeeded");
                    } else {
                        System.err.println("❌ Compensating refund to sender returned non-2xx: " + refundResponse.getStatusCode());
                    }
                } catch (Exception ex) {
                    System.err.println("❌ Compensating refund to sender failed: " + ex.getMessage());
                }

                savedTransaction.setStatus("FAILED");
                savedTransaction = repository.save(savedTransaction);
                System.out.println("❌ Transaction FAILED (credit failed & refunded sender): " + savedTransaction);
                return savedTransaction;
            }

            // Step 4: Mark transaction as SUCCESS
            savedTransaction.setStatus("SUCCESS");
            savedTransaction = repository.save(savedTransaction);
            System.out.println("✅ Transaction SUCCESS: " + savedTransaction);

        } catch (HttpClientErrorException e) {
            System.err.println("❌ Wallet service returned error: " + e.getResponseBodyAsString());
            if (holdReference != null && !captured) {
                tryReleaseHold(walletServiceUrl, holdReference, headers);
            }
            savedTransaction.setStatus("FAILED");
            savedTransaction = repository.save(savedTransaction);
            System.out.println("❌ Transaction FAILED saved (4xx): " + savedTransaction);
            return savedTransaction;
        } catch (Exception e) {
            System.err.println("❌ Transaction failed: " + e.getMessage());
            if (holdReference != null && !captured) {
                tryReleaseHold(walletServiceUrl, holdReference, headers);
            }
            savedTransaction.setStatus("FAILED");
            savedTransaction = repository.save(savedTransaction);
            System.out.println("❌ Transaction FAILED saved: " + savedTransaction);
            return savedTransaction;
        }

        // Publish to Kafka after save
        try {
            publisher.publish(savedTransaction);
        } catch (Exception e) {
            // Log and continue; do not fail the transaction on publish failure
            logger.error("Failed to publish transaction {}", e.getMessage());
        }
        return savedTransaction;
    }

    public Optional<TransactionResponse> getById(Long id) {
        return repository.findById(id).map(this::toResponse);
    }
    // Helper: best-effort release via path-style endpoint
    private void tryReleaseHold(String walletServiceUrl, String holdReference, HttpHeaders headers) {
        if (holdReference == null) return;
        try {
            // Use path-style release (matches WalletController: POST /release/{holdReference})
            String releaseUrl = walletServiceUrl + "/release/" + holdReference;
            System.out.println("ℹ️ Attempting hold release via: " + releaseUrl);
            ResponseEntity<String> releaseResp = restTemplate.postForEntity(releaseUrl, null, String.class);
            System.out.println("ℹ️ Release response: status=" + releaseResp.getStatusCode() + " body=" + releaseResp.getBody());
        } catch (Exception ex) {
            // Best-effort: log and move on (we don't want the whole transaction to crash on release failure)
            System.err.println("❌ Failed to release hold [" + holdReference + "]: " + ex.getMessage());
        }
    }
    public List<Transaction> getTransactionsByUser(Long userId) {
        return repository.findBySenderAccountIdOrReceiverAccountId(userId, userId);
    }
    private TransactionResponse toResponse(Transaction t) {
        TransactionResponse r = new TransactionResponse();
        r.setId(t.getId());
        r.setSenderAccountId(t.getSenderAccountId());
        r.setReceiverAccountId(t.getReceiverAccountId());
        r.setAmount(t.getAmount());
        r.setCurrency(t.getCurrency());
        r.setDescription(t.getDescription());
        r.setCreatedAt(t.getCreatedAt());
        r.setStatus(t.getStatus());
        return r;
    }
}
