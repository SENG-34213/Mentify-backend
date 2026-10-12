package com.mentify.material.security;

import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public interface MaterialAuthorizationService {

    UUID requireUploadCourseAccess(UUID courseId, MaterialPurpose purpose, String authorizationHeader);

    void assertCanView(Material material, String authorizationHeader);

    void assertCanDownload(Material material, String authorizationHeader);

    void assertCanManage(Material material, String authorizationHeader);

    Specification<Material> visibilitySpecification(String authorizationHeader);
}
