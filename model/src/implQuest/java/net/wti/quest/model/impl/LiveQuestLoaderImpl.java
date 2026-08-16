package net.wti.quest.model.impl;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiBodyExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.visitor.ComposableXapiVisitor;
import net.wti.model.core.AbstractClasspathXapiLoader;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import xapi.fu.In2;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.time.X_Time;

import java.io.InputStream;

///
/// QuestLoaderImpl:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 12:05
public class LiveQuestLoaderImpl extends AbstractClasspathXapiLoader<LiveQuest> {

    private static final String QUESTS_SUFFIX = "models/lv";
    private static final String XAPI_FILE_SUFFIX = ".xapi";

    @Override
    public void loadFromClasspath(final In2<String, LiveQuest> callback) {
        loadAllFromClasspath(QUESTS_SUFFIX, XAPI_FILE_SUFFIX, (resourceName, stream) -> {
            final ModelManifest manifest = X_Model.getService().findManifest(LiveQuest.class);
            if (manifest == null) {
                throw new IllegalStateException("No manifest found for " + LiveQuest.class.getName());
            }
            for (UiContainerExpr rootElement : loadElements(resourceName, stream)) {
                visitQuestTree(rootElement, null, null, resourceName, callback);
            }
        });
    }

    private void visitQuestTree(
            final UiContainerExpr element,
            final LiveQuest parent,
            final Integer inheritedDayIndex,
            final String resourceName,
            final In2<String, LiveQuest> callback) {
        if (!isQuestElement(element)) {
            throw new IllegalArgumentException("Root element must be <Quest>; you sent: " + element.toSource());
        }

        final LiveQuest quest = X_Model.create(LiveQuest.class);
        quest.setCreatedAtMillis(X_Time.nowMillisLong());
        final Integer[] dayIndexHolder = new Integer[]{inheritedDayIndex};
        final String[] idHolder = new String[1];

        final ComposableXapiVisitor<LiveQuestLoaderImpl> visitor = ComposableXapiVisitor.onMissingFail(LiveQuestLoaderImpl.class)
                .withUiAttrTerminal((attr, scope) -> {

                    final String attrName = attr.getNameString();
                    final String raw = attr.getExpression().toSource();

                    if ("id".equals(attrName)) {
                        idHolder[0] = stripQuotes(raw);
                    }

                    switch (attrName) {
                        case "liveKey":
                            quest.setLiveKey(stripQuotes(raw));
                            break;
                        case "dayIndex":
                            dayIndexHolder[0] = parseInt(raw);
                            quest.setDayIndex(dayIndexHolder[0]);
                            break;
                        case "effectivePriority":
                            quest.setEffectivePriority(parseInt(raw));
                            break;
                        case "deadlineMillis":
                            quest.setDeadlineMillis(parseLong(raw));
                            break;
                        case "skip":
                            quest.setSkip(parseBoolean(raw));
                            break;
                        case "scheduleTemplateKey":
                            quest.setScheduleTemplateKey(stripQuotes(raw));
                            break;
                        case "status":
                            quest.setStatus(parseStatus(stripQuotes(raw)));
                            break;
                        case "tags":
                            quest.setTags(parseTags(raw));
                            break;
                        default:
                            // ignore fields not currently mapped to LiveQuest
                    }
                });

        for (UiAttrExpr attr : element.getAttributes()) {
            attr.accept(visitor, this);
        }

        if ((quest.getLiveKey() == null || quest.getLiveKey().isEmpty()) && idHolder[0] != null) {
            quest.setLiveKey(deriveLiveKeyFromId(idHolder[0]));
        }
        if (quest.getDayIndex() == null) {
            quest.setDayIndex(dayIndexHolder[0]);
        }

        assignKeyParentage(quest, parent);
        final UiBodyExpr body = element.getBody();
        if (body != null) {
            // recurse into child quest elements
            for (Expression expr : body.getChildren()) {
                expr.accept(ComposableXapiVisitor.onMissingFail(LiveQuestLoaderImpl.class)
                                .withUiContainerExpr(child -> {
                                    visitQuestTree(child, quest, quest.getDayIndex(), resourceName, callback);
                                    // do not visit contents again
                                    return false;
                                })
                        , this);
            }
        }
        callback.in(resourceName, quest);
    }

    private void assignKeyParentage(final LiveQuest quest, final LiveQuest parent) {
        final String liveKey = quest.getLiveKey();
        if (liveKey == null || liveKey.isEmpty()) {
            return;
        }

        if (parent != null && parent.getKey() != null) {
            final ModelKey key = LiveQuest.KEY_BUILDER_LIVE.buildKey(liveKey).setParent(parent.getKey());
            quest.setKey(key);
            quest.setParentDayKey(parent.getParentDayKey());
            if (quest.getDayIndex() == null) {
                quest.setDayIndex(parent.getDayIndex());
            }
            return;
        }

        if (quest.getDayIndex() != null) {
            final ModelKey dayKey = ModelDay.newKey(quest.getDayIndex());
            quest.setParentDayKey(dayKey);
            quest.setKey(LiveQuest.newKey(dayKey, liveKey));
        }
    }

    private boolean isQuestElement(final UiContainerExpr element) {
        return element != null && "Quest".equals(element.getName());
    }

    private String deriveLiveKeyFromId(final String id) {
        final String clean = stripQuotes(id);
        final int idx = clean.indexOf("/lv/");
        return idx >= 0 && idx + 4 < clean.length() ? clean.substring(idx + 4) : clean;
    }

    private String[] parseTags(final String raw) {
        String txt = stripQuotes(raw).trim();
        if (txt.startsWith("[") && txt.endsWith("]")) {
            txt = txt.substring(1, txt.length() - 1).trim();
        }
        if (txt.isEmpty()) {
            return new String[0];
        }
        final String[] bits = txt.split(",");
        for (int i = 0; i < bits.length; i++) {
            bits[i] = stripQuotes(bits[i].trim());
        }
        return bits;
    }

    private String stripQuotes(final String in) {
        if (in == null) {
            return null;
        }
        String s = in.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private Integer parseInt(final String in) {
        try {
            return Integer.valueOf(stripQuotes(in));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseLong(final String in) {
        try {
            return Long.valueOf(stripQuotes(in));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean parseBoolean(final String in) {
        final String v = stripQuotes(in);
        if ("true".equalsIgnoreCase(v)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(v)) return Boolean.FALSE;
        return null;
    }

    private QuestStatus parseStatus(final String in) {
        try {
            return QuestStatus.valueOf(in);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    protected ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

}
