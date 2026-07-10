package com.mentify.service.registration;

import com.mentify.exception.InvalidRoleException;
import com.mentify.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentIdGenerator {

    private final StudentProfileRepository studentProfileRepository;

    public String normalizeGrade(String grade) {
        try {
            int gradeNumber = Integer.parseInt(grade.trim());
            if (gradeNumber < 1 || gradeNumber > 13) {
                throw new InvalidRoleException("Grade must be between 1 and 13");
            }
            return String.format("%02d", gradeNumber);
        } catch (NumberFormatException exception) {
            throw new InvalidRoleException("Grade must be a valid number");
        }
    }

    public String generate(String normalizedGrade) {
        int nextStudentNumber = studentProfileRepository.findLastStudentNumberByGrade(normalizedGrade) + 1;
        return String.format("TIT-%s-%02d", normalizedGrade, nextStudentNumber);
    }
}
