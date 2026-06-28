package net.wti.quest.api;

import xapi.fu.data.SetLike;
import xapi.model.api.Model;

///
/// HasRequirements:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/06/2026 @ 21:59
public interface HasRequirements extends Model, HasAcl {

    QuestRequirements getToComplete();
    HasRequirements setToComplete(QuestRequirements requirements);

    QuestRequirements getToSee();
    HasRequirements setToSee(QuestRequirements requirements);

    QuestRequirements getToStart();
    HasRequirements setToStart(QuestRequirements requirements);

    QuestRequirements getToEdit();
    HasRequirements setToEdit(QuestRequirements requirements);

    default void copyRequirementsFrom(final HasRequirements requirements) {
        if (requirements.getToComplete() != null) {
            setToComplete(requirements.getToComplete());
        }
        if (requirements.getToSee() != null) {
            setToSee(requirements.getToSee());
        }
        if (requirements.getToStart() != null) {
            setToStart(requirements.getToStart());
        }
        if (requirements.getToEdit() != null) {
            setToEdit(requirements.getToEdit());
        }
    }
}
