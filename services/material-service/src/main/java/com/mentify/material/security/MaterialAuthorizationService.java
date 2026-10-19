package com.mentify.material.security;

import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialResourceType;
import com.mentify.material.enums.MaterialVisibility;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public interface MaterialAuthorizationService {

    UUID requireUploadCourseAccess(UUID courseId, MaterialPurpose purpose, String authorizationHeader);

    void assertCanView(Material material, String authorizationHeader);

    void assertCanDownload(Material material, String authorizationHeader);

    void assertCanManage(Material material, String authorizationHeader);

    void assertCanAssociate(
            Material material,
            MaterialResourceType resourceType,
            UUID resourceId,
            UUID courseId,
            UUID ownerId,
            MaterialVisibility visibility,
            String authorizationHeader
    );

    Specification<Material> visibilitySpecification(String authorizationHeader);
}
