package net.wti.quest.impl

import net.wti.quest.api.QuestCompletionCodec
import net.wti.quest.api.QuestCompletionCommand
import spock.lang.Specification
import spock.lang.Shared
import xapi.model.tools.PrimitiveSerializerDefault

class QuestCompletionCodecSpec extends Specification {
    @Shared def codec = new PrimitiveSerializerDefault()
    @Shared def command = new QuestCompletionCommand(QuestCompletionCommand.PROTOCOL,'operation-1','full-day','full-live')
    def 'exact four-string command and six-string result preserve images including newlines'() {
        expect:
        QuestCompletionCodec.decodeCommand(codec,QuestCompletionCodec.encodeCommand(codec,command)) == command
        def result = QuestCompletionCodec.decodeResult(codec,QuestCompletionCodec.encodeResult(codec,command,'live\nimage','history\nimage'))
        result.command == command && result.liveImage == 'live\nimage' && result.historyImage == 'history\nimage'
    }
    def 'framing rejects extra data truncated images bad protocol ids and UTF8 or size failures'() {
        when:
        action()
        then:
        thrown(Exception)
        where:
        action << [
            { QuestCompletionCodec.decodeCommand(codec,QuestCompletionCodec.encodeCommand(codec,command)+'x') },
            { QuestCompletionCodec.decodeResult(codec,QuestCompletionCodec.encodeCommand(codec,command)) },
            { new QuestCompletionCommand('wrong','id','day','live') },
            { new QuestCompletionCommand(QuestCompletionCommand.PROTOCOL,'bad token','day','live') },
            { QuestCompletionCodec.decodeCommand(codec,'a' * (QuestCompletionCodec.MAX_BYTES+1)) },
            { QuestCompletionCodec.encodeResult(codec,command,'\uD800','history') },
            { QuestCompletionCodec.encodeResult(codec,command,'live','') }
        ]
    }
}
