package com.mentify.communication.websocket;

import com.mentify.communication.TestJwtDecoderConfig;
import com.mentify.communication.client.CourseServiceClient;
import com.mentify.communication.client.EntrollmentServiceClient;
import com.mentify.communication.dto.request.WebSocketTestMessageRequest;
import com.mentify.communication.dto.response.WebSocketTestMessageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "spring.datasource.url=jdbc:h2:mem:communication_ws_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mentify.security.enabled=true",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/realms/mentify"
})
@Import(TestJwtDecoderConfig.class)
class WebSocketTestControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private CourseServiceClient courseServiceClient;

    @MockBean
    private EntrollmentServiceClient entrollmentServiceClient;

    private WebSocketStompClient stompClient;
    private StompSession sessionA;
    private StompSession sessionB;

    @AfterEach
    void tearDown() throws Exception {
        if (sessionA != null && sessionA.isConnected()) {
            sessionA.disconnect();
        }
        if (sessionB != null && sessionB.isConnected()) {
            sessionB.disconnect();
        }
    }

    @Test
    void twoClientsReceiveBroadcastFromTestTopic() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtDecoder.decode("task4-token")).thenReturn(jwt(userId, "student@mentify.com"));

        sessionA = connect("task4-token");
        sessionB = connect("task4-token");

        BlockingQueue<WebSocketTestMessageResponse> clientA = new ArrayBlockingQueue<>(1);
        BlockingQueue<WebSocketTestMessageResponse> clientB = new ArrayBlockingQueue<>(1);

        sessionA.subscribe("/topic/test", frameHandler(clientA, WebSocketTestMessageResponse.class));
        sessionB.subscribe("/topic/test", frameHandler(clientB, WebSocketTestMessageResponse.class));
        Thread.sleep(1000);

        sessionA.send("/app/test", WebSocketTestMessageRequest.builder().content("Hello from STOMP").build());

        WebSocketTestMessageResponse responseA = clientA.poll(5, TimeUnit.SECONDS);
        WebSocketTestMessageResponse responseB = clientB.poll(5, TimeUnit.SECONDS);

        assertThat(responseA).isNotNull();
        assertThat(responseB).isNotNull();
        assertThat(responseA.getContent()).isEqualTo("Hello from STOMP");
        assertThat(responseB.getContent()).isEqualTo("Hello from STOMP");
        assertThat(responseA.getReceivedAt()).isNotNull();
        assertThat(responseB.getReceivedAt()).isNotNull();
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
                .claim("realm_access", Map.of("roles", List.of("STUDENT")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}