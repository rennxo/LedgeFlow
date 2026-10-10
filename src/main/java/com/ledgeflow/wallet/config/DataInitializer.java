package com.ledgeflow.wallet.config;

import com.ledgeflow.wallet.dto.transaction.DepositRequest;
import com.ledgeflow.wallet.dto.transaction.TransferRequest;
import com.ledgeflow.wallet.entity.User;
import com.ledgeflow.wallet.entity.Wallet;
import com.ledgeflow.wallet.enums.Currency;
import com.ledgeflow.wallet.enums.KycTier;
import com.ledgeflow.wallet.enums.Role;
import com.ledgeflow.wallet.repository.UserRepository;
import com.ledgeflow.wallet.repository.WalletRepository;
import com.ledgeflow.wallet.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransferService transferService;

    public DataInitializer(UserRepository userRepository,
                           WalletRepository walletRepository,
                           PasswordEncoder passwordEncoder,
                           TransferService transferService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.transferService = transferService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("Initializing LedgeFlow demonstration data...");

        // 1. Create Users
        User admin = new User("admin", "admin@ledgeflow.com", passwordEncoder.encode("Admin@123"), "System Administrator", Role.ROLE_ADMIN);
        User alice = new User("alice", "alice@ledgeflow.com", passwordEncoder.encode("Alice@123"), "Alice Smith", Role.ROLE_USER);
        User bob = new User("bob", "bob@ledgeflow.com", passwordEncoder.encode("Bob@123"), "Bob Johnson", Role.ROLE_USER);

        userRepository.save(admin);
        userRepository.save(alice);
        userRepository.save(bob);

        // 2. Create Wallets with predefined identifiers
        Wallet aliceWallet = new Wallet("LF-100001", alice, Currency.USD, KycTier.TIER_2);
        Wallet bobWallet = new Wallet("LF-100002", bob, Currency.USD, KycTier.TIER_2);

        walletRepository.save(aliceWallet);
        walletRepository.save(bobWallet);

        // 3. Fund Alice & Bob and execute initial transfer
        transferService.deposit(new DepositRequest("LF-100001", new BigDecimal("2500.00"), "Initial Account Top-Up"), "IDEMP-SEED-001");
        transferService.deposit(new DepositRequest("LF-100002", new BigDecimal("1000.00"), "Initial Account Top-Up"), "IDEMP-SEED-002");

        transferService.transfer(
                new TransferRequest("LF-100001", "LF-100002", new BigDecimal("250.00"), "Dinner Split Payment"),
                "IDEMP-SEED-003"
        );

        log.info("==================================================================");
        log.info("LedgeFlow Initialized successfully!");
        log.info("Demo User 1: alice / Alice@123  (Wallet: LF-100001, Balance: $2250.00 USD)");
        log.info("Demo User 2: bob   / Bob@123    (Wallet: LF-100002, Balance: $1250.00 USD)");
        log.info("Admin User : admin / Admin@123");
        log.info("Swagger UI : http://localhost:8080/swagger-ui/index.html");
        log.info("Web App    : http://localhost:8080/");
        log.info("H2 Console : http://localhost:8080/h2-console");
        log.info("==================================================================");
    }
}
