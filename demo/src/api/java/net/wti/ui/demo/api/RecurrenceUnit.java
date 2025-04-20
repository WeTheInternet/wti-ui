package net.wti.ui.demo.api;

/// RecurrenceUnit:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 21:05
public enum RecurrenceUnit {
    /// A ONCE recurrence means this task only happens once. It may or may not have a deadline
    ONCE,
    /// A DAILY task happens at the same time every day
    DAILY,
    /// A WEEKLY task happens at the same time one particular day of the week
    /// You will add mode recurrences for each day of the week a particular task may be active
    WEEKLY,
    /// A BIWEEKLY task is similar to WEEKLY, but allows schedule units of "every second week".
    /// If you do something every Thursday and every second Saturday, you add one WEEKLY, one BIWEEKLY.
    BIWEEKLY,
    /// A TRIWEEKLY task is like BIWEEKLY, but allows scheduling items that happen every third week.
    TRIWEEKLY,
    /// A MONTHLY task is something that happens on a particular day of the month (up to 31).
    /// For months with fewer than 31 days, all tasks past the last day of the month are due that month.
    MONTHLY,
    /// A YEARLY task is calculated in seconds since midnight January 1 (allowing you to pick a day of the year).
    /// Selecting new years eve on the year of a leap year will result in a task due on the previous, 365th day of the year.
    YEARLY;
}
