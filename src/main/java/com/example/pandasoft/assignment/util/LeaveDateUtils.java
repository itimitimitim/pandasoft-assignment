package com.example.pandasoft.assignment.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public final class LeaveDateUtils {

    private LeaveDateUtils() {
    }

    public static Set<LocalDate> parseHolidays(String holidayConfig) {
        Set<LocalDate> holidays = new HashSet<>();
        if (holidayConfig == null || holidayConfig.isBlank()) {
            return holidays;
        }

        for (String value : holidayConfig.split(",")) {
            holidays.add(LocalDate.parse(value.trim()));
        }
        return holidays;
    }

    public static int workingDays(
            LocalDate start,
            LocalDate end,
            Set<LocalDate> holidays) {
        int days = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            boolean weekday = date.getDayOfWeek() != DayOfWeek.SATURDAY
                    && date.getDayOfWeek() != DayOfWeek.SUNDAY;
            if (weekday && !holidays.contains(date)) {
                days++;
            }
        }
        return days;
    }
}
