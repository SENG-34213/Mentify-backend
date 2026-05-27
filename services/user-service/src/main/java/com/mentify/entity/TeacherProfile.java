package com.mentify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "teacher_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfile extends BaseEntity {

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true, length = 50)
    private String teacherCode;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Past
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String nic;

    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid phone number")
    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String specialization;

    private LocalDate hireDate;
}
