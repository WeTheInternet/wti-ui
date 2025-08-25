package net.wti.ui.demo.api;

import xapi.string.X_String;

/// TimeUtils:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/04/2025 @ 23:31
public class TimeUtils {
    public static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };
    private static final int[] CUMULATIVE_DAYS = {
            0,   // Jan
            31,  // Feb
            59,  // Mar
            90,  // Apr
            120, // May
            151, // Jun
            181, // Jul
            212, // Aug
            243, // Sep
            273, // Oct
            304, // Nov
            334  // Dec
    };

    private static final int[] DAYS_IN_MONTH = {
            31, 28, 31, 30, 31, 30,
            31, 31, 30, 31, 30, 31
    };

    /// Converts a day-of-year into a label like "April 10th"
    public static String formatDayOfYear(int dayOfYear) {
        int month = 0;
        while (month < 12 && dayOfYear >= DAYS_IN_MONTH[month]) {
            dayOfYear -= DAYS_IN_MONTH[month++];
        }
        String suffix = X_String.numberWithOrdinal(dayOfYear + 1);
        return MONTHS[month] + " " + suffix;
    }

    /// Convert day-of-year (0-365) to "April 10th" format.
    /// Ignores Feb 29th, for now.
    public static String dateOfDayOfYear(int dayOfYear) {
        for (int i = 11; i >= 0; i--) {
            if (dayOfYear > CUMULATIVE_DAYS[i]) {
                int dayOfMonth = dayOfYear - CUMULATIVE_DAYS[i];
                return MONTHS[i] + " the " + X_String.numberWithOrdinal(dayOfMonth);
            }
        }
        return "Invalid";
    }

}

