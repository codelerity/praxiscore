/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2026 Neil C Smith.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 * 
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.core.syntax;

/**
 * This class represents the basic tokens of the Pcl script format.
 */
public class Token {

    /**
     * The various token types.
     */
    public static enum Type {

        /**
         * An end of line token.
         */
        EOL,
        /**
         * A comment token.
         */
        COMMENT,
        /**
         * A plain text token.
         */
        PLAIN,
        /**
         * A quoted text token, contained within double quotes ({@code "..."})
         * in the script.
         */
        QUOTED,
        /**
         * A nested (sub-command) token, contained within square brackets
         * ({@code [...]}) in the script, and intended to be executed as a
         * command with the token substituted by the command result.
         */
        SUBCOMMAND,
        /**
         * A braced text token, contained within braces ({@code {...}}) in the
         * script.
         */
        BRACED
    };

    private String text;
    private Type type;
    private int startIndex;
    private int endIndex;

    /**
     * Create a Token of the specified type. The text should be the tokenized
     * text only, with any escaping already done. The start and end indexes
     * refer to the Token's position in the full-text being tokenized, and not
     * the text of the token itself.
     *
     * @param type
     * @param text
     * @param startIndex
     * @param endIndex
     */
    public Token(Type type, String text, int startIndex, int endIndex) {
        if (type == null || text == null) {
            throw new NullPointerException("Token type or text cannot be null");
        }
        if (endIndex < startIndex) {
            throw new IllegalArgumentException("End index cannot be less than start index");
        }
        this.type = type;
        this.text = text;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    /**
     * Get the tokenized text (all escape sequences will have been parsed if
     * required by the token type)
     *
     * @return
     */
    public String getText() {
        return this.text;
    }

    /**
     * Get the type of this token.
     *
     * @return
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Get the start index (inclusive) for this token's position in the script.
     *
     * @return
     */
    public int getStartIndex() {
        return this.startIndex;
    }

    /**
     * Get the end index (exclusive) of this token's position in the script.
     *
     * @return
     */
    public int getEndIndex() {
        return this.endIndex;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("TOKEN Type = ");
        buf.append(this.type);
        buf.append(", text = ");
        buf.append(this.text);
        buf.append(", start index = ");
        buf.append(this.startIndex);
        buf.append(", end index = ");
        buf.append(this.endIndex);
        return buf.toString();

    }

}
