package com.ledgeflow.wallet.dto.wallet;

import com.ledgeflow.wallet.enums.Currency;
import jakarta.validation.constraints.NotNull;

public class CreateWalletRequest {

    @NotNull(message = "Currency is required")
    private Currency currency;

    public CreateWalletRequest() {
    }

    public CreateWalletRequest(Currency currency) {
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
