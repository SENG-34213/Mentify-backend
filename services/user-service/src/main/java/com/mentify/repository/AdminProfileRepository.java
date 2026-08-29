package com.mentify.repository;

import com.mentify.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, UUID> {

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(admin_code FROM 'TIT-ADM-([0-9]+)$') AS INTEGER)), 0)
            FROM admin_profiles
            WHERE admin_code IS NOT NULL
            """, nativeQuery = true)
    int findLastAdminNumber();
}
