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
import org.praxislive.core.Value;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PNumber;
import org.praxislive.script.Command;
import org.praxislive.script.Env;
import org.praxislive.script.InlineCommand;
import org.praxislive.script.Namespace;

/**
 * Various operator commands.
 */
class OpCmds {

    private static final Map<String, Command> COMMANDS = Map.of(
            "+", new Add(),
            "==", new Equivalent(true),
            "!=", new Equivalent(false),
            "*", new Multiply()
    );

    private OpCmds() {
    }

    static void install(Map<String, Command> commands) {
        commands.putAll(COMMANDS);
    }

    static List<PNumber> asNumbers(List<Value> values) {
        return values.stream()
                .map(v -> PNumber.from(v).orElseThrow(()
                        -> new IllegalArgumentException("Not a number : " + v)))
                .toList();
    }

    static boolean allIntegers(List<PNumber> values) {
        return values.stream().allMatch(PNumber::isInteger);
    }

    private static class Add implements InlineCommand {

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            if (args.isEmpty()) {
                return List.of(PNumber.ZERO);
            }
            List<PNumber> values = asNumbers(args);
            if (allIntegers(values)) {
                int sum = values.stream()
                        .mapToInt(PNumber::toIntValue)
                        .sum();
                return List.of(PNumber.of(sum));
            } else {
                double sum = values.stream()
                        .mapToDouble(PNumber::value)
                        .sum();
                return List.of(PNumber.of(sum));
            }
        }

        @Override
        public String description() {
            return CoreCommands.message("add.description");
        }

    }

    private static class Equivalent implements InlineCommand {

        private final boolean equivalent;

        private Equivalent(boolean equivalent) {
            this.equivalent = equivalent;
        }

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            if (args.size() != 2) {
                throw new IllegalArgumentException();
            }
            Value v1 = args.get(0);
            Value v2 = args.get(1);
            boolean eq = v1.equivalent(v2) || v2.equivalent(v1);
            if (equivalent) {
                return List.of(PBoolean.of(eq));
            } else {
                return List.of(PBoolean.of(!eq));
            }

        }

        @Override
        public String description() {
            if (equivalent) {
                return CoreCommands.message("equivalent.description");
            } else {
                return CoreCommands.message("not-equivalent.description");
            }
        }

    }

    private static class Multiply implements InlineCommand {

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            if (args.isEmpty()) {
                return List.of(PNumber.ONE);
            }
            List<PNumber> values = asNumbers(args);
            if (allIntegers(values)) {
                int sum = values.stream()
                        .mapToInt(PNumber::toIntValue)
                        .reduce(1, (a, b) -> a * b);
                return List.of(PNumber.of(sum));
            } else {
                double sum = values.stream()
                        .mapToDouble(PNumber::value)
                        .reduce(1, (a, b) -> a * b);
                return List.of(PNumber.of(sum));
            }
        }

        @Override
        public String description() {
            return CoreCommands.message("multiply.description");
        }

    }

}
