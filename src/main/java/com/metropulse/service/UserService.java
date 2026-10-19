package com.metropulse.service;

import com.metropulse.dto.form.RegistrationForm;
import com.metropulse.entity.AppUser;
import com.metropulse.enums.Role;
import com.metropulse.exception.DuplicateAccountException;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.exception.ResourceNotFoundException;
import com.metropulse.repository.AppUserRepository;
import com.metropulse.security.MetroUserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Accounts: registration, login lookup (UserDetailsService) and the current-user entity. */
@Service
public class UserService implements UserDetailsService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmailIgnoreCase(normalize(email))
                .map(MetroUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
    }

    /** Registers a commuter. The role is fixed here - a client can never choose it. */
    @Transactional
    public AppUser registerCommuter(RegistrationForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new InvalidRequestException("Passwords do not match");
        }
        return createUser(form.getDisplayName(), form.getEmail(), form.getPassword(), Role.COMMUTER);
    }

    @Transactional
    public AppUser createUser(String displayName, String email, String rawPassword, Role role) {
        String normalized = normalize(email);
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new DuplicateAccountException();
        }
        return users.save(new AppUser(displayName.strip(), normalized, passwordEncoder.encode(rawPassword), role));
    }

    @Transactional(readOnly = true)
    public AppUser require(long userId) {
        return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Transactional(readOnly = true)
    public boolean exists(String email) {
        return users.existsByEmailIgnoreCase(normalize(email));
    }

    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
