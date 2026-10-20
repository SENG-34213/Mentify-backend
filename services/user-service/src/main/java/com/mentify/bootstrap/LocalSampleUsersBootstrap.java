package com.mentify.bootstrap;

import com.mentify.config.SampleUsersProperties;
import com.mentify.entity.User;
import com.mentify.repository.UserRepository;
import com.mentify.service.authentication.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalSampleUsersBootstrap implements ApplicationRunner {

    private final SampleUsersProperties sampleUsersProperties;
    private final UserRepository userRepository;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!sampleUsersProperties.isEnabled()) {
            log.info("Local sample users bootstrap is disabled");
            return;
        }
        if (!StringUtils.hasText(sampleUsersProperties.getPassword())) {
            throw new IllegalStateException("MENTIFY_SAMPLE_USERS_PASSWORD must be configured for sample authentication");
        }

        String domain = "@" + sampleUsersProperties.getEmailDomain().toLowerCase();
        int provisionedUsers = 0;
        for (User user : userRepository.findAll()) {
            if (user.getEmail() == null || !user.getEmail().toLowerCase().endsWith(domain)) {
                continue;
            }
            if (sampleUsersProperties.isResetPasswordOnStartup()) {
                authServiceClient.provision(user.getId(), user.getEmail(), sampleUsersProperties.getPassword());
                provisionedUsers++;
            }
        }
        log.info("Sample users bootstrap completed; provisioned {} users in auth-service", provisionedUsers);
    }
}
