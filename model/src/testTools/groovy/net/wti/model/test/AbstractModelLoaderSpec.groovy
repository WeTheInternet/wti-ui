package net.wti.model.test

import net.wti.model.core.AbstractClasspathXapiLoader
import spock.lang.Specification
import xapi.fu.In2
import xapi.model.api.Model

class AbstractModelLoaderSpec extends Specification {

    protected static abstract class ExposedLoader<M extends Model> extends AbstractClasspathXapiLoader<M> {

        @Override
        protected ClassLoader classLoader() {
            return Thread.currentThread().contextClassLoader
        }

        @Override
        void loadFromClasspath(final In2<String, M> callback) {
            // no-op for base tests
        }

        Object manifestFor(final Class<? extends Model> type) {
            return findManifest(type)
        }

        String metaInfFor(final String suffix) {
            return metaInfPath(suffix)
        }
    }

    protected void assertMetaInfPath(final ExposedLoader<?> loader, final String suffix) {
        assert loader.metaInfFor(suffix) == "META-INF/" + suffix
    }

    protected void assertManifestPresent(final ExposedLoader<?> loader, final Class<? extends Model> type) {
        assert loader.manifestFor(type) != null
    }
}
