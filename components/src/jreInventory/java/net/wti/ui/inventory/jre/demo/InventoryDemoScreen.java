package net.wti.ui.inventory.jre.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.wti.ui.inventory.api.InventoryController;
import net.wti.ui.inventory.impl.InventoryTable;
import net.wti.ui.inventory.model.Inventory;
import xapi.model.X_Model;

/// InventoryDemoScreen:
///
/// Example ScreenAdapter showing usage of InventoryTable in a libGDX app.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 03:58
public class InventoryDemoScreen extends ScreenAdapter {

    private final Stage stage;
    private final InventoryTable table;
    private final InventoryController controller;

    public InventoryDemoScreen() {
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Inventory model = X_Model.create(Inventory.class);
        model.setLimit(16);
        controller = new InventoryController(model);
        table = new InventoryTable(skin, controller);
        table.setPosition(20, 20);
        table.setInventory(model);

        stage = new Stage(new ScreenViewport());
        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void dispose() {
        table.dispose();
        stage.dispose();
    }
}

