package net.wti.ui.form.impl

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Label
import net.wti.ui.gdx.theme.GdxTheme
import spock.lang.Specification

class WtiFormFieldBooleanSpec extends Specification {

    static HeadlessApplication app

    def setupSpec() {
        app = new HeadlessApplication(new ApplicationAdapter() {}, new HeadlessApplicationConfiguration())
    }

    def cleanupSpec() {
        app?.exit()
        app = null
    }

    def "checkbox uses the bound value and writes user changes"() {
        given:
        def values = [true]
        def field = new WtiFormFieldBoolean(theme(), { values[0] }, { values[0] = it })

        expect:
        field.getType() == net.wti.ui.form.api.FieldType.checkbox
        field.getCheckBox().isChecked()

        when:
        field.getCheckBox().setChecked(false)

        then:
        !values[0]
    }

    def "null bound values normalize to false"() {
        given:
        def values = [null]
        def field = new WtiFormFieldBoolean(theme(), { values[0] }, { values[0] = it })

        expect:
        !field.getCheckBox().isChecked()
        values[0] == false
    }

    def "form convenience method adds a bound checkbox field"() {
        given:
        def values = [false]
        def form = new WtiForm(theme())

        when:
        def field = form.addBooleanField({ values[0] }, { values[0] = it })
        field.getCheckBox().setChecked(true)

        then:
        values[0]
        form.getChildren().contains(field)
    }

    private GdxTheme theme() {
        def skin = new Skin()
        def regions = new com.badlogic.gdx.utils.Array<TextureRegion>()
        regions.add(new TextureRegion())
        def font = new BitmapFont(new BitmapFont.BitmapFontData(), regions, false)
        skin.add("default", new Label.LabelStyle(font, Color.WHITE))
        def checkBoxStyle = new com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle()
        checkBoxStyle.font = font
        skin.add("default", checkBoxStyle)
        Stub(GdxTheme) {
            getSkin() >> skin
        }
    }
}
