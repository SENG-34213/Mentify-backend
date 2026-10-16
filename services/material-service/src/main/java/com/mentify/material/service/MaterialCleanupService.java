package com.mentify.material.service;

import com.mentify.material.dto.request.MaterialCleanupRequest;
import com.mentify.material.dto.response.MaterialCleanupResponse;

public interface MaterialCleanupService {

    MaterialCleanupResponse audit(MaterialCleanupRequest request);

    MaterialCleanupResponse cleanup(MaterialCleanupRequest request);
}
