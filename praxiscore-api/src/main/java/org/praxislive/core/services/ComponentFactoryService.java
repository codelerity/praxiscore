/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2023 Neil C Smith.
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

import java.util.List;
import java.util.stream.Stream;
import org.praxislive.core.Component;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PReference;

/**
 * A {@link Service} for creating new component instances. The implementation of
 * this service will discover all available {@link ComponentFactory} and either
 * create an instance of the component via
 * {@link ComponentFactory#createComponent(org.praxislive.core.ComponentType)}
 * or delegate creation to the correct
 * {@link ComponentFactory#componentRedirect()}.
 */
public class ComponentFactoryService implements Service {

    /**
     * Control ID of the new instance control.
     */
    public final static String NEW_INSTANCE = "new-instance";

    /**
     * ControlInfo for the new instance control.
     */
    public final static ControlInfo NEW_INSTANCE_INFO
            = ControlInfo.createFunctionInfo(
                    List.of(ComponentType.info()),
                    List.of(PReference.info(Component.class)),
                    PMap.EMPTY);

    @Override
    public Stream<String> controls() {
        return Stream.of(NEW_INSTANCE);
    }

    @Override
    public ControlInfo getControlInfo(String control) {
        if (NEW_INSTANCE.equals(control)) {
            return NEW_INSTANCE_INFO;
        }
        throw new IllegalArgumentException();
    }
}
