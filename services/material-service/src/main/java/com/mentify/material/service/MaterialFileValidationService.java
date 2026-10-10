package com.mentify.material.service;

import com.mentify.material.dto.ValidatedMaterialFile;
import org.springframework.web.multipart.MultipartFile;

public interface MaterialFileValidationService {

    ValidatedMaterialFile validate(MultipartFile file);
}
