package com.mentify.communication;

import com.mentify.communication.security.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:communication_service_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mentify.security.enabled=true"
})
@Import(TestJwtDecoderConfig.class)
class CommunicationServiceApplicationTests {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Test
    void contextLoads() {
        assertThat(securityFilterChain).isNotNull();
        assertThat(authenticatedUserService).isNotNull();
    }
}
