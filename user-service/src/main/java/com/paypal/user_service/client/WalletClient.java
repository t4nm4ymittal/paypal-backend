package com.paypal.user_service.client;


import com.paypal.user_service.Dto.CreateWalletRequest;
import com.paypal.user_service.Dto.WalletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "wallet-service",
        url = "${WALLET_SERVICE_URL:http://localhost:8093/api/v1/wallets}"
)
public interface WalletClient {

    @PostMapping
    WalletResponse createWallet(@RequestBody CreateWalletRequest request);
}