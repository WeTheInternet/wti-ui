package net.wti.time.impl

import net.wti.time.api.DayIndex
import net.wti.time.api.DurationUnit
import net.wti.time.api.ModelDuration
import spock.lang.Specification
import spock.lang.Unroll
import xapi.model.X_Model

/// ModelDurationUtilSpec
///
/// Tests for ModelDurationUtil.add():
/// - DAY and WEEK increments
/// - Negative deltas
/// - times==0 short‑circuit
/// - Validation of null duration/amount/unit
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:59
class ModelDurationUtilSpec extends Specification {

    private ModelDuration newDuration(int amount, DurationUnit unit) {
        ModelDuration dur = X_Model.create(ModelDuration)
        dur.setAmount(amount)
        dur.setUnit(unit)
        return dur
    }

    @Unroll
    def "add DAY duration: base=#baseNum, amount=#amount, times=#times -> #expected"() {
        given:
        DayIndex base = DayIndex.of(baseNum)
        ModelDuration dur = newDuration(amount, DurationUnit.DAY)

        when:
        DayIndex result = ModelDurationUtil.add(base, dur, times)

        then:
        result.dayNum == expected

        where:
        baseNum | amount | times || expected
        0       | 1      | 1     || 1
        0       | 2      | 3     || 6
        5       | 1      | -2    || 3
        -10     | 3      | 2     || -4
    }

    @Unroll
    def "add WEEK duration: base=#baseNum, amount=#amount, times=#times -> #expected"() {
        given:
        DayIndex base = DayIndex.of(baseNum)
        ModelDuration dur = newDuration(amount, DurationUnit.WEEK)

        when:
        DayIndex result = ModelDurationUtil.add(base, dur, times)

        then:
        result.dayNum == expected

        where:
        baseNum | amount | times || expected
        0       | 1      | 1     || 7
        0       | 2      | 1     || 14
        3       | 1      | -1    || -4
        10      | 3      | 2     || 10 + 3 * 2 * 7
    }

    def "add with times==0 returns base DayIndex"() {
        given:
        DayIndex base = DayIndex.of(42)
        ModelDuration dur = newDuration(5, DurationUnit.DAY)

        when:
        DayIndex result = ModelDurationUtil.add(base, dur, 0)

        then:
        result.is(base)
    }

    def "add convenience method (times=1)"() {
        given:
        DayIndex base = DayIndex.of(10)
        ModelDuration dur = newDuration(3, DurationUnit.DAY)

        when:
        DayIndex result = ModelDurationUtil.add(base, dur)

        then:
        result.dayNum == 13
    }

    def "add rejects null base or duration"() {
        when:
        ModelDurationUtil.add(null, newDuration(1, DurationUnit.DAY), 1)

        then:
        thrown(IllegalArgumentException)

        when:
        ModelDurationUtil.add(DayIndex.of(0), null, 1)

        then:
        thrown(IllegalArgumentException)
    }

    def "add rejects null amount or unit"() {
        given:
        DayIndex base = DayIndex.of(0)
        ModelDuration dur = X_Model.create(ModelDuration)

        when: "unit missing"
        dur.setAmount(1)
        dur.setUnit(null)
        ModelDurationUtil.add(base, dur, 1)

        then:
        thrown(IllegalArgumentException)

        when: "amount missing"
        dur = X_Model.create(ModelDuration)
        dur.setAmount(null)
        dur.setUnit(DurationUnit.DAY)
        ModelDurationUtil.add(base, dur, 1)

        then:
        thrown(IllegalArgumentException)
    }

    def "MONTH and YEAR units are currently unsupported"() {
        given:
        DayIndex base = DayIndex.of(0)
        ModelDuration monthly = newDuration(1, DurationUnit.MONTH)
        ModelDuration yearly = newDuration(1, DurationUnit.YEAR)

        when:
        ModelDurationUtil.add(base, monthly, 1)

        then:
        thrown(UnsupportedOperationException)

        when:
        ModelDurationUtil.add(base, yearly, 1)

        then:
        thrown(UnsupportedOperationException)
    }
}
