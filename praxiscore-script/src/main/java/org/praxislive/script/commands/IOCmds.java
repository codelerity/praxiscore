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
import org.praxislive.core.services.UserInputService;
import org.praxislive.script.Command;
import org.praxislive.script.Namespace;
import org.praxislive.script.StackFrame;

class IOCmds {

    private static final Map<String, Command> COMMANDS = Map.of(
            "user-input", new UserInput(),
            "user-input-confirm", new UserInputConfirm(),
            "user-input-map", new UserInputMap(),
            "user-input-select", new UserInputSelect()
    );

    private IOCmds() {
    }

    static void install(Map<String, Command> commands) {
        commands.putAll(COMMANDS);
    }
    
    private static class UserInput implements Command {

        @Override
        public StackFrame createStackFrame(Namespace namespace, List<Value> args) throws Exception {
            return StackFrame.serviceCall(UserInputService.class, UserInputService.USER_INPUT, args);
        }

        @Override
        public String description() {
            return CoreCommands.message("user-input.description");
        }

    }

    private static class UserInputConfirm implements Command {

        @Override
        public StackFrame createStackFrame(Namespace namespace, List<Value> args) throws Exception {
            return StackFrame.serviceCall(UserInputService.class, UserInputService.USER_INPUT_CONFIRM, args);
        }

        @Override
        public String description() {
            return CoreCommands.message("user-input-confirm.description");
        }

    }

    private static class UserInputMap implements Command {

        @Override
        public StackFrame createStackFrame(Namespace namespace, List<Value> args) throws Exception {
            return StackFrame.serviceCall(UserInputService.class, UserInputService.USER_INPUT_MAP, args);
        }

        @Override
        public String description() {
            return CoreCommands.message("user-input-map.description");
        }

    }

    private static class UserInputSelect implements Command {

        @Override
        public StackFrame createStackFrame(Namespace namespace, List<Value> args) throws Exception {
            return StackFrame.serviceCall(UserInputService.class, UserInputService.USER_INPUT_SELECT, args);
        }

        @Override
        public String description() {
            return CoreCommands.message("user-input-select.description");
        }

    }
}
