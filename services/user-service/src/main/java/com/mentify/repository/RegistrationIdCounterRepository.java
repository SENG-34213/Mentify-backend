package com.mentify.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class RegistrationIdCounterRepository {

    private static final String NEXT_NUMBER_SQL = """
            INSERT INTO registration_id_counters (
                registration_role, registration_year, registration_month, last_number
            ) VALUES (:role, :year, :month, 1)
            ON CONFLICT (registration_role, registration_year, registration_month)
            DO UPDATE SET last_number = registration_id_counters.last_number + 1
            RETURNING last_number
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public int nextNumber(String role, int year, int month) {
        Number nextNumber = (Number) entityManager.createNativeQuery(NEXT_NUMBER_SQL)
                .setParameter("role", role)
                .setParameter("year", year)
                .setParameter("month", month)
                .getSingleResult();
        return nextNumber.intValue();
    }
}
