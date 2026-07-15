package com.mentify.service;

import java.util.UUID;

public interface StudentValidationService {
    boolean studentExists(UUID studentId);
}
