package com.mentify.communication.websocket;

import com.mentify.communication.TestJwtDecoderConfig;
import com.mentify.communication.client.CourseServiceClient;
import com.mentify.communication.client.EntrollmentServiceClient;
import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.GroupMember;
import com.mentify.communication.enums.GroupMemberRole;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.repository.CommunicationGroupRepository;
import com.mentify.communication.repository.GroupMemberRepository;
import com.mentify.communication.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:communication_ws_group_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mentify.security.enabled=true",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/realms/mentify"
})
@Import(TestJwtDecoderConfig.class)
class CommunicationWebSocketControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunicationGroupRepository communicationGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private MessageService messageService;

    @MockBean
    private CourseServiceClient courseServiceClient;

    @MockBean
    private EntrollmentServiceClient entrollmentServiceClient;

    private WebSocketStompClient stompClient;
    private StompSession session;

    @AfterEach
    void tearDown() throws Exception {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    @Test
    void memberCanSendMessageAndReceiveBroadcast() throws Exception {
        UUID userId = UUID.randomUUID();

        when(jwtDecoder.decode("group-token")).thenReturn(jwt(userId, "teacher@mentify.com"));

        CommunicationGroup group = communicationGroupRepository.save(CommunicationGroup.builder()
                .courseId(UUID.randomUUID())
                .name("Java Programming Discussion")
                .description("Official communication group")
                .status(GroupStatus.ACTIVE)
                .build());
            UUID groupId = group.getId();

        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(userId)
                .role(GroupMemberRole.TEACHER)
                .joinedAt(LocalDateTime.now())
                .build();
        member.setActive(true);
        groupMemberRepository.save(member);

        MessageResponse expectedResponse = MessageResponse.builder()
            .id(UUID.randomUUID())
            .groupId(groupId)
            .senderId(userId)
            .content("Hello group")
            .type(com.mentify.communication.enums.MessageType.TEXT)
            .sentAt(LocalDateTime.now())
            .build();

        when(messageService.sendMessage(groupId, SendMessageRequest.builder().content("Hello group").build(), userId))
            .thenReturn(expectedResponse);

        session = connect("group-token");

        BlockingQueue<MessageResponse> responses = new ArrayBlockingQueue<>(1);
        session.subscribe("/topic/groups/" + groupId, frameHandler(responses, MessageResponse.class));
        Thread.sleep(1000);

        session.send("/app/groups/" + groupId + "/messages", SendMessageRequest.builder().content("Hello group").build());

        MessageResponse broadcast = responses.poll(5, TimeUnit.SECONDS);

        assertThat(broadcast).isNotNull();
        assertThat(broadcast).isEqualTo(expectedResponse);
    }

    private StompSession connect(String token) throws Exception {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        return stompClient.connect("ws://localhost:" + port + "/ws", handshakeHeaders, connectHeaders, new StompSessionHandlerAdapter() {
        }).get(5, TimeUnit.SECONDS);
    }

    private <T> StompFrameHandler frameHandler(BlockingQueue<T> queue, Class<T> payloadType) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add(payloadType.cast(payload));
            }
        };
    }

    private Jwt jwt(UUID userId, String username) {
        return Jwt.withTokenValue("token-value")
                .header("alg", "none")
                .subject(userId.toString())
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", List.of("TEACHER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}