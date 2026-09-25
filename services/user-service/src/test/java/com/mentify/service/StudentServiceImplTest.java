package com.mentify.service;

import com.mentify.mapper.StudentMapper;
import com.mentify.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StudentServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentMapper studentMapper;


}
