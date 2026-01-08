package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDate;

/**
 * Holiday check response
 */
public class HolidayCheckResponse {
    
    private LocalDate valueDate;
    private String calendar;
    private boolean holiday;
    private boolean businessDay;
    private String holidayName;

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public String getCalendar() {
        return calendar;
    }

    public void setCalendar(String calendar) {
        this.calendar = calendar;
    }

    public boolean isHoliday() {
        return holiday;
    }

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }

    public boolean isBusinessDay() {
        return businessDay;
    }

    public void setBusinessDay(boolean businessDay) {
        this.businessDay = businessDay;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }
}

