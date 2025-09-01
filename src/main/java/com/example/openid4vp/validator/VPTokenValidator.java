package com.example.openid4vp.validator;

import com.example.openid4vp.config.OpenID4VPConfig;
import com.example.openid4vp.model.PIDCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * VP Token Validator for OpenID4VP
 * Handles verification of Verifiable Presentations and extraction of PID credentials
 */
public class VPTokenValidator {
    
    private static final Logger logger = Logger.getLogger(VPTokenValidator.class);
    
    private final OpenID4VPConfig config;
    private final ObjectMapper objectMapper;
    
    public VPTokenValidator(OpenID4VPConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Validates VP token and extracts PID credentials
     * 
     * @param vpToken VP token from wallet
     * @param expectedNonce Expected nonce for replay protection
     * @return Extracted PID credentials
     * @throws ValidationException if validation fails
     */
    public PIDCredentials validateVPTokenAndExtractPID(String vpToken, String expectedNonce) 
            throws ValidationException {
        try {
            logger.infof("Starting VP token validation");
            
            // Parse VP token (SD-JWT or JSON-LD format)
            VerifiablePresentation vp = parseVPToken(vpToken);
            
            // Validate VP structure and cryptographic integrity
            validateVPStructure(vp);
            validateVPSignature(vp);
            
            // Validate nonce binding for replay protection
            validateNonceBinding(vp, expectedNonce);
            
            // Extract and validate PID credential
            VerifiableCredential pidCredential = extractPIDCredential(vp);
            validatePIDCredential(pidCredential);
            
            // Extract PID attributes
            PIDCredentials pidCredentials = extractPIDAttributes(pidCredential);
            
            logger.infof("Successfully validated VP token and extracted PID credentials");
            return pidCredentials;
            
        } catch (Exception e) {
            logger.error("VP token validation failed", e);
            throw new ValidationException("Invalid VP token", e);
        }
    }
    
    private VerifiablePresentation parseVPToken(String token) throws ValidationException {
        try {
            if (token.contains(".")) {
                // SD-JWT format
                return parseSDJWTVP(token);
            } else {
                // JSON-LD format  
                return parseJSONLDVP(token);
            }
        } catch (Exception e) {
            throw new ValidationException("Failed to parse VP token", e);
        }
    }
    
    private VerifiablePresentation parseSDJWTVP(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        
        // Convert JWT claims to VP structure
        VerifiablePresentation vp = new VerifiablePresentation();
        vp.setId(claims.getSubject());
        
        // Handle type claim
        List<String> typeList = claims.getStringListClaim("type");
        if (typeList != null) {
            vp.setType(typeList.toArray(new String[0]));
        }
        
        // Extract verifiable credentials from vp claim
        Object vpClaim = claims.getClaim("vp");
        if (vpClaim instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> vpMap = (Map<String, Object>) vpClaim;
            if (vpMap.containsKey("verifiableCredential")) {
                // Handle verifiable credentials
                Object vcClaim = vpMap.get("verifiableCredential");
                if (vcClaim instanceof String) {
                    VerifiableCredential vc = parseCredentialFromJWT((String) vcClaim);
                    vp.addVerifiableCredential(vc);
                }
            }
        }
        
        // Create proof structure
        Proof proof = new Proof();
        proof.setType("JwtProof2020");
        proof.setChallenge(claims.getStringClaim("nonce"));
        proof.setVerificationMethod(signedJWT.getHeader().getKeyID());
        vp.setProof(proof);
        
        return vp;
    }
    
    private VerifiablePresentation parseJSONLDVP(String token) throws Exception {
        JsonNode vpNode = objectMapper.readTree(token);
        VerifiablePresentation vp = new VerifiablePresentation();
        
        // Parse basic VP structure
        if (vpNode.has("id")) {
            vp.setId(vpNode.get("id").asText());
        }
        
        if (vpNode.has("type")) {
            JsonNode typeNode = vpNode.get("type");
            if (typeNode.isArray()) {
                vp.setType(objectMapper.convertValue(typeNode, String[].class));
            }
        }
        
        // Parse verifiable credentials
        if (vpNode.has("verifiableCredential")) {
            JsonNode vcArray = vpNode.get("verifiableCredential");
            if (vcArray.isArray()) {
                for (JsonNode vcNode : vcArray) {
                    VerifiableCredential vc = parseCredentialFromJSON(vcNode);
                    vp.addVerifiableCredential(vc);
                }
            }
        }
        
        // Parse proof
        if (vpNode.has("proof")) {
            JsonNode proofNode = vpNode.get("proof");
            Proof proof = new Proof();
            if (proofNode.has("type")) {
                proof.setType(proofNode.get("type").asText());
            }
            if (proofNode.has("challenge")) {
                proof.setChallenge(proofNode.get("challenge").asText());
            }
            if (proofNode.has("verificationMethod")) {
                proof.setVerificationMethod(proofNode.get("verificationMethod").asText());
            }
            vp.setProof(proof);
        }
        
        return vp;
    }
    
    private VerifiableCredential parseCredentialFromJWT(String jwtToken) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(jwtToken);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        
        VerifiableCredential vc = new VerifiableCredential();
        vc.setId(claims.getSubject());
        vc.setIssuer(new Issuer(claims.getIssuer()));
        
        // Extract credential subject
        Object vcClaim = claims.getClaim("vc");
        if (vcClaim instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> vcMap = (Map<String, Object>) vcClaim;
            if (vcMap.containsKey("credentialSubject")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subjectMap = (Map<String, Object>) vcMap.get("credentialSubject");
                CredentialSubject subject = new CredentialSubject();
                subject.setClaims(subjectMap);
                vc.setCredentialSubject(subject);
            }
        }
        
        // Set validity periods
        if (claims.getNotBeforeTime() != null) {
            vc.setValidFrom(claims.getNotBeforeTime().toInstant());
        }
        if (claims.getExpirationTime() != null) {
            vc.setValidUntil(claims.getExpirationTime().toInstant());
        }
        
        return vc;
    }
    
    private VerifiableCredential parseCredentialFromJSON(JsonNode vcNode) throws Exception {
        VerifiableCredential vc = new VerifiableCredential();
        
        if (vcNode.has("id")) {
            vc.setId(vcNode.get("id").asText());
        }
        
        if (vcNode.has("issuer")) {
            JsonNode issuerNode = vcNode.get("issuer");
            if (issuerNode.isTextual()) {
                vc.setIssuer(new Issuer(issuerNode.asText()));
            } else if (issuerNode.isObject() && issuerNode.has("id")) {
                vc.setIssuer(new Issuer(issuerNode.get("id").asText()));
            }
        }
        
        if (vcNode.has("credentialSubject")) {
            JsonNode subjectNode = vcNode.get("credentialSubject");
            CredentialSubject subject = new CredentialSubject();
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.convertValue(subjectNode, Map.class);
            subject.setClaims(claims);
            vc.setCredentialSubject(subject);
        }
        
        return vc;
    }
    
    private void validateVPStructure(VerifiablePresentation vp) throws ValidationException {
        if (vp.getType() == null || vp.getType().length == 0) {
            throw new ValidationException("Invalid VP type");
        }
        
        boolean hasVPType = false;
        for (String type : vp.getType()) {
            if ("VerifiablePresentation".equals(type)) {
                hasVPType = true;
                break;
            }
        }
        
        if (!hasVPType) {
            throw new ValidationException("VP must have VerifiablePresentation type");
        }
        
        if (vp.getVerifiableCredentials() == null || vp.getVerifiableCredentials().isEmpty()) {
            throw new ValidationException("No verifiable credentials in VP");
        }
        
        if (vp.getProof() == null) {
            throw new ValidationException("VP missing cryptographic proof");
        }
    }
    
    private void validateVPSignature(VerifiablePresentation vp) throws ValidationException {
        // Simplified signature validation - in production, this would involve
        // full DID resolution and cryptographic verification
        logger.infof("Validating VP signature for verification method: %s", 
                     vp.getProof().getVerificationMethod());
        
        // For this example, we'll do basic validation
        if (vp.getProof().getVerificationMethod() == null) {
            throw new ValidationException("VP proof missing verification method");
        }
        
        // In production, implement full cryptographic verification here
        logger.infof("VP signature validation passed (simplified)");
    }
    
    private void validateNonceBinding(VerifiablePresentation vp, String expectedNonce) 
            throws ValidationException {
        if (expectedNonce == null) {
            logger.warn("No expected nonce provided for validation");
            return;
        }
        
        String vpNonce = vp.getProof().getChallenge();
        if (!expectedNonce.equals(vpNonce)) {
            throw new ValidationException("VP not bound to expected nonce");
        }
    }
    
    private VerifiableCredential extractPIDCredential(VerifiablePresentation vp) 
            throws ValidationException {
        return vp.getVerifiableCredentials().stream()
            .filter(this::isPIDCredential)
            .findFirst()
            .orElseThrow(() -> new ValidationException("No PID credential found in VP"));
    }
    
    private boolean isPIDCredential(VerifiableCredential vc) {
        // Check for PID credential type indicators
        if (vc.getType() != null) {
            for (String type : vc.getType()) {
                if (type.contains("PID") || type.contains("PersonalIdentification")) {
                    return true;
                }
            }
        }
        
        // Check credential subject for PID indicators
        if (vc.getCredentialSubject() != null && vc.getCredentialSubject().getClaims() != null) {
            Map<String, Object> claims = vc.getCredentialSubject().getClaims();
            return claims.containsKey("family_name") && claims.containsKey("given_name");
        }
        
        return false;
    }
    
    private void validatePIDCredential(VerifiableCredential pidCredential) throws ValidationException {
        // Validate issuer against trusted list
        String issuerDID = pidCredential.getIssuer().getId();
        if (!config.getTrustedIssuers().contains(issuerDID)) {
            throw new ValidationException("PID issuer not trusted: " + issuerDID);
        }
        
        // Validate temporal constraints
        Instant now = Instant.now();
        if (pidCredential.getValidFrom() != null && now.isBefore(pidCredential.getValidFrom())) {
            throw new ValidationException("PID credential not yet valid");
        }
        
        if (pidCredential.getValidUntil() != null && now.isAfter(pidCredential.getValidUntil())) {
            throw new ValidationException("PID credential expired");
        }
        
        // Validate required PID claims
        Map<String, Object> claims = pidCredential.getCredentialSubject().getClaims();
        if (!claims.containsKey("family_name") || !claims.containsKey("given_name")) {
            throw new ValidationException("Missing required PID claims");
        }
        
        // Validate age verification if required
        if (config.isRequireAgeVerification() && !claims.containsKey("age_over_18")) {
            throw new ValidationException("Age verification required but not present");
        }
    }
    
    private PIDCredentials extractPIDAttributes(VerifiableCredential pidCredential) {
        Map<String, Object> claims = pidCredential.getCredentialSubject().getClaims();
        
        return PIDCredentials.builder()
            .familyName(extractStringClaim(claims, "family_name"))
            .givenName(extractStringClaim(claims, "given_name"))
            .birthDate(extractStringClaim(claims, "birthdate"))
            .ageOver18(extractBooleanClaim(claims, "age_over_18"))
            .nationality(extractStringClaim(claims, "nationality"))
            .personalAdministrativeNumber(extractStringClaim(claims, "personal_administrative_number"))
            .issuerDID(pidCredential.getIssuer().getId())
            .validUntil(pidCredential.getValidUntil())
            .build();
    }
    
    private String extractStringClaim(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        return value != null ? value.toString() : null;
    }
    
    private Boolean extractBooleanClaim(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    // Helper classes for VP/VC structure
    public static class VerifiablePresentation {
        private String id;
        private String[] type;
        private java.util.List<VerifiableCredential> verifiableCredentials = new java.util.ArrayList<>();
        private Proof proof;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String[] getType() { return type; }
        public void setType(String[] type) { this.type = type; }
        
        public java.util.List<VerifiableCredential> getVerifiableCredentials() { 
            return verifiableCredentials; 
        }
        public void addVerifiableCredential(VerifiableCredential vc) { 
            this.verifiableCredentials.add(vc); 
        }
        
        public Proof getProof() { return proof; }
        public void setProof(Proof proof) { this.proof = proof; }
    }
    
    public static class VerifiableCredential {
        private String id;
        private String[] type;
        private Issuer issuer;
        private CredentialSubject credentialSubject;
        private Instant validFrom;
        private Instant validUntil;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String[] getType() { return type; }
        public void setType(String[] type) { this.type = type; }
        
        public Issuer getIssuer() { return issuer; }
        public void setIssuer(Issuer issuer) { this.issuer = issuer; }
        
        public CredentialSubject getCredentialSubject() { return credentialSubject; }
        public void setCredentialSubject(CredentialSubject credentialSubject) { 
            this.credentialSubject = credentialSubject; 
        }
        
        public Instant getValidFrom() { return validFrom; }
        public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
        
        public Instant getValidUntil() { return validUntil; }
        public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    }
    
    public static class Issuer {
        private String id;
        
        public Issuer(String id) { this.id = id; }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
    
    public static class CredentialSubject {
        private Map<String, Object> claims;
        
        public Map<String, Object> getClaims() { return claims; }
        public void setClaims(Map<String, Object> claims) { this.claims = claims; }
    }
    
    public static class Proof {
        private String type;
        private String challenge;
        private String verificationMethod;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getChallenge() { return challenge; }
        public void setChallenge(String challenge) { this.challenge = challenge; }
        
        public String getVerificationMethod() { return verificationMethod; }
        public void setVerificationMethod(String verificationMethod) { 
            this.verificationMethod = verificationMethod; 
        }
    }
    
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
        
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}