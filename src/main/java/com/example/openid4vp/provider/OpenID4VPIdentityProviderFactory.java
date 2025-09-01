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
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(
            createConfigProperty("walletEndpoint", "Wallet Endpoint", 
                "OpenID4VP endpoint for wallet integration", ProviderConfigProperty.STRING_TYPE, true),
            createConfigProperty("trustedIssuers", "Trusted PID Issuers", 
                "Comma-separated list of trusted PID issuer DIDs", ProviderConfigProperty.STRING_TYPE, true),
            createConfigProperty("requireAgeVerification", "Require Age Verification", 
                "Require age_over_18 claim in PID", ProviderConfigProperty.BOOLEAN_TYPE, false, "true"),
            createConfigProperty("presentationDefinition", "Custom Presentation Definition", 
                "Custom JSON presentation definition (optional)", ProviderConfigProperty.TEXT_TYPE, false),
            createConfigProperty("maxVPTokenSize", "Maximum VP Token Size", 
                "Maximum size in bytes for VP tokens (default: 102400)", ProviderConfigProperty.STRING_TYPE, false, "102400"),
            createConfigProperty("validationTimeout", "Validation Timeout", 
                "VP validation timeout in seconds (default: 30)", ProviderConfigProperty.STRING_TYPE, false, "30")
        );
    }
    
    private ProviderConfigProperty createConfigProperty(String name, String label, String helpText, 
                                                       String type, boolean required) {
        return createConfigProperty(name, label, helpText, type, required, null);
    }
    
    private ProviderConfigProperty createConfigProperty(String name, String label, String helpText, 
                                                       String type, boolean required, String defaultValue) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name);
        property.setLabel(label);
        property.setHelpText(helpText);
        property.setType(type);
        property.setRequired(required);
        if (defaultValue != null) {
            property.setDefaultValue(defaultValue);
        }
        return property;
    }
}