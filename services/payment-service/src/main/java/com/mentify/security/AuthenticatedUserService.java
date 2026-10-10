package com.mentify.security;

import java.util.UUID;

public interface AuthenticatedUserService {

    UUID getCurrentUserId();
}
