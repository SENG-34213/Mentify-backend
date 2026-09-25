package com.mentify.quiz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.AiQuizGenerationClient;
import com.mentify.quiz.client.dto.AiGeneratedQuizDraftResponse;
import com.mentify.quiz.config.AiQuizGenerationProperties;
import com.mentify.quiz.dto.response.AiQuizDraftResponse;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizGenerationDifficulty;
import com.mentify.quiz.exception.AiQuizGenerationException;
import com.mentify.quiz.exception.InvalidQuestionOptionsException;
import com.mentify.quiz.mapper.AiQuizDraftMapper;
import com.mentify.quiz.service.AiQuizGenerationService;
import com.mentify.quiz.service.CourseQuizAuthorizationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiQuizGenerationServiceImpl implements AiQuizGenerationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_UPSTREAM_ERROR_LENGTH = 500;

    private final CourseQuizAuthorizationService courseQuizAuthorizationService;
    private final AiQuizGenerationClient aiQuizGenerationClient;
    private final AiQuizDraftMapper aiQuizDraftMapper;
    private final AiQuizGenerationProperties properties;

    @Override
    public ApiResponse<AiQuizDraftResponse> generateDraft(
            MultipartFile file,
            UUID courseId,
            Integer questionCount,
            QuizGenerationDifficulty difficulty,
            QuestionType questionType,
            String authorizationHeader
    ) {
        validateSettings(questionCount, difficulty, questionType);
        if (hasAuthorizationHeader(authorizationHeader)) {
            courseQuizAuthorizationService.assertCanCreateQuizForCourse(courseId, authorizationHeader);
        }

        AiGeneratedQuizDraftResponse generatedDraft = callAiService(
                file,
                courseId,
                questionCount,
                difficulty,
                questionType,
                authorizationHeader
        );

        AiQuizDraftResponse draft = aiQuizDraftMapper.toDraftResponse(
                generatedDraft,
                difficulty,
                questionType,
                properties.getDefaultQuestionMarks()
        );

        return ApiResponse.<AiQuizDraftResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("AI quiz draft generated successfully")
                .data(draft)
                .build();
    }

    private void validateSettings(Integer questionCount, QuizGenerationDifficulty difficulty, QuestionType questionType) {
        if (questionCount == null || questionCount < 1 || questionCount > properties.getMaxQuestionCount()) {
            throw new InvalidQuestionOptionsException("Question count must be between 1 and " + properties.getMaxQuestionCount());
        }
        if (difficulty == null) {
            throw new InvalidQuestionOptionsException("Difficulty is required");
        }
        if (questionType != QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER) {
            throw new InvalidQuestionOptionsException("Only MULTIPLE_CHOICE_SINGLE_ANSWER is supported");
        }
    }

    private boolean hasAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader != null && !authorizationHeader.isBlank();
    }

    private AiGeneratedQuizDraftResponse callAiService(
            MultipartFile file,
            UUID courseId,
            Integer questionCount,
            QuizGenerationDifficulty difficulty,
            QuestionType questionType,
            String authorizationHeader
    ) {
        try {
            ApiResponse<AiGeneratedQuizDraftResponse> response = aiQuizGenerationClient.generateQuiz(
                    file,
                    courseId.toString(),
                    questionCount.toString(),
                    difficulty.name(),
                    questionType.name(),
                    authorizationHeader
            );
            if (response == null || response.getData() == null) {
                throw new AiQuizGenerationException(HttpStatus.BAD_GATEWAY, "AI service returned an empty quiz draft");
            }
            return response.getData();
        } catch (FeignException.BadRequest ex) {
            throw aiServiceException(HttpStatus.BAD_REQUEST, "AI quiz generation request was rejected", ex);
        } catch (FeignException.UnprocessableEntity ex) {
            throw aiServiceException(HttpStatus.UNPROCESSABLE_ENTITY, "AI could not generate a valid quiz from this document", ex);
        } catch (FeignException.TooManyRequests ex) {
            throw aiServiceException(HttpStatus.TOO_MANY_REQUESTS, "AI quiz generation rate limit exceeded", ex);
        } catch (FeignException.GatewayTimeout ex) {
            throw aiServiceException(HttpStatus.GATEWAY_TIMEOUT, "AI quiz generation timed out", ex);
        } catch (FeignException.ServiceUnavailable ex) {
            throw aiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AI service is unavailable", ex);
        } catch (FeignException ex) {
            throw aiServiceException(HttpStatus.BAD_GATEWAY, "AI quiz generation failed", ex);
        }
    }

    private AiQuizGenerationException aiServiceException(HttpStatus status, String baseMessage, FeignException ex) {
        return new AiQuizGenerationException(status, withUpstreamDetail(baseMessage, ex));
    }

    private String withUpstreamDetail(String baseMessage, FeignException ex) {
        String detail = extractUpstreamErrorDetail(ex);
        if (detail == null || detail.isBlank() || baseMessage.equalsIgnoreCase(detail)) {
            return baseMessage;
        }
        return baseMessage + ": " + detail;
    }

    private String extractUpstreamErrorDetail(FeignException ex) {
        String responseBody = ex.contentUTF8();
        if (responseBody == null || responseBody.isBlank()) {
            if (ex.status() < 0) {
                return "AI service is unreachable" + connectionFailureDetail(ex);
            }
            return "AI service responded with HTTP " + ex.status();
        }

        String trimmedBody = responseBody.trim();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(trimmedBody);
            List<String> details = new ArrayList<>();
            addTextField(details, root, "message");
            addTextField(details, root, "error");
            addTextField(details, root, "detail");
            addValidationErrors(details, root.get("errors"));
            if (!details.isEmpty()) {
                return limitDetail(String.join("; ", details));
            }
        } catch (JsonProcessingException ignored) {
            // Fall through and expose the plain response body when the AI service returned non-JSON text.
        }

        return limitDetail(trimmedBody);
    }

    private void addTextField(List<String> details, JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value != null && value.isTextual() && !value.asText().isBlank()) {
            details.add(value.asText().trim());
        }
    }

    private void addValidationErrors(List<String> details, JsonNode errors) {
        if (errors == null || errors.isNull()) {
            return;
        }
        if (errors.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = errors.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                details.add(field.getKey() + ": " + readableJsonValue(field.getValue()));
            }
            return;
        }
        if (errors.isArray()) {
            errors.forEach(error -> details.add(readableJsonValue(error)));
        }
    }

    private String readableJsonValue(JsonNode value) {
        return value.isTextual() ? value.asText().trim() : value.toString();
    }

    private String connectionFailureDetail(FeignException ex) {
        Throwable cause = rootCause(ex);
        String detail = cause != null ? cause.getMessage() : ex.getMessage();
        if (detail == null || detail.isBlank()) {
            return "";
        }
        return ": " + limitDetail(detail.trim());
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root == throwable ? null : root;
    }

    private String limitDetail(String detail) {
        if (detail.length() <= MAX_UPSTREAM_ERROR_LENGTH) {
            return detail;
        }
        return detail.substring(0, MAX_UPSTREAM_ERROR_LENGTH) + "...";
    }
}
