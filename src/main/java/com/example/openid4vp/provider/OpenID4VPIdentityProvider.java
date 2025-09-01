package com.example.openid4vp.provider;

import com.example.openid4vp.config.OpenID4VPConfig;
import com.example.openid4vp.model.PIDCredentials;
import com.example.openid4vp.security.SecurityConfiguration;
import com.example.openid4vp.validator.VPTokenValidator;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.FederatedIdentityModel;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Collections;

/**
 * Simplified OpenID4VP Identity Provider for Keycloak
 * Handles authentication using EU Digital Identity Wallet PID credentials
 */
public class OpenID4VPIdentityProvider extends AbstractIdentityProvider {
    
    private static final Logger logger = Logger.getLogger(OpenID4VPIdentityProvider.class);
    
    private final VPTokenValidator vpTokenValidator;
    private final SecurityConfiguration securityConfig;
    private final OpenID4VPConfig openID4VPConfig;
    
    public OpenID4VPIdentityProvider(KeycloakSession session, OpenID4VPConfig config) {
        super(session, config.getModel());
        this.openID4VPConfig = config;
        this.vpTokenValidator = new VPTokenValidator(config);
        this.securityConfig = new SecurityConfiguration();
    }
    
    @Override
    public Response performLogin(AuthenticationRequest request) {
        try {
            logger.infof("Starting OpenID4VP authentication flow for realm: %s", 
                         request.getRealm().getName());
            
            // Generate presentation definition for PID credentials
            String presentationDefinition = createPIDPresentationDefinition();
            
            // Generate secure nonce for replay protection
            String nonce = securityConfig.generateSecureNonce();
            
            // Build OpenID4VP authorization request
            URI authorizationUri = buildAuthorizationRequest(
                presentationDefinition, 
                request.getRedirectUri(),
                nonce,
                request.getState().getEncoded()
            );
            
            // Store nonce in session for later validation
            session.setAttribute("openid4vp.nonce", nonce);
            session.setAttribute("openid4vp.request_time", Instant.now());
            
            logger.infof("Redirecting to wallet: %s", authorizationUri);
            return Response.seeOther(authorizationUri).build();
            
        } catch (Exception e) {
            logger.error("Failed to initiate OpenID4VP flow", e);
            throw new IdentityBrokerException("OpenID4VP authentication failed", e);
        }
    }
    
    @Override
    public Response callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        try {
            logger.infof("Processing OpenID4VP callback for realm: %s", realm.getName());
            
            // Validate request timing
            Instant requestTime = (Instant) session.getAttribute("openid4vp.request_time");
            if (requestTime != null) {
                securityConfig.validateRequestTiming(requestTime);
            }
            
            // Extract VP token from callback request (simplified)
            String vpToken = extractVPTokenFromCallback();
            
            if (vpToken == null) {
                logger.error("No VP token found in callback request");
                throw new IdentityBrokerException("No VP token in callback");
            }
            
            // Validate VP token size for security
            securityConfig.validateVPTokenSize(vpToken);
            
            // Retrieve stored nonce for validation
            String expectedNonce = (String) session.getAttribute("openid4vp.nonce");
            
            // Create simplified PID credentials (in production, this would use vpTokenValidator)
            PIDCredentials pidCredentials = vpTokenValidator.validateVPTokenAndExtractPID(vpToken, expectedNonce);
            
            // Create brokered identity context
            BrokeredIdentityContext identity = createIdentityContext(pidCredentials);
            
            logger.infof("Successfully processed PID credentials for user: %s", 
                         identity.getUsername());
            
            return callback.authenticated(identity);
            
        } catch (Exception e) {
            logger.error("OpenID4VP callback processing failed", e);
            return callback.error("authentication_failed");
        } finally {
            // Clean up session attributes
            session.removeAttribute("openid4vp.nonce");
            session.removeAttribute("openid4vp.request_time");
        }
    }
    
    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        // OpenID4VP doesn't use traditional tokens - return empty response
        return Response.noContent().build();
    }
    
    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, 
                             BrokeredIdentityContext context) {
        
        logger.infof("Creating new user from PID: %s", context.getUsername());
        
        // Set basic user properties
        user.setEnabled(true);
        user.setEmailVerified(false); // PID doesn't include email by default
        
        // Apply PID attributes
        applyPIDAttributes(user, context);
        
        // Add default roles for PID-verified users
        addPIDVerifiedRoles(realm, user);
        
        // Set user federation metadata
        user.setSingleAttribute("federation_provider", "openid4vp");
        user.setSingleAttribute("pid_verification_date", Instant.now().toString());
        user.setSingleAttribute("verification_level", "high"); // eIDAS high assurance
        
        logger.infof("Created PID-verified user: %s", user.getUsername());
    }
    
    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, 
                                  BrokeredIdentityContext context) {
        
        logger.infof("Updating existing user from PID: %s", context.getUsername());
        
        // Update PID attributes (some may have changed)
        applyPIDAttributes(user, context);
        
        // Update verification timestamp
        user.setSingleAttribute("pid_verification_date", Instant.now().toString());
        user.setSingleAttribute("last_pid_update", Instant.now().toString());
        
        // Ensure user remains enabled and verified
        user.setEnabled(true);
        
        logger.infof("Updated PID-verified user: %s", user.getUsername());
    }
    
    private String createPIDPresentationDefinition() {
        return "{\n" +
               "  \"id\": \"pid-authentication\",\n" +
               "  \"input_descriptors\": [{\n" +
               "    \"id\": \"eu.europa.ec.eudiw.pid.1\",\n" +
               "    \"format\": {\n" +
               "      \"vc+sd-jwt\": {\"sd-jwt_alg_values\": [\"ES256\", \"ES384\", \"EdDSA\"]},\n" +
               "      \"mso_mdoc\": {\"alg\": [\"ES256\", \"ES384\", \"ES512\", \"EdDSA\"]}\n" +
               "    },\n" +
               "    \"name\": \"EU Digital Identity PID\",\n" +
               "    \"purpose\": \"Authentication with PID credentials\",\n" +
               "    \"constraints\": {\n" +
               "      \"fields\": [\n" +
               "        {\"path\": [\"$.family_name\"], \"intent_to_retain\": false},\n" +
               "        {\"path\": [\"$.given_name\"], \"intent_to_retain\": false},\n" +
               "        {\"path\": [\"$.birthdate\"], \"intent_to_retain\": false},\n" +
               "        {\"path\": [\"$.age_over_18\"], \"intent_to_retain\": false}\n" +
               "      ]\n" +
               "    }\n" +
               "  }]\n" +
               "}";
    }
    
    private URI buildAuthorizationRequest(String presentationDef, String redirectUri, 
                                        String nonce, String state) {
        String walletEndpoint = openID4VPConfig.getWalletEndpoint();
        try {
            return new URI(walletEndpoint + "/authorize"
                + "?response_type=vp_token"
                + "&client_id=" + java.net.URLEncoder.encode(redirectUri, "UTF-8")
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8")
                + "&presentation_definition=" + java.net.URLEncoder.encode(presentationDef, "UTF-8")
                + "&nonce=" + java.net.URLEncoder.encode(nonce, "UTF-8")
                + "&state=" + java.net.URLEncoder.encode(state, "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build authorization URI", e);
        }
    }
    
    private String extractVPTokenFromCallback() {
        // Simplified VP token extraction - in production this would parse the actual callback
        return "mock.vp.token"; // Placeholder for demonstration
    }
    
    private PIDCredentials createSimplifiedPIDCredentials(String vpToken) {
        // Simplified credential creation - in production this would parse and validate the VP
        return PIDCredentials.builder()
            .familyName("Doe")
            .givenName("John")
            .birthDate("1990-01-01")
            .ageOver18(true)
            .nationality("DE")
            .issuerDID("did:web:example.gov")
            .validUntil(Instant.now().plusSeconds(3600))
            .build();
    }
    
    private BrokeredIdentityContext createIdentityContext(PIDCredentials pidCredentials) {
        BrokeredIdentityContext context = new BrokeredIdentityContext(
            generateUserId(pidCredentials));
        
        // Set basic identity attributes
        context.setUsername(generateUsername(pidCredentials));
        context.setFirstName(pidCredentials.getGivenName());
        context.setLastName(pidCredentials.getFamilyName());
        
        // Add PID-specific attributes
        context.setUserAttribute("birth_date", pidCredentials.getBirthDate());
        context.setUserAttribute("age_over_18", String.valueOf(pidCredentials.getAgeOver18()));
        context.setUserAttribute("nationality", pidCredentials.getNationality());
        context.setUserAttribute("pid_issuer", pidCredentials.getIssuerDID());
        if (pidCredentials.getValidUntil() != null) {
            context.setUserAttribute("pid_valid_until", pidCredentials.getValidUntil().toString());
        }
        
        if (pidCredentials.getPersonalAdministrativeNumber() != null) {
            context.setUserAttribute("personal_admin_number", 
                                   pidCredentials.getPersonalAdministrativeNumber());
        }
        
        context.setIdpConfig(getConfig());
        context.setIdp(this);
        
        return context;
    }
    
    private String generateUserId(PIDCredentials pidCredentials) {
        // Generate stable user ID based on PID attributes
        String composite = pidCredentials.getFamilyName() + "|" + 
                          pidCredentials.getGivenName() + "|" + 
                          pidCredentials.getBirthDate() + "|" +
                          pidCredentials.getIssuerDID();
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(composite);
    }
    
    private String generateUsername(PIDCredentials pidCredentials) {
        // Generate readable username
        String baseName = pidCredentials.getGivenName().toLowerCase() + "." + 
                         pidCredentials.getFamilyName().toLowerCase();
        return baseName.replaceAll("[^a-z0-9.]", "");
    }
    
    private void applyPIDAttributes(UserModel user, BrokeredIdentityContext context) {
        // Apply core identity attributes
        if (context.getFirstName() != null) {
            user.setFirstName(context.getFirstName());
        }
        if (context.getLastName() != null) {
            user.setLastName(context.getLastName());
        }
        
        // Apply PID-specific attributes
        copyAttributeIfPresent(user, context, "birth_date");
        copyAttributeIfPresent(user, context, "age_over_18");
        copyAttributeIfPresent(user, context, "nationality");
        copyAttributeIfPresent(user, context, "pid_issuer");
        copyAttributeIfPresent(user, context, "pid_valid_until");
        copyAttributeIfPresent(user, context, "personal_admin_number");
    }
    
    private void copyAttributeIfPresent(UserModel user, BrokeredIdentityContext context, 
                                      String attributeName) {
        String value = context.getUserAttribute(attributeName);
        if (value != null) {
            user.setSingleAttribute(attributeName, value);
        }
    }
    
    private void addPIDVerifiedRoles(RealmModel realm, UserModel user) {
        // Add role indicating PID verification
        RoleModel pidVerifiedRole = realm.getRole("pid-verified");
        if (pidVerifiedRole == null) {
            pidVerifiedRole = realm.addRole("pid-verified");
            pidVerifiedRole.setDescription("User verified with EU Digital Identity PID");
        }
        user.grantRole(pidVerifiedRole);
        
        // Add age-based roles if available
        String ageOver18 = user.getFirstAttribute("age_over_18");
        if ("true".equals(ageOver18)) {
            RoleModel adultRole = realm.getRole("adult-verified");
            if (adultRole == null) {
                adultRole = realm.addRole("adult-verified");
                adultRole.setDescription("User verified as adult via PID");
            }
            user.grantRole(adultRole);
        }
    }
}