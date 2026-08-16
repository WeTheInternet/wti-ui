package net.wti.model.core;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.CompilationUnit;
import net.wti.lang.parser.ast.Node;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.model.spi.XapiModelLoaderService;
import xapi.fu.In2;
import xapi.fu.data.ListLike;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.model.X_Model;
import xapi.model.api.Model;

import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

///
/// AbstractClasspathXapiLoader:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 12:19
public abstract class AbstractClasspathXapiLoader <M extends Model> implements XapiModelLoaderService<M> {

    @FunctionalInterface
    protected interface ResourceStreamConsumer {
        void accept(String resourceName, InputStream stream) throws Exception;
    }

    protected String metaInfPath(final String suffix) {
        return "META-INF/" + suffix;
    }

    protected Object findManifest(final Class<? extends Model> modelClass) {
        return X_Model.getService().findManifest(modelClass);
    }

    public void loadFromClasspath(final Class<M> modelClass, final In2<String, M> callback) {
        final Object manifest = findManifest(modelClass);
        if (manifest == null) {
            throw new IllegalStateException("No model manifest found for " + modelClass.getName());
        }
        loadFromClasspath(callback);
    }

    protected void loadAllFromClasspath(
            final String suffix,
            final String fileSuffix,
            final ResourceStreamConsumer consumer
    ) {
        final ClassLoader loader = classLoader();
        final String resourceFolder = metaInfPath(suffix);

        try {
            final Enumeration<URL> roots = loader.getResources(resourceFolder);
            while (roots.hasMoreElements()) {
                final URL root = roots.nextElement();
                scanRoot(root, resourceFolder, fileSuffix, consumer);
            }
        } catch (Exception e) {
            onLoadError(resourceFolder, e);
        }
    }

    protected void scanRoot(
            final URL root,
            final String resourceFolder,
            final String fileSuffix,
            final ResourceStreamConsumer consumer
    ) throws Exception {
        final String protocol = root.getProtocol();
        if ("file".equals(protocol)) {
            final String decodedPath = URLDecoder.decode(root.getPath(), "UTF-8");
            final File dir = new File(decodedPath);
            final File[] children = dir.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                if (!child.isFile()) {
                    continue;
                }
                final String name = child.getName();
                if (!name.endsWith(fileSuffix)) {
                    continue;
                }
                final String resourceName = resourceFolder + "/" + name;
                try (InputStream in = classLoader().getResourceAsStream(resourceName)) {
                    if (in != null) {
                        consumer.accept(resourceName, in);
                    }
                }
            }
            return;
        }

        if ("jar".equals(protocol)) {
            final JarURLConnection conn = (JarURLConnection) root.openConnection();
            final JarFile jar = conn.getJarFile();
            final Enumeration<JarEntry> entries = jar.entries();
            final String prefix = resourceFolder.endsWith("/") ? resourceFolder : resourceFolder + "/";
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                if (!name.startsWith(prefix) || !name.endsWith(fileSuffix)) {
                    continue;
                }
                try (InputStream in = classLoader().getResourceAsStream(name)) {
                    if (in != null) {
                        consumer.accept(name, in);
                    }
                }
            }
            return;
        }

        throw new UnsupportedOperationException("Unsupported classpath URL protocol: " + protocol);
    }

    protected abstract ClassLoader classLoader();

    protected void onLoadError(final String resourceName, final Throwable error) {
        // default no-op
        Log.tryLog(AbstractClasspathXapiLoader.class, this, Log.LogLevel.ERROR,
                "Failed to load resource", resourceName, error);
        throw new IllegalStateException("Failed to load resource" + resourceName, error);
    }

    protected SizedIterable<UiContainerExpr> loadElements(final String resourceName, final InputStream stream) {
        try {
            return JavaParser.parseXapiMany(stream, "UTF-8", false);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse " + resourceName, e);
        }
    }

}
