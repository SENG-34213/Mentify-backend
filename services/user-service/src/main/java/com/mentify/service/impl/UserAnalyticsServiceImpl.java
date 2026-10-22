package com.mentify.service.impl;

import com.mentify.dto.UserRegistrationOverviewResponse;
import com.mentify.dto.UserAnalyticsOverviewResponse;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.repository.UserRepository;
import com.mentify.service.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAnalyticsServiceImpl implements UserAnalyticsService {

    private static final int RECENT_USER_LIMIT = 5;

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserRegistrationOverviewResponse getRegistrationOverview() {
        Map<Role, Long> usersByRole = Arrays.stream(Role.values())
                .collect(Collectors.toMap(Function.identity(), userRepository::countByRole));
        Map<AccountStatus, Long> usersByAccountStatus = Arrays.stream(AccountStatus.values())
                .collect(Collectors.toMap(Function.identity(), userRepository::countByAccountStatus));

        return UserRegistrationOverviewResponse.builder()
                .totalUsers(userRepository.count())
                .usersByRole(usersByRole)
                .usersByAccountStatus(usersByAccountStatus)
                .recentStudents(userRepository.findTop5ByRoleOrderByCreatedAtDesc(Role.STUDENT).stream()
                        .limit(RECENT_USER_LIMIT)
                        .map(UserRegistrationOverviewResponse.RegisteredUserSummary::from)
                        .toList())
                .recentTeachers(userRepository.findTop5ByRoleOrderByCreatedAtDesc(Role.TEACHER).stream()
                        .limit(RECENT_USER_LIMIT)
                        .map(UserRegistrationOverviewResponse.RegisteredUserSummary::from)
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAnalyticsOverviewResponse getAnalyticsOverview() {
        Map<AccountStatus, Long> accountStatusMix = Arrays.stream(AccountStatus.values())
                .collect(Collectors.toMap(Function.identity(), userRepository::countByAccountStatus));

        return new UserAnalyticsOverviewResponse(
                userRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.TEACHER),
                userRepository.countByRole(Role.ADMIN) + userRepository.countByRole(Role.SUPER_ADMIN),
                accountStatusMix
        );
    }
}
