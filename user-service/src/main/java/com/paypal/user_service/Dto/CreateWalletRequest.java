package com.paypal.user_service.Dto;

public class CreateWalletRequest {
    private Long userId;
    private String currency;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}