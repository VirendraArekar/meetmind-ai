package com.meetmind.config;

import com.meetmind.user.Role;
import com.meetmind.user.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<String> defaultRoles = List.of("ROLE_ADMIN", "ROLE_MEMBER", "ROLE_VIEWER");
        for (String roleName : defaultRoles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder()
                        .name(roleName)
                        .description("System role " + roleName)
                        .build());
            }
        }
    }
}
