package com.mentify.service;

import com.mentify.dto.GradeRequest;
import com.mentify.dto.GradeResponse;
import com.mentify.entity.Grade;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.GradeRepository;
import com.mentify.service.impl.GradeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private CourseRepository courseRepository;

    private GradeServiceImpl gradeService;

    @BeforeEach
    void setUp() {
        gradeService = new GradeServiceImpl(gradeRepository, courseRepository);
    }

    @Test
    void createGrade_whenRequestIsValid_savesGradeAndReturnsCreatedResponse() {
        UUID gradeId = UUID.randomUUID();
        GradeRequest request = validRequest("  Grade 3  ");

        when(gradeRepository.existsByNameIgnoreCase("Grade 3")).thenReturn(false);
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade grade = invocation.getArgument(0);
            grade.setId(gradeId);
            return grade;
        });

        ApiResponse<GradeResponse> response = gradeService.createGrade(request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getMessage()).isEqualTo("Grade created successfully");
        assertThat(response.getData().getId()).isEqualTo(gradeId);
        assertThat(response.getData().getName()).isEqualTo("Grade 3");

        ArgumentCaptor<Grade> gradeCaptor = ArgumentCaptor.forClass(Grade.class);
        verify(gradeRepository).save(gradeCaptor.capture());
        assertThat(gradeCaptor.getValue().getName()).isEqualTo("Grade 3");
        assertThat(gradeCaptor.getValue().getDescription()).isEqualTo("Primary grade");
    }

    @Test
    void createGrade_whenNameExists_throwsResourceAlreadyExistsException() {
        GradeRequest request = validRequest("Grade 3");
        when(gradeRepository.existsByNameIgnoreCase("Grade 3")).thenReturn(true);

        assertThatThrownBy(() -> gradeService.createGrade(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Grade already exists with name: 'Grade 3'");

        verify(gradeRepository, never()).save(any());
    }

    @Test
    void updateGrade_whenRequestIsValid_updatesGradeAndReturnsOkResponse() {
        UUID gradeId = UUID.randomUUID();
        Grade grade = grade(gradeId, "Grade 3");
        GradeRequest request = validRequest("  Grade 4  ");

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
        when(gradeRepository.existsByNameIgnoreCaseAndIdNot("Grade 4", gradeId)).thenReturn(false);
        when(gradeRepository.save(grade)).thenReturn(grade);

        ApiResponse<GradeResponse> response = gradeService.updateGrade(gradeId, request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Grade updated successfully");
        assertThat(response.getData().getName()).isEqualTo("Grade 4");
        assertThat(grade.getName()).isEqualTo("Grade 4");
    }

    @Test
    void updateGrade_whenGradeDoesNotExist_throwsResourceNotFoundException() {
        UUID gradeId = UUID.randomUUID();
        when(gradeRepository.findById(gradeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.updateGrade(gradeId, validRequest("Grade 4")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Grade not found with id: '" + gradeId + "'");

        verify(gradeRepository, never()).save(any());
    }

    @Test
    void getGradeById_whenGradeExists_returnsGrade() {
        UUID gradeId = UUID.randomUUID();
        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(gradeId, "Grade 5")));

        ApiResponse<GradeResponse> response = gradeService.getGradeById(gradeId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().getName()).isEqualTo("Grade 5");
    }

    @Test
    void getAllGrades_returnsAllGrades() {
        when(gradeRepository.findAll()).thenReturn(List.of(
                grade(UUID.randomUUID(), "Grade 3"),
                grade(UUID.randomUUID(), "Grade 4")
        ));

        ApiResponse<List<GradeResponse>> response = gradeService.getAllGrades();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData()).extracting(GradeResponse::getName).containsExactly("Grade 3", "Grade 4");
    }

    @Test
    void deleteGrade_whenUnused_deletesGradeAndReturnsOkResponse() {
        UUID gradeId = UUID.randomUUID();
        Grade grade = grade(gradeId, "Grade 3");

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
        when(courseRepository.existsByGradeId(gradeId)).thenReturn(false);

        ApiResponse<Object> response = gradeService.deleteGrade(gradeId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Grade deleted successfully");
        verify(gradeRepository).delete(grade);
    }

    @Test
    void deleteGrade_whenAssignedToCourse_throwsResourceAlreadyExistsException() {
        UUID gradeId = UUID.randomUUID();
        Grade grade = grade(gradeId, "Grade 3");

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
        when(courseRepository.existsByGradeId(gradeId)).thenReturn(true);

        assertThatThrownBy(() -> gradeService.deleteGrade(gradeId))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Grade cannot be deleted because it is assigned to one or more courses");

        verify(gradeRepository, never()).delete(any());
    }

    private GradeRequest validRequest(String name) {
        return GradeRequest.builder()
                .name(name)
                .description("  Primary grade  ")
                .build();
    }

    private Grade grade(UUID gradeId, String name) {
        Grade grade = Grade.builder()
                .name(name)
                .description("Primary grade")
                .build();
        grade.setId(gradeId);
        grade.setActive(true);
        return grade;
    }
}
