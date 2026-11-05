package net.wti.quest.api;

import java.math.BigDecimal;

/// Immutable eligibility calculation only. No award, balance, claim or transaction is recorded.
/// A null policy leaves multipliers and milestone identities absent (needs configuration).
/// Pending current-day completion retains the streak through yesterday but has no eligibility ID.
public final class DailyRoutineRewardPreview {
    private final boolean configured, currentDayComplete, weeklyBonusOnCompletion;
    private final long consecutiveDays, consecutiveDaysOnCompletion;
    private final BigDecimal multiplierOnCompletion, weeklyBonus;
    private final String dailyMilestoneId, weeklyMilestoneId, rewardUnit;

    /// Captures the evaluator's result without performing an award.
    public DailyRoutineRewardPreview(final boolean configured, final boolean currentDayComplete,
                                     final long consecutiveDays, final long consecutiveDaysOnCompletion,
                                     final BigDecimal multiplierOnCompletion, final boolean weeklyBonusOnCompletion,
                                     final BigDecimal weeklyBonus, final String rewardUnit,
                                     final String dailyMilestoneId, final String weeklyMilestoneId) {
        this.configured = configured;
        this.currentDayComplete = currentDayComplete;
        this.consecutiveDays = consecutiveDays;
        this.consecutiveDaysOnCompletion = consecutiveDaysOnCompletion;
        this.multiplierOnCompletion = multiplierOnCompletion;
        this.weeklyBonusOnCompletion = weeklyBonusOnCompletion;
        this.weeklyBonus = weeklyBonus;
        this.rewardUnit = rewardUnit;
        this.dailyMilestoneId = dailyMilestoneId;
        this.weeklyMilestoneId = weeklyMilestoneId;
    }

    /// Whether an explicit valid policy was supplied.
    public boolean isConfigured() { return configured; }
    /// Whether current-day completion has acknowledged evidence.
    public boolean isCurrentDayComplete() { return currentDayComplete; }
    /// Completed streak, including yesterday while today remains pending.
    public long getConsecutiveDays() { return consecutiveDays; }
    /// Streak if today's routine is completed; unchanged when already complete.
    public long getConsecutiveDaysOnCompletion() { return consecutiveDaysOnCompletion; }
    /// Configured multiplier for completion; null when configuration is absent.
    public BigDecimal getMultiplierOnCompletion() { return multiplierOnCompletion; }
    /// Whether today's completion would close a seven-day block.
    public boolean isWeeklyBonusOnCompletion() { return weeklyBonusOnCompletion; }
    /// Preview amount for that block, otherwise null; may still depend on pending completion.
    public BigDecimal getWeeklyBonus() { return weeklyBonus; }
    /// Explicit bonus unit, null when configuration is absent.
    public String getRewardUnit() { return rewardUnit; }
    /// Eligible daily milestone identity, null while pending or unconfigured.
    public String getDailyMilestoneId() { return dailyMilestoneId; }
    /// Eligible weekly milestone identity, null while pending/unconfigured or outside a boundary.
    public String getWeeklyMilestoneId() { return weeklyMilestoneId; }
    /// Eligibility only; does not mean the milestone is unclaimed or awarded.
    public boolean isDailyEligible() { return dailyMilestoneId != null; }
    /// Eligibility only; does not mean the milestone is unclaimed or awarded.
    public boolean isWeeklyEligible() { return weeklyMilestoneId != null; }
}
