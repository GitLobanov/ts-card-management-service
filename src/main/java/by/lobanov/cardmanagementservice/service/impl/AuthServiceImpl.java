package by.lobanov.cardmanagementservice.service.impl;

import by.lobanov.cardmanagementservice.exception.BadRequestException;
import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.model.constant.RoleType;
import by.lobanov.cardmanagementservice.model.dto.request.RegisterRequest;
import by.lobanov.cardmanagementservice.model.entity.Role;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.repository.RoleRepository;
import by.lobanov.cardmanagementservice.repository.UserRepository;
import by.lobanov.cardmanagementservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerUser(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail();
        logger.info("Registration attempt for email: {}", email);

        if (userRepository.existsByEmail(email)) {
            logger.warn("Registration failed: Email {} already exists.", email);
            throw new BadRequestException("Email '" + email + "' is already taken!");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> {
                    logger.error("Default role ROLE_USER not found in database!");
                    return new ResourceNotFoundException("Default role ROLE_USER not found.");
                });

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);
        logger.info("User registered successfully with email: {}", email);
    }
}
