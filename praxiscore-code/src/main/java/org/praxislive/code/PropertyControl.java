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
 *
 */
package org.praxislive.code;

import java.util.List;
import org.praxislive.code.userapi.Config;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.praxislive.code.userapi.Inject;
import org.praxislive.code.userapi.OnChange;
import org.praxislive.code.userapi.OnError;
import org.praxislive.code.userapi.P;
import org.praxislive.code.userapi.Property;
import org.praxislive.code.userapi.ReadOnly;
import org.praxislive.code.userapi.Transient;
import org.praxislive.code.userapi.Type;
import org.praxislive.core.Call;
import org.praxislive.core.Control;
import org.praxislive.core.PacketRouter;
import org.praxislive.core.Port;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.PortInfo;
import org.praxislive.core.TreeWriter;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PNumber;
import org.praxislive.core.types.PString;
import org.praxislive.core.Value;
import org.praxislive.core.services.LogLevel;

/**
 *
 */
public class PropertyControl extends Property implements Control {

    private final ControlInfo info;
    private final Binding binding;
    private final Method onChange;
    private final Method onError;
    private final boolean readOnly;

    private CodeContext<?> context;

    private boolean latestSet;
    private long latest;

    private PropertyControl(ControlInfo info,
            Binding binding,
            Method onChange,
            Method onError) {
        this.info = info;
        this.binding = binding;
        this.readOnly = info == null
                || info.controlType() == ControlInfo.Type.ReadOnlyProperty;
        this.onChange = onChange;
        this.onError = onError;
    }

    @Override
    protected Value getImpl() {
        return binding.get();
    }

    @Override
    protected double getImpl(double def) {
        return binding.get(def);
    }

    @Override
    protected void setImpl(long time, Value arg) throws Exception {
        binding.set(arg);
        setLatest(time);
        if (hasLinks()) {
            updateLinks(arg);
            context.flush();
        }
    }

    @Override
    protected void setImpl(long time, double value) throws Exception {
        binding.set(value);
        setLatest(time);
        if (hasLinks()) {
            updateLinks(value);
            context.flush();
        }
    }

    private void checkInvoke(long time, boolean error) {
        if (error) {
            if (onError != null) {
                context.invoke(time, onError);
            }
        } else {
            if (onChange != null) {
                context.invoke(time, onChange);
            }
        }
    }

    private void attach(CodeContext<?> context, PropertyControl previous) {
        this.context = context;
        if (previous != null) {
            binding.attach(context, previous.binding);
            latest = previous.latest;
            latestSet = previous.latestSet;
            try {
                binding.set(previous.binding.get());
            } catch (Exception ex) {
                // do nothing?
            }
            super.attach(context, (Property) previous);
        } else {
            binding.attach(context, null);
            super.attach(context, null);
        }
    }

    @Override
    public void call(Call call, PacketRouter router) throws Exception {
        if (call.isRequest()) {
            List<Value> args = call.args();
            int argCount = args.size();
            long time = call.time();
            if (argCount > 0 && !readOnly) {
                if (isLatest(time)) {
                    finishAnimating();
                    try {
                        setImpl(time, (Value) args.get(0));
                        checkInvoke(time, false);
                    } catch (Exception ex) {
                        checkInvoke(time, true);
                        throw ex;
                    }
                }
                if (call.isReplyRequired()) {
                    router.route(call.reply(args));
                }
            } else {
                // ignore quiet hint?
                router.route(call.reply(get()));
            }
        }
    }

    private void setLatest(long time) {
        latestSet = true;
        latest = time;
    }

    private boolean isLatest(long time) {
        if (latestSet) {
            return (time - latest) >= 0;
        } else {
            return true;
        }

    }

    public static abstract class Binding {

        protected void attach(CodeContext<?> context) {
            // no op hook
        }

        protected void attach(CodeContext<?> context, Binding previous) {
            attach(context);
        }

        protected void reset(boolean full) {
            // no op hook
        }

        public abstract void set(Value value) throws Exception;

        public abstract void set(double value) throws Exception;

        public abstract Value get();

        public double get(double def) {
            return PNumber.from(get()).orElse(PNumber.of(def)).value();
        }

        public abstract ArgumentInfo getArgumentInfo();

        public abstract Value getDefaultValue();

    }

    private static class DefaultBinding extends Binding {

        private final ArgumentInfo argInfo;
        private final Value def;

        private Value argValue;
        private double dblValue;

        private DefaultBinding() {
            this(Value.info(), PString.EMPTY);
        }

        private DefaultBinding(ArgumentInfo argInfo, Value def) {
            this.argInfo = argInfo;
            this.def = def;
            this.argValue = def;
        }

        @Override
        public void set(Value value) throws Exception {
            this.argValue = value;
        }

        @Override
        public void set(double value) throws Exception {
            this.dblValue = value;
            this.argValue = null;
        }

        @Override
        public Value get() {
            if (argValue == null) {
                return PNumber.of(dblValue);
            } else {
                return argValue;
            }
        }

        @Override
        public double get(double def) {
            if (argValue == null) {
                return dblValue;
            } else {
                return super.get(def);
            }
        }

        @Override
        public ArgumentInfo getArgumentInfo() {
            return argInfo;
        }

        @Override
        public Value getDefaultValue() {
            return def;
        }

    }

    public static class Descriptor extends ControlDescriptor<Descriptor> {

        private final PropertyControl control;
        private final Field propertyField;
        private final boolean synthetic;
        private final boolean writable;

        private Descriptor(String id,
                int index,
                ControlInfo info,
                Binding binding,
                Field field,
                Method onChange,
                Method onError
        ) {
            super(Descriptor.class, id, Category.Property, index);
            control = new PropertyControl(info, binding, onChange, onError);
            propertyField = field;
            synthetic = false;
            if (info.controlType() == ControlInfo.Type.ReadOnlyProperty
                    || info.properties().getBoolean(ControlInfo.KEY_TRANSIENT, false)) {
                writable = false;
            } else {
                writable = true;
            }
        }

        private Descriptor(String id, int index, Binding binding, Field field) {
            super(Descriptor.class, id, Category.Synthetic, index);
            control = new PropertyControl(null, binding, null, null);
            propertyField = field;
            synthetic = true;
            writable = false;
        }

        @Override
        public ControlInfo controlInfo() {
            return control.info;
        }

        @Override
        public void attach(CodeContext<?> context, Descriptor previous) {
            if (previous != null) {
                control.attach(context, previous.control);
            } else {
                control.attach(context, null);
            }
            if (propertyField != null) {
                try {
                    propertyField.set(context.getDelegate(), control);
                } catch (Exception ex) {
                    context.getLog().log(LogLevel.ERROR, ex);
                }
            }
        }

        @Override
        public Control control() {
            return control;
        }

        public PortDescriptor createPortDescriptor() {
            return new PortDescImpl(id(), index(), control);
        }

        @Override
        public void onReset() {
            control.reset();
        }

        @Override
        public void onStop() {
            control.finishAnimating();
            if (synthetic) {
                try {
                    control.binding.set(control.binding.getDefaultValue());
                } catch (Exception ex) {
                    control.context.getLog().log(LogLevel.ERROR, ex);
                }
            }
        }

        @Override
        public void write(TreeWriter writer) {
            if (writable) {
                Value val = control.get();
                Value def = control.binding.getDefaultValue();
                if (val != null && !val.equals(def)) {
                    writer.writeProperty(id(), val);
                }
            }
        }

        public static Descriptor create(CodeConnector<?> connector,
                P ann, Field field) {
            Binding binding = findBinding(connector, field);
            if (binding == null) {
                return null;
            }
            return create(connector, ann.value(), field, binding);
        }

        private static Descriptor create(CodeConnector<?> connector, int index,
                Field field, Binding binding) {
            field.setAccessible(true);
            String id = connector.findID(field);
            Method onChange = null;
            Method onError = null;
            OnChange onChangeAnn = field.getAnnotation(OnChange.class);
            if (onChangeAnn != null) {
                onChange = extractMethod(connector, onChangeAnn.value());
            }
            OnError onErrorAnn = field.getAnnotation(OnError.class);
            if (onErrorAnn != null) {
                onError = extractMethod(connector, onErrorAnn.value());
            }
            Field propertyField = null;
            if (Property.class.isAssignableFrom(field.getType())) {
                propertyField = field;
            }
            ControlInfo info;
            if (field.isAnnotationPresent(ReadOnly.class)) {
                info = ControlInfo.createReadOnlyPropertyInfo(
                        List.of(binding.getArgumentInfo()),
                        PMap.EMPTY);
            } else {
                info = ControlInfo.createPropertyInfo(
                        binding.getArgumentInfo(),
                        binding.getDefaultValue(),
                        buildProperties(field));
            }
            if (info.properties().getBoolean("preferred", false)) {
                connector.exposeForPreferred(id);
            }
            return new Descriptor(id, index, info, binding, propertyField, onChange, onError);
        }

        private static PMap buildProperties(Field field) {
            PMap.Builder builder = PMap.builder();
            if (field.isAnnotationPresent(Transient.class)) {
                builder.put(ControlInfo.KEY_TRANSIENT, true);
            }
            if (field.isAnnotationPresent(Config.Preferred.class)) {
                builder.put("preferred", true);
            }
            return builder.build();
        }

        static Descriptor create(CodeConnector<?> connector, Inject ann, Field field) {
            Binding binding = findBinding(connector, field);
            if (binding == null) {
                return null;
            }
            field.setAccessible(true);
            String id = connector.findID(field);
            int index = connector.getSyntheticIndex();
            Field propertyField = null;
            if (Property.class.isAssignableFrom(field.getType())) {
                propertyField = field;
            }
            return new Descriptor(id, index, binding, propertyField);
        }

        @SuppressWarnings("deprecation")
        private static Binding findBinding(CodeConnector<?> connector, Field field) {
            Class<?> type = field.getType();
            Binding binding = null;
            if (field.isAnnotationPresent(Type.Number.class)
                    || NumberBinding.isBindableFieldType(type)) {
                binding = NumberBinding.create(connector, field);
            } else if (field.isAnnotationPresent(Type.Integer.class)
                    || IntegerBinding.isBindableFieldType(type)) {
                binding = IntegerBinding.create(connector, field);
            } else if (field.isAnnotationPresent(Type.String.class)
                    || StringBinding.isBindableFieldType(type)) {
                binding = StringBinding.create(connector, field);
            } else if (field.isAnnotationPresent(Type.Boolean.class)
                    || BooleanBinding.isBindableFieldType(type)) {
                binding = BooleanBinding.create(connector, field);
            } else if (ValueBinding.isBindableFieldType(type)) {
                binding = ValueBinding.create(connector, field);
            }

            if (binding == null && Property.class.isAssignableFrom(type)) {
                Type typeAnn = field.getAnnotation(Type.class);
                if (typeAnn != null) {
                    binding = new DefaultBinding(
                            connector.infoFromType(typeAnn),
                            connector.defaultValueFromType(typeAnn));
                } else {
                    binding = new DefaultBinding();
                }
            }

            return binding;
        }

        private static Method extractMethod(CodeConnector<?> connector, String methodName) {
            try {
                Method m = connector.getDelegate().getClass().getDeclaredMethod(methodName);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ex) {

            } catch (Exception ex) {

            }
            return null;
        }

    }

    private static class PortDescImpl extends PortDescriptor<PortDescImpl> implements ControlInput.Link {

        private final PropertyControl control;

        private ControlInput port;

        private PortDescImpl(String id, int index, PropertyControl control) {
            super(PortDescImpl.class, id, Category.Property, index);
            this.control = control;
        }

        @Override
        public void attach(CodeContext<?> context, PortDescImpl previous) {
            if (previous != null) {
                port = previous.port;
                port.setLink(this);
            } else {
                port = new ControlInput(this);
            }
        }

        @Override
        public Port port() {
            assert port != null;
            return port;
        }

        @Override
        public PortInfo portInfo() {
            return ControlInput.INFO;
        }

        @Override
        public void receive(long time, double value) {
            try {
                control.setImpl(time, value);
                control.checkInvoke(time, false);
            } catch (Exception ex) {
                control.checkInvoke(time, true);
                // @TODO log to user
            }
        }

        @Override
        public void receive(long time, Value value) {
            try {
                control.setImpl(time, (Value) value);
                control.checkInvoke(time, false);
            } catch (Exception ex) {
                control.checkInvoke(time, true);
                // @TODO log to user
            }
        }

    }

}
