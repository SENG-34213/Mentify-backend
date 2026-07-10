package com.mentify.userservice.service.impl;

import com.mentify.userservice.entity.User;
import com.mentify.userservice.exception.UserNotFoundException;
import com.mentify.userservice.repository.UserRepository;
import com.mentify.userservice.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setActive(active);
        userRepository.save(user);
    }
}
