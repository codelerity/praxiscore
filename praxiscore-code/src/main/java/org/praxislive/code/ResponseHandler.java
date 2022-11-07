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
package org.praxislive.code;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.praxislive.code.userapi.Async;
import org.praxislive.core.Call;
import org.praxislive.core.Control;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.PacketRouter;
import org.praxislive.core.Value;
import org.praxislive.core.services.LogLevel;
import org.praxislive.core.types.PError;

/**
 *
 */
class ResponseHandler extends ControlDescriptor implements Control {

    static final String ID = "_reply";
    private static final PError UNKNOWN_ERROR = PError.of("Unknown error");

    private final Map<Integer, AsyncReference> resultMap;

    private CodeContext<?> context;

    ResponseHandler(int index) {
        super(ID, Category.Internal, index);
        this.resultMap = new HashMap<>();
    }

    @Override
    public void attach(CodeContext<?> context, Control previous) {
        if (previous instanceof ResponseHandler) {
            ResponseHandler prev = (ResponseHandler) previous;
            resultMap.putAll(prev.resultMap);
            prev.resultMap.clear();
        }
        this.context = context;
    }

    @Override
    public void call(Call call, PacketRouter router) throws Exception {
        if (call.isReply()) {
            var async = retrieveAsync(call.matchID());
            if (async != null) {
                async.complete(call);
            }
        } else if (call.isError()) {
            var error = extractError(call.args());
            var async = retrieveAsync(call.matchID());
            if (async != null) {
                async.fail(error);
            } else {
                context.getLog().log(LogLevel.ERROR, error);
            }
        } else if (call.isReplyRequired()) {
            router.route(call.error(PError.of("Unexpected call")));
        }
        cleanResultMap();
    }

    @Override
    public Control getControl() {
        return this;
    }

    @Override
    public ControlInfo getInfo() {
        return null;
    }

    @Override
    public void dispose() {
        resultMap.forEach((id, ref) -> {
            Async<?> async = ref.get();
            if (async != null) {
                async.fail(PError.of("Disposed"));
            }
        });
        resultMap.clear();
    }

    void register(int id, Async<Call> async) {
        cleanResultMap();
        resultMap.put(id, new AsyncReference(async));
    }

    private void cleanResultMap() {
        resultMap.entrySet().removeIf(e -> e.getValue().get() == null);
    }

    private Async<Call> retrieveAsync(int id) {
        var ref = resultMap.remove(id);
        if (ref != null) {
            return ref.get();
        } else {
            return null;
        }
    }

    private PError extractError(List<Value> args) {
        if (args.isEmpty()) {
            return UNKNOWN_ERROR;
        } else {
            return PError.from(args.get(0))
                    .orElse(PError.of(args.get(0).toString()));
        }
    }

    private static class AsyncReference extends WeakReference<Async<Call>> {

        AsyncReference(Async<Call> referent) {
            super(referent);
        }

    }

}
