package net.wti.ui.form;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.ui.api.FieldType;
import net.wti.ui.gdx.theme.GdxTheme;
import xapi.fu.In1;
import xapi.fu.Out1;
import xapi.fu.Pointer;

/// WtiFormFieldInteger:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/03/2025 @ 08:06
public class WtiFormFieldInteger extends WtiFormField<Integer> {

    public WtiFormFieldInteger(final GdxTheme theme, final Out1<Integer> getter, final In1<Integer> setter) {
        super(theme, FieldType.integer, getter, setter);
    }

    public WtiFormFieldInteger(final GdxTheme theme) {
        super(theme, FieldType.integer);
    }

    @Override
    protected In1<Integer> initSetter(final Pointer<Integer> value) {
        final Slider slider = new Slider(0, 100, 1, false, theme.getSkin());
        add(slider);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(final ChangeEvent event, final Actor actor) {
                final int val = (int) slider.getValue();
                if (validate(val)) {
                    value.in(val);
                }
            }
        });
        return val -> {
            if (validate(val)) {
                value.in(val);
                slider.setValue(val);
            }
        };
    }
}
