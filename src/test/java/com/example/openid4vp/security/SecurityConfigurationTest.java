package com.example.openid4vp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigurationTest {
    
    private SecurityConfiguration securityConfig;
    
    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfiguration();
    }
    
    @Test
    void testGenerateSecureNonce() {
        String nonce1 = securityConfig.generateSecureNonce();
        String nonce2 = securityConfig.generateSecureNonce();
        
        assertNotNull(nonce1);
        assertNotNull(nonce2);
        assertNotEquals(nonce1, nonce2);
        
        // Verify Base64 URL encoding
        assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(nonce1));
        
        // Verify minimum entropy (128 bits = 16 bytes = ~22 Base64 chars)
        assertTrue(nonce1.length() >= 22);
    }
    
    @Test
    void testValidateVPTokenSize() {
        String validToken = "valid.jwt.token";
        String oversizedToken = "x".repeat(200000); // 200KB
        
        assertDoesNotThrow(() -> securityConfig.validateVPTokenSize(validToken));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateVPTokenSize(oversizedToken));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateVPTokenSize(null));
    }
    
    @Test
    void testValidateRequestTiming() {
        Instant now = Instant.now();
        Instant validTime = now.minus(1, ChronoUnit.MINUTES);
        Instant expiredTime = now.minus(15, ChronoUnit.MINUTES);
        Instant futureTime = now.plus(5, ChronoUnit.MINUTES);
        
        assertDoesNotThrow(() -> securityConfig.validateRequestTiming(validTime));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateRequestTiming(expiredTime));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateRequestTiming(futureTime));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateRequestTiming(null));
    }
    
    @Test
    void testValidateNonceFormat() {
        String validNonce = securityConfig.generateSecureNonce();
        String shortNonce = Base64.getUrlEncoder().encodeToString("short".getBytes());
        String invalidBase64 = "invalid@#$%";
        
        assertDoesNotThrow(() -> securityConfig.validateNonceFormat(validNonce));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateNonceFormat(shortNonce));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateNonceFormat(invalidBase64));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateNonceFormat(null));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateNonceFormat(""));
    }
    
    @Test
    void testSanitizeInput() {
        String cleanInput = "clean text";
        String dirtyInput = "<script>alert('xss')</script>";
        String quotedInput = "text with \"quotes\" and 'apostrophes'";
        
        assertEquals("clean text", securityConfig.sanitizeInput(cleanInput));
        assertEquals("scriptalert(xss)/script", securityConfig.sanitizeInput(dirtyInput));
        assertEquals("text with quotes and apostrophes", securityConfig.sanitizeInput(quotedInput));
        assertNull(securityConfig.sanitizeInput(null));
    }
    
    @Test
    void testValidateDIDFormat() {
        String validDID = "did:web:example.com";
        String validDIDWithPath = "did:web:example.com:user:123";
        String invalidPrefix = "notadid:web:example.com";
        String noMethod = "did:";
        String noIdentifier = "did:web:";
        String invalidMethod = "did:web-invalid:example.com";
        
        assertDoesNotThrow(() -> securityConfig.validateDIDFormat(validDID));
        assertDoesNotThrow(() -> securityConfig.validateDIDFormat(validDIDWithPath));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateDIDFormat(invalidPrefix));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateDIDFormat(noMethod));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateDIDFormat(noIdentifier));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateDIDFormat(invalidMethod));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateDIDFormat(null));
    }
    
    @Test
    void testValidateWalletEndpoint() {
        String validHTTPS = "https://wallet.example.com";
        String validHTTPSWithPath = "https://wallet.example.com/authorize";
        String invalidHTTP = "http://wallet.example.com";
        String invalidURL = "not-a-url";
        
        assertDoesNotThrow(() -> securityConfig.validateWalletEndpoint(validHTTPS));
        assertDoesNotThrow(() -> securityConfig.validateWalletEndpoint(validHTTPSWithPath));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateWalletEndpoint(invalidHTTP));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateWalletEndpoint(invalidURL));
        
        assertThrows(SecurityException.class, 
            () -> securityConfig.validateWalletEndpoint(null));
    }
    
    @Test
    void testGenerateSessionToken() {
        String token1 = securityConfig.generateSessionToken();
        String token2 = securityConfig.generateSessionToken();
        
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        
        // Verify Base64 URL encoding
        assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(token1));
        
        // Verify length (256 bits = 32 bytes = ~43 Base64 chars)
        assertTrue(token1.length() >= 43);
    }
}