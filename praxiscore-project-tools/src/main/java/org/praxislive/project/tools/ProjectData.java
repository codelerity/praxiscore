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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.praxislive.core.Value;
import org.praxislive.core.types.PError;
import org.praxislive.core.types.PReference;
import org.praxislive.core.types.PResource;
import org.praxislive.project.ProjectElement;
import org.praxislive.project.ProjectModel;
import org.praxislive.project.SyntaxUtils;
import org.praxislive.script.Namespace;
import org.praxislive.script.ScriptStackFrame;
import org.praxislive.script.ScriptUtils;
import org.praxislive.script.StackFrame;
import org.praxislive.script.Variable;

class ProjectData {

    private static final String CONSTANT = "_PROJECT_DATA";

    private static final List<String> ALLOWED_COMMANDS = List.of(
            "@", "~", "file", "array", "map",
            "libraries", "sources", "shared-code-add",
            "include"
    );

    private final Path projectFile;
    private final List<ProjectElement> setupElements;
    private final List<ProjectElement> buildElements;
    private final List<ProjectElement> runElements;
    private final Set<ProjectElement> executedElements;

    private ProjectData(Path projectFile, ProjectModel model, boolean active) {
        this.projectFile = projectFile;
        this.setupElements = new ArrayList<>(model.setupElements());
        this.buildElements = new ArrayList<>(model.buildElements());
        this.runElements = new ArrayList<>(model.runElements());
        executedElements = new HashSet<>();
        if (active) {
            executedElements.addAll(setupElements);
            executedElements.addAll(runElements);
        }
    }

    StackFrame execute(Namespace namespace, boolean run) {
        List<ProjectElement> elements = new ArrayList<>();
        for (ProjectElement element : setupElements) {
            if (executedElements.add(element)) {
                elements.add(element);
            }
        }
        for (ProjectElement element : buildElements) {
            if (executedElements.add(element)) {
                elements.add(element);
            }
        }
        if (run) {
            elements.addAll(runElements);
        }

        if (elements.isEmpty()) {
            return StackFrame.empty();
        }

        Namespace ns = namespace.createChild();
        StackFrame frame = ScriptStackFrame.forScript(ns,
                "cd " + projectFile.getParent().toUri()).inline().build();
        List<String> errors = new ArrayList<>();

        for (ProjectElement element : elements) {
            switch (element) {
                case ProjectElement.File file -> {
                    frame = frame.andThen(v -> {
                        return fileToStackFrame(ns, file)
                                .onError(errs -> {
                                    errors.add(handleError(element, errs));
                                    return StackFrame.empty();
                                });
                    });
                }
                case ProjectElement.Line line -> {
                    frame = frame.andThen(v -> {
                        return ScriptStackFrame.forScript(ns, line.line()).build()
                                .onError(errs -> {
                                    errors.add(handleError(element, errs));
                                    return StackFrame.empty();
                                });
                    });
                }
            }
        }

        return frame.andThen(v -> {
            if (errors.isEmpty()) {
                return StackFrame.empty();
            } else {
                throw new RuntimeException(errors.stream().collect(Collectors.joining()));
            }
        });

    }

    private StackFrame fileToStackFrame(Namespace namespace, ProjectElement.File element) {
        String path = element.file().getPath().toLowerCase(Locale.ROOT);
        String script = "include %s".formatted(SyntaxUtils.valueToToken(
                projectFile.getParent().toUri(),
                PResource.of(element.file())));
        if (path.endsWith(".pxr") || path.endsWith(".pxg")) {
            return ScriptStackFrame.forScript(namespace, script)
                    .trapErrors()
                    .allowedCommands(ALLOWED_COMMANDS)
                    .build();
        } else {
            return ScriptStackFrame.forScript(namespace, script).build();
        }
    }

    private String handleError(ProjectElement element, List<Value> errors) {
        String header = switch (element) {
            case ProjectElement.File file ->
                "Errors :\n" + file.file().getPath() + "\n";
            case ProjectElement.Line line ->
                "Errors :\n" + line.line() + "\n";
        };
        return errors.stream()
                .flatMap(v -> PError.from(v).stream())
                .map(err -> err.type() + "\n" + err.message())
                .collect(Collectors.joining("\n", header, "\n"));
    }

    /**
     * Create a task stackframe with the project data context. The provided
     * function will be called asynchronously, wrapped in a stackframe that
     * finds or creates the project data.
     *
     * @param namespace current namespace
     * @param init stackframe constructor
     * @return compound stackframe
     */
    static StackFrame task(Namespace namespace, Function<ProjectData, StackFrame> init) {
        return findProjectData(namespace)
                .andThen(v -> {
                    ProjectData data = PReference.from(v.getFirst())
                            .flatMap(ref -> ref.as(ProjectData.class))
                            .orElseThrow(() -> new IllegalStateException(
                                    "Project data constant does not contain project data"));
                    return init.apply(data);
                });
    }

    private static StackFrame findProjectData(Namespace namespace) {
        Variable dataVar = namespace.getVariable(ProjectData.CONSTANT);
        if (dataVar != null) {
            return StackFrame.supply(() -> List.of(dataVar.getValue()));
        } else {
            URI file = Optional.ofNullable(namespace.getVariable("_FILE"))
                    .map(Variable::getValue)
                    .flatMap(PResource::from)
                    .map(PResource::value)
                    .orElse(null);
            URI pwd = ScriptUtils.currentWorkingDirectory(namespace);
            Namespace globalNS = ScriptUtils.findGlobalNamespace(namespace)
                    .orElseThrow(() -> new IllegalStateException("No global namespace"));
            return StackFrame.async(() -> PReference.of(createProjectData(file, pwd)))
                    .andThenMap(args -> {
                        globalNS.createConstant(ProjectData.CONSTANT, args.getFirst());
                        return args;
                    });
        }
    }

    private static ProjectData createProjectData(URI file, URI pwd) throws Exception {
        Path projectFile;
        boolean active;
        if (file != null && file.getPath().endsWith("project.pxp")) {
            projectFile = Path.of(file);
            active = true;
        } else {
            projectFile = Path.of(pwd).resolve("project.pxp");
            active = false;
        }
        if (!Files.exists(projectFile)) {
            throw new UnsupportedOperationException("No project file found");
        }
        ProjectModel model = ProjectModel.parse(
                projectFile.getParent().toUri(),
                Files.readString(projectFile));
        return new ProjectData(projectFile, model, active);
    }

}
