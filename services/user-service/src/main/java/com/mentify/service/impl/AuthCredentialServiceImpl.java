package com.mentify.service.impl;

import com.mentify.entity.User;
import com.mentify.repository.UserRepository;
import com.mentify.service.AuthCredentialService;
import com.mentify.service.authentication.AuthServiceClient;
import com.mentify.service.registration.AuthUserProvisionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AuthCredentialServiceImpl implements AuthCredentialService {
    private final UserRepository users;
    private final AuthServiceClient authServiceClient;
    private final ConcurrentMap<String, String> pendingPasswords = new ConcurrentHashMap<>();

    @Override
    public String createUser(AuthUserProvisionRequest request) {
        return UUID.randomUUID().toString();
    }

    @Override
    @Transactional
    public void setInitialPassword(String userId, String password) {
        User user = users.findByAuthUserId(userId).orElse(null);
        if (user == null) {
            pendingPasswords.put(userId, password);
            return;
        }
        users.save(user);
        authServiceClient.provision(user.getId(), user.getEmail(), password);
    }

    @Override
    public void changePassword(String userId, String currentPassword, String newPassword) {
        authServiceClient.changePassword(UUID.fromString(userId), currentPassword, newPassword);
    }

    @Override
    public void unlock(String userId) {
        authServiceClient.unlock(UUID.fromString(userId));
    }

    @Override
    public boolean isAccountLocked(String userId) {
        return authServiceClient.isAccountLocked(UUID.fromString(userId));
    }

    @Override
    @Transactional
    public void applyPendingPassword(String userId) {
        String password = pendingPasswords.remove(userId);
        if (password == null) {
            return;
        }
        User user = users.findByAuthUserId(userId).orElse(null);
        if (user == null) {
            pendingPasswords.put(userId, password);
            return;
        }
        users.save(user);
        authServiceClient.provision(user.getId(), user.getEmail(), password);
    }

    @Override
    public void deleteUser(String userId) {
        pendingPasswords.remove(userId);
        try {
            authServiceClient.delete(UUID.fromString(userId));
        } catch (IllegalArgumentException exception) {
            // Legacy auth identifiers are not UUIDs; there is no auth record to remove.
        }
    }
}
