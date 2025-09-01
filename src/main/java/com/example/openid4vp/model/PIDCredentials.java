package com.example.openid4vp.model;

import java.time.Instant;

/**
 * Data model for PID (Personal Identification Data) credentials
 * extracted from EU Digital Identity Wallet
 */
public class PIDCredentials {
    
    private String familyName;
    private String givenName;
    private String birthDate;
    private Boolean ageOver18;
    private String nationality;
    private String personalAdministrativeNumber;
    private String issuerDID;
    private Instant validUntil;
    
    public PIDCredentials() {}
    
    public PIDCredentials(String familyName, String givenName, String birthDate, 
                         Boolean ageOver18, String nationality, 
                         String personalAdministrativeNumber, String issuerDID, 
                         Instant validUntil) {
        this.familyName = familyName;
        this.givenName = givenName;
        this.birthDate = birthDate;
        this.ageOver18 = ageOver18;
        this.nationality = nationality;
        this.personalAdministrativeNumber = personalAdministrativeNumber;
        this.issuerDID = issuerDID;
        this.validUntil = validUntil;
    }
    
    public String getFamilyName() {
        return familyName;
    }
    
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }
    
    public String getGivenName() {
        return givenName;
    }
    
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }
    
    public String getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
    
    public Boolean getAgeOver18() {
        return ageOver18;
    }
    
    public void setAgeOver18(Boolean ageOver18) {
        this.ageOver18 = ageOver18;
    }
    
    public String getNationality() {
        return nationality;
    }
    
    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
    
    public String getPersonalAdministrativeNumber() {
        return personalAdministrativeNumber;
    }
    
    public void setPersonalAdministrativeNumber(String personalAdministrativeNumber) {
        this.personalAdministrativeNumber = personalAdministrativeNumber;
    }
    
    public String getIssuerDID() {
        return issuerDID;
    }
    
    public void setIssuerDID(String issuerDID) {
        this.issuerDID = issuerDID;
    }
    
    public Instant getValidUntil() {
        return validUntil;
    }
    
    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }
    
    @Override
    public String toString() {
        return "PIDCredentials{" +
                "familyName='" + familyName + '\'' +
                ", givenName='" + givenName + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", ageOver18=" + ageOver18 +
                ", nationality='" + nationality + '\'' +
                ", issuerDID='" + issuerDID + '\'' +
                ", validUntil=" + validUntil +
                '}';
    }
    
    /**
     * Builder pattern for PIDCredentials
     */
    public static class Builder {
        private String familyName;
        private String givenName;
        private String birthDate;
        private Boolean ageOver18;
        private String nationality;
        private String personalAdministrativeNumber;
        private String issuerDID;
        private Instant validUntil;
        
        public Builder familyName(String familyName) {
            this.familyName = familyName;
            return this;
        }
        
        public Builder givenName(String givenName) {
            this.givenName = givenName;
            return this;
        }
        
        public Builder birthDate(String birthDate) {
            this.birthDate = birthDate;
            return this;
        }
        
        public Builder ageOver18(Boolean ageOver18) {
            this.ageOver18 = ageOver18;
            return this;
        }
        
        public Builder nationality(String nationality) {
            this.nationality = nationality;
            return this;
        }
        
        public Builder personalAdministrativeNumber(String personalAdministrativeNumber) {
            this.personalAdministrativeNumber = personalAdministrativeNumber;
            return this;
        }
        
        public Builder issuerDID(String issuerDID) {
            this.issuerDID = issuerDID;
            return this;
        }
        
        public Builder validUntil(Instant validUntil) {
            this.validUntil = validUntil;
            return this;
        }
        
        public PIDCredentials build() {
            return new PIDCredentials(familyName, givenName, birthDate, ageOver18, 
                                    nationality, personalAdministrativeNumber, 
                                    issuerDID, validUntil);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}