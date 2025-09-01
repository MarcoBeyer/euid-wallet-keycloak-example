package com.example.openid4vp.provider;

import com.example.openid4vp.config.OpenID4VPConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Factory for OpenID4VP Identity Provider
 */
public class OpenID4VPIdentityProviderFactory 
    extends AbstractIdentityProviderFactory<OpenID4VPIdentityProvider> {
    
    public static final String PROVIDER_ID = "openid4vp";
    
    @Override
    public String getName() {
        return "OpenID for Verifiable Presentations";
    }
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public OpenID4VPIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OpenID4VPIdentityProvider(session, new OpenID4VPConfig(model));
    }
    
    @Override
    public IdentityProviderModel createConfig() {
        IdentityProviderModel model = new IdentityProviderModel();
        model.setProviderId(PROVIDER_ID);
        return model;
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(
            createConfigProperty("walletEndpoint", "Wallet Endpoint", 
                "OpenID4VP endpoint for wallet integration", ProviderConfigProperty.STRING_TYPE),
            createConfigProperty("trustedIssuers", "Trusted PID Issuers", 
                "Comma-separated list of trusted PID issuer DIDs", ProviderConfigProperty.STRING_TYPE),
            createConfigProperty("requireAgeVerification", "Require Age Verification", 
                "Require age_over_18 claim in PID", ProviderConfigProperty.BOOLEAN_TYPE),
            createConfigProperty("presentationDefinition", "Custom Presentation Definition", 
                "Custom JSON presentation definition (optional)", ProviderConfigProperty.TEXT_TYPE),
            createConfigProperty("maxVPTokenSize", "Maximum VP Token Size", 
                "Maximum size in bytes for VP tokens (default: 102400)", ProviderConfigProperty.STRING_TYPE),
            createConfigProperty("validationTimeout", "Validation Timeout", 
                "VP validation timeout in seconds (default: 30)", ProviderConfigProperty.STRING_TYPE)
        );
    }
    
    private ProviderConfigProperty createConfigProperty(String name, String label, String helpText, String type) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name);
        property.setLabel(label);
        property.setHelpText(helpText);
        property.setType(type);
        return property;
    }
}