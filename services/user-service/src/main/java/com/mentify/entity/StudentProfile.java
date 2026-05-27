package com.mentify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentify.enums.AttendanceMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile extends BaseEntity {

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true, length = 50)
    private String admissionId;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Past
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid phone number")
    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String guardianName;

    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid phone number")
    @Column(length = 20)
    private String guardianPhone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttendanceMode attendanceMode;

    @Column(length = 100)
    private String gradeGroupChatId;

    /**
     * Helper to get the full name of the student.
     */
    @JsonIgnore
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    /**
     * Calculates the student's age based on their Date of Birth.
     * @return Age in years, or 0 if DOB is null.
     */
    @JsonIgnore
    public int getAge() {
        if (this.dateOfBirth == null) {
            return 0;
        }
        return Period.between(this.dateOfBirth, LocalDate.now()).getYears();
    }
}
