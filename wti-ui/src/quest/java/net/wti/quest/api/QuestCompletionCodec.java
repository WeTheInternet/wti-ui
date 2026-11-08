package net.wti.quest.api;

import xapi.model.api.PrimitiveSerializer;
import xapi.source.lex.StringCharIterator;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

/// Four primitive strings, optionally followed by two captured model-image strings. Never hydrates models.
public final class QuestCompletionCodec {
    public static final int MAX_BYTES = 4 * 1024 * 1024;
    private QuestCompletionCodec() {}

    public static String encodeCommand(final PrimitiveSerializer codec, final QuestCompletionCommand command) {
        return bounded(fields(codec, command));
    }
    public static QuestCompletionCommand decodeCommand(final PrimitiveSerializer codec, final String body) {
        final StringCharIterator chars = new StringCharIterator(bounded(body));
        final QuestCompletionCommand command = command(codec, chars);
        if (chars.hasNext()) throw new IllegalArgumentException("Trailing completion input");
        return command;
    }
    public static String encodeResult(final PrimitiveSerializer codec, final QuestCompletionCommand command,
                                      final String liveImage, final String historyImage) {
        image(liveImage); image(historyImage);
        return bounded(fields(codec, command) + codec.serializeString(liveImage) + codec.serializeString(historyImage));
    }
    public static EncodedResult decodeResult(final PrimitiveSerializer codec, final String body) {
        final StringCharIterator chars = new StringCharIterator(bounded(body));
        final QuestCompletionCommand command = command(codec, chars);
        final String live = string(codec, chars), history = string(codec, chars);
        image(live); image(history);
        if (chars.hasNext()) throw new IllegalArgumentException("Trailing completion result");
        return new EncodedResult(command, live, history);
    }
    private static String fields(final PrimitiveSerializer codec, final QuestCompletionCommand c) {
        return codec.serializeString(c.getProtocol()) + codec.serializeString(c.getOperationId())
                + codec.serializeString(c.getDayKey()) + codec.serializeString(c.getLiveKey());
    }
    private static QuestCompletionCommand command(final PrimitiveSerializer codec, final StringCharIterator chars) {
        return new QuestCompletionCommand(string(codec, chars), string(codec, chars),
                string(codec, chars), string(codec, chars));
    }
    private static String string(final PrimitiveSerializer codec, final StringCharIterator chars) {
        if (!chars.hasNext()) throw new IllegalArgumentException("Truncated completion frame");
        try { return codec.deserializeString(chars); }
        catch (final RuntimeException | AssertionError malformed) {
            throw new IllegalArgumentException("Malformed completion frame");
        }
    }
    private static void image(final String image) {
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("Completion image is required");
    }
    private static String bounded(final String text) {
        if (text == null || text.length() > MAX_BYTES) throw new IllegalArgumentException("Completion frame exceeds bound");
        try {
            if (StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(text)).remaining() > MAX_BYTES) {
                throw new IllegalArgumentException("Completion frame exceeds bound");
            }
        } catch (final CharacterCodingException malformed) {
            throw new IllegalArgumentException("Malformed completion text");
        }
        return text;
    }
    public static final class EncodedResult {
        private final QuestCompletionCommand command;
        private final String liveImage, historyImage;
        private EncodedResult(final QuestCompletionCommand command, final String liveImage, final String historyImage) {
            this.command = command; this.liveImage = liveImage; this.historyImage = historyImage;
        }
        public QuestCompletionCommand getCommand() { return command; }
        public String getLiveImage() { return liveImage; }
        public String getHistoryImage() { return historyImage; }
    }
}
