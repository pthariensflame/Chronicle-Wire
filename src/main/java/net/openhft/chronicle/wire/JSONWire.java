/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

/**
 * JSON wire format
 * <p/>
 * At the moment, this is a cut down version of the YAML wire format.
 */
public class JSONWire extends TextWire {
    static final BytesStore COMMA = BytesStore.from(",");

    public JSONWire(Bytes bytes, boolean use8bit) {
        super(bytes, use8bit);
    }

    public JSONWire(Bytes bytes) {
        this(bytes, false);
    }

    @NotNull
    public static JSONWire from(@NotNull String text) {
        return new JSONWire(Bytes.from(text));
    }

    public static String asText(@NotNull Wire wire) {
        long pos = wire.bytes().readPosition();
        JSONWire tw = new JSONWire(nativeBytes());
        wire.copyTo(tw);
        wire.bytes().readPosition(pos);

        return tw.toString();
    }

    @NotNull
    @Override
    protected TextValueOut createValueOut() {
        return new JSONValueOut();
    }

    @NotNull
    @Override
    protected TextValueIn createValueIn() {
        return new JSONValueIn();
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Quotes needsQuotes(@NotNull CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"' || ch < ' ')
                return Quotes.DOUBLE;
        }
        return Quotes.NONE;
    }

    void escape(@NotNull CharSequence s) {
        bytes.writeUnsignedByte('"');
        if (needsQuotes(s) == Quotes.NONE) {
            bytes.appendUtf8(s);
        } else {
            escape0(s, Quotes.DOUBLE);
        }
        bytes.writeUnsignedByte('"');
    }

    @NotNull
    @Override
    protected StringBuilder readField(@NotNull StringBuilder sb) {
        consumePadding();
        int code = peekCode();
        if (code == '}') {
            valueIn.popState();
            bytes.readSkip(1);
            consumePadding();
            code = peekCode();
        }
        if (code == '{') {
            valueIn.pushState();
            bytes.readSkip(1);
        }
        return super.readField(sb);
    }

    class JSONValueOut extends TextValueOut {
        public void elementSeparator() {
            sep = COMMA;
        }

        protected void popState() {
        }

        protected void pushState() {
            leaf = true;
        }

        protected void afterOpen() {
            sep = EMPTY;
        }

        protected void afterClose() {

        }

        @Override
        protected void addNewLine(long pos) {

        }

        @Override
        protected void newLine() {
        }

        @Override
        protected void endField() {
            sep = COMMA;
        }

        protected void fieldValueSeperator() {
            bytes.writeUnsignedByte(':');
        }

        public void writeComment(@NotNull CharSequence s) {
        }
    }

    class JSONValueIn extends TextValueIn {
    }
}
