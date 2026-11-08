package net.wti.gdx.theme.raeleus.sgx

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import spock.lang.Specification

/// Parse the real packaged skin/atlas metadata; no texture, font, window or GL is constructed.
class SgxDrawableReferencesSpec extends Specification {
    def "every SGX style drawable and tint base resolves to a packaged atlas region or alias"() {
        given:
        def root = new FileHandle(getClass().getResource('/cc-by-4/raeleus/sgx/sgx-ui.atlas').toURI().path).parent()
        def atlas = new TextureAtlas.TextureAtlasData(root.child('sgx-ui.atlas'), root, false)
        Set<String> regions = atlas.regions.collect { it.name } as Set
        JsonValue skin = new JsonReader().parse(root.child('sgx-ui.json'))
        Set<String> aliases = skin.get('com.badlogic.gdx.scenes.scene2d.ui.Skin$TintedDrawable').collect { it.name } as Set
        Map<String, String> references = [:]
        skin.each { group ->
            Class<?> type = Class.forName(group.name, false, getClass().classLoader)
            group.each { style ->
                style.each { property ->
                    def field = type.fields.find { it.name == property.name }
                    if (field != null && Drawable.isAssignableFrom(field.type) && property.isString()) {
                        references["${group.name}/${style.name}/${property.name}".toString()] = property.asString()
                    }
                    if (group.name == 'com.badlogic.gdx.scenes.scene2d.ui.Skin$TintedDrawable' && property.name == 'name') {
                        references["tint/${style.name}".toString()] = property.asString()
                    }
                }
            }
        }

        expect:
        !regions.empty
        references.size() > 50
        references.findAll { key, value -> !regions.contains(value) && !aliases.contains(value) }.isEmpty()

        and: 'recurrence labels retain their existing public style names and use the matching calendar states'
        def labels = skin.get('com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle')
        ['recurrence-cell': 'calendar-inactive', 'recurrence-cell-active': 'calendar-active',
         'recurrence-cell-over': 'calendar-hover', 'recurrence-cell-selected': 'calendar-selected'].every { name, background ->
            labels.get(name).getString('background') == background && regions.contains(background)
        }
    }
}
