package com.mentify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    private int loginAttempts = 0;

    @Column(nullable = false)
    private boolean emailVerified = false;

    private LocalDateTime lastLogin;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private StudentProfile studentProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private TeacherProfile teacherProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private AdminProfile adminProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Address address;

    /**
     * Safely manages the bidirectional relationship with StudentProfile.
     */
    public void setStudentProfile(StudentProfile studentProfile) {
        if (studentProfile == null) {
            if (this.studentProfile != null) {
                this.studentProfile.setUser(null);
            }
        } else {
            studentProfile.setUser(this);
        }
        this.studentProfile = studentProfile;
    }

    /**
     * Safely manages the bidirectional relationship with TeacherProfile.
     */
    public void setTeacherProfile(TeacherProfile teacherProfile) {
        if (teacherProfile == null) {
            if (this.teacherProfile != null) {
                this.teacherProfile.setUser(null);
            }
        } else {
            teacherProfile.setUser(this);
        }
        this.teacherProfile = teacherProfile;
    }

    /**
     * Safely manages the bidirectional relationship with AdminProfile.
     */
    public void setAdminProfile(AdminProfile adminProfile) {
        if (adminProfile == null) {
            if (this.adminProfile != null) {
                this.adminProfile.setUser(null);
            }
        } else {
            adminProfile.setUser(this);
        }
        this.adminProfile = adminProfile;
    }

    /**
     * Safely manages the bidirectional relationship with Address.
     */
    public void setAddress(Address address) {
        if (address == null) {
            if (this.address != null) {
                this.address.setUser(null);
            }
        } else {
            address.setUser(this);
        }
        this.address = address;
    }

    /**
     * Business logic helper to handle failed login attempts.
     */
    public void incrementLoginAttempts() {
        this.loginAttempts++;
        if (this.loginAttempts >= 5) {
            this.accountNonLocked = false;
        }
    }

    /**
     * Business logic helper to reset login attempts upon successful login.
     */
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.accountNonLocked = true;
    }

    /**
     * Checks if the user is fully active and allowed to log in.
     */
    @JsonIgnore
    public boolean isFullyActive() {
        return this.isActive() && 
               this.accountStatus == AccountStatus.ACTIVE && 
               this.accountNonLocked;
    }
}
