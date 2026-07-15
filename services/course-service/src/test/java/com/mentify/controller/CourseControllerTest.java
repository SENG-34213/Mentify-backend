package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.enums.CourseStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CourseController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    @Test
    void createCourse_whenRequestIsValid_returnsCreatedCourse() throws Exception {
        CourseResponse courseResponse = CourseResponse.builder()
                .id(UUID.randomUUID())
                .courseName("Mathematics")
                .courseDescription("Grade 10 mathematics")
                .courseThumbnail("math.png")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .gradeId(UUID.randomUUID())
                .assignedTeacherId(UUID.randomUUID())
                .subject("Mathematics")
                .online(true)
                .discountOfferPercent(new BigDecimal("20.00"))
                .visible(true)
                .courseStatus(CourseStatus.DRAFT)
                .isPublished(false)
                .numberOfStudents(0)
                .build();
        when(courseService.createCourse(any(CourseRequest.class))).thenReturn(
                ApiResponse.<CourseResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Course created successfully")
                        .data(courseResponse)
                        .build()
        );

        mockMvc.perform(post("/api/v1/course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Course created successfully"))
                .andExpect(jsonPath("$.data.courseName").value("Mathematics"))
                .andExpect(jsonPath("$.data.courseDescription").value("Grade 10 mathematics"))
                .andExpect(jsonPath("$.data.courseStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.published").value(false))
                .andExpect(jsonPath("$.data.numberOfStudents").value(0))
                .andExpect(jsonPath("$.data.subject").value("Mathematics"))
                .andExpect(jsonPath("$.data.online").value(true))
                .andExpect(jsonPath("$.data.discountOfferPercent").value(20.0))
                .andExpect(jsonPath("$.data.visible").value(true));

        verify(courseService).createCourse(any(CourseRequest.class));
    }

    @Test
    void createCourse_whenCourseNameIsBlank_returnsBadRequest() throws Exception {
        CourseRequest request = validRequest();
        request.setCourseName(" ");

        mockMvc.perform(post("/api/v1/course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_whenMonthlyFeeIsZero_returnsBadRequest() throws Exception {
        CourseRequest request = validRequest();
        request.setCourseFeeMonthly(BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_whenGradeIdIsMissing_returnsBadRequest() throws Exception {
        CourseRequest request = validRequest();
        request.setGradeId(null);

        mockMvc.perform(post("/api/v1/course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourse_whenRequestIsValid_returnsUpdatedCourse() throws Exception {
        UUID courseId = UUID.randomUUID();
        CourseResponse courseResponse = CourseResponse.builder()
                .id(courseId)
                .courseName("Advanced Mathematics")
                .courseDescription("Updated Grade 10 mathematics")
                .courseThumbnail("math-v2.png")
                .courseFeeMonthly(new BigDecimal("3000.00"))
                .gradeId(UUID.randomUUID())
                .assignedTeacherId(UUID.randomUUID())
                .subject("Mathematics")
                .online(false)
                .discountOfferPercent(new BigDecimal("10.00"))
                .visible(true)
                .courseStatus(CourseStatus.PUBLISHED)
                .isPublished(true)
                .numberOfStudents(14)
                .build();
        when(courseService.updateCourse(eq(courseId), any(CourseRequest.class))).thenReturn(
                ApiResponse.<CourseResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Course updated successfully")
                        .data(courseResponse)
                        .build()
        );

        CourseRequest request = validRequest();
        request.setCourseName("Advanced Mathematics");
        request.setCourseDescription("Updated Grade 10 mathematics");
        request.setIsOnline(false);
        request.setDiscountOfferPercent(new BigDecimal("10.00"));
        request.setIsPublished(true);

        mockMvc.perform(put("/api/v1/course/{courseId}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Course updated successfully"))
                .andExpect(jsonPath("$.data.courseName").value("Advanced Mathematics"))
                .andExpect(jsonPath("$.data.courseDescription").value("Updated Grade 10 mathematics"))
                .andExpect(jsonPath("$.data.courseStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.published").value(true));

        verify(courseService).updateCourse(eq(courseId), any(CourseRequest.class));
    }

    @Test
    void updateCourse_whenSubjectIsBlank_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        CourseRequest request = validRequest();
        request.setSubject(" ");

        mockMvc.perform(put("/api/v1/course/{courseId}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourse_hasAdminAndSuperAdminPreAuthorize() throws NoSuchMethodException {
        Method updateMethod = CourseController.class.getDeclaredMethod("updateCourse", UUID.class, CourseRequest.class);
        PreAuthorize preAuthorize = updateMethod.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("ADMIN");
        assertThat(preAuthorize.value()).contains("SUPER_ADMIN");
    }

        @Test
        void deleteCourse_whenCourseExists_returnsOk() throws Exception {
                UUID courseId = UUID.randomUUID();
                doReturn(ApiResponse.<Object>builder()
                                .status(HttpStatus.OK)
                                .message("Course deleted successfully")
                                .build())
                                .when(courseService).deleteCourse(courseId);

                mockMvc.perform(delete("/api/v1/course/{courseId}", courseId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Course deleted successfully"));

                verify(courseService).deleteCourse(courseId);
        }

        @Test
        void deleteCourse_hasAdminAndSuperAdminPreAuthorize() throws NoSuchMethodException {
                Method deleteMethod = CourseController.class.getDeclaredMethod("deleteCourse", UUID.class);
                PreAuthorize preAuthorize = deleteMethod.getAnnotation(PreAuthorize.class);

                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).contains("ADMIN");
                assertThat(preAuthorize.value()).contains("SUPER_ADMIN");
        }

    private CourseRequest validRequest() {
        return CourseRequest.builder()
                .courseName("Mathematics")
                .courseDescription("Grade 10 mathematics")
                .courseThumbnail("math.png")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .gradeId(UUID.randomUUID())
                .assignedTeacherId(UUID.randomUUID())
                .subject("Mathematics")
                .isOnline(true)
                .discountOfferPercent(new BigDecimal("20.00"))
                .isVisible(true)
                .build();
    }
}
