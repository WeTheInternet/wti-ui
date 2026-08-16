package net.wti.game.impl

import net.wti.game.api.GameRejection
import net.wti.game.api.GameCommand
import net.wti.game.api.GameCommandResult
import net.wti.game.api.GameDataModel
import net.wti.game.spi.GameCommandAuthorizer
import net.wti.game.spi.GameCommandContext
import net.wti.game.spi.GameCommandHandler
import net.wti.game.spi.GameCommandResultFactory
import spock.lang.Specification
import xapi.fu.In1
import xapi.jre.model.ModelServiceJre
import xapi.model.X_Model

class QueuedLocalGameSessionSpec extends Specification {

    void setupSpec() {
        final ModelServiceJre service = X_Model.getService() as ModelServiceJre
        service.register(TestGameCommand)
        service.register(TestGameResult)
        service.register(TestGameDataModel)
        service.getOrMakeModelManifest(TestGameCommand)
        service.getOrMakeModelManifest(TestGameResult)
        service.getOrMakeModelManifest(TestGameDataModel)
    }

    def "submit is asynchronous and a pump handles and publishes FIFO"() {
        given:
        def handled = []
        def notified = []
        def session = session(
                { context -> null },
                { context ->
                    handled << context.command.commandId
                    accepted(context, context.command.clientSequence)
                }
        )
        session.addResultListener({ result -> notified << result } as In1<TestGameResult>)

        when:
        session.submit(command("one", 1))
        session.submit(command("two", 2))

        then:
        handled.empty
        notified.empty
        session.pendingCommands() == 2

        when:
        def pumped = session.pump()

        then:
        pumped == 2
        handled == ["one", "two"]
        notified*.commandId == ["one", "two"]
        notified*.serverOrder == [1L, 2L]
        session.pendingCommands() == 0
    }

    def "duplicate command id republishes the retained original without mutation"() {
        given:
        int mutations = 0
        def notified = []
        def session = session(
                { context -> null },
                { context ->
                    mutations++
                    accepted(context, 9)
                }
        )
        session.addResultListener({ result -> notified << result } as In1<TestGameResult>)

        when:
        session.submit(command("same", 1))
        session.submit(command("same", 1))
        def pumped = session.pump()

        then:
        pumped == 2
        mutations == 1
        notified.size() == 2
        notified[0].is(notified[1])
        notified*.serverOrder == [1L, 1L]
        notified*.resultingRevision == [9L, 9L]
    }

    def "explicit authorization rejection bypasses the game handler"() {
        given:
        int mutations = 0
        def notified = []
        def session = session(
                { context -> GameRejection.UNAUTHORIZED },
                { context ->
                    mutations++
                    accepted(context, 1)
                }
        )
        session.addResultListener({ result -> notified << result } as In1<TestGameResult>)

        when:
        session.submit(command("denied", 1).setActorId("intruder") as TestGameCommand)

        then:
        notified.empty

        when:
        session.pump()

        then:
        mutations == 0
        notified.size() == 1
        !notified[0].accepted
        notified[0].reasonCode == GameRejection.UNAUTHORIZED
        notified[0].commandId == "denied"
        notified[0].serverOrder == 1L
    }

    def "commands submitted by a listener wait for the next pump boundary"() {
        given:
        def notified = []
        def session = session(
                { context -> null },
                { context -> accepted(context, context.command.clientSequence) }
        )
        session.addResultListener({ result ->
            notified << result.commandId
            if (result.commandId == "first") {
                session.submit(command("listener-added", 2))
            }
        } as In1<TestGameResult>)

        when:
        session.submit(command("first", 1))
        def firstPump = session.pump()

        then:
        firstPump == 1
        notified == ["first"]
        session.pendingCommands() == 1

        when:
        def secondPump = session.pump()

        then:
        secondPump == 1
        notified == ["first", "listener-added"]
        session.pendingCommands() == 0
    }

    def "evicted command ids are outside the configured deduplication horizon"() {
        given:
        def handled = []
        def session = session(
                { context -> null },
                { context ->
                    handled << context.command.commandId
                    accepted(context, handled.size())
                },
                1
        )

        when:
        session.submit(command("old", 1))
        session.pump()
        session.submit(command("new", 2))
        session.pump()
        session.submit(command("old", 1))
        session.pump()

        then:
        handled == ["old", "new", "old"]
    }

    def "concrete XApi subtypes inherit and round-trip command and result envelopes"() {
        given:
        def commandManifest = X_Model.getService().findManifest(TestGameCommand)
        def resultManifest = X_Model.getService().findManifest(TestGameResult)

        expect:
        commandManifest.getMethodData("commandId").type == String
        commandManifest.getMethodData("clientSequence").type == Long.TYPE
        commandManifest.getMethodData("expectedRevision").type == Long.TYPE
        commandManifest.getMethodData("inventoryInstanceId").type == String
        resultManifest.getMethodData("accepted").type == Boolean.TYPE
        resultManifest.getMethodData("serverOrder").type == Long.TYPE
        resultManifest.getMethodData("resultingRevision").type == Long.TYPE
        resultManifest.getMethodData("remainingCount").type == Integer.TYPE

        when:
        def originalCommand = command("round-trip", 7)
                .setExpectedRevision(41) as TestGameCommand
        originalCommand.inventoryInstanceId = "stack-17"
        def decodedCommand = X_Model.deserialize(
                TestGameCommand,
                X_Model.serialize(TestGameCommand, originalCommand)
        )

        def originalResult = X_Model.create(TestGameResult)
        originalResult.commandId = "round-trip"
        originalResult.accepted = true
        originalResult.reasonCode = "accepted"
        originalResult.serverOrder = 12
        originalResult.resultingRevision = 42
        originalResult.eventType = "inventory-consumed"
        originalResult.remainingCount = 3
        def decodedResult = X_Model.deserialize(
                TestGameResult,
                X_Model.serialize(TestGameResult, originalResult)
        )

        then:
        decodedCommand.commandId == "round-trip"
        decodedCommand.sessionId == "local-session"
        decodedCommand.actorId == "player-one"
        decodedCommand.clientSequence == 7L
        decodedCommand.expectedRevision == 41L
        decodedCommand.inventoryInstanceId == "stack-17"
        decodedResult.commandId == "round-trip"
        decodedResult.accepted
        decodedResult.reasonCode == "accepted"
        decodedResult.serverOrder == 12L
        decodedResult.resultingRevision == 42L
        decodedResult.eventType == "inventory-consumed"
        decodedResult.remainingCount == 3
    }

    def "durable game data marker inherits XApi model behavior without classifying messages"() {
        given:
        def manifest = X_Model.getService().findManifest(TestGameDataModel)

        expect:
        manifest.getMethodData("displayName").type == String
        GameDataModel.isAssignableFrom(TestGameDataModel)
        !GameDataModel.isAssignableFrom(GameCommand)
        !GameDataModel.isAssignableFrom(GameCommandResult)

        when:
        def original = X_Model.create(TestGameDataModel)
        original.displayName = "authoritative-or-replica"
        def decoded = X_Model.deserialize(
                TestGameDataModel,
                X_Model.serialize(TestGameDataModel, original)
        )

        then:
        decoded.displayName == "authoritative-or-replica"
    }

    private static QueuedLocalGameSession<TestGameCommand, TestGameResult> session(
            Closure<String> authorization,
            Closure<TestGameResult> handler,
            int retention = 8
    ) {
        new QueuedLocalGameSession<TestGameCommand, TestGameResult>(
                "local-session",
                retention,
                authorization as GameCommandAuthorizer<TestGameCommand>,
                handler as GameCommandHandler<TestGameCommand, TestGameResult>,
                ({ context, reason -> rejected(context, reason) }
                        as GameCommandResultFactory<TestGameCommand, TestGameResult>)
        )
    }

    private static TestGameCommand command(String id, long sequence) {
        def command = X_Model.create(TestGameCommand)
        command.commandId = id
        command.sessionId = "local-session"
        command.actorId = "player-one"
        command.clientSequence = sequence
        command.expectedRevision = 0
        command.inventoryInstanceId = "stack-${sequence}"
        command
    }

    private static TestGameResult accepted(
            GameCommandContext<TestGameCommand> context,
            long revision
    ) {
        def result = X_Model.create(TestGameResult)
        result.accepted = true
        result.reasonCode = "accepted"
        result.resultingRevision = revision
        result.eventType = "accepted"
        result
    }

    private static TestGameResult rejected(
            GameCommandContext<TestGameCommand> context,
        String reason
    ) {
        def result = X_Model.create(TestGameResult)
        result.resultingRevision = context.command.expectedRevision
        result.eventType = "rejected"
        result
    }
}
