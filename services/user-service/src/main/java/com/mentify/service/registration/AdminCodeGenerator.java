package com.mentify.service.registration;

import com.mentify.repository.AdminProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminCodeGenerator {

    private final AdminProfileRepository adminProfileRepository;

    public String generate() {
        int nextNumber = adminProfileRepository.findLastAdminNumber() + 1;
        return String.format("TIT-ADM-%03d", nextNumber);
    }
}
