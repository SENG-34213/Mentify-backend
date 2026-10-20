package com.mentify.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public RSAKey jwtSigningKey(AuthProperties properties) {
        if (hasConfiguredKeyPair(properties)) {
            return configuredKeyPair(properties);
        }
        if (hasPartialKeyPair(properties)) {
            throw new IllegalStateException(
                    "Both JWT private and public keys must be configured together."
            );
        }
        if (!properties.isAllowEphemeralKeys()) {
            throw new IllegalStateException(
                    "JWT signing keys are not configured. Set AUTH_JWT_PRIVATE_KEY_BASE64 and "
                            + "AUTH_JWT_PUBLIC_KEY_BASE64, or explicitly enable ephemeral keys for local development."
            );
        }
        try {
            RSAKey key = new RSAKeyGenerator(2048)
                    .keyID(properties.getKeyId())
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
            System.err.println("WARNING: auth-service is using an ephemeral JWT key. "
                    + "Configure persistent RSA keys before production deployment.");
            return key;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate JWT signing key", exception);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(@Qualifier("jwtSigningKey") RSAKey signingKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey)));
    }

    private boolean hasConfiguredKeyPair(AuthProperties properties) {
        return properties.getPrivateKeyBase64() != null && !properties.getPrivateKeyBase64().isBlank()
                && properties.getPublicKeyBase64() != null && !properties.getPublicKeyBase64().isBlank();
    }

    private boolean hasPartialKeyPair(AuthProperties properties) {
        return (properties.getPrivateKeyBase64() != null && !properties.getPrivateKeyBase64().isBlank())
                || (properties.getPublicKeyBase64() != null && !properties.getPublicKeyBase64().isBlank());
    }

    private RSAKey configuredKeyPair(AuthProperties properties) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decode(properties.getPrivateKeyBase64())));
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decode(properties.getPublicKeyBase64())));
            return new RSAKey.Builder((RSAPublicKey) publicKey)
                    .privateKey((RSAPrivateKey) privateKey)
                    .keyID(properties.getKeyId())
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Configured JWT RSA keys are invalid", exception);
        }
    }

    private byte[] decode(String encodedKey) {
        String normalized = encodedKey.trim()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized.getBytes(StandardCharsets.US_ASCII));
    }
}
