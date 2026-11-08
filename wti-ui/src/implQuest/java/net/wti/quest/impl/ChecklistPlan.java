package net.wti.quest.impl;

import net.wti.quest.api.ChecklistSnapshot;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestStatus;
import xapi.model.api.ModelKey;
import xapi.model.service.ModelService;

import java.util.ArrayList;
import java.util.List;

/// Working state for one checklist inspection/start/completion operation.
/// ChecklistProjectionService assembles it from persisted definitions and progress;
/// the completion services share those results without owning the projection service.
/// It deliberately remains an ordinary Java helper: ModelService, derived eligibility
/// and in-flight acknowledged progress have no separate persisted or cached identity.
/// Only snapshot() exposes immutable rendering values. The plan itself is mutable,
/// package-private, and is never registered as an XApi model or sent over the wire.
final class ChecklistPlan {
    final ModelService models;
    final ModelKey day;
    final int dayNum;
    final List<ChecklistPlanItem> items = new ArrayList<>();

    ChecklistPlan(final ModelService models, final ModelKey day, final int dayNum) {
        this.models = models;
        this.day = day;
        this.dayNum = dayNum;
    }

    boolean childrenFinished() {
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).live == null || items.get(i).live.getStatus() != QuestStatus.FINISHED) {
                return false;
            }
        }
        return true;
    }

    boolean hasMissingProgress() {
        for (final ChecklistPlanItem item : items) {
            if (item.live == null) {
                return true;
            }
        }
        return false;
    }

    /// Keep the advertised start action and the independent service guard in agreement.
    String startFailure() {
        if (day == null) {
            return "Explicit owner/day required";
        }
        for (final ChecklistPlanItem item : items) {
            if (Boolean.FALSE.equals(item.definition.getActive())) {
                return "Inactive checklist definitions cannot be started";
            }
        }
        final LiveQuest rootLive = items.get(0).live;
        if (rootLive != null && rootLive.getStatus() != null && rootLive.getStatus() != QuestStatus.ACTIVE
                && hasMissingProgress()) {
            return "Inactive checklist has missing progress; definition changed";
        }
        return null;
    }

    ChecklistSnapshot snapshot() {
        final List<ChecklistSnapshot.Entry> children = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                children.add(entry(items.get(i), true));
            }
        }
        return new ChecklistSnapshot(entry(items.get(0), childrenFinished()), children,
                hasMissingProgress() && startFailure() == null);
    }

    private ChecklistSnapshot.Entry entry(final ChecklistPlanItem item, final boolean eligible) {
        final LiveQuest live = item.live;
        final QuestStatus status = live == null ? null : live.getStatus();
        final boolean active = status == null || status == QuestStatus.ACTIVE;
        return new ChecklistSnapshot.Entry(QuestProgressKeys.encoded(models, item.definition.getKey()),
                item.key == null ? null : QuestProgressKeys.encoded(models, item.key),
                live == null ? item.definition.getTitle() : live.getTitle(),
                live == null ? item.definition.getDescription() : live.getDescription(), item.quantity,
                status, live != null, live != null && active && eligible);
    }
}
