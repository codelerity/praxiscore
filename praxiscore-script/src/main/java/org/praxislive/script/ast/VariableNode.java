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
package org.praxislive.script.ast;

import java.util.List;
import org.praxislive.core.Value;
import org.praxislive.script.Namespace;
import org.praxislive.script.Variable;

/**
 *
 *
 */
public class VariableNode extends Node {

    private String id;
    private Namespace namespace;

    public VariableNode(String id) {
        if (id == null) {
            throw new NullPointerException();
        }
        this.id = id;
    }

    @Override
    public void init(Namespace namespace) {
        super.init(namespace);
        this.namespace = namespace;
    }

    @Override
    public void reset() {
        super.reset();
        this.namespace = null;
    }

    @Override
    public void writeResult(List<Value> args) throws Exception {
        if (namespace == null) {
            throw new IllegalStateException();
        }
        Variable var = namespace.getVariable(id);
        if (var == null) {
            throw new Exception("Can't find variable " + id + " in namespace " + namespace);
        }
        args.add(var.getValue());
    }

}
