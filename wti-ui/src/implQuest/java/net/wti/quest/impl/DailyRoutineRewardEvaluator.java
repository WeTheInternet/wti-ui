package net.wti.quest.impl;

import net.wti.quest.api.DailyRoutineHistory;
import net.wti.quest.api.DailyRoutineHistory.DaySummary;
import net.wti.quest.api.DailyRoutineRewardPolicy;
import net.wti.quest.api.DailyRoutineRewardPreview;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/// Pure configurable routine eligibility, independent of stores, UI, chores and claw payouts.
/// The named week convention produces nonoverlapping seven-day blocks within each streak.
/// A future ledger must validate authoritative history and atomically claim milestone IDs;
/// this evaluator neither awards anything nor provides cross-device transaction safety.
public final class DailyRoutineRewardEvaluator {

    /// Validates acknowledged summary consistency and calculates current/after-completion previews.
    /// A null policy disables reward eligibility while retaining the completion/streak summary.
    public DailyRoutineRewardPreview preview(final DailyRoutineHistory history,
                                             final DailyRoutineRewardPolicy policy) {
        final Map<Integer, Boolean> days = validatedDays(history);
        final boolean complete = Boolean.TRUE.equals(days.get(history.getCurrentDay()));
        long cursor = (long) history.getCurrentDay() - (complete ? 0 : 1);
        long consecutive = 0;
        while (cursor >= history.getFirstDay() && Boolean.TRUE.equals(days.get((int) cursor))) {
            consecutive++;
            cursor--;
        }
        final long afterCompletion = consecutive + (complete ? 0 : 1);
        if (policy == null) {
            return new DailyRoutineRewardPreview(false, complete, consecutive, afterCompletion,
                    null, false, null, null, null, null);
        }
        validatePolicy(policy);
        BigDecimal multiplier = null;
        for (final Map.Entry<Integer, BigDecimal> tier : policy.getComboMultipliers().entrySet()) {
            if (tier.getKey() <= afterCompletion) multiplier = tier.getValue();
        }
        final boolean weeklyOnCompletion = afterCompletion % 7 == 0;
        final String dailyId = complete ? identity("daily", history, policy, history.getCurrentDay()) : null;
        final String weeklyId = complete && weeklyOnCompletion
                ? identity("rolling-seven-day-block", history, policy, (long) history.getCurrentDay() - 6) : null;
        return new DailyRoutineRewardPreview(true, complete, consecutive, afterCompletion,
                multiplier, weeklyOnCompletion, weeklyOnCompletion ? policy.getWeeklyBonus() : null,
                policy.getRewardUnit(), dailyId, weeklyId);
    }

    /// Missing closed days break a streak. Duplicate evidence for the same result is harmless;
    /// conflicting summaries or evidence reused for another day require upstream reconciliation.
    private static Map<Integer, Boolean> validatedDays(final DailyRoutineHistory history) {
        if (history == null) throw new IllegalArgumentException("history is required");
        requireKey(history.getChildKey(), "childKey");
        requireKey(history.getRoutineKey(), "routineKey");
        if (history.getFirstDay() > history.getCurrentDay()) {
            throw new IllegalArgumentException("firstDay must not follow currentDay");
        }
        final Map<Integer, Boolean> days = new HashMap<>();
        final Map<String, Integer> evidence = new HashMap<>();
        for (final DaySummary day : history.getDays()) {
            if (day == null) throw new IllegalArgumentException("Null day summary");
            if (!history.getChildKey().equals(day.getChildKey()) || !history.getRoutineKey().equals(day.getRoutineKey())) {
                throw new IllegalArgumentException("Summary belongs to a different child or routine");
            }
            requireKey(day.getAcknowledgementKey(), "acknowledgementKey");
            if (day.getDay() < history.getFirstDay() || day.getDay() > history.getCurrentDay()) {
                throw new IllegalArgumentException("Summary lies outside the declared history window");
            }
            final Boolean prior = days.put(day.getDay(), day.isComplete());
            if (prior != null && prior != day.isComplete()) {
                throw new IllegalArgumentException("Conflicting completion summaries for one day");
            }
            final Integer priorDay = evidence.put(day.getAcknowledgementKey(), day.getDay());
            if (priorDay != null && priorDay != day.getDay()) {
                throw new IllegalArgumentException("Acknowledgement reused for another day");
            }
        }
        return days;
    }

    private static void validatePolicy(final DailyRoutineRewardPolicy policy) {
        requireKey(policy.getPolicyKey(), "policyKey");
        requireKey(policy.getRewardUnit(), "rewardUnit");
        if (policy.getComboBasis() != DailyRoutineRewardPolicy.ComboBasis.CONSECUTIVE_COMPLETED_DAYS
                || policy.getWeekConvention() != DailyRoutineRewardPolicy.WeekConvention.ROLLING_SEVEN_DAY_BLOCKS) {
            throw new IllegalArgumentException("Explicit supported combo basis and week convention required");
        }
        if (!policy.getComboMultipliers().containsKey(1)) {
            throw new IllegalArgumentException("Explicit day-one multiplier required");
        }
        for (final Map.Entry<Integer, BigDecimal> tier : policy.getComboMultipliers().entrySet()) {
            if (tier.getKey() < 1 || tier.getValue() == null || tier.getValue().signum() < 0) {
                throw new IllegalArgumentException("Combo thresholds must be positive and multipliers nonnegative");
            }
        }
        if (policy.getWeeklyBonus() == null || policy.getWeeklyBonus().signum() < 0) {
            throw new IllegalArgumentException("Explicit nonnegative weekly bonus required");
        }
    }

    /// Length-prefixed identities preserve arbitrary owner keys without delimiter collisions.
    /// A milestone identifies a policy version, child, routine and exact day/block boundary.
    private static String identity(final String kind, final DailyRoutineHistory history,
                                   final DailyRoutineRewardPolicy policy, final long firstDay) {
        return "routine-reward-v1|" + part(kind) + part(policy.getPolicyKey())
                + part(history.getChildKey()) + part(history.getRoutineKey())
                + firstDay + ":" + history.getCurrentDay();
    }

    private static String part(final String value) { return value.length() + ":" + value; }

    private static void requireKey(final String value, final String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
    }
}
