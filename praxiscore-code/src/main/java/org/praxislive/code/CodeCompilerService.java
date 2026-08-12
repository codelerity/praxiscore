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
package org.praxislive.code;

import java.util.stream.Stream;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.Info;
import org.praxislive.core.services.LogLevel;
import org.praxislive.core.services.Service;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;

/**
 * A {@link Service} for handling code compilation. This service is used by
 * {@link CodeComponentFactoryService} and {@link SharedCodeService}. It can run
 * in a separate process. The compile control accepts a structured map of
 * sources and configuration, returning a map of compiled classes, logs and
 * libraries.
 */
public /*@TODO final*/ class CodeCompilerService implements Service {

    /**
     * Control ID of the compile control.
     */
    public static final String COMPILE = "compile";

    /**
     * Request map key for the required map of source. The value should be a
     * PMap of Java sources as strings, keyed by the binary name of each source.
     */
    public static final String KEY_SOURCES = "sources";

    /**
     * Request map key for the optional map of shared classes to compile
     * against. The value should be a PMap of Java bytecode (as PBytes) keyed by
     * binary name.
     */
    public static final String KEY_SHARED_CLASSES = "shared-classes";

    /**
     * Request map key for the optional log level to use when compiling classes.
     *
     * @see LogLevel
     */
    public static final String KEY_LOG_LEVEL = "log-level";

    /**
     * Response map key for the compiled classes. The value will be a PMap of
     * Java bytecode (as PBytes) keyed by binary name.
     */
    public static final String KEY_CLASSES = "classes";

    /**
     * Response map key for the optional array of log messages generated during
     * compilation.
     */
    public static final String KEY_LOG = "log";

    /**
     * Response map key for the optional array of additional library files (as
     * PResource) against which the source has been compiled.
     */
    public static final String KEY_EXT_CLASSPATH = "ext-classpath";

    private static final PMap REQUEST_SCHEMA = PMap.of(
            KEY_SOURCES, Info.argument().type(PMap.class).build(),
            KEY_SHARED_CLASSES, Info.argument().type(PMap.class).optional().build(),
            KEY_LOG, Info.argument().fromEnumValues(LogLevel.class).optional().build()
    );

    private static final PMap RESPONSE_SCHEMA = PMap.of(
            KEY_CLASSES, Info.argument().type(PMap.class).build(),
            KEY_LOG, Info.argument().type(PArray.class).optional().build(),
            KEY_EXT_CLASSPATH, Info.argument().type(PArray.class).optional().build()
    );

    /**
     * ControlInfo for the compile control. The control is a function that
     * accepts a map containing sources, as well as optional log level and map
     * of existing classes to compile against. It returns a map containing
     * compiled classes, as well as optional log messages and list of additional
     * library files required by the compiled classes.
     */
    public static final ControlInfo COMPILE_INFO = Info.control().function()
            .inputs(Info.argument().type(PMap.class)
                    .attribute(PMap.KEY_SCHEMA, REQUEST_SCHEMA)
                    .build())
            .outputs(Info.argument().type(PMap.class)
                    .attribute(PMap.KEY_SCHEMA, RESPONSE_SCHEMA)
                    .build())
            .build();

    /**
     * Control ID of the libraries control.
     */
    public static final String LIBRARIES = "libraries";

    /**
     * ControlInfo for the libraries control. The control is a function that
     * accepts an optional list of libraries to add, and returns the list of all
     * added libraries. Provided URI's of libraries will be resolved using
     * {@link LibraryResolver} implementations. Resolution may add additional
     * dependencies. This function will only return directly added libraries -
     * see {@link #LIBRARIES_ALL_INFO}.
     */
    public static final ControlInfo LIBRARIES_INFO = Info.control().function()
            .inputs(Info.argument().type(PArray.class).optional().build())
            .outputs(Info.argument().type(PMap.class).build())
            .build();

    /**
     * Control ID of the libraries-all control.
     */
    public static final String LIBRARIES_ALL = "libraries-all";

    /**
     * ControlInfo for the libraries-all control. The control is a function
     * taking no arguments and returning a list of all added libraries,
     * including any resolved dependencies.
     */
    public static final ControlInfo LIBRARIES_ALL_INFO = Info.control().function()
            .outputs(Info.argument().type(PMap.class).build())
            .build();

    /**
     * Control ID for the libraries-system control.
     */
    public static final String LIBRARIES_SYSTEM = "libraries-system";

    /**
     * ControlInfo for the libraries-system control. The control is a function
     * taking no arguments and returning a list of all libraries provided by the
     * CORE system the compiler service is running on.
     */
    public static final ControlInfo LIBRARIES_SYSTEM_INFO = LIBRARIES_ALL_INFO;

    /**
     * Control ID for the libraries-path control.
     */
    public static final String LIBRARIES_PATH = "libraries-path";

    /**
     * ControlInfo for the libraries-path control. The control is a function
     * taking no arguments and returning a list of all file paths (eg.
     * classpath) corresponding to all the libraries and dependencies.
     */
    public static final ControlInfo LIBRARIES_PATH_INFO = LIBRARIES_ALL_INFO;

    /**
     * Control ID for the options control.
     */
    public static final String OPTIONS = "options";

    /**
     * ControlInfo for the options control. The control is a function taking an
     * optional map of compiler configurations to use. Only the {@code release}
     * key is required to be supported if this control is made available,
     * corresponding to the {@code --release} option of the Java compiler.
     */
    public static final ControlInfo OPTIONS_INFO = Info.control().function()
            .inputs(Info.argument().type(PMap.class).optional().build())
            .outputs(Info.argument().type(PMap.class).build())
            .build();

    /**
     * A component info for this protocol. Can be used with
     * {@link Info.ComponentInfoBuilder#merge(org.praxislive.core.ComponentInfo)}.
     */
    public static final ComponentInfo API_INFO = Info.component(cmp -> cmp
            .protocol(CodeCompilerService.class)
            .control(COMPILE, COMPILE_INFO)
    );

    /**
     * A component info for this protocol, including all optional control. Can
     * be used with
     * {@link Info.ComponentInfoBuilder#merge(org.praxislive.core.ComponentInfo)}.
     */
    public static final ComponentInfo FULL_API_INFO = Info.component(cmp -> cmp
            .protocol(CodeCompilerService.class)
            .control(COMPILE, COMPILE_INFO)
            .control(LIBRARIES, LIBRARIES_INFO)
            .control(LIBRARIES_ALL, LIBRARIES_ALL_INFO)
            .control(LIBRARIES_PATH, LIBRARIES_PATH_INFO)
            .control(LIBRARIES_SYSTEM, LIBRARIES_SYSTEM_INFO)
            .control(OPTIONS, OPTIONS_INFO)
    );

    @Override
    public Stream<String> controls() {
        return Stream.of(COMPILE);
    }

    @Override
    public Stream<String> optionalControls() {
        return Stream.of(LIBRARIES, LIBRARIES_ALL, LIBRARIES_PATH, LIBRARIES_SYSTEM, OPTIONS);
    }

    @Override
    public ControlInfo getControlInfo(String control) {
        return switch (control) {
            case COMPILE ->
                COMPILE_INFO;
            case LIBRARIES ->
                LIBRARIES_INFO;
            case LIBRARIES_ALL ->
                LIBRARIES_ALL_INFO;
            case LIBRARIES_PATH ->
                LIBRARIES_PATH_INFO;
            case LIBRARIES_SYSTEM ->
                LIBRARIES_SYSTEM_INFO;
            case OPTIONS ->
                OPTIONS_INFO;
            default ->
                throw new IllegalStateException("Unexpected value: " + (control));
        };
    }
}
