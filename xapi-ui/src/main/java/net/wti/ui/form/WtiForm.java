package net.wti.ui.form;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.api.FieldType;
import net.wti.ui.gdx.theme.GdxTheme;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.model.api.Model;
import xapi.string.X_String;

/// WtiForm:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 23:48
public class WtiForm extends Table {

    private static final In1Out1<Object, String> NOT_NULL = o -> o == null ? "Cannot be empty" : null;
    private final GdxTheme theme;

    public WtiForm(final GdxTheme theme) {
        super(theme.getSkin());
        this.theme = theme;
    }

    private <T> In1Out1<T, String> notNull() {
        //noinspection unchecked
        return (In1Out1<T, String>) NOT_NULL;
    }

    public WtiFormField<Integer> addIntegerField(final Model model, final String fieldName, final String displayName) {
        final Out1<Integer> getProp = ()->model.getProperty(fieldName);
        final In1<Integer> setProp = value -> model.setProperty(fieldName, value);
        row();
        add(new Label(displayName, theme.getSkin()));
        final WtiFormField<Integer> field = addIntegerField(getProp, setProp);
        row();
        add(field.getErrorMessage()).colspan(2);
        return field;
    }

    public WtiFormField<Integer> addIntegerField(final Model model, final String fieldName) {
        final Out1<Integer> getProp = ()->model.getProperty(fieldName);
        final In1<Integer> setProp = value -> model.setProperty(fieldName, value);
        final WtiFormField<Integer> field = addIntegerField(getProp, setProp);
        return field;
    }

    public WtiFormField<Integer> addIntegerField(final Out1<Integer> getter, final In1<Integer> setter) {
        final WtiFormField<Integer> field = new WtiFormField<>(theme, FieldType.integer, getter, setter);
        field.addValidator(notNull());
        add(field);
        return field;
    }

    public WtiFormField<String> addTextField(final FieldType type, final Model model, final String fieldName) {
        final Out1<String> getProp = ()->model.getProperty(fieldName);
        final In1<String> setProp = value -> model.setProperty(fieldName, value);
        return addTextField(type, X_String.toTitleCase(fieldName), getProp, setProp);
    }

    public WtiFormField<String> addTextField(final FieldType type, final String fieldName, final Out1<String> getter, final In1<String> setter) {
        final WtiFormField<String> field = new WtiFormField<>(theme, type, getter, setter);
        row();
        add(new Label(fieldName, theme.getSkin()));
        add(field);
        row();
        add(field.getErrorMessage());
        return field;
    }
}
