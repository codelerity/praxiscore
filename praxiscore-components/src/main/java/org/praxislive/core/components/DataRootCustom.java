/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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

import org.praxislive.core.code.CoreRootContainerDelegate;

// default imports
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import org.praxislive.core.*;
import org.praxislive.core.types.*;
import org.praxislive.code.userapi.*;

import static org.praxislive.code.userapi.Constants.*;
import org.praxislive.core.code.DataRootContainerDelegate;

/**
 * Acts as a base type for a custom root supporting non-realtime safe behaviour.
 */
@GenerateTemplate(DataRootCustom.TEMPLATE_PATH)
public class DataRootCustom extends DataRootContainerDelegate {

    final static String TEMPLATE_PATH = "resources/root_data.pxj";

    // PXJ-BEGIN:body
    @Override
    @SupportedTypes(filter = "core:*|data:*")
    public void init() {

    }

    // PXJ-END:body
}
