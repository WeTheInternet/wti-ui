package net.wti.ui.form;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.ui.api.FieldType;
import net.wti.ui.gdx.theme.GdxTheme;
import xapi.fu.In1;
import xapi.fu.Out1;
import xapi.fu.Pointer;

/// WtiFormFieldTextBase:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/03/2025 @ 08:09
public class WtiFormFieldTextBase<T> extends WtiFormField<T> {

    public WtiFormFieldTextBase(final GdxTheme theme, final FieldType type, final Out1<T> getter, final In1<T> setter) {
        super(theme, type, getter, setter);
    }

    public WtiFormFieldTextBase(final GdxTheme theme, final FieldType type) {
        super(theme, type);
    }

    @Override
    protected In1<T> initSetter(final Pointer<T> value) {
        final T val = value.out1();
        TextField text = new TextField(val == null ? "" : val.toString(), theme.getSkin());
        text.addListener(new ChangeListener() {
            @Override
            public void changed(final ChangeEvent event, final Actor actor) {
                final String txt = text.getText();
                final T val = translate(type, txt);
                if (val == null) {
                    if (errorMessage.getText().isEmpty()) {
                        errorMessage.setText("Invalid value \"" + txt + "\"");
                    }
                } else {
                    System.out.println("Changed! " + val);
                    if (validate(val)) {
                        value.in(val);
                    }
                }
            }
        });
        add(text);
        return newVal -> {
            if (validate(newVal)) {
                value.in(newVal);
                text.setText(newVal == null ? "" : newVal.toString());
            }
        };
    }

    protected T translate(final FieldType type, final String text) {
        final Object result;
        switch (type) {
            case integer:
                try {
                    result = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    errorMessage.setText("Invalid number: " + text);
                    return null;
                }
                break;
            default:
                result = text;
        }
        //noinspection unchecked
        return (T) result;
    }

}
