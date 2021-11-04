/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Neil C Smith.
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
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PNumber;

/**
 *
 */
public class SystemManagerService implements Service {
    
    public final static String SYSTEM_EXIT = "system-exit";
    
    public final static ControlInfo SYSTEM_EXIT_INFO
            = Info.control(c -> c.function()
                .inputs(i -> i.type(PNumber.class)
                    .property(PNumber.KEY_IS_INTEGER, PBoolean.TRUE)
                    .property(ArgumentInfo.KEY_OPTIONAL, PBoolean.TRUE)
                )
            );
    
    @Override
    public Stream<String> controls() {
        return Stream.of(SYSTEM_EXIT);
    }
    
    @Override
    public ControlInfo getControlInfo(String control) {
        if (SYSTEM_EXIT.equals(control)) {
            return SYSTEM_EXIT_INFO;
        }
        throw new IllegalArgumentException();
    }
    
}
