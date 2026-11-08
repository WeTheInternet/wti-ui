package net.wti.quest.model.impl;

import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.JsonContainerExpr;
import net.wti.lang.parser.ast.visitor.ComposableXapiVisitor;
import net.wti.model.core.AbstractClasspathXapiLoader;
import net.wti.quest.api.QuestDefinition;
import net.wti.quest.api.ChildRef;
import net.wti.quest.api.ChildRole;
import net.wti.quest.api.ChildStartPolicy;
import net.wti.quest.api.CompletionPolicy;
import xapi.fu.In2;
import xapi.model.X_Model;
import xapi.model.api.ModelManifest;

import java.util.ArrayList;
import java.util.List;

/// QuestDefinitionLoaderImpl
///
/// Classpath .xapi loader for QuestDefinition resources under META-INF/models/qdef.
/// Composition is an ordered `children = [<ChildRef definitionKey = "definition-id" />]`
/// attribute. References name reusable definitions; they are not live instances or checkmarks.
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
        def.setCompletionPolicy(CompletionPolicy.ALL_OF);
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
                                    def.setTags(parseTags(attr.getExpression()));
                                    break;
                                case "completionPolicy":
                                    def.setCompletionPolicy(parseEnum(CompletionPolicy.class, raw, resourceName));
                                    break;
                                case "minimumRequired":
                                    def.setMinimumRequired(parseNonNegativeInt(raw, "minimumRequired", resourceName));
                                    break;
                                case "children":
                                    readChildren(def, attr.getExpression(), resourceName);
                                    break;
                                default:
                                    // Recurrence-rule parsing remains separate from composition loading.
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

    private void readChildren(final QuestDefinition definition, final Expression expression, final String resourceName) {
        if (!(expression instanceof JsonContainerExpr) || !((JsonContainerExpr) expression).isArray()) {
            throw new IllegalArgumentException(resourceName + ": children must be an array of ChildRef elements");
        }
        for (final Expression child : ((JsonContainerExpr) expression).getValues()) {
            if (!(child instanceof UiContainerExpr) || !"ChildRef".equals(((UiContainerExpr) child).getName())) {
                throw new IllegalArgumentException(resourceName + ": children must contain ChildRef elements");
            }
            final ChildRef reference = createChildReference(definition);
            for (final UiAttrExpr attr : ((UiContainerExpr) child).getAttributes()) {
                final String name = attr.getNameString();
                final String raw = attr.getExpression().toSource();
                switch (name) {
                    case "definitionKey":
                        final String id = stripQuotes(raw).trim();
                        if (id.isEmpty()) {
                            throw new IllegalArgumentException(resourceName + ": ChildRef definitionKey must not be empty");
                        }
                        reference.setDefinitionKey(QuestDefinition.newKey(id));
                        break;
                    case "role":
                        reference.setRole(parseEnum(ChildRole.class, raw, resourceName));
                        break;
                    case "startPolicy":
                        reference.setStartPolicy(parseEnum(ChildStartPolicy.class, raw, resourceName));
                        break;
                    case "startAfterNSiblingsComplete":
                        reference.setStartAfterNSiblingsComplete(parseNonNegativeInt(raw, name, resourceName));
                        break;
                    case "quantity":
                        final int quantity = parseNonNegativeInt(raw, name, resourceName);
                        if (quantity == 0) {
                            throw new IllegalArgumentException(resourceName + ": ChildRef quantity must be positive");
                        }
                        reference.setQuantity(quantity);
                        break;
                    case "inheritTags":
                        final Boolean inherit = parseBoolean(raw);
                        if (inherit == null) {
                            throw new IllegalArgumentException(resourceName + ": inheritTags must be true or false");
                        }
                        reference.setInheritTags(inherit);
                        break;
                    case "additionalTags":
                        reference.setAdditionalTags(parseTags(attr.getExpression()));
                        break;
                    default:
                        throw new IllegalArgumentException(resourceName + ": unknown ChildRef attribute " + name);
                }
            }
            if (reference.getDefinitionKey() == null) {
                throw new IllegalArgumentException(resourceName + ": ChildRef requires definitionKey");
            }
            definition.children().add(reference);
        }
    }

    /// Create a fresh embedded relationship for the owning definition.
    /// The definition may not have its key yet while its attributes are being parsed.
    /// Leave relationship-key assignment to ModelList.add; definitionKey separately
    /// identifies the reusable quest definition referenced by this relationship.
    /// Override this seam if the loader later needs an explicit ownership/key policy.
    protected ChildRef createChildReference(final QuestDefinition definition) {
        final ChildRef reference = X_Model.create(ChildRef.class);
        // Boxed numeric null becomes zero on the existing wire. Materialize the authored
        // count default before parsing overrides; an explicit zero quantity remains invalid.
        reference.setQuantity(1);
        reference.setRole(ChildRole.PARALLEL);
        reference.setStartPolicy(ChildStartPolicy.AFTER_PARENT_START);
        return reference;
    }

    private <E extends Enum<E>> E parseEnum(final Class<E> type, final String raw, final String resourceName) {
        try {
            return Enum.valueOf(type, stripQuotes(raw));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(resourceName + ": invalid " + type.getSimpleName() + " " + raw, error);
        }
    }

    private int parseNonNegativeInt(final String raw, final String name, final String resourceName) {
        final Integer value = parseInt(raw);
        if (value == null || value < 0) {
            throw new IllegalArgumentException(resourceName + ": " + name + " must be a non-negative integer");
        }
        return value;
    }

    private String[] parseTags(final Expression expression) {
        if (expression instanceof JsonContainerExpr && ((JsonContainerExpr) expression).isArray()) {
            final List<String> tags = new ArrayList<>();
            // Element boundaries belong to the AST; a comma inside one string is not a separator.
            for (final Expression value : ((JsonContainerExpr) expression).getValues()) {
                tags.add(stripQuotes(value.toSource()));
            }
            return tags.toArray(new String[0]);
        }
        // Retain the legacy comma-separated scalar spelling used by existing definitions.
        return parseTags(expression.toSource());
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
        if ("true".equalsIgnoreCase(v)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(v)) {
            return Boolean.FALSE;
        }
        return null;
    }

    @Override
    protected ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
