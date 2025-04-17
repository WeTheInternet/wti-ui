package net.wti.ui.form;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.api.FieldType;
import net.wti.ui.gdx.theme.GdxTheme;
import xapi.collect.fifo.SimpleFifo;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.fu.Pointer;

/// WtiFormField:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 09/03/2025 @ 00:02
public class WtiFormField<T> extends Table  {

    protected final FieldType type;
    protected final In1<T> setter;
    protected final Out1<T> getter;
    protected final SimpleFifo<In1Out1<T, String>> validators;
    protected final Label errorMessage;
    protected final GdxTheme theme;

    public WtiFormField(final GdxTheme theme, final FieldType type, final Out1<T> getter, final In1<T> setter) {
        this(theme, type, Pointer.pointerJoin(setter, getter));
    }
    public WtiFormField(final GdxTheme theme, final FieldType type) {
        this(theme, type, Pointer.pointerTo(null));
    }

    private WtiFormField(final GdxTheme theme, final FieldType type, final Pointer<T> value) {
        this.type = type;
        this.theme = theme;
        getter = initGetter(value);
        setter = initSetter(value);
        validators = new SimpleFifo<>();
        row();
        errorMessage = new Label("", theme.getSkin());
        errorMessage.setFontScale(0.75f);
        errorMessage.setColor(Color.ORANGE);
    }

    protected Out1<T> initGetter(final Pointer<T> value) {
        return value;
    }

    protected In1<T> initSetter(final Pointer<T> value) {
        return value;
    }

    public Label getErrorMessage() {
        return errorMessage;
    }

    public void addValidator(In1Out1<T, String> validator) {
        validators.give(validator);
    }

    protected boolean validate(final T val) {
        for (In1Out1<T, String> validator : validators) {
            final String message = validator.io(val);
            if (message != null && !message.isEmpty()) {
                System.err.println("Failed validation: " + message);
                errorMessage.setText(message);
                return false;
            }
        }
        errorMessage.setText("");
        return val != null;
    }

    public FieldType getType() {
        return type;
    }

    void setValue(final T value) {
        setter.in(value);
    }

    T getValue() {
        return getter.out1();
    }
}
