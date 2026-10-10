package com.ledgeflow.wallet.service;

import com.ledgeflow.wallet.dto.wallet.BalanceResponse;
import com.ledgeflow.wallet.dto.wallet.CreateWalletRequest;
import com.ledgeflow.wallet.dto.wallet.WalletResponse;
import com.ledgeflow.wallet.entity.User;
import com.ledgeflow.wallet.entity.Wallet;
import com.ledgeflow.wallet.enums.AccountStatus;
import com.ledgeflow.wallet.enums.Currency;
import com.ledgeflow.wallet.enums.KycTier;
import com.ledgeflow.wallet.exception.ResourceNotFoundException;
import com.ledgeflow.wallet.repository.UserRepository;
import com.ledgeflow.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public WalletService(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WalletResponse createWallet(String username, CreateWalletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        walletRepository.findByUserIdAndCurrency(user.getId(), request.getCurrency())
                .ifPresent(w -> {
                    throw new IllegalArgumentException("User already has a wallet for currency: " + request.getCurrency());
                });

        Wallet wallet = createDefaultWalletForUser(user, request.getCurrency());
        return mapToResponse(wallet);
    }

    @Transactional
    public Wallet createDefaultWalletForUser(User user, Currency currency) {
        String walletNumber = generateUniqueWalletNumber();
        Wallet wallet = new Wallet(walletNumber, user, currency, KycTier.TIER_1);
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return walletRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByNumber(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletNumber));
        return mapToResponse(wallet);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletNumber));

        return new BalanceResponse(
                wallet.getWalletNumber(),
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getAvailableBalance(),
                wallet.getHeldBalance()
        );
    }

    @Transactional
    public WalletResponse updateKycTier(String walletNumber, KycTier newTier) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletNumber));

        wallet.setKycTier(newTier);
        return mapToResponse(walletRepository.save(wallet));
    }

    @Transactional
    public WalletResponse setStatus(String walletNumber, AccountStatus status) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletNumber));

        wallet.setStatus(status);
        return mapToResponse(walletRepository.save(wallet));
    }

    private String generateUniqueWalletNumber() {
        String walletNumber;
        do {
            int randomPart = 100000 + secureRandom.nextInt(900000);
            walletNumber = "LF-" + randomPart;
        } while (walletRepository.findByWalletNumber(walletNumber).isPresent());
        return walletNumber;
    }

    public WalletResponse mapToResponse(Wallet wallet) {
        WalletResponse resp = new WalletResponse();
        resp.setId(wallet.getId());
        resp.setWalletNumber(wallet.getWalletNumber());
        resp.setUsername(wallet.getUser() != null ? wallet.getUser().getUsername() : "PLATFORM");
        resp.setCurrency(wallet.getCurrency());
        resp.setBalance(wallet.getBalance());
        resp.setAvailableBalance(wallet.getAvailableBalance());
        resp.setHeldBalance(wallet.getHeldBalance());
        resp.setKycTier(wallet.getKycTier());
        resp.setDailyTransferLimit(wallet.getKycTier().getDailyTransferLimit());
        resp.setStatus(wallet.getStatus());
        resp.setCreatedAt(wallet.getCreatedAt());
        return resp;
    }
}
