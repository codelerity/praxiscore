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
package org.praxislive.script;

import java.util.Objects;
import org.praxislive.core.Value;

/**
 * Default constant implementation used by
 * {@link Namespace#createConstant(java.lang.String, org.praxislive.core.Value)}.
 *
 */
final class ConstantImpl implements Variable {

    private final Value value;

    public ConstantImpl(Value value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public void setValue(Value value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

}
