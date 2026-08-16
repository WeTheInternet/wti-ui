package net.wti.quest.api;

import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.string.X_String;

///
/// ModelQuest:
///
/// Base type for all quest subclasses, containing only the common metadata all quest models should contain
///
/// Created by James X. Nelson (James@WeTheInter.net) on 11/04/2026 @ 22:38
@SuppressWarnings("UnusedReturnValue")
public interface ModelQuest <M extends ModelQuest<M>> extends Model, HasRequirements {

    /// Human-visible title.
    String getTitle();
    default String title() {
        String explicit = getTitle();
        if (explicit == null) {
            // tests should not allow this state
            assert false : getClass() + " : " + getType() + " without a title: " + this;
            final ModelKey key = getKey();
            if (key == null) {
                throw new IllegalStateException("No title or key set in model " + getType() + " : " + this);
            }
            return key.getId();
        }
        return explicit;
    }
    M setTitle(final String title);

    /// Optional description (markdown/text).
    String getDescription();
    default String description() {
        final String explicit = getDescription();
        return explicit == null ? "" : explicit;
    }
    M setDescription(final String description);

}
