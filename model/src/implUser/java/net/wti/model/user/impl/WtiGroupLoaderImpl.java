package net.wti.model.user.impl;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.visitor.ComposableXapiVisitor;
import net.wti.model.api.WtiGroup;
import net.wti.model.core.AbstractClasspathXapiLoader;
import net.wti.model.user.core.UserGroupStore;
import net.wti.model.user.spi.WtiGroupLoader;
import xapi.fu.In2;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;

import java.io.InputStream;

/// WtiGroupLoaderImpl
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 09:13
public class WtiGroupLoaderImpl extends AbstractClasspathXapiLoader<WtiGroup> implements WtiGroupLoader {

    private static final String GROUP_SUFFIX = "models/" + WtiGroup.MODEL_WTI_GROUP;  // META-INF/models/wg
    private static final String XAPI_FILE_SUFFIX = ".xapi";

    @Override
    public void loadInto(final UserGroupStore store) {
        loadFromClasspath((resource, group) -> {
            if (group != null) {
                store.putGroup(group);
            }
        });
    }

    @Override
    public void loadFromClasspath(final In2<String, WtiGroup> callback) {
        loadAllFromClasspath(GROUP_SUFFIX, XAPI_FILE_SUFFIX, (resourceName, stream) -> {
            final ModelManifest manifest = X_Model.getService().findManifest(WtiGroup.class);
            if (manifest == null) {
                throw new IllegalStateException("No manifest found for " + WtiGroup.class.getName());
            }
            for (UiContainerExpr root : loadElements(resourceName, stream)) {
                final WtiGroup group = elementToGroup(root);
                callback.in(resourceName, group);
            }

        });
    }

    private WtiGroup elementToGroup(final UiContainerExpr root) {
        if (root == null || !"WtiGroup".equals(root.getName())) {
            throw new IllegalArgumentException("Invalid group tag (expected <WtiGroup ... />) " + ":\n" + root);
        }

        final WtiGroup group = X_Model.create(WtiGroup.class);
        final String[] groupName = new String[1];

        final ComposableXapiVisitor<WtiGroupLoaderImpl> visitor =
                ComposableXapiVisitor.onMissingFail(WtiGroupLoaderImpl.class)
                        .withUiAttrTerminal((UiAttrExpr attr, WtiGroupLoaderImpl scope) -> {
                            final String n = attr.getNameString();
                            final String raw = attr.getExpression().toSource();
                            if ("id".equals(n) || "name".equals(n) || "group".equals(n)) {
                                groupName[0] = stripQuotes(raw);
                            }
                        });

        for (UiAttrExpr attr : root.getAttributes()) {
            attr.accept(visitor, this);
        }

        if (groupName[0] != null) {
            group.setKey(WtiGroup.newKey(groupName[0]));
        }
        return group;
    }

    @Override
    public ModelKey keyForGroup(final String groupName) {
        return WtiGroup.newKey(groupName);
    }

    private String stripQuotes(final String in) {
        if (in == null) return null;
        String s = in.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    @Override
    protected ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}