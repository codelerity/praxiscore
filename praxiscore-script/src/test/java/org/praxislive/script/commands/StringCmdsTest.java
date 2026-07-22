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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PBoolean;
import org.praxislive.script.Command;
import org.praxislive.script.InlineCommand;

import static org.junit.jupiter.api.Assertions.*;
import static org.praxislive.script.commands.Utils.*;

/**
 *
 */
public class StringCmdsTest {

    private static final Map<String, Command> CMDS;

    static {
        CMDS = new HashMap<>();
        StringCmds.install(CMDS);
    }

    @Test
    public void testStringContains() throws Exception {
        logTest("testStringContains");
        InlineCommand contains = (InlineCommand) CMDS.get("string-contains");
        List<Value> result = contains.process(env(), namespace(),
                PArray.ofObjects("This is our test String", "test").asList());
        assertEquals(1, result.size());
        assertEquals(PBoolean.TRUE, result.get(0));
        result = contains.process(env(), namespace(),
                PArray.ofObjects("This is another String", "test").asList());
        assertEquals(1, result.size());
        assertEquals(PBoolean.FALSE, result.get(0));
    }

    @Test
    public void testStringJoin() throws Exception {
        logTest("testStringContains");
        InlineCommand join = (InlineCommand) CMDS.get("string-join");
        PArray baseList = PArray.ofObjects("One", "Two", "Three");
        List<Value> result = join.process(env(), namespace(),
                PArray.ofObjects(baseList).asList());
        assertEquals(1, result.size());
        assertEquals("OneTwoThree", result.get(0).toString());

        result = join.process(env(), namespace(),
                PArray.ofObjects(baseList, ":").asList());
        assertEquals(1, result.size());
        assertEquals("One:Two:Three", result.get(0).toString());

        result = join.process(env(), namespace(),
                PArray.ofObjects(baseList, " ", ">>> ").asList());
        assertEquals(1, result.size());
        assertEquals(">>> One Two Three", result.get(0).toString());

        result = join.process(env(), namespace(),
                PArray.ofObjects(baseList, " ", ">>> ", " <<<").asList());
        assertEquals(1, result.size());
        assertEquals(">>> One Two Three <<<", result.get(0).toString());

        result = join.process(env(), namespace(),
                PArray.ofObjects(PArray.EMPTY, " ", ">>> ", " <<<").asList());
        assertEquals(1, result.size());
        assertEquals(">>>  <<<", result.get(0).toString());
    }

    @Test
    public void testStringMatches() throws Exception {
        logTest("testStringContains");
        InlineCommand matches = (InlineCommand) CMDS.get("string-matches");
        List<Value> result = matches.process(env(), namespace(),
                PArray.ofObjects("This is our test String", ".*\\Wtest\\W.*").asList());
        assertEquals(1, result.size());
        assertEquals(PBoolean.TRUE, result.get(0));
        result = matches.process(env(), namespace(),
                PArray.ofObjects("This is another String", ".*\\Wtest\\W.*").asList());
        assertEquals(1, result.size());
        assertEquals(PBoolean.FALSE, result.get(0));
    }

    @Test
    public void testStringSplit() throws Exception {
        logTest("testStringContains");
        InlineCommand split = (InlineCommand) CMDS.get("string-split");
        List<Value> result = split.process(env(), namespace(),
                PArray.ofObjects("This is our test String", "\\s").asList());
        assertEquals(1, result.size());
        assertEquals(List.of("This", "is", "our", "test", "String"),
                PArray.from(result.get(0)).orElseThrow().asListOf(String.class));
        result = split.process(env(), namespace(),
                PArray.ofObjects("This is another String", "\\R").asList());
        assertEquals(1, result.size());
        assertEquals(List.of("This is another String"),
                PArray.from(result.get(0)).orElseThrow().asListOf(String.class));
    }

}
