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
package org.praxislive.tinkerforge;

import org.praxislive.code.CodeContext;
import org.praxislive.code.ControlDescriptor;
import org.praxislive.core.Call;
import org.praxislive.core.Control;
import org.praxislive.core.PacketRouter;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PMap;

/**
 *
 */
class ConnectedProperty implements Control {
    
    private final static ControlInfo INFO =
            ControlInfo.createReadOnlyPropertyInfo(
                    PBoolean.info(), PMap.EMPTY);
    
    private TFCodeContext context;
    
    @Override
    public void call(Call call, PacketRouter router) throws Exception {
        if (call.isReplyRequired()) {
            router.route(call.reply(context.isConnected()));
        }
    }

//    @Override
    public ControlInfo getInfo() {
        return INFO;

    }

    public static class Descriptor extends ControlDescriptor<Descriptor> {

        private final ConnectedProperty control;

        public Descriptor(String id, int index) {
            super(Descriptor.class, id, ControlDescriptor.Category.Property, index);
            control = new ConnectedProperty();
        }

        @Override
        public ControlInfo controlInfo() {
            return control.getInfo();
        }

        @Override
        public void attach(CodeContext<?> context, Descriptor previous) {
            control.context = (TFCodeContext) context;
        }

        @Override
        public Control control() {
            return control;
        }

    }
    
}
