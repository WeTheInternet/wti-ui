package net.wti.ui.inventory.impl

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.inventory.api.InventoryController;
import net.wti.ui.inventory.model.Inventory;
import spock.lang.Specification;
import xapi.model.X_Model;

/// InventoryTableSpec:
///
/// Spock specification for InventoryTable (View refresh behavior).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 04:11
class InventoryTableSpec extends Specification {

    static HeadlessApplication app

    def setupSpec() {
        // Configure and launch headless app to populate Gdx.files, Gdx.graphics, etc.
        def config = new HeadlessApplicationConfiguration()
        app = new HeadlessApplication(new ApplicationAdapter() {}, config)
        // Optionally override display mode:
        Gdx.graphics.setWindowedMode(800, 600)
    }

    def cleanupSpec() {
        // Shutdown LibGDX
        if (app != null) {
            app.exit()
            app = null
        }
    }

    def "table rebuilds grid on refresh"() {
        given:
        Skin skin = new Skin()
        Inventory model = X_Model.create(Inventory.class)
        model.setLimit(2)
        def ctl = new InventoryController(model)
        def table = new InventoryTable(skin, ctl)
        table.setInventory(model)

        when:
        table.refresh()

        then:
        table.getChildren().size == 2
    }
}

