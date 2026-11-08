package net.wti.quest.impl

import net.wti.quest.api.DailyRoutineHistory
import net.wti.quest.api.DailyRoutineHistory.DaySummary
import net.wti.quest.api.DailyRoutineRewardPolicy
import spock.lang.Specification
import spock.lang.Unroll

import static net.wti.quest.api.DailyRoutineRewardPolicy.ComboBasis.CONSECUTIVE_COMPLETED_DAYS
import static net.wti.quest.api.DailyRoutineRewardPolicy.WeekConvention.ROLLING_SEVEN_DAY_BLOCKS

class DailyRoutineRewardEvaluatorSpec extends Specification {
    private final DailyRoutineRewardEvaluator evaluator = new DailyRoutineRewardEvaluator()

    // Deliberately test-only values; production selects no reward settings.
    private static DailyRoutineRewardPolicy policy(Map<Integer, BigDecimal> tiers = [1: 1.25G, 3: 2.5G],
                                                  BigDecimal bonus = 17.0G, String id = 'policy-v1') {
        new DailyRoutineRewardPolicy(id, CONSECUTIVE_COMPLETED_DAYS, ROLLING_SEVEN_DAY_BLOCKS, tiers, 'test-units', bonus)
    }

    private static DaySummary day(int number, boolean complete = true, String child = 'child-a', String routine = 'routine-a',
                                  String evidence = "ack:$number") {
        new DaySummary(child, routine, number, complete, evidence)
    }

    private static DailyRoutineHistory history(int current, List<DaySummary> days, int first = 0,
                                               String child = 'child-a', String routine = 'routine-a') {
        new DailyRoutineHistory(child, routine, first, current, days)
    }

    def 'missing configuration keeps reward eligibility disabled without an identity multiplier'() {
        when:
        def result = evaluator.preview(history(6, (0..6).collect { day(it) }), null)

        then:
        result.currentDayComplete
        result.consecutiveDays == 7
        !result.configured
        !result.dailyEligible
        !result.weeklyEligible
        result.multiplierOnCompletion == null
        result.weeklyBonus == null
        result.rewardUnit == null
        result.dailyMilestoneId == null
        result.weeklyMilestoneId == null
    }

    def 'fresh pending today preserves yesterday streak until the next boundary'() {
        given:
        def completed = (0..5).collect { day(it) }

        when:
        def today = evaluator.preview(history(6, completed), policy())
        def incompleteToday = evaluator.preview(history(6, completed + day(6, false)), policy())
        def tomorrow = evaluator.preview(history(7, completed), policy())

        then:
        today.consecutiveDays == 6
        today.consecutiveDaysOnCompletion == 7
        today.weeklyBonusOnCompletion
        today.weeklyBonus == 17.0G
        today.multiplierOnCompletion == 2.5G
        !today.currentDayComplete
        !today.dailyEligible
        !today.weeklyEligible
        incompleteToday.consecutiveDays == 6
        !incompleteToday.dailyEligible
        tomorrow.consecutiveDays == 0
        tomorrow.consecutiveDaysOnCompletion == 1
        !tomorrow.weeklyBonusOnCompletion
        tomorrow.multiplierOnCompletion == 1.25G
    }

    @Unroll
    def 'closed incomplete or missing day breaks previous streak (explicit=#explicit)'() {
        given:
        def days = (0..2).collect { day(it) } + (explicit ? [day(3, false)] : []) + [day(4), day(5)]

        when:
        def result = evaluator.preview(history(5, days), policy())

        then:
        result.consecutiveDays == 2
        result.dailyEligible
        !result.weeklyEligible
        result.multiplierOnCompletion == 1.25G

        where:
        explicit << [false, true]
    }

    @Unroll
    def 'explicit rolling seven-day blocks at streak length #count'() {
        when:
        def result = evaluator.preview(history(count - 1, (0..<count).collect { day(it) }), policy())

        then:
        result.consecutiveDays == count
        result.consecutiveDaysOnCompletion == count
        result.dailyEligible
        result.weeklyEligible == eligible
        result.weeklyBonusOnCompletion == eligible
        result.weeklyBonus == (eligible ? 17.0G : null)

        where:
        count | eligible
        1     | false
        6     | false
        7     | true
        8     | false
        13    | false
        14    | true
    }

    def 'duplicates and input order cannot increase streak or change milestone identity'() {
        given:
        def days = (0..6).collect { day(it) }

        when:
        def original = evaluator.preview(history(6, days), policy())
        def repeated = evaluator.preview(history(6, days.reverse() + [day(2), day(6, true, 'child-a', 'routine-a', 'retry-ack')]), policy())

        then:
        repeated.consecutiveDays == 7
        repeated.dailyMilestoneId == original.dailyMilestoneId
        repeated.weeklyMilestoneId == original.weeklyMilestoneId
    }

    def 'child routine policy and milestone boundaries remain distinct'() {
        given:
        def base = evaluator.preview(history(6, (0..6).collect { day(it) }), policy())
        def child = evaluator.preview(history(6, (0..6).collect { day(it, true, 'child-b') }, 0, 'child-b'), policy())
        def routine = evaluator.preview(history(6, (0..6).collect { day(it, true, 'child-a', 'routine-b') }, 0, 'child-a', 'routine-b'), policy())
        def version = evaluator.preview(history(6, (0..6).collect { day(it) }), policy([1: 9.0G], 21.0G, 'policy-v2'))
        def nextBlock = evaluator.preview(history(13, (0..13).collect { day(it) }), policy())

        expect:
        [base, child, routine, version, nextBlock].collect { it.weeklyMilestoneId }.toSet().size() == 5
        version.multiplierOnCompletion == 9.0G
        version.weeklyBonus == 21.0G
        evaluator.preview(history(6, [], 0, 'child-b'), policy()).consecutiveDays == 0
    }

    def 'length-prefixed milestone identity resists ambiguous delimiters'() {
        expect:
        evaluator.preview(history(0, [day(0, true, 'a:b', 'c')], 0, 'a:b', 'c'), policy()).dailyMilestoneId !=
                evaluator.preview(history(0, [day(0, true, 'a', 'b:c')], 0, 'a', 'b:c'), policy()).dailyMilestoneId
    }

    @Unroll
    def 'reject invalid or contradictory acknowledged history: #label'() {
        when:
        evaluator.preview(input, policy())

        then:
        thrown(IllegalArgumentException)

        where:
        label                 | input
        'missing history'     | null
        'missing child'       | history(0, [], 0, ' ')
        'missing routine'     | history(0, [], 0, 'child-a', '')
        'reversed window'     | history(0, [], 1)
        'null day'            | history(0, [null])
        'unacknowledged'      | history(0, [day(0, true, 'child-a', 'routine-a', ' ')])
        'wrong child'         | history(0, [day(0, true, 'child-b')])
        'wrong routine'       | history(0, [day(0, true, 'child-a', 'routine-b')])
        'future day'          | history(0, [day(1)])
        'before enrollment'   | history(0, [day(-1)])
        'conflicting result'  | history(0, [day(0), day(0, false)])
        'reused evidence'     | history(1, [day(0), day(1, true, 'child-a', 'routine-a', 'ack:0')])
    }

    @Unroll
    def 'reject incomplete reward configuration #label'() {
        when:
        evaluator.preview(history(0, []), input)

        then:
        thrown(IllegalArgumentException)

        where:
        label                | input
        'no initial tier'    | policy([3: 2.0G])
        'empty tiers'        | policy([:])
        'zero threshold'     | policy([0: 1.0G, 1: 2.0G])
        'negative factor'    | policy([1: -1.0G])
        'null factor'        | policy([1: null])
        'negative bonus'     | policy([1: 1.0G], -1.0G)
        'missing bonus'      | policy([1: 1.0G], null)
        'unversioned policy' | policy([1: 1.0G], 1.0G, ' ')
        'missing combo'      | new DailyRoutineRewardPolicy('p', null, ROLLING_SEVEN_DAY_BLOCKS, [1: 1.0G], 'u', 1.0G)
        'missing week'       | new DailyRoutineRewardPolicy('p', CONSECUTIVE_COMPLETED_DAYS, null, [1: 1.0G], 'u', 1.0G)
        'missing unit'       | new DailyRoutineRewardPolicy('p', CONSECUTIVE_COMPLETED_DAYS, ROLLING_SEVEN_DAY_BLOCKS, [1: 1.0G], '', 1.0G)
    }

    def 'negative enrollment days and minimum integer boundary do not wrap'() {
        expect:
        evaluator.preview(history(-1, (-7..-1).collect { day(it) }, -7), policy()).weeklyEligible
        evaluator.preview(history(Integer.MIN_VALUE, [], Integer.MIN_VALUE), policy()).consecutiveDays == 0
        evaluator.preview(history(Integer.MIN_VALUE, [day(Integer.MIN_VALUE)], Integer.MIN_VALUE), policy()).consecutiveDays == 1
    }

    def 'input snapshots are immutable and preserve explicit zero-valued policy'() {
        given:
        def days = [day(0)]
        def tiers = [1: 0.0G]
        def input = history(0, days)
        def settings = policy(tiers, 0.0G)
        days.clear()
        tiers[1] = 42.0G

        when:
        def result = evaluator.preview(input, settings)

        then:
        result.consecutiveDays == 1
        result.multiplierOnCompletion == 0.0G
        input.days.size() == 1
        settings.comboMultipliers == [1: 0.0G]
    }
}
