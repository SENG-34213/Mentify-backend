package com.mentify.security;

import java.util.UUID;

public interface AuthenticatedUserService {

    UUID getCurrentUserId();

    boolean hasAnyRole(String... roles);
}
