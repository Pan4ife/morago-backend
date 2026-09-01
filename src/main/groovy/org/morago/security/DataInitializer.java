package org.morago.security;

import lombok.RequiredArgsConstructor;
import org.morago.model.Role;
import org.morago.model.RoleName;
import org.morago.model.User;
import org.morago.repository.RoleRepository;
import org.morago.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Profile("dev")

public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() > 0) {
            return;
        }
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setName(RoleName.USER);
        roleRepository.save(userRole);

        Role translatorRole = new Role();
        translatorRole.setName(RoleName.TRANSLATOR);
        roleRepository.save(translatorRole);

        User tripleRoleUser = new User();
        tripleRoleUser.setEmail("admin@morago.com");
        tripleRoleUser.setPassword(passwordEncoder.encode("admin123"));
        tripleRoleUser.setRoles(Set.of(userRole, adminRole, translatorRole));
        tripleRoleUser.setBalance(BigDecimal.ZERO);
        userRepository.save(tripleRoleUser);

        User doubleRoleUser = new User();
        doubleRoleUser.setEmail("user@morago.com");
        doubleRoleUser.setPassword(passwordEncoder.encode("user123"));
        doubleRoleUser.setRoles(Set.of(userRole, adminRole));
        doubleRoleUser.setBalance(BigDecimal.ZERO);
        userRepository.save(doubleRoleUser);

        User singleRoleUser = new User();
        singleRoleUser.setEmail("translator@morago.com");
        singleRoleUser.setPassword(passwordEncoder.encode("translator123"));
        singleRoleUser.setRoles(Set.of(translatorRole));
        singleRoleUser.setBalance(BigDecimal.ZERO);
        userRepository.save(singleRoleUser);
    }
}
