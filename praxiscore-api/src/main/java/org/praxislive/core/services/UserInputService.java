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
package org.praxislive.core.services;

import java.util.stream.Stream;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.Info;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PString;

/**
 * A Service for obtaining user input. All the controls take a message to show
 * the user and return the input entered by the user. Implementations return an
 * error call if the user cancels input. The response should be validated by the
 * caller.
 */
public class UserInputService implements Service {

    /**
     * Function control to request user text input. The control accepts up to 3
     * arguments - the message to show the user, an optional default value to
     * pre-fill the input, and an optional {@link ArgumentInfo} describing the
     * required input. The control returns the value entered by the user.
     * <p>
     * For multi-line input, use an ArgumentInfo with
     * {@link PString#KEY_MIME_TYPE}, or provide a multi-line default value. A
     * single newline may be provided as an empty default for multi-line input.
     */
    public static final String USER_INPUT = "user-input";

    /**
     * Control info for the {@link #USER_INPUT} control.
     */
    public static final ControlInfo USER_INPUT_INFO = Info.control(c -> c.function()
            .inputs(
                    a -> a.string(),
                    a -> a.type(Value.class)
                            .attribute(ArgumentInfo.KEY_OPTIONAL, true),
                    a -> a.type(ArgumentInfo.class)
                            .attribute(ArgumentInfo.KEY_OPTIONAL, true)
            )
            .outputs(a -> a.type(Value.class))
    );

    /**
     * Function control to request a Yes/No response from the user. The control
     * accepts a message to show the user, and returns a boolean value - true
     * for yes, false for no. Cancellation is treated as distinct from a no
     * input.
     */
    public static final String USER_INPUT_CONFIRM = "user-input-confirm";

    /**
     * Control info for the {@link #USER_INPUT_CONFIRM} control.
     */
    public static final ControlInfo USER_INPUT_CONFIRM_INFO = Info.control(c -> c.function()
            .inputs(a -> a.string())
            .outputs(a -> a.type(PBoolean.class))
    );

    /**
     * Function control to request the user fill out values for each key of a
     * map. The control accepts a message to show the user, and a map containing
     * the required keys and default values. The response will be a map with the
     * keys and the user response for each key.
     */
    public static final String USER_INPUT_MAP = "user-input-map";

    /**
     * Control info for the {@link #USER_INPUT_MAP} control.
     */
    public static final ControlInfo USER_INPUT_MAP_INFO = Info.control(c -> c.function()
            .inputs(
                    a -> a.string(),
                    a -> a.type(PMap.class)
            )
            .outputs(a -> a.type(PMap.class))
    );

    /**
     * Function control to request the user select from a range of values. The
     * control accepts a message and an array of possible responses. The
     * response will be the array value selected by the user.
     */
    public static final String USER_INPUT_SELECT = "user-input-select";

    /**
     * Control info for the {@link #USER_INPUT_SELECT} control.
     */
    public static final ControlInfo USER_INPUT_SELECT_INFO = Info.control(c -> c.function()
            .inputs(
                    a -> a.string(),
                    a -> a.type(PArray.class)
            )
            .outputs(a -> a.type(Value.class))
    );

    @Override
    public Stream<String> controls() {
        return Stream.of(USER_INPUT, USER_INPUT_CONFIRM, USER_INPUT_MAP, USER_INPUT_SELECT);
    }

    @Override
    public ControlInfo getControlInfo(String control) {
        return switch (control) {
            case USER_INPUT ->
                USER_INPUT_INFO;
            case USER_INPUT_CONFIRM ->
                USER_INPUT_CONFIRM_INFO;
            case USER_INPUT_MAP ->
                USER_INPUT_MAP_INFO;
            case USER_INPUT_SELECT ->
                USER_INPUT_SELECT_INFO;
            default ->
                throw new IllegalArgumentException();
        };
    }

}
