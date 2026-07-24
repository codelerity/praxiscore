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
package org.praxislive.script.commands;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PString;
import org.praxislive.script.Command;
import org.praxislive.script.Env;
import org.praxislive.script.InlineCommand;
import org.praxislive.script.Namespace;

/**
 * Various String related commands.
 */
class StringCmds {

    private static final Map<String, Command> COMMANDS = Map.of(
            "string-contains", new StringContains(),
            "string-join", new StringJoin(),
            "string-matches", new StringMatches(),
            "string-split", new StringSplit()
    );

    private StringCmds() {
    }

    static void install(Map<String, Command> commands) {
        commands.putAll(COMMANDS);
    }

    private static class StringContains implements InlineCommand {

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            if (args.size() != 2) {
                throw new IllegalArgumentException();
            }
            boolean result = args.get(0).toString().contains(args.get(1).toString());
            return List.of(PBoolean.of(result));
        }

        @Override
        public String description() {
            return CoreCommands.message("string-contains.description");
        }
    }
    
    private static class StringJoin implements InlineCommand {

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            int argCount = args.size();
            if (argCount < 1 || argCount > 4) {
                throw new IllegalArgumentException();
            }
            PArray array = PArray.from(args.get(0)).orElseThrow(IllegalArgumentException::new);
            String delim = argCount > 1 ? args.get(1).toString() : "";
            String prefex = argCount > 2 ? args.get(2).toString() : "";
            String suffix = argCount > 3 ? args.get(3).toString() : "";
            String result = array.stream()
                    .map(Value::toString)
                    .collect(Collectors.joining(delim, prefex, suffix));
            return List.of(PString.of(result));
        }

        @Override
        public String description() {
            return CoreCommands.message("string-join.description");
        }
        
    }

    private static class StringMatches implements InlineCommand {

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            if (args.size() != 2) {
                throw new IllegalArgumentException();
            }
            boolean result = args.get(0).toString().matches(args.get(1).toString());
            return List.of(PBoolean.of(result));
        }

        @Override
        public String description() {
            return CoreCommands.message("string-matches.description");
        }

    }
    
    private static class StringSplit implements InlineCommand {

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            if (args.size() != 2) {
                throw new IllegalArgumentException();
            }
            PArray result = Stream.of(args.get(0).toString().split(args.get(1).toString()))
                    .map(PString::of)
                    .collect(PArray.collector());
            return List.of(result);
        }

        @Override
        public String description() {
            return CoreCommands.message("string-split.description");
        }

        
    }

}
