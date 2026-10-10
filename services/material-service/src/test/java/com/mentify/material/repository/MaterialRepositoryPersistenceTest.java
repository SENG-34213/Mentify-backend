package com.mentify.material.repository;

import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
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

// Pinned so CI's SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop cannot replace the Flyway schema under test.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
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
    void flywayMigrationCreatedRequiredIndexesAndConstraints() {
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
                "idx_materials_checksum",
                "idx_materials_created_at"
        );

        Set<String> constraints = jdbcTemplate.queryForList(
                        "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'MATERIALS'",
                        String.class
                )
                .stream()
                .map(constraint -> constraint.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertThat(constraints).contains(
                "pk_materials",
                "uk_materials_storage_key",
                "chk_materials_title_non_blank",
                "chk_materials_storage_key_not_url",
                "chk_materials_file_size_positive",
                "chk_materials_type",
                "chk_materials_purpose",
                "chk_materials_status"
        );
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
}
