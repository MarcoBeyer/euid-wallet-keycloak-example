package com.example.openid4vp.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PIDCredentialsTest {
    
    @Test
    void testBuilderPattern() {
        Instant validUntil = Instant.now().plusSeconds(3600);
        
        PIDCredentials credentials = PIDCredentials.builder()
            .familyName("Doe")
            .givenName("John")
            .birthDate("1990-01-01")
            .ageOver18(true)
            .nationality("US")
            .personalAdministrativeNumber("123456789")
            .issuerDID("did:web:issuer.gov")
            .validUntil(validUntil)
            .build();
        
        assertEquals("Doe", credentials.getFamilyName());
        assertEquals("John", credentials.getGivenName());
        assertEquals("1990-01-01", credentials.getBirthDate());
        assertTrue(credentials.getAgeOver18());
        assertEquals("US", credentials.getNationality());
        assertEquals("123456789", credentials.getPersonalAdministrativeNumber());
        assertEquals("did:web:issuer.gov", credentials.getIssuerDID());
        assertEquals(validUntil, credentials.getValidUntil());
    }
    
    @Test
    void testConstructorAndGettersSetters() {
        Instant validUntil = Instant.now().plusSeconds(3600);
        
        PIDCredentials credentials = new PIDCredentials(
            "Smith", "Jane", "1985-05-15", true, "DE", 
            "DEU987654321", "did:web:germany.gov", validUntil
        );
        
        assertEquals("Smith", credentials.getFamilyName());
        assertEquals("Jane", credentials.getGivenName());
        assertEquals("1985-05-15", credentials.getBirthDate());
        assertTrue(credentials.getAgeOver18());
        assertEquals("DE", credentials.getNationality());
        assertEquals("DEU987654321", credentials.getPersonalAdministrativeNumber());
        assertEquals("did:web:germany.gov", credentials.getIssuerDID());
        assertEquals(validUntil, credentials.getValidUntil());
    }
    
    @Test
    void testDefaultConstructor() {
        PIDCredentials credentials = new PIDCredentials();
        
        assertNull(credentials.getFamilyName());
        assertNull(credentials.getGivenName());
        assertNull(credentials.getBirthDate());
        assertNull(credentials.getAgeOver18());
        assertNull(credentials.getNationality());
        assertNull(credentials.getPersonalAdministrativeNumber());
        assertNull(credentials.getIssuerDID());
        assertNull(credentials.getValidUntil());
    }
    
    @Test
    void testSetters() {
        PIDCredentials credentials = new PIDCredentials();
        Instant validUntil = Instant.now();
        
        credentials.setFamilyName("Johnson");
        credentials.setGivenName("Alice");
        credentials.setBirthDate("1992-12-25");
        credentials.setAgeOver18(true);
        credentials.setNationality("FR");
        credentials.setPersonalAdministrativeNumber("FR123456789");
        credentials.setIssuerDID("did:web:france.gov");
        credentials.setValidUntil(validUntil);
        
        assertEquals("Johnson", credentials.getFamilyName());
        assertEquals("Alice", credentials.getGivenName());
        assertEquals("1992-12-25", credentials.getBirthDate());
        assertTrue(credentials.getAgeOver18());
        assertEquals("FR", credentials.getNationality());
        assertEquals("FR123456789", credentials.getPersonalAdministrativeNumber());
        assertEquals("did:web:france.gov", credentials.getIssuerDID());
        assertEquals(validUntil, credentials.getValidUntil());
    }
    
    @Test
    void testToString() {
        PIDCredentials credentials = PIDCredentials.builder()
            .familyName("Test")
            .givenName("User")
            .birthDate("2000-01-01")
            .ageOver18(true)
            .nationality("IT")
            .issuerDID("did:web:italy.gov")
            .build();
        
        String toString = credentials.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("Test"));
        assertTrue(toString.contains("User"));
        assertTrue(toString.contains("2000-01-01"));
        assertTrue(toString.contains("true"));
        assertTrue(toString.contains("IT"));
        assertTrue(toString.contains("did:web:italy.gov"));
    }
    
    @Test
    void testBuilderWithNullValues() {
        PIDCredentials credentials = PIDCredentials.builder()
            .familyName("Test")
            .givenName("User")
            // Other fields left null
            .build();
        
        assertEquals("Test", credentials.getFamilyName());
        assertEquals("User", credentials.getGivenName());
        assertNull(credentials.getBirthDate());
        assertNull(credentials.getAgeOver18());
        assertNull(credentials.getNationality());
        assertNull(credentials.getPersonalAdministrativeNumber());
        assertNull(credentials.getIssuerDID());
        assertNull(credentials.getValidUntil());
    }
    
    @Test
    void testBuilderImmutability() {
        PIDCredentials.Builder builder = PIDCredentials.builder()
            .familyName("Original")
            .givenName("Name");
        
        PIDCredentials first = builder.build();
        
        // Modify builder after first build
        builder.familyName("Modified");
        PIDCredentials second = builder.build();
        
        // First instance should not be affected
        assertEquals("Original", first.getFamilyName());
        assertEquals("Modified", second.getFamilyName());
    }
}