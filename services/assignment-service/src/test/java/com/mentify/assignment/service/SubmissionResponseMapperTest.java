package com.mentify.assignment.service;

import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.UserServiceClient;
import com.mentify.quiz.client.dto.StudentLookupRequest;
import com.mentify.quiz.client.dto.StudentLookupResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionResponseMapperTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Test
    void toResponses_looksUpStudentNamesInOneRequest() {
        UUID studentId = UUID.randomUUID();
        Submission first = submission(studentId);
        Submission second = submission(studentId);
        StudentLookupResponse student = new StudentLookupResponse();
        student.setStudentId(studentId);
        student.setFirstName("Jane");
        student.setLastName("Doe");
        when(userServiceClient.lookupStudents(new StudentLookupRequest(List.of(studentId)), "Bearer token"))
                .thenReturn(ApiResponse.<List<StudentLookupResponse>>builder()
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .data(List.of(student))
                        .build());

        List<com.mentify.assignment.dto.response.SubmissionResponse> responses =
                new SubmissionResponseMapper(userServiceClient).toResponses(
                        List.of(first, second),
                        "Bearer token"
                );

        assertThat(responses).extracting("studentName").containsExactly("Jane Doe", "Jane Doe");
        verify(userServiceClient).lookupStudents(new StudentLookupRequest(List.of(studentId)), "Bearer token");
    }

    private Submission submission(UUID studentId) {
        return Submission.builder()
                .studentId(studentId)
                .attemptNumber(1)
                .status(SubmissionStatus.SUBMITTED)
                .build();
    }
}
