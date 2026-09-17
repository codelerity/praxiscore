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

import java.util.List;
import java.util.Map;
import org.praxislive.core.Value;
import org.praxislive.script.Command;
import org.praxislive.script.CommandInstaller;
import org.praxislive.script.Namespace;
import org.praxislive.script.StackFrame;

/**
 * Central installer for project tools commands.
 */
public final class ProjectCommands implements CommandInstaller {

    private static final Map<String, Command> COMMANDS = Map.ofEntries(
            Map.entry("project", new Project()),
            Map.entry("project-build", new Project("build")),
            Map.entry("project-run", new Project("run"))
    );

    private static final Map<String, Command> SUB_COMMANDS = Map.ofEntries(
            Map.entry("build", new ProjectExecute(false)),
            Map.entry("run", new ProjectExecute(true))
    );

    @Override
    public void install(Map<String, Command> commands) {
        commands.putAll(COMMANDS);
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
                subCmd = args.getFirst().toString();
                subArgs = args.stream().skip(1).toList();
            }
            Command sub = SUB_COMMANDS.get(subCmd);
            return sub.createStackFrame(namespace, subArgs);
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
