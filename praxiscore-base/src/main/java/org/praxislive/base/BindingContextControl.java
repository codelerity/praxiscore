/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2019 Neil C Smith.
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
package org.praxislive.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import org.praxislive.core.Call;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.PacketRouter;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Control;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.ExecutionContext;
import org.praxislive.core.Value;
import org.praxislive.core.protocols.ComponentProtocol;

/**
 * An implementation of BindingContext based around a single Control. A
 * container just needs to provide an instance as a (hidden) control, and make
 * it available via Lookup.
 */
public class BindingContextControl implements Control, BindingContext {

    private final static System.Logger LOG
            = System.getLogger(BindingContextControl.class.getName());
    private static final long LOW_SYNC_DELAY = TimeUnit.MILLISECONDS.toNanos(1000);
    private static final long MED_SYNC_DELAY = TimeUnit.MILLISECONDS.toNanos(200);
    private static final long HIGH_SYNC_DELAY = TimeUnit.MILLISECONDS.toNanos(50);
    private static final long INVOKE_TIMEOUT = TimeUnit.MILLISECONDS.toNanos(5000);
    private static final long QUIET_TIMEOUT = TimeUnit.MILLISECONDS.toNanos(200);

    private final ExecutionContext context;
    private final PacketRouter router;
    private final ControlAddress controlAddress;
    private final Map<ControlAddress, BindingImpl> bindings;
    private final BindingSyncQueue syncQueue;

    /**
     * Create a BindingContextControl.
     *
     * @param controlAddress address of the control for sending and receiving
     * all messages
     * @param context the execution context (required for sync clock)
     * @param router the router for sending all messages
     */
    public BindingContextControl(
            ControlAddress controlAddress,
            ExecutionContext context,
            PacketRouter router
    ) {
        this.controlAddress = Objects.requireNonNull(controlAddress);
        this.context = Objects.requireNonNull(context);
        this.router = Objects.requireNonNull(router);
        bindings = new LinkedHashMap<>();
        syncQueue = new BindingSyncQueue(context.getTime());
        context.addClockListener(this::tick);
    }

    @Override
    public void bind(ControlAddress address, Binding.Adaptor adaptor) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(adaptor);
        BindingImpl binding = bindings.computeIfAbsent(address, BindingImpl::new);
        binding.addAdaptor(adaptor);
    }

    @Override
    public void unbind(ControlAddress address, Binding.Adaptor adaptor) {
        BindingImpl binding = bindings.get(address);
        if (binding != null) {
            binding.removeAdaptor(adaptor);
            if (binding.isEmpty()) {
                bindings.remove(address);
            }
        }
    }

    @Override
    public void call(Call call, PacketRouter router) throws Exception {
        if (call.isReply() || call.isError()) {
            if (call.from().controlID().equals(ComponentProtocol.INFO)) {
                ComponentAddress infoOf = call.from().component();
                bindings.forEach((a, b) -> {
                    if (infoOf.equals(a.component())) {
                        b.process(call);
                    }
                });
            } else {
                var binding = bindings.get(call.from());
                if (binding != null) {
                    binding.process(call);
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }

    }

    private void tick(ExecutionContext source) {
        syncQueue.setTime(source.getTime());
        BindingImpl b;
        while ((b = syncQueue.poll()) != null) {
            b.processSync();
        }
    }

    private static class BindingSyncQueue {

        private final PriorityQueue<BindingSyncElement> q;
        private long time;

        private BindingSyncQueue(long time) {
            q = new PriorityQueue<>();
            this.time = time;
        }

        private void setTime(long time) {
            this.time = time;
        }

        private void add(BindingImpl binding, long time) {
            q.add(new BindingSyncElement(binding, time));
        }

        private BindingImpl poll() {
            if (!q.isEmpty() && q.peek().time - time <= 0) {
                var element = q.poll();
                return element != null ? element.binding : null;
            }
            return null;
        }

    }
    
    private static class BindingSyncElement
            implements Comparable<BindingSyncElement> {
        
        private final BindingImpl binding;
        private final long time;
        
        private BindingSyncElement(BindingImpl binding, long time) {
            this.binding = binding;
            this.time = time;
        }

        @Override
        public int compareTo(BindingSyncElement o) {
            long timeDiff = time - o.time;
            if (timeDiff == 0) {
                return hashCode() - o.hashCode();
            }
            return timeDiff < 0 ? -1 : 1;
        }
        
    }

    private class BindingImpl extends Binding {

        private final List<Binding.Adaptor> adaptors;
        private final ControlAddress boundAddress;
        private ControlInfo bindingInfo;
        private long syncPeriod;

//        private int lastCallID;
        private int infoMatchID;
        private boolean isProperty;
        private Call activeCall;
        private Adaptor activeAdaptor;
        private List<Value> values;

        private BindingImpl(ControlAddress boundAddress) {
            adaptors = new ArrayList<>();
            this.boundAddress = boundAddress;
            values = Collections.emptyList();
        }

        private void addAdaptor(Adaptor adaptor) {
            if (adaptor == null) {
                throw new NullPointerException();
            }
            if (adaptors.contains(adaptor)) {
                return;
            }
            adaptors.add(adaptor);
            bind(adaptor);
            updateAdaptorConfiguration(adaptor); // duplicate functionality
            if (bindingInfo == null) {
                sendInfoRequest();
            }
        }

        private void removeAdaptor(Adaptor adaptor) {
            if (adaptors.remove(adaptor)) {
                unbind(adaptor);
            }
            updateSyncConfiguration();
        }

        private boolean isEmpty() {
            return adaptors.isEmpty();
        }

//        private void removeAll() {
//            Iterator<Adaptor> itr = adaptors.iterator();
//            while (itr.hasNext()) {
//                Adaptor adaptor = itr.next();
//                unbind(adaptor);
//                itr.remove();
//            }
//            updateSyncConfiguration();
//        }
        @Override
        protected void send(Adaptor adaptor, List<Value> args) {
            Call call;
            if (adaptor.getValueIsAdjusting()) {
                call = Call.createQuiet(boundAddress, controlAddress,
                        context.getTime(), args);
            } else {
                call = Call.create(boundAddress, controlAddress,
                        context.getTime(), args);
            }
            router.route(call);
            activeCall = call;
            activeAdaptor = adaptor;
            values = args;
            for (Adaptor ad : adaptors) {
                if (ad != adaptor) {
                    ad.update();
                }
            }
        }

        @Override
        protected void updateAdaptorConfiguration(Adaptor adaptor) {
            updateSyncConfiguration();
        }

        private void updateSyncConfiguration() {
            if (isProperty) {
                LOG.log(System.Logger.Level.DEBUG, "Updating sync configuration on {0}", boundAddress);
                boolean active = false;
                SyncRate highRate = SyncRate.None;
                for (Adaptor a : adaptors) {
                    if (a.isActive()) {
                        active = true;
                        SyncRate aRate = a.getSyncRate();
                        if (aRate.compareTo(highRate) > 0) {
                            highRate = aRate;
                        }
                    }
                }
                if (!active || highRate == SyncRate.None) {
                    syncPeriod = 0;
                } else {
                    syncPeriod = delayForRate(highRate);
                    processSync();
                }
            } else {
                syncPeriod = 0;
            }

        }

        private long delayForRate(SyncRate rate) {
            switch (rate) {
                case Low:
                    return LOW_SYNC_DELAY;
                case Medium:
                    return MED_SYNC_DELAY;
                case High:
                    return HIGH_SYNC_DELAY;
            }
            throw new IllegalArgumentException();
        }

        private void sendInfoRequest() {
            ControlAddress toAddress = ControlAddress.of(boundAddress.component(),
                    ComponentProtocol.INFO);
            Call call = Call.create(toAddress, controlAddress, context.getTime());
            infoMatchID = call.matchID();
            router.route(call);
        }

        private void processInfo(Call call) {
            if (call.matchID() == infoMatchID) {
                List<Value> args = call.args();
                if (args.size() > 0) {
                    ComponentInfo compInfo = null;
                    try {
                        compInfo = ComponentInfo.from(args.get(0)).get();
                        // @TODO on null?
                        bindingInfo = compInfo.controlInfo(boundAddress.controlID());
                        ControlInfo.Type type = bindingInfo.controlType();
                        isProperty = (type == ControlInfo.Type.Property)
                                || (type == ControlInfo.Type.ReadOnlyProperty);

                    } catch (Exception ex) {
                        isProperty = false;
                        bindingInfo = null;
                        LOG.log(System.Logger.Level.WARNING, "" + call + "\n" + compInfo, ex);
                    }
                    for (Adaptor a : adaptors) {
                        a.updateBindingConfiguration();
                    }
                    updateSyncConfiguration();
                }
            }
        }

        private void processInfoError(Call call) {
            isProperty = false;
            bindingInfo = null;
            LOG.log(System.Logger.Level.WARNING, "Couldn't get info for {0}", boundAddress);
            for (Adaptor a : adaptors) {
                a.updateBindingConfiguration();
            }
            updateSyncConfiguration();
        }

        private void process(Call call) {
            if (call.isReply()) {
                processResponse(call);
            } else if (call.isError()) {
                processError(call);
            }
        }

        private void processResponse(Call call) {
            if (activeCall != null && call.matchID() == activeCall.matchID()) {
                if (activeAdaptor != null) {
                    activeAdaptor.onResponse(call.args());
                    activeAdaptor = null;
                }
                if (isProperty) {
                    values = call.args();
                    for (Adaptor a : adaptors) {
                        a.update();
                    }
                }
                activeCall = null;
            } else if (call.matchID() == infoMatchID) {
                processInfo(call);
            }
        }

        private void processError(Call call) {
            if (activeCall != null && call.matchID() == activeCall.matchID()) {
                if (activeAdaptor != null) {
                    activeAdaptor.onError(call.args());
                    activeAdaptor = null;
                } else {
                    LOG.log(System.Logger.Level.DEBUG, "Error on sync call - {0}", call.from());
                }
                activeCall = null;
            } else if (call.matchID() == infoMatchID) {
                processInfoError(call);
            }
        }

        private void processSync() {
            long now = context.getTime();
            if (syncPeriod > 0) {
                syncQueue.add(this, now + syncPeriod);
            }

            if (activeCall != null) {
                if (activeCall.isReplyRequired()) {
                    if ((now - activeCall.time()) < INVOKE_TIMEOUT) {
                        return;
                    }
                } else {
                    if ((now - activeCall.time()) < QUIET_TIMEOUT) {
                        return;
                    }
                }
            }
            if (isProperty) {
                Call call = Call.create(boundAddress, controlAddress, now);
                router.route(call);
                activeCall = call;
                activeAdaptor = null;
            }

        }

        @Override
        public Optional<ControlInfo> getControlInfo() {
            return Optional.ofNullable(bindingInfo);
        }

        @Override
        public List<Value> getValues() {
            return values;
        }

    }
}
