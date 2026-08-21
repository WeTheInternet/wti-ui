package net.wti.ui.components.browser;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.components.IsSkinnable;

/// Reusable Skin-driven empty/no-results presentation.
public class EntityBrowserEmptyView extends Table implements IsSkinnable {

    private final Skin skin;
    private final Label titleLabel;
    private final Label messageLabel;

    /// Creates an empty view using the Skin's default Label style.
    public EntityBrowserEmptyView(
            final Skin skin,
            final String title,
            final String message
    ) {
        this(skin, title, message, null, null, null);
    }

    /// Creates an empty view with optional illustration and label styles.
    public EntityBrowserEmptyView(
            final Skin skin,
            final String title,
            final String message,
            final Actor illustration,
            final Label.LabelStyle titleStyle,
            final Label.LabelStyle messageStyle
    ) {
        if (skin == null) {
            throw new IllegalArgumentException("skin cannot be null");
        }
        this.skin = skin;
        this.titleLabel = createLabel(title, titleStyle);
        this.messageLabel = createLabel(message, messageStyle);

        defaults().growX();
        if (illustration != null) {
            add(illustration).center();
            row();
        }
        if (titleLabel != null) {
            add(titleLabel).center();
            row();
        }
        if (messageLabel != null) {
            add(messageLabel).padTop(titleLabel == null ? 0f : 4f).center();
            row();
        }
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    /// Returns the optional title label.
    public Label getTitleLabel() {
        return titleLabel;
    }

    /// Returns the optional explanatory label.
    public Label getMessageLabel() {
        return messageLabel;
    }

    private Label createLabel(
            final String text,
            final Label.LabelStyle labelStyle
    ) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        final Label label = labelStyle == null
                ? new Label(text, skin)
                : new Label(text, labelStyle);
        label.setWrap(true);
        label.setAlignment(Align.center);
        return label;
    }
}
