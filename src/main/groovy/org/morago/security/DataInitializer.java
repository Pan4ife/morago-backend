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

        Role userRole = new Role();
        userRole.setName(RoleName.USER);
        roleRepository.save(userRole);

        Role translatorRole = new Role();
        translatorRole.setName(RoleName.TRANSLATOR);
        roleRepository.save(translatorRole);

        User user = new User();
        user.setEmail("user@morago.com");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setRoles(Set.of(userRole));
        userRepository.save(user);


        User translator = new User();
        translator.setEmail("translator@morago.com");
        translator.setPassword(passwordEncoder.encode("trans123"));
        translator.setRoles(Set.of(translatorRole));
        userRepository.save(translator);
    }
}
