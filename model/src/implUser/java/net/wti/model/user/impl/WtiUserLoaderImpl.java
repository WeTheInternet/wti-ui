package net.wti.model.user.impl;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.visitor.ComposableXapiVisitor;
import net.wti.model.api.WtiGroup;
import net.wti.model.api.WtiUser;
import net.wti.model.core.AbstractClasspathXapiLoader;
import net.wti.model.user.core.UserGroupStore;
import net.wti.model.user.spi.WtiUserLoader;
import xapi.fu.In2;
import xapi.fu.java.X_Jdk;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.fu.data.SetLike;

import java.io.InputStream;

/// WtiUserLoaderImpl
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 20:47
public class WtiUserLoaderImpl extends AbstractClasspathXapiLoader<WtiUser> implements WtiUserLoader {

    private static final String USER_SUFFIX = "models/" + WtiUser.MODEL_WTI_USER; // META-INF/models/wu
    private static final String XAPI_FILE_SUFFIX = ".xapi";

    @Override
    public void loadInto(final UserGroupStore store) {
        loadFromClasspath((resource, user) -> {
            if (user != null) {
                store.putUser(user);
            }
        });
    }

    @Override
    public void loadFromClasspath(final In2<String, WtiUser> callback) {
        loadAllFromClasspath(USER_SUFFIX, XAPI_FILE_SUFFIX, (resourceName, stream) -> {
            final ModelManifest manifest = X_Model.getService().findManifest(WtiUser.class);
            if (manifest == null) {
                throw new IllegalStateException("No manifest found for " + WtiUser.class.getName());
            }

            for (UiContainerExpr root : loadElements(resourceName, stream)) {
                final WtiUser user = elementToUser(root);
                callback.in(resourceName, user);
            }
        });
    }

    private WtiUser elementToUser(final UiContainerExpr root) {
        if (root == null || !"WtiUser".equals(root.getName())) {
            throw new IllegalArgumentException("Invalid user tag (expected <WtiUser ... />) " + ":\n" + root);
        }
        final WtiUser user = X_Model.create(WtiUser.class);
        final String[] username = new String[1];
        final SetLike<String> groups = X_Jdk.setLinked();

        final ComposableXapiVisitor<WtiUserLoaderImpl> visitor =
                ComposableXapiVisitor.onMissingFail(WtiUserLoaderImpl.class)
                        .withUiAttrTerminal((UiAttrExpr attr, WtiUserLoaderImpl scope) -> {
                            final String n = attr.getNameString();
                            final String raw = attr.getExpression().toSource();
                            if ("id".equals(n) || "username".equals(n) || "name".equals(n)) {
                                username[0] = stripQuotes(raw);
                            } else if ("groups".equals(n)) {
                                final String[] parsed = parseList(raw);
                                for (String g : parsed) groups.add(g);
                            }
                        });

        for (UiAttrExpr attr : root.getAttributes()) {
            attr.accept(visitor, this);
        }

        if (username[0] != null) {
            user.setKey(WtiUser.newKey(username[0]));
        }
        user.setGroups(groups);
        return user;
    }

    @Override
    public ModelKey keyForUsername(final String username) {
        return WtiUser.newKey(username);
    }

    private String[] parseList(final String raw) {
        String txt = stripQuotes(raw).trim();
        if (txt.startsWith("[") && txt.endsWith("]")) {
            txt = txt.substring(1, txt.length() - 1).trim();
        }
        if (txt.isEmpty()) return new String[0];
        String[] bits = txt.split(",");
        for (int i = 0; i < bits.length; i++) bits[i] = stripQuotes(bits[i].trim());
        return bits;
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