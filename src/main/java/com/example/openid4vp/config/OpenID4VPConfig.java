package com.example.openid4vp.config;

import org.keycloak.models.IdentityProviderModel;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for OpenID4VP Identity Provider
 */
public class OpenID4VPConfig {
    
    private final IdentityProviderModel model;
    
    public OpenID4VPConfig() {
        this.model = null;
    }
    
    public OpenID4VPConfig(IdentityProviderModel model) {
        this.model = model;
    }
    
    public String getWalletEndpoint() {
        return getConfigValue("walletEndpoint");
    }
    
    public List<String> getTrustedIssuers() {
        String issuers = getConfigValue("trustedIssuers");
        return issuers != null ? Arrays.asList(issuers.split(",\\s*")) : Collections.emptyList();
    }
    
    public boolean isRequireAgeVerification() {
        return Boolean.parseBoolean(getConfigValueOrDefault("requireAgeVerification", "true"));
    }
    
    public String getCustomPresentationDefinition() {
        return getConfigValue("presentationDefinition");
    }
    
    public int getMaxVPTokenSize() {
        String size = getConfigValueOrDefault("maxVPTokenSize", "102400");
        try {
            return Integer.parseInt(size);
        } catch (NumberFormatException e) {
            return 102400; // 100KB default
        }
    }
    
    public Duration getValidationTimeout() {
        String timeout = getConfigValueOrDefault("validationTimeout", "30");
        try {
            return Duration.ofSeconds(Long.parseLong(timeout));
        } catch (NumberFormatException e) {
            return Duration.ofSeconds(30); // 30 seconds default
        }
    }
    
    private String getConfigValue(String key) {
        if (model == null) return null;
        Map<String, String> config = model.getConfig();
        return config != null ? config.get(key) : null;
    }
    
    private String getConfigValueOrDefault(String key, String defaultValue) {
        String value = getConfigValue(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get the underlying IdentityProviderModel
     */
    public IdentityProviderModel getModel() {
        return model;
    }
}