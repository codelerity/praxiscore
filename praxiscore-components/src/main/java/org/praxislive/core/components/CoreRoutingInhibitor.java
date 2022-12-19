/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2022 Neil C Smith.
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
package org.praxislive.core.components;

import org.praxislive.code.GenerateTemplate;

import org.praxislive.core.code.CoreCodeDelegate;

// default imports
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import org.praxislive.core.*;
import org.praxislive.core.types.*;
import org.praxislive.code.userapi.*;
import static org.praxislive.code.userapi.Constants.*;

/**
 *
 * 
 */
@GenerateTemplate(CoreRoutingInhibitor.TEMPLATE_PATH)
public class CoreRoutingInhibitor extends CoreCodeDelegate {
    
    final static String TEMPLATE_PATH = "resources/routing_inhibitor.pxj";

    // PXJ-BEGIN:body

    @P(1) @Type.Number(min = 0, max = 60)
    double time;
    @P(100)
    Property _timer;
    
    @Out(1) Output out;

    @Override
    public void stopping() {
        _timer.set(0);
    }
    
    @In(1) void in(Value arg) {
        if (_timer.isAnimating() && D(_timer) < time) {
            return;
        }
        _timer.set(0).to(100).in(100);
        out.send(arg);
    }
    
    // PXJ-END:body
    
}
