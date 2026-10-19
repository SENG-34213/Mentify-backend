package com.mentify.material.repository;

import com.mentify.material.dto.request.MaterialSearchCriteria;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Use Hibernate-generated schema for this Flyway-free test profile.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class MaterialRepositoryPersistenceTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAndLoadsMaterialMetadataWithGeneratedUuidAndTimestamps() {
        UUID uploaderId = UUID.randomUUID();
        Material material = newMaterial("course/week-1/intro.pdf", uploaderId);

        Material saved = materialRepository.saveAndFlush(material);
        entityManager.clear();

        Material loaded = materialRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getTitle()).isEqualTo("Introduction to Algebra");
        assertThat(loaded.getDescription()).isEqualTo("Week 1 class handout.");
        assertThat(loaded.getOriginalFilename()).isEqualTo("intro.pdf");
        assertThat(loaded.getStorageKey()).isEqualTo("course/week-1/intro.pdf");
        assertThat(loaded.getContentType()).isEqualTo("application/pdf");
        assertThat(loaded.getFileSize()).isEqualTo(1_024_000L);
        assertThat(loaded.getType()).isEqualTo(MaterialType.PDF);
        assertThat(loaded.getPurpose()).isEqualTo(MaterialPurpose.LEARNING_MATERIAL);
        assertThat(loaded.getStatus()).isEqualTo(MaterialStatus.UPLOADED);
        assertThat(loaded.getUploadedBy()).isEqualTo(uploaderId);
        assertThat(loaded.getChecksum()).isEqualTo("sha256:abc123");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void persistsEveryEnumValueAsString() {
        Arrays.stream(MaterialType.values()).forEach(type -> {
            Material material = newMaterial("types/" + type.name().toLowerCase(Locale.ROOT), UUID.randomUUID());
            material.setType(type);
            materialRepository.save(material);
        });
        Arrays.stream(MaterialPurpose.values()).forEach(purpose -> {
            Material material = newMaterial("purposes/" + purpose.name().toLowerCase(Locale.ROOT), UUID.randomUUID());
            material.setPurpose(purpose);
            materialRepository.save(material);
        });
        Arrays.stream(MaterialStatus.values()).forEach(status -> {
            Material material = newMaterial("statuses/" + status.name().toLowerCase(Locale.ROOT), UUID.randomUUID());
            material.setStatus(status);
            materialRepository.save(material);
        });

        materialRepository.flush();
        entityManager.clear();

        assertThat(materialRepository.findAll()).extracting(Material::getType).contains(MaterialType.values());
        assertThat(materialRepository.findAll()).extracting(Material::getPurpose).contains(MaterialPurpose.values());
        assertThat(materialRepository.findAll()).extracting(Material::getStatus).contains(MaterialStatus.values());
    }

    @Test
    void repositoryQueriesFindByStorageKeyUploaderStatusPurposeAndChecksum() {
        UUID uploaderId = UUID.randomUUID();
        Material firstMaterial = newMaterial("materials/first.pdf", uploaderId);
        firstMaterial.setCreatedAt(LocalDateTime.of(2026, 10, 11, 8, 0));
        firstMaterial.setUpdatedAt(LocalDateTime.of(2026, 10, 11, 8, 0));
        Material first = materialRepository.saveAndFlush(firstMaterial);

        Material second = newMaterial("materials/second.pdf", uploaderId);
        second.setStatus(MaterialStatus.AVAILABLE);
        second.setPurpose(MaterialPurpose.AI_SOURCE);
        second.setChecksum("sha256:shared");
        second.setCreatedAt(LocalDateTime.of(2026, 10, 11, 9, 0));
        second.setUpdatedAt(LocalDateTime.of(2026, 10, 11, 9, 0));
        materialRepository.saveAndFlush(second);

        Material third = newMaterial("materials/third.pdf", UUID.randomUUID());
        third.setStatus(MaterialStatus.AVAILABLE);
        third.setPurpose(MaterialPurpose.AI_SOURCE);
        third.setChecksum("sha256:shared");
        third.setCreatedAt(LocalDateTime.of(2026, 10, 11, 10, 0));
        third.setUpdatedAt(LocalDateTime.of(2026, 10, 11, 10, 0));
        materialRepository.saveAndFlush(third);
        entityManager.clear();

        assertThat(materialRepository.findByStorageKey("materials/first.pdf"))
                .isPresent()
                .get()
                .extracting(Material::getId)
                .isEqualTo(first.getId());
        assertThat(materialRepository.existsByStorageKey("materials/first.pdf")).isTrue();
        assertThat(materialRepository.findAllByUploadedByOrderByCreatedAtDesc(uploaderId))
                .extracting(Material::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(materialRepository.findAllByStatusOrderByCreatedAtDesc(MaterialStatus.AVAILABLE))
                .extracting(Material::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(materialRepository.findAllByPurposeAndStatusOrderByCreatedAtDesc(
                        MaterialPurpose.AI_SOURCE, MaterialStatus.AVAILABLE))
                .extracting(Material::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(materialRepository.findAllByChecksum("sha256:shared"))
                .extracting(Material::getId)
                .containsExactlyInAnyOrder(second.getId(), third.getId());
    }

    @Test
    void rejectsDuplicateStorageKeys() {
        materialRepository.saveAndFlush(newMaterial("materials/duplicate.pdf", UUID.randomUUID()));

        assertThatThrownBy(() -> materialRepository.saveAndFlush(
                newMaterial("materials/duplicate.pdf", UUID.randomUUID())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsPublicUrlAsStorageKey() {
        Material material = newMaterial("https://example-bucket.s3.amazonaws.com/public.pdf", UUID.randomUUID());

        assertThatThrownBy(() -> materialRepository.saveAndFlush(material))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsNonPositiveFileSize() {
        Material material = newMaterial("materials/empty.pdf", UUID.randomUUID());
        material.setFileSize(0L);

        assertThatThrownBy(() -> materialRepository.saveAndFlush(material))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void hibernateSchemaCreatesRequiredIndexesAndConstraints() {
        Set<String> indexes = jdbcTemplate.queryForList(
                        "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'MATERIALS'",
                        String.class
                )
                .stream()
                .map(index -> index.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertThat(indexes).contains(
                "idx_materials_uploaded_by",
                "idx_materials_status",
                "idx_materials_type",
                "idx_materials_purpose",
                "idx_materials_course_id",
                "idx_materials_checksum",
                "idx_materials_created_at",
                "idx_materials_title",
                "idx_materials_original_filename",
                "idx_materials_course_status_created"
        );

        Set<String> auditIndexes = jdbcTemplate.queryForList(
                        "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'MATERIAL_AUDIT_EVENTS'",
                        String.class
                )
                .stream()
                .map(index -> index.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertThat(auditIndexes).contains(
                "idx_material_audit_material_id",
                "idx_material_audit_action_created"
        );

        Set<String> materialConstraintTypes = jdbcTemplate.queryForList(
                        "SELECT CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'MATERIALS'",
                        String.class
                )
                .stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        assertThat(materialConstraintTypes).contains("PRIMARY KEY", "UNIQUE", "CHECK");

        Set<String> auditConstraintTypes = jdbcTemplate.queryForList(
                        "SELECT CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'MATERIAL_AUDIT_EVENTS'",
                        String.class
                )
                .stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        assertThat(auditConstraintTypes).contains("PRIMARY KEY", "CHECK");
    }

    @Test
    void specificationSearchMatchesTitleOrOriginalFilenameCaseInsensitively() {
        Material algebra = newMaterial("materials/algebra.pdf", UUID.randomUUID());
        algebra.setTitle("Introduction to Algebra");
        algebra.setOriginalFilename("week-one-intro.pdf");
        Material savedAlgebra = materialRepository.saveAndFlush(algebra);

        Material geometry = newMaterial("materials/geometry.pdf", UUID.randomUUID());
        geometry.setTitle("Geometry Notes");
        geometry.setOriginalFilename("polygons.pdf");
        materialRepository.saveAndFlush(geometry);
        entityManager.clear();

        MaterialSearchCriteria byTitle = criteria("ALGEBRA", null, null, null, null, null, null, null, null);
        assertThat(materialRepository.findAll(MaterialSpecifications.visibleFilter(byTitle)))
                .extracting(Material::getId)
                .containsExactly(savedAlgebra.getId());

        MaterialSearchCriteria byFilename = criteria("WEEK-ONE", null, null, null, null, null, null, null, null);
        assertThat(materialRepository.findAll(MaterialSpecifications.visibleFilter(byFilename)))
                .extracting(Material::getId)
                .containsExactly(savedAlgebra.getId());
    }

    @Test
    void specificationFiltersByTypePurposeStatusUploaderCourseAndCreatedDate() {
        UUID uploaderId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        Material matching = newMaterial("materials/matching.pdf", uploaderId);
        matching.setStatus(MaterialStatus.AVAILABLE);
        matching.setCourseId(courseId);
        Material savedMatching = materialRepository.saveAndFlush(matching);

        Material wrongPurpose = newMaterial("materials/wrong-purpose.pdf", uploaderId);
        wrongPurpose.setStatus(MaterialStatus.AVAILABLE);
        wrongPurpose.setPurpose(MaterialPurpose.AI_SOURCE);
        wrongPurpose.setCourseId(courseId);
        Material savedWrongPurpose = materialRepository.saveAndFlush(wrongPurpose);

        Material outsideDate = newMaterial("materials/outside-date.pdf", uploaderId);
        outsideDate.setStatus(MaterialStatus.AVAILABLE);
        outsideDate.setCourseId(courseId);
        Material savedOutsideDate = materialRepository.saveAndFlush(outsideDate);
        updateTimestamps(savedMatching.getId(), LocalDateTime.of(2026, 10, 11, 9, 0));
        updateTimestamps(savedWrongPurpose.getId(), LocalDateTime.of(2026, 10, 11, 9, 30));
        updateTimestamps(savedOutsideDate.getId(), LocalDateTime.of(2026, 10, 12, 9, 0));
        entityManager.clear();

        MaterialSearchCriteria criteria = criteria(
                null,
                MaterialType.PDF,
                MaterialPurpose.LEARNING_MATERIAL,
                MaterialStatus.AVAILABLE,
                uploaderId,
                courseId,
                LocalDateTime.of(2026, 10, 11, 0, 0),
                LocalDateTime.of(2026, 10, 11, 23, 59),
                null
        );

        assertThat(materialRepository.findAll(MaterialSpecifications.visibleFilter(criteria)))
                .extracting(Material::getId)
                .containsExactly(savedMatching.getId());
    }

    @Test
    void specificationSupportsPaginationSortingAndMaterialIdScope() {
        Material first = newMaterial("materials/scope-first.pdf", UUID.randomUUID());
        first.setTitle("Alpha");
        first.setStatus(MaterialStatus.AVAILABLE);
        Material savedFirst = materialRepository.saveAndFlush(first);

        Material second = newMaterial("materials/scope-second.pdf", UUID.randomUUID());
        second.setTitle("Beta");
        second.setStatus(MaterialStatus.AVAILABLE);
        Material savedSecond = materialRepository.saveAndFlush(second);

        Material outsideScope = newMaterial("materials/scope-third.pdf", UUID.randomUUID());
        outsideScope.setTitle("Gamma");
        outsideScope.setStatus(MaterialStatus.AVAILABLE);
        materialRepository.saveAndFlush(outsideScope);
        entityManager.clear();

        MaterialSearchCriteria scopedCriteria = criteria(
                null,
                null,
                null,
                MaterialStatus.AVAILABLE,
                null,
                null,
                null,
                null,
                Set.of(savedFirst.getId(), savedSecond.getId())
        );

        Page<Material> firstPage = materialRepository.findAll(
                MaterialSpecifications.visibleFilter(scopedCriteria),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "title"))
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent())
                .extracting(Material::getId)
                .containsExactly(savedSecond.getId());
    }

    private Material newMaterial(String storageKey, UUID uploadedBy) {
        return Material.builder()
                .title("Introduction to Algebra")
                .description("Week 1 class handout.")
                .originalFilename("intro.pdf")
                .storageKey(storageKey)
                .contentType("application/pdf")
                .fileSize(1_024_000L)
                .type(MaterialType.PDF)
                .purpose(MaterialPurpose.LEARNING_MATERIAL)
                .uploadedBy(uploadedBy)
                .checksum("sha256:abc123")
                .build();
    }

    private MaterialSearchCriteria criteria(
            String search,
            MaterialType type,
            MaterialPurpose purpose,
            MaterialStatus status,
            UUID uploadedBy,
            UUID courseId,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            Set<UUID> materialIds
    ) {
        return new MaterialSearchCriteria(
                search,
                type,
                purpose,
                status,
                uploadedBy,
                courseId,
                null,
                null,
                createdFrom,
                createdTo,
                materialIds
        );
    }

    private void updateTimestamps(UUID materialId, LocalDateTime timestamp) {
        jdbcTemplate.update(
                "UPDATE materials SET created_at = ?, updated_at = ? WHERE id = ?",
                timestamp,
                timestamp,
                materialId
        );
    }
}
