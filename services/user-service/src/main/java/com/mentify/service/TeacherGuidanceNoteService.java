package com.mentify.service;

import com.mentify.dto.TeacherGuidanceNoteRequest;
import com.mentify.dto.TeacherGuidanceNoteResponse;
import com.mentify.entity.TeacherGuidanceNote;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.exception.InvalidUserStateException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.repository.TeacherGuidanceNoteRepository;
import com.mentify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherGuidanceNoteService {
    private final TeacherGuidanceNoteRepository noteRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeacherGuidanceNoteResponse create(UUID studentId, String identityId, TeacherGuidanceNoteRequest request) {
        User teacher = teacher(identityId);
        if (!userRepository.existsByIdAndRole(studentId, Role.STUDENT)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }
        TeacherGuidanceNote note = noteRepository.save(TeacherGuidanceNote.builder()
                .studentId(studentId).teacherId(teacher.getId()).note(request.getNote().trim()).build());
        return toResponse(note, teacher);
    }

    @Transactional(readOnly = true)
    public List<TeacherGuidanceNoteResponse> list(UUID studentId, String identityId) {
        User teacher = teacher(identityId);
        return noteRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(note -> toResponse(note, note.getTeacherId().equals(teacher.getId()) ? teacher :
                        userRepository.findById(note.getTeacherId()).orElse(null)))
                .toList();
    }

    private User teacher(String identityId) {
        User user;
        try { user = userRepository.findById(UUID.fromString(identityId)).orElse(null); }
        catch (IllegalArgumentException ex) { user = null; }
        if (user == null) user = userRepository.findByAuthUserId(identityId).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER) {
            throw new InvalidUserStateException("Authenticated user is not a teacher");
        }
        return user;
    }

    private TeacherGuidanceNoteResponse toResponse(TeacherGuidanceNote note, User teacher) {
        return TeacherGuidanceNoteResponse.builder().id(note.getId()).studentId(note.getStudentId())
                .teacherId(note.getTeacherId()).teacherName(teacher == null ? null :
                        (teacher.getFirstName() + " " + teacher.getLastName()).trim())
                .teacherEmail(teacher == null ? null : teacher.getEmail()).note(note.getNote())
                .createdAt(note.getCreatedAt()).updatedAt(note.getUpdatedAt()).build();
    }
}
