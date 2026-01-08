package com.mulesoft.connectors.swift.internal.model;

/**
 * BIC lookup response
 */
public class BicLookupResponse {
    
    private String bicCode;
    private boolean valid;
    private String institutionName;
    private String branchInformation;
    private String countryCode;
    private String city;
    private boolean active;

    public String getBicCode() {
        return bicCode;
    }

    public void setBicCode(String bicCode) {
        this.bicCode = bicCode;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getBranchInformation() {
        return branchInformation;
    }

    public void setBranchInformation(String branchInformation) {
        this.branchInformation = branchInformation;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

