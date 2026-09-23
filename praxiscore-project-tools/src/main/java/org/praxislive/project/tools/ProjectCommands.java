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
package org.praxislive.project.tools;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.praxislive.core.OrderedMap;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PString;
import org.praxislive.script.Command;
import org.praxislive.script.CommandInstaller;
import org.praxislive.script.Namespace;
import org.praxislive.script.ScriptStackFrame;
import org.praxislive.script.StackFrame;

/**
 * Central installer for project tools commands.
 */
public final class ProjectCommands implements CommandInstaller {

    private static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(ProjectCommands.class.getPackageName() + ".Messages");

    private static final Map<String, Command> COMMANDS = Map.ofEntries(
            Map.entry("project", new Project()),
            Map.entry("project-build", new Project("build")),
            Map.entry("project-run", new Project("run"))
    );

    private static final Map<String, Command> SUB_COMMANDS = OrderedMap.ofEntries(
            Map.entry("build", new ProjectExecute(false)),
            Map.entry("run", new ProjectExecute(true))
    );

    @Override
    public void install(Map<String, Command> commands) {
        commands.putAll(COMMANDS);
    }

    static String message(String key) {
        return MESSAGES.getString(key);
    }

    static String message(String key, Object... arguments) {
        return MessageFormat.format(MESSAGES.getString(key), arguments);
    }

    private static class Project implements Command {

        private final String subcommand;

        private Project() {
            this(null);
        }

        private Project(String subcommand) {
            this.subcommand = subcommand;
        }

        @Override
        public StackFrame createStackFrame(Namespace namespace, List<Value> args) throws Exception {
            String subCmd = subcommand;
            List<Value> subArgs = args;
            if (subCmd == null) {
                if (args.isEmpty()) {
                    return createCommandChooser(namespace);
                } else {
                    subCmd = args.getFirst().toString();
                    subArgs = args.stream().skip(1).toList();
                }
            }
            Command sub = SUB_COMMANDS.get(subCmd);
            return sub.createStackFrame(namespace, subArgs);
        }

        @Override
        public String description() {
            return message("project.description");
        }

        private StackFrame createCommandChooser(Namespace namespace) throws Exception {
            List<PString> options = SUB_COMMANDS.keySet().stream()
                    .map(PString::of)
                    .toList();
            return ScriptStackFrame.forScript(namespace, """
                        user-input-select $message $options
                        """)
                    .createConstant("message", PString.of(message("project.choose")))
                    .createConstant("options", PArray.of(options))
                    .build()
                    // catch error from select but not from subcommand
                    .onError(err -> StackFrame.empty())
                    .andThen(res -> {
                        if (!res.isEmpty()) {
                            try {
                                return SUB_COMMANDS.get(res.getFirst().toString())
                                        .createStackFrame(namespace, res);
                            } catch (Exception ex) {
                            }
                        }
                        return StackFrame.empty();
                    });
        }

    }

    private static class ProjectExecute implements Command {

        private final boolean run;

        private ProjectExecute(boolean run) {
            this.run = run;
        }

        @Override
        public StackFrame createStackFrame(Namespace namespace, List<Value> args) throws Exception {
            return ProjectData.task(namespace, pd -> pd.execute(namespace, run));
        }

    }

}
