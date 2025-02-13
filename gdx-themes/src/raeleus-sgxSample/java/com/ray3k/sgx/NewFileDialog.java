package com.ray3k.sgx;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/// NewFileDialog:
///
/// Adapted from theme example zip from [Raeleus blog](https://ray3k.wordpress.com/sgx-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:59
public class NewFileDialog extends Dialog {

    private final TextField fileName;

    public NewFileDialog(String title, Skin skin) {
        super(title, skin);

        Table content = this.getContentTable();

        fileName = new TextField("", skin);
        content.add(fileName).fillX().expandX();

        button("Create", true);
        button("Cancel", false);

        key(Keys.ENTER, true);
        key(Keys.ESCAPE, false);
    }


    protected String getResult() {
        return fileName.getText();
    }


    @Override
    public Dialog show(Stage stage, Action action) {
        super.show(stage, action);
        stage.setKeyboardFocus(fileName);
        return this;
    }
}