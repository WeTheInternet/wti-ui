package net.wti.ui.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.api.IsDeadlineView;
import xapi.time.X_Time;

import java.util.concurrent.TimeUnit;

import static xapi.time.X_Time.isPast;

/// DeadlineView:
///
/// A self-updating {@link Label} that displays time remaining until a task's deadline. Automatically changes color
/// based on urgency and redraws at a latency interval appropriate to the distance from the deadline.
///
/// Implements {@link IsDeadlineView<Actor>} to expose deadline state accessors.
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:10 CST
public class DeadlineView extends Label implements IsDeadlineView<Actor> {

    private static final long ONE_SECOND = 1_000;
    private static final long ONE_MINUTE = 60_000;
    private static final long ONE_HOUR = 3_600_000;
    private static final long ONE_DAY = 24 * ONE_HOUR;
    private static final long TEN_SECONDS = 10_000;
    private long latency = ONE_MINUTE;

    private float lastDraw;

    private Double deadline;
    private final Color overdueColor;
    private final Color urgentColor;
    private final Color soonColor;
    private final Color laterColor;
    private final Integer alarmMinutes;

    public DeadlineView(final Double deadline, final Skin skin, final Integer alarmMinutes) {
        super(printDeadline(deadline), skin);
        this.overdueColor = skin.getColor("font-overdue");
        this.urgentColor = skin.getColor("font-urgent");
        this.soonColor = skin.getColor("font-soon");
        this.laterColor = skin.getColor("font-later");
        this.alarmMinutes = alarmMinutes;
        if (deadline != null) {
            setDeadline(deadline);
        }
    }

    @Override
    public boolean isPastDeadline() {
        return hasDeadline() && isPast(deadline);
    }

    @Override
    public boolean isNearDeadline() {
        return alarmMinutes != null && hasDeadline() && isPast(deadline - TimeUnit.MINUTES.toMillis(alarmMinutes));
    }

    @Override
    public boolean hasDeadline() {
        return deadline != null;
    }

    @Override
    public void setDeadline(double deadline) {
        this.deadline = deadline;
        if (deadline > ONE_MINUTE) {
            if (deadline > ONE_HOUR) {
                latency = ONE_MINUTE;
            } else {
                latency = TEN_SECONDS;
            }
        } else {
            latency = ONE_SECOND;
        }
    }

    public static String printDeadline(Double deadline) {
        if (deadline == null) {
            return "";
        }
        String suffix = "";
        if (isPast(deadline)) {
            suffix = "!";
        }
        return X_Time.print(deadline - X_Time.nowMillis()) + suffix;
    }

    @Override
    public void draw(final Batch batch, final float parentAlpha) {
        if (hasDeadline()) {
            // only recompute lazily.
            if (isPast(lastDraw + latency)) {

                if (isPast(deadline)) {
                    setColor(getOverdueColor());
                } else if (isLater(deadline)) {
                    setColor(getLaterColor());
                } else if (isUrgent(deadline)) {
                    setColor(getUrgentColor());
                } else {
                    setColor(getSoonColor());
                }

                setText(printDeadline(deadline));
                lastDraw = (float)X_Time.nowMillis();
            }
        }
        super.draw(batch, parentAlpha);
    }

    protected boolean isUrgent(final Double deadline) {
        return isPast(deadline - ONE_HOUR);
    }

    protected boolean isSoon(final Double deadline) {
        return isPast(deadline - ONE_DAY);
    }

    protected boolean isLater(final Double deadline) {
        return !isPast(deadline - ONE_DAY);
    }

    public Color getOverdueColor() {
        return overdueColor;
    }

    public Color getLaterColor() {
        return laterColor;
    }

    public Color getSoonColor() {
        return soonColor;
    }

    public Color getUrgentColor() {
        return urgentColor;
    }

    @Override
    public Double getDeadline() {
        return deadline;
    }

    @Override
    public Integer getAlarmMinutes() {
        return alarmMinutes;
    }

    @Override
    public Actor uiSpecific() {
        return this;
    }
}
