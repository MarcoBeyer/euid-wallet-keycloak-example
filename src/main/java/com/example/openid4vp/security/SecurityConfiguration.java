package com.example.openid4vp.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Security configuration and utilities for OpenID4VP
 */
public class SecurityConfiguration {
    
    // Minimum nonce entropy (128 bits per OpenID4VP spec)
    private static final int NONCE_ENTROPY_BITS = 128;
    
    // Maximum VP token size to prevent DoS attacks (100KB)
    private static final int DEFAULT_MAX_VP_TOKEN_SIZE = 100 * 1024;
    
    // VP validation timeout
    private static final Duration DEFAULT_VP_VALIDATION_TIMEOUT = Duration.ofSeconds(30);
    
    // Maximum request processing time window
    private static final Duration MAX_REQUEST_WINDOW = Duration.ofMinutes(10);
    
    private final SecureRandom secureRandom;
    
    public SecurityConfiguration() {
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Generates a cryptographically secure nonce for replay protection
     * 
     * @return Base64-encoded secure nonce
     */
    public String generateSecureNonce() {
        byte[] bytes = new byte[NONCE_ENTROPY_BITS / 8];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * Generates a secure state parameter
     * 
     * @return Base64-encoded secure state
     */
    public String generateSecureState() {
        return generateSecureNonce(); // Same entropy requirements
    }
    
    /**
     * Validates VP token size to prevent DoS attacks
     * 
     * @param vpToken VP token to validate
     * @throws SecurityException if token exceeds maximum size
     */
    public void validateVPTokenSize(String vpToken) throws SecurityException {
        validateVPTokenSize(vpToken, DEFAULT_MAX_VP_TOKEN_SIZE);
    }
    
    /**
     * Validates VP token size with custom limit
     * 
     * @param vpToken VP token to validate
     * @param maxSize Maximum allowed size in bytes
     * @throws SecurityException if token exceeds maximum size
     */
    public void validateVPTokenSize(String vpToken, int maxSize) throws SecurityException {
        if (vpToken == null) {
            throw new SecurityException("VP token cannot be null");
        }
        
        if (vpToken.getBytes().length > maxSize) {
            throw new SecurityException("VP token exceeds maximum allowed size: " + maxSize);
        }
    }
    
    /**
     * Validates request timing to prevent replay attacks
     * 
     * @param requestTime Time when the request was initiated
     * @throws SecurityException if request is too old
     */
    public void validateRequestTiming(Instant requestTime) throws SecurityException {
        if (requestTime == null) {
            throw new SecurityException("Request time cannot be null");
        }
        
        Instant now = Instant.now();
        Duration elapsed = Duration.between(requestTime, now);
        
        // VP requests must be processed within reasonable time window
        if (elapsed.compareTo(MAX_REQUEST_WINDOW) > 0) {
            throw new SecurityException("VP request expired - too much time elapsed since initiation");
        }
        
        // Prevent future timestamps (clock skew tolerance: 1 minute)
        if (elapsed.isNegative() && elapsed.abs().compareTo(Duration.ofMinutes(1)) > 0) {
            throw new SecurityException("VP request timestamp is too far in the future");
        }
    }
    
    /**
     * Validates nonce format and entropy
     * 
     * @param nonce Nonce to validate
     * @throws SecurityException if nonce is invalid
     */
    public void validateNonceFormat(String nonce) throws SecurityException {
        if (nonce == null || nonce.trim().isEmpty()) {
            throw new SecurityException("Nonce cannot be null or empty");
        }
        
        // Validate Base64 format
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(nonce);
            if (decoded.length < NONCE_ENTROPY_BITS / 8) {
                throw new SecurityException("Nonce does not meet minimum entropy requirements");
            }
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Invalid nonce format", e);
        }
    }
    
    /**
     * Sanitizes input to prevent injection attacks
     * 
     * @param input Input string to sanitize
     * @return Sanitized string
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'&]", "")
                   .trim();
    }
    
    /**
     * Validates DID format
     * 
     * @param did DID to validate
     * @throws SecurityException if DID format is invalid
     */
    public void validateDIDFormat(String did) throws SecurityException {
        if (did == null || did.trim().isEmpty()) {
            throw new SecurityException("DID cannot be null or empty");
        }
        
        if (!did.startsWith("did:")) {
            throw new SecurityException("Invalid DID format - must start with 'did:'");
        }
        
        // Basic DID format validation (method:identifier)
        String[] parts = did.split(":");
        if (parts.length < 3) {
            throw new SecurityException("Invalid DID format - insufficient parts");
        }
        
        // Validate method name (alphanumeric)
        if (!parts[1].matches("^[a-zA-Z0-9]+$")) {
            throw new SecurityException("Invalid DID method name");
        }
    }
    
    /**
     * Validates URL format for wallet endpoints
     * 
     * @param url URL to validate
     * @throws SecurityException if URL is invalid or not HTTPS
     */
    public void validateWalletEndpoint(String url) throws SecurityException {
        if (url == null || url.trim().isEmpty()) {
            throw new SecurityException("Wallet endpoint cannot be null or empty");
        }
        
        if (!url.startsWith("https://")) {
            throw new SecurityException("Wallet endpoint must use HTTPS");
        }
        
        try {
            new java.net.URL(url);
        } catch (java.net.MalformedURLException e) {
            throw new SecurityException("Invalid wallet endpoint URL format", e);
        }
    }
    
    /**
     * Creates a secure session token for internal use
     * 
     * @return Secure session token
     */
    public String generateSessionToken() {
        byte[] bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}