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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.praxislive.script.Command;

import static org.junit.jupiter.api.Assertions.*;
import static org.praxislive.script.commands.Utils.*;

public class CoreCommandsTest {

    @Test
    public void testCommandDescriptions() {
        logTest("testCommandDescriptions");
        Map<String, Command> commands = new HashMap<>();
        new CoreCommands().install(commands);
        commands.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> {
                    assertFalse(e.getValue().description().isBlank(),
                            () -> "Command " + e.getKey() + " does not have a description");
                });

    }

}
