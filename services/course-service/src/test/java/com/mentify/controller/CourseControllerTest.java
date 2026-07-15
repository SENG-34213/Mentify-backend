package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.enums.CourseStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

        mockMvc.perform(post("/api/courses")
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

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_whenMonthlyFeeIsZero_returnsBadRequest() throws Exception {
        CourseRequest request = validRequest();
        request.setCourseFeeMonthly(BigDecimal.ZERO);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_whenGradeIdIsMissing_returnsBadRequest() throws Exception {
        CourseRequest request = validRequest();
        request.setGradeId(null);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
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
