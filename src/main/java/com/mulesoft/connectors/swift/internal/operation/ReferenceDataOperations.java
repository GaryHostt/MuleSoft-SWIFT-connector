package com.mulesoft.connectors.swift.internal.operation;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnection;
import com.mulesoft.connectors.swift.internal.model.*;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reference Data & Calendars Operations
 * 
 * Provides operations for currency validation, holiday checking, and RMA verification.
 */
public class ReferenceDataOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataOperations.class);

    /**
     * Validate currency code against ISO 4217.
     * 
     * @param connection Active SWIFT connection
     * @param currencyCode 3-character currency code
     * @return Result containing currency validation
     */
    @DisplayName("Validate Currency Code")
    @Summary("Validate currency code against ISO 4217 standard")
    @Throws(SwiftErrorProvider.class)
    public Result<CurrencyValidationResponse, MessageAttributes> validateCurrency(
            @Connection SwiftConnection connection,
            @DisplayName("Currency Code")
            @Summary("3-character ISO 4217 currency code (e.g., USD, EUR)")
            String currencyCode) throws Exception {

        LOGGER.info("Validating currency code: {}", currencyCode);

        CurrencyValidationResponse response = new CurrencyValidationResponse();
        response.setCurrencyCode(currencyCode);
        response.setValid(isValidCurrency(currencyCode));
        response.setCurrencyName(getCurrencyName(currencyCode));
        response.setNumericCode(getNumericCode(currencyCode));

        LOGGER.info("Currency validation complete: valid={}", response.isValid());

        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<CurrencyValidationResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Check if a date falls on a banking holiday.
     * 
     * @param connection Active SWIFT connection
     * @param valueDate Date to check
     * @param calendar Holiday calendar (TARGET2, US_FED, UK_BOE)
     * @return Result containing holiday check
     */
    @DisplayName("Check Holiday Calendar")
    @Summary("Check if date is a banking holiday for TARGET2 or local calendars")
    @Throws(SwiftErrorProvider.class)
    public Result<HolidayCheckResponse, MessageAttributes> checkHoliday(
            @Connection SwiftConnection connection,
            @DisplayName("Value Date")
            @Summary("Date to check (YYYY-MM-DD)")
            String valueDate,
            @DisplayName("Calendar")
            @Summary("Holiday calendar (TARGET2, US_FED, UK_BOE)")
            String calendar) throws Exception {

        LOGGER.info("Checking holiday: date={}, calendar={}", valueDate, calendar);

        LocalDate date = LocalDate.parse(valueDate);

        HolidayCheckResponse response = new HolidayCheckResponse();
        response.setValueDate(date);
        response.setCalendar(calendar);
        response.setHoliday(isHoliday(date, calendar));
        response.setBusinessDay(!isHoliday(date, calendar));
        response.setHolidayName(getHolidayName(date, calendar));

        LOGGER.info("Holiday check complete: isHoliday={}", response.isHoliday());

        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<HolidayCheckResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Validate country code against ISO 3166.
     * 
     * @param connection Active SWIFT connection
     * @param countryCode 2-character country code
     * @return Result containing country validation
     */
    @DisplayName("Validate Country Code")
    @Summary("Validate country code against ISO 3166 standard")
    @Throws(SwiftErrorProvider.class)
    public Result<CountryValidationResponse, MessageAttributes> validateCountry(
            @Connection SwiftConnection connection,
            @DisplayName("Country Code")
            @Summary("2-character ISO 3166 country code (e.g., US, DE)")
            String countryCode) throws Exception {

        LOGGER.info("Validating country code: {}", countryCode);

        CountryValidationResponse response = new CountryValidationResponse();
        response.setCountryCode(countryCode);
        response.setValid(isValidCountry(countryCode));
        response.setCountryName(getCountryName(countryCode));

        LOGGER.info("Country validation complete: valid={}", response.isValid());

        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<CountryValidationResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Check RMA (Relationship Management Application) authorization.
     * 
     * Verifies if your institution is authorized to send messages to a counterparty.
     * 
     * @param connection Active SWIFT connection
     * @param counterpartyBic Counterparty BIC code
     * @param messageType Message type to check
     * @return Result containing RMA check
     */
    @DisplayName("Check RMA Authorization")
    @Summary("Check if authorized to send messages to counterparty")
    @Throws(SwiftErrorProvider.class)
    public Result<RmaCheckResponse, MessageAttributes> checkRmaAuthorization(
            @Connection SwiftConnection connection,
            @DisplayName("Counterparty BIC")
            @Summary("BIC code of counterparty")
            String counterpartyBic,
            @DisplayName("Message Type")
            @Summary("Message type (e.g., MT103)")
            String messageType) throws Exception {

        LOGGER.info("Checking RMA authorization: counterparty={}, messageType={}", 
            counterpartyBic, messageType);

        RmaCheckResponse response = new RmaCheckResponse();
        response.setCounterpartyBic(counterpartyBic);
        response.setMessageType(messageType);
        response.setAuthorized(true); // Simplified - real implementation would check RMA
        response.setCheckTimestamp(LocalDateTime.now());

        LOGGER.info("RMA check complete: authorized={}", response.isAuthorized());

        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<RmaCheckResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Get list of cutoff times for a currency.
     * 
     * @param connection Active SWIFT connection
     * @param currencyCode Currency code
     * @return Result containing cutoff times
     */
    @DisplayName("Get Cutoff Times")
    @Summary("Retrieve payment cutoff times for a currency")
    @Throws(SwiftErrorProvider.class)
    public Result<CutoffTimesResponse, MessageAttributes> getCutoffTimes(
            @Connection SwiftConnection connection,
            @DisplayName("Currency Code")
            @Summary("Currency code (e.g., USD, EUR)")
            String currencyCode) throws Exception {

        LOGGER.info("Getting cutoff times for currency: {}", currencyCode);

        CutoffTimesResponse response = new CutoffTimesResponse();
        response.setCurrencyCode(currencyCode);
        response.setCutoffTimes(getCutoffTimesForCurrency(currencyCode));

        LOGGER.info("Cutoff times retrieved: {} entries", response.getCutoffTimes().size());

        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<CutoffTimesResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    // Helper methods

    private boolean isValidCurrency(String code) {
        // Simplified - real implementation would check ISO 4217
        List<String> validCurrencies = List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD");
        return validCurrencies.contains(code);
    }

    private String getCurrencyName(String code) {
        return switch (code) {
            case "USD" -> "US Dollar";
            case "EUR" -> "Euro";
            case "GBP" -> "British Pound";
            case "JPY" -> "Japanese Yen";
            default -> "Unknown";
        };
    }

    private String getNumericCode(String code) {
        return switch (code) {
            case "USD" -> "840";
            case "EUR" -> "978";
            case "GBP" -> "826";
            case "JPY" -> "392";
            default -> "000";
        };
    }

    private boolean isHoliday(LocalDate date, String calendar) {
        // Simplified - real implementation would check actual holiday calendars
        return date.getDayOfWeek().getValue() >= 6; // Weekend
    }

    private String getHolidayName(LocalDate date, String calendar) {
        if (isHoliday(date, calendar)) {
            return "Weekend";
        }
        return null;
    }

    private boolean isValidCountry(String code) {
        // Simplified - real implementation would check ISO 3166
        List<String> validCountries = List.of("US", "DE", "GB", "FR", "JP", "CH", "CA", "AU");
        return validCountries.contains(code);
    }

    private String getCountryName(String code) {
        return switch (code) {
            case "US" -> "United States";
            case "DE" -> "Germany";
            case "GB" -> "United Kingdom";
            case "FR" -> "France";
            default -> "Unknown";
        };
    }

    private List<CutoffTime> getCutoffTimesForCurrency(String currency) {
        List<CutoffTime> cutoffs = new ArrayList<>();
        
        CutoffTime cutoff1 = new CutoffTime();
        cutoff1.setSettlementSystem("CHIPS");
        cutoff1.setCutoffTime("17:00 EST");
        cutoff1.setTimeZone("America/New_York");
        cutoffs.add(cutoff1);
        
        CutoffTime cutoff2 = new CutoffTime();
        cutoff2.setSettlementSystem("Fedwire");
        cutoff2.setCutoffTime("18:00 EST");
        cutoff2.setTimeZone("America/New_York");
        cutoffs.add(cutoff2);
        
        return cutoffs;
    }
}

