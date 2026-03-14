
package com.easyschedule.backend.auth.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.easyschedule.backend.auth.models.ERole;
import com.easyschedule.backend.auth.models.Role;
import com.easyschedule.backend.auth.repositories.RoleRepository;

@Component
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataLoader(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
            roleRepository.save(new Role(ERole.ROLE_MODERATOR));
            roleRepository.save(new Role(ERole.ROLE_USER));
        }
    }
}
