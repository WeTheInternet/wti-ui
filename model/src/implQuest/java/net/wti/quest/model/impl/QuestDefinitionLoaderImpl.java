package net.wti.quest.model.impl;

import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.visitor.ComposableXapiVisitor;
import net.wti.model.core.AbstractClasspathXapiLoader;
import net.wti.quest.api.QuestDefinition;
import xapi.fu.In2;
import xapi.model.X_Model;
import xapi.model.api.ModelManifest;

/// QuestDefinitionLoaderImpl
///
/// Classpath .xapi loader for QuestDefinition resources under META-INF/models/qdef.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 09:08
public class QuestDefinitionLoaderImpl extends AbstractClasspathXapiLoader<QuestDefinition> {

    private static final String QUEST_DEF_SUFFIX = "models/qdef";
    private static final String XAPI_FILE_SUFFIX = ".xapi";

    @Override
    public void loadFromClasspath(final In2<String, QuestDefinition> callback) {
        loadAllFromClasspath(QUEST_DEF_SUFFIX, XAPI_FILE_SUFFIX, (resourceName, stream) -> {
            final ModelManifest manifest = X_Model.getService().findManifest(QuestDefinition.class);
            if (manifest == null) {
                throw new IllegalStateException("No manifest found for " + QuestDefinition.class.getName());
            }

            for (UiContainerExpr root : loadElements(resourceName, stream)) {
                visitDefinition(root, resourceName, callback);
            }

        });
    }

    private void visitDefinition(
            final UiContainerExpr element,
            final String resourceName,
            final In2<String, QuestDefinition> callback
    ) {
        if (element == null || !"QuestDefinition".equals(element.getName())) {
            throw new IllegalArgumentException("Root element must be <QuestDefinition>; got: " + (element == null ? "null" : element.getName()));
        }

        final QuestDefinition def = X_Model.create(QuestDefinition.class);
        final String[] id = new String[1];

        final ComposableXapiVisitor<QuestDefinitionLoaderImpl> visitor =
                ComposableXapiVisitor.onMissingFail(QuestDefinitionLoaderImpl.class)
                        .withUiAttrTerminal((UiAttrExpr attr, QuestDefinitionLoaderImpl scope) -> {
                            final String name = attr.getNameString();
                            final String raw = attr.getExpression().toSource();

                            if ("id".equals(name)) {
                                id[0] = stripQuotes(raw);
                                return;
                            }
                            switch (name) {
                                case "name":
                                case "title":
                                    def.setTitle(stripQuotes(raw));
                                    break;
                                case "description":
                                    def.setDescription(stripQuotes(raw));
                                    break;
                                case "priority":
                                    def.setPriority(parseInt(raw));
                                    break;
                                case "scheduleTemplateKey":
                                    def.setScheduleTemplateKey(stripQuotes(raw));
                                    break;
                                case "defaultAlarmMinutes":
                                    def.setDefaultAlarmMinutes(parseInt(raw));
                                    break;
                                case "defaultGracePeriodMinutes":
                                    def.setDefaultGracePeriodMinutes(parseInt(raw));
                                    break;
                                case "active":
                                    def.setActive(parseBoolean(raw));
                                    break;
                                case "auto":
                                    def.setAuto(parseBoolean(raw));
                                    break;
                                case "tags":
                                    def.setTags(parseTags(raw));
                                    break;
                                default:
                                    // rule/composition parsing to be expanded next pass
                            }
                        });

        for (UiAttrExpr attr : element.getAttributes()) {
            attr.accept(visitor, this);
        }

        if (id[0] != null && !id[0].trim().isEmpty()) {
            def.setKey(QuestDefinition.newKey(id[0]));
        }
        callback.in(resourceName, def);
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
        final String s = in.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
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

    private Boolean parseBoolean(final String in) {
        final String v = stripQuotes(in);
        if ("true".equalsIgnoreCase(v)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(v)) return Boolean.FALSE;
        return null;
    }

    @Override
    protected ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
