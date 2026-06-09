package com.mentify.service.registration;

import com.mentify.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherCodeGenerator {

    private final TeacherProfileRepository teacherProfileRepository;

    public String generate() {
        int nextNumber = teacherProfileRepository.findLastTeacherNumber() + 1;
        return String.format("TIT-TCH-%03d", nextNumber);
    }
}
