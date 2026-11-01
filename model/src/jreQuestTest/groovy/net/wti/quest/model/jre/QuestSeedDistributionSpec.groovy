package net.wti.quest.model.jre

import net.wti.quest.api.ChildRef
import spock.lang.Specification
import spock.lang.TempDir
import xapi.model.api.ModelManifest
import xapi.model.tools.ModelSerializerDefault

import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile

class QuestSeedDistributionSpec extends Specification {
    @TempDir File directory

    private String run(File launcher, List<String> arguments, String logName, String javaHome) {
        def log = new File(directory, logName)
        def builder = new ProcessBuilder([launcher.absolutePath] + arguments)
        builder.redirectErrorStream(true).redirectOutput(log)
        ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','DISPLAY','WAYLAND_DISPLAY','DBUS_SESSION_BUS_ADDRESS','BROWSER'].each { builder.environment().remove(it) }
        builder.environment().put('JAVA_HOME', javaHome)
        builder.environment().put('JAVA_OPTS', '-verbose:class -Djava.awt.headless=true -XX:ActiveProcessorCount=1 -Xmx192m')
        def process = builder.start()
        process.outputStream.close()
        try {
            assert process.waitFor(30, TimeUnit.SECONDS): 'Owned seed launcher failed to terminate'
            assert process.exitValue() == 0: log.getText('UTF-8')
            log.getText('UTF-8')
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly()
                assert process.waitFor(10, TimeUnit.SECONDS): 'Owned launcher did not tear down'
            }
        }
    }

    def "installed launcher runs on selected Java#runtime and preserves real two-run seed with literal argv"() {
        given:
        def installation = new File(System.getProperty('quest.seed.distribution'))
        def launcher = new File(installation, 'bin/lifequest-quest-seed')
        def source = new File(directory, 'authored source $literal; — 水')
        def input = new File(source, 'META-INF/models/qdef/packing-checklists.xapi')
        input.parentFile.mkdirs()
        Files.copy(new File(System.getProperty('quest.seed.fixture')).toPath(), input.toPath())
        def store = new File(directory, 'models $literal; — 水')
        def artifact = new File(directory, 'wire $literal;/quest-policy.rpc')
        def namespace = "definition-owner & 'kid' — 水"
        def arguments = ['--seed-root=' + source.absolutePath, '--model-dir=' + store.absolutePath,
                         '--namespace=' + namespace, '--manifest-out=' + artifact.absolutePath]

        when:
        def first = run(launcher, arguments, 'first.log', System.getProperty('quest.seed.java'+runtime))
        def records = new File(store, namespace + '/qdef').listFiles().toList()
        def before = (records + artifact).collectEntries { [(it.absolutePath): it.bytes] }
        def second = run(launcher, arguments, 'second.log', System.getProperty('quest.seed.java'+runtime))
        def imported = ModelSerializerDefault.deserializeModule(artifact.getText('UTF-8'))
        def loaded = first.readLines().find {
            it.contains('xapi.model.api.ModelManifest source:') || it.contains('Loaded xapi.model.api.ModelManifest from ')
        }
        def origin = loaded =~ /(?:source:|from) (file:[^\r\n]+?\.jar)(?:\]|$)/
        assert origin.find(): 'Missing actual child ModelManifest JAR origin: ' + loaded
        def runtimeJar = new File(new URI(origin.group(1))).canonicalFile
        def actualClass = new JarFile(runtimeJar).withCloseable {
            it.getInputStream(it.getJarEntry('xapi/model/api/ModelManifest.class')).bytes
        }
        def expectedClass = ModelManifest.getResourceAsStream('/xapi/model/api/ModelManifest.class').withCloseable { it.bytes }

        then:
        Integer.parseInt(System.getProperty('quest.seed.build-java')) >= 8
        first.contains('Acknowledged 5 new quest definitions')
        second.contains('Acknowledged 0 new quest definitions')
        records.size() == 5
        before.every { path, bytes -> Arrays.equals(bytes, new File(path).bytes) }
        imported.getManifest('qdef').getMethodData('children').typeParams as List == [ChildRef]
        runtimeJar.parentFile == new File(installation, 'lib').canonicalFile
        Arrays.equals(actualClass, expectedClass) // Actual launcher bytecode, not just this Test classpath.

        where:
        runtime << [8, 17]
    }
}
