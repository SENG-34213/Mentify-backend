package com.mentify.material.service;

import com.mentify.material.dto.response.MaterialResponse;
import com.mentify.material.enums.MaterialPurpose;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MaterialUploadService {

    MaterialResponse upload(String title, String description, MaterialPurpose purpose, MultipartFile file, UUID uploadedBy);
}
