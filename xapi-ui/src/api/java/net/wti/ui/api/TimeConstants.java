package net.wti.ui.api;

/// # TimeConstants
///
/// Shared constants representing useful units of time in milliseconds.
/// Used throughout the app in scheduling, time formatting, delay logic,
/// recurrence math, and human-readable time string generation.
///
/// All values are multiples of milliseconds, suitable for direct arithmetic.
///
/// ### ✅ Included Constants
///
/// | Constant           | Value (ms) | Description                             |
/// |--------------------|------------|-----------------------------------------|
/// | [#ONE_MILLI]       | 1          | A single millisecond                    |
/// | [#ONE_SECOND]      | 1_000      | One second                              |
/// | [#TEN_SECONDS]     | 10_000     | Ten seconds                             |
/// | [#THIRTY_SECONDS]  | 30_000     | Thirty seconds                          |
/// | [#ONE_MINUTE]      | 60_000     | One minute                              |
/// | [#FIVE_MINUTES]    | 300_000    | Five minutes                            |
/// | [#TEN_MINUTES]     | 600_000    | Ten minutes                             |
/// | [#FIFTEEN_MINUTES] | 900_000    | Fifteen minutes                         |
/// | [#THIRTY_MINUTES]  | 1_800_000  | Thirty minutes                          |
/// | [#ONE_HOUR]        | 3_600_000  | One hour                                |
/// | [#TWO_HOURS]       | 7_200_000  | Two hours                               |
/// | [#SIX_HOURS]       | 21_600_000 | Six hours                               |
/// | [#TWELVE_HOURS]    | 43_200_000 | Half a day                              |
/// | [#ONE_DAY]         | 86_400_000 | One day                                 |
/// | [#TWO_DAYS]        | 172_800_000| Two days                                |
/// | [#SEVEN_DAYS]      | 604_800_000| One week                                |
/// | [#FOURTEEN_DAYS]   | 1_209_600_000| Two weeks                             |
/// | [#TWENTY_ONE_DAYS] | 1_814_400_000| Three weeks                           |
/// | [#ONE_MONTH]       | 2_592_000_000L | Approximated as 30 days             |
/// | [#ONE_YEAR]        | 31_536_000_000L| 365 days                            |
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 20/04/2025 @ 23:57 CST
public interface TimeConstants {

    // -- Atomic unit --

    /// ONE_MILLI = 1
    /// One millisecond. Smallest time unit.
    long ONE_MILLI = 1;

    // -- Seconds --

    /// ONE_SECOND = 1_000
    /// One second = 1,000 ms
    long ONE_SECOND = 1_000;

    /// TEN_SECONDS = 10_000
    /// Ten seconds = 10 * 1_000
    long TEN_SECONDS = 10_000;

    /// THIRTY_SECONDS = 30_000
    /// Thirty seconds = 30 * 1_000
    long THIRTY_SECONDS = 30_000;

    // -- Minutes --

    /// ONE_MINUTE = 60_000
    /// One minute = 60 seconds = 60 * 1_000 ms
    long ONE_MINUTE = 60_000;

    /// FIVE_MINUTES = 300_000
    /// Five minutes = 5 * 60 * 1_000 ms
    long FIVE_MINUTES = 300_000;

    /// TEN_MINUTES = 600_000
    /// Ten minutes = 10 * 60 * 1_000 ms
    long TEN_MINUTES = 600_000;

    /// FIFTEEN_MINUTES = 900_000
    /// Fifteen minutes = 15 * 60 * 1_000 ms
    long FIFTEEN_MINUTES = 900_000;

    /// THIRTY_MINUTES = 1_800_000
    /// Thirty minutes = 30 * 60 * 1_000 ms
    long THIRTY_MINUTES = 1_800_000;

    // -- Hours --

    /// ONE_HOUR = 3_600_000
    /// One hour = 60 minutes = 60 * 60 * 1_000 ms
    long ONE_HOUR = 3_600_000;

    /// TWO_HOURS = 7_200_000
    /// Two hours = 2 * 60 * 60 * 1_000 ms
    long TWO_HOURS = 7_200_000;

    /// SIX_HOURS = 21_600_000
    /// Six hours = 6 * 60 * 60 * 1_000 ms
    long SIX_HOURS = 21_600_000;

    /// TWELVE_HOURS = 43_200_000
    /// Twelve hours = 12 * 60 * 60 * 1_000 ms
    long TWELVE_HOURS = 43_200_000;

    // -- Days --

    /// ONE_DAY = 86_400_000
    /// One day = 24 * 60 * 60 * 1_000 ms
    long ONE_DAY = 86_400_000;

    /// TWO_DAYS = 172_800_000
    /// Two days = 2 * 24 * 60 * 60 * 1_000 ms
    long TWO_DAYS = 172_800_000;

    /// SEVEN_DAYS = 604_800_000
    /// Seven days = 7 * 24 * 60 * 60 * 1_000 ms = One week
    long SEVEN_DAYS = 604_800_000;

    /// FOURTEEN_DAYS = 1_209_600_000
    /// Fourteen days = 2 weeks = 14 * 24 * 60 * 60 * 1_000 ms
    long FOURTEEN_DAYS = 1_209_600_000;

    /// TWENTY_ONE_DAYS = 1_814_400_000
    /// Three weeks = 21 * 24 * 60 * 60 * 1_000 ms
    long TWENTY_ONE_DAYS = 1_814_400_000;

    // -- Long-form approximations --

    /// ONE_MONTH = 2_592_000_000
    /// Approximate month = 30 * 24 * 60 * 60 * 1_000 ms
    long ONE_MONTH = 2_592_000_000L;

    /// ONE_YEAR = 31_536_000_000
    /// Non-leap year = 365 * 24 * 60 * 60 * 1_000 ms
    long ONE_YEAR = 31_536_000_000L;
}