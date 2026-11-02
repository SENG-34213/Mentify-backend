package com.mentify.service.registration;

import com.mentify.repository.RegistrationIdCounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class MonthlyRegistrationIdGenerator {

    private final RegistrationIdCounterRepository counterRepository;
    private final Clock clock;

    @Autowired
    public MonthlyRegistrationIdGenerator(RegistrationIdCounterRepository counterRepository) {
        this(counterRepository, Clock.systemDefaultZone());
    }

    MonthlyRegistrationIdGenerator(RegistrationIdCounterRepository counterRepository, Clock clock) {
        this.counterRepository = counterRepository;
        this.clock = clock;
    }

    public String generateStudentId() {
        return generate("STUDENT", "STU");
    }

    public String generateTeacherId() {
        return generate("TEACHER", "THR");
    }

    private String generate(String role, String code) {
        LocalDate registrationDate = LocalDate.now(clock);
        int number = counterRepository.nextNumber(
                role,
                registrationDate.getYear(),
                registrationDate.getMonthValue());
        return String.format("TIT-%s-%04d%02d-%d", code,
                registrationDate.getYear(),
                registrationDate.getMonthValue(),
                number);
    }
}
