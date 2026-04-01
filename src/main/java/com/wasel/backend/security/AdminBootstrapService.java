package com.wasel.backend.security;

import com.wasel.backend.config.AppBootstrapProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapService implements ApplicationRunner {

    private final AppBootstrapProperties properties;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(
            AppBootstrapProperties properties,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.seedAdmin() || userAccountRepository.existsByEmailIgnoreCase(properties.adminEmail())) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setEmail(properties.adminEmail().toLowerCase());
        admin.setPassword(passwordEncoder.encode(properties.adminPassword()));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userAccountRepository.save(admin);
    }
}
