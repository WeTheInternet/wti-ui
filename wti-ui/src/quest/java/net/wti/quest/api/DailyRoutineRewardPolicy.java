package net.wti.quest.api;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/// Explicit opt-in reward settings. There are no household defaults or payout operations.
/// policyKey must identify an immutable policy version; changed rules require a new key.
/// The supported strategies are available choices, not a product decision about combo/week meaning.
public final class DailyRoutineRewardPolicy {
    /// Available combo meaning; within-day item combos are not implemented.
    public enum ComboBasis { CONSECUTIVE_COMPLETED_DAYS }
    /// Available week meaning; calendar-week rewards are not implemented.
    public enum WeekConvention { ROLLING_SEVEN_DAY_BLOCKS }

    private final String policyKey, rewardUnit;
    private final ComboBasis comboBasis;
    private final WeekConvention weekConvention;
    private final Map<Integer, BigDecimal> comboMultipliers;
    private final BigDecimal weeklyBonus;

    /// Multipliers apply at supplied inclusive streak thresholds, including an explicit day-one tier.
    /// Weekly bonus is a separate configured amount; it is not multiplied by the daily combo.
    public DailyRoutineRewardPolicy(final String policyKey, final ComboBasis comboBasis,
                                    final WeekConvention weekConvention, final Map<Integer, BigDecimal> comboMultipliers,
                                    final String rewardUnit, final BigDecimal weeklyBonus) {
        this.policyKey = policyKey;
        this.comboBasis = comboBasis;
        this.weekConvention = weekConvention;
        this.comboMultipliers = Collections.unmodifiableMap(new TreeMap<>(comboMultipliers));
        this.rewardUnit = rewardUnit;
        this.weeklyBonus = weeklyBonus;
    }

    /// Immutable policy version identity used in milestone IDs.
    public String getPolicyKey() { return policyKey; }
    /// Explicitly selected combo meaning.
    public ComboBasis getComboBasis() { return comboBasis; }
    /// Explicitly selected week meaning.
    public WeekConvention getWeekConvention() { return weekConvention; }
    /// Immutable ascending threshold-to-multiplier map.
    public Map<Integer, BigDecimal> getComboMultipliers() { return comboMultipliers; }
    /// Caller-defined unit of the weekly bonus; no wallet is implied.
    public String getRewardUnit() { return rewardUnit; }
    /// Configured separate bonus amount for each completed week block.
    public BigDecimal getWeeklyBonus() { return weeklyBonus; }
}
