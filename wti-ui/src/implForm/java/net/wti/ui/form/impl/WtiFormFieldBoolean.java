package net.wti.ui.form.impl;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.ui.form.api.FieldType;
import net.wti.ui.gdx.theme.GdxTheme;
import xapi.fu.In1;
import xapi.fu.Out1;
import xapi.fu.Pointer;

/// A generic, form-like boolean field backed by a libGDX {@link CheckBox}.
///
/// The checkbox uses the consumer skin's standard checkbox style. A consuming
/// game can provide a game-specific default style through its own skin or wrap
/// this field without moving game-specific assets into wti-ui.
public class WtiFormFieldBoolean extends WtiFormField<Boolean> {

    private CheckBox checkBox;
    private boolean synchronizing;

    public WtiFormFieldBoolean(final GdxTheme theme, final Out1<Boolean> getter, final In1<Boolean> setter) {
        super(theme, FieldType.checkbox, getter, setter);
    }

    public WtiFormFieldBoolean(final GdxTheme theme) {
        super(theme, FieldType.checkbox);
    }

    @Override
    protected In1<Boolean> initSetter(final Pointer<Boolean> value) {
        final CheckBox box = createCheckBox();
        checkBox = box;
        add(box);
        if (value.out1() == null) {
            value.in(false);
        }
        box.setChecked(Boolean.TRUE.equals(value.out1()));
        box.addListener(new ChangeListener() {
            @Override
            public void changed(final ChangeEvent event, final Actor actor) {
                if (synchronizing) {
                    return;
                }
                final boolean checked = box.isChecked();
                if (validate(checked)) {
                    value.in(checked);
                } else {
                    setChecked(box, !checked);
                }
            }
        });
        return newValue -> {
            final boolean checked = Boolean.TRUE.equals(newValue);
            if (!validate(checked)) {
                return;
            }
            setChecked(box, checked);
            value.in(checked);
        };
    }

    /// Return the underlying generic Scene2D checkbox for accessibility or layout customization.
    public CheckBox getCheckBox() {
        return checkBox;
    }

    /// Create the checkbox using the consumer skin's standard checkbox style.
    protected CheckBox createCheckBox() {
        return new CheckBox("", theme.getSkin());
    }

    private void setChecked(final CheckBox box, final boolean checked) {
        synchronizing = true;
        try {
            box.setChecked(checked);
        } finally {
            synchronizing = false;
        }
    }
}
