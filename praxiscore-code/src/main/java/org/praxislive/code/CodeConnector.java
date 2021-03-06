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
 *
 */
package org.praxislive.code;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.praxislive.code.userapi.AuxIn;
import org.praxislive.code.userapi.AuxOut;
import org.praxislive.code.userapi.Config;
import org.praxislive.code.userapi.Data;
import org.praxislive.code.userapi.ID;
import org.praxislive.code.userapi.In;
import org.praxislive.code.userapi.Inject;
import org.praxislive.code.userapi.Out;
import org.praxislive.code.userapi.P;
import org.praxislive.code.userapi.Property;
import org.praxislive.code.userapi.ReadOnly;
import org.praxislive.code.userapi.Ref;
import org.praxislive.code.userapi.T;
import org.praxislive.code.userapi.Type;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Info;
import org.praxislive.core.Lookup;
import org.praxislive.core.Value;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PString;
import org.praxislive.core.services.LogBuilder;

/**
 * Base class for analysing a {@link CodeDelegate} and creating the resources
 * required for its wrapping {@link CodeContext}. An instance of a CodeConnector
 * subclass should be passed in to the CodeContext constructor.
 *
 * @param <D> type of CodeDelegate this connector works with
 */
public abstract class CodeConnector<D extends CodeDelegate> {

    // adapted from http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
    private final static Pattern idRegex = Pattern.compile(
            String.format("%s|%s|%s|%s",
                    "(?<=.)_",
                    "(?<=[A-Z])(?=[A-Z][a-z])",
                    "(?<=[^A-Z])(?=[A-Z])",
                    "(?<=[A-Za-z])(?=[^A-Za-z])"
            )
    );

    private final static List<Plugin> ALL_PLUGINS
            = Lookup.SYSTEM.findAll(Plugin.class).collect(Collectors.toList());

    private final CodeFactory<D> factory;
    private final LogBuilder log;
    private final D delegate;
    private final Map<ControlDescriptor.Category, Map<Integer, ControlDescriptor>> controls;
    private final Map<PortDescriptor.Category, Map<Integer, PortDescriptor>> ports;
    private final Map<String, ReferenceDescriptor> refs;

    private List<Plugin> plugins;
    private Map<String, ControlDescriptor> extControls;
    private Map<String, PortDescriptor> extPorts;
    private Map<String, ReferenceDescriptor> extRefs;
    private ComponentInfo info;
    private int syntheticIdx = Integer.MIN_VALUE;
    private int internalIdx = Integer.MIN_VALUE;
    private boolean hasPropertyField;

    /**
     * Create a CodeConnector for the provided delegate.
     *
     * @param task CodeFactory.Task factory task creating context
     * @param delegate delegate instance
     */
    public CodeConnector(CodeFactory.Task<D> task, D delegate) {
        this.factory = task.getFactory();
        this.log = task.getLog();
        this.delegate = delegate;
        controls = new EnumMap<>(ControlDescriptor.Category.class);
        ports = new EnumMap<>(PortDescriptor.Category.class);
        refs = new TreeMap<>();
    }

    /**
     * Process will be called by the CodeContext. Subclasses may override to
     * extend, but should ensure to call the superclass method.
     */
    protected void process() {
        plugins = ALL_PLUGINS.stream().filter(p -> p.isSupportedConnector(this))
                .collect(Collectors.toList());
        Class<? extends CodeDelegate> cls = delegate.getClass();
        analyseFields(cls.getDeclaredFields());
        analyseMethods(cls.getDeclaredMethods());
        addDefaultControls();
        addDefaultPorts();
        buildExternalData();
    }

    /**
     * Access the delegate instance.
     *
     * @return delegate
     */
    public D getDelegate() {
        return delegate;
    }

    /**
     * Get the {@link LogBuilder} for logging messages during processing.
     *
     * @return log builder
     */
    public LogBuilder getLog() {
        return log;
    }

    /**
     * Called by the CodeContext to access all processed control descriptors.
     * Subclasses may override to extend, but should ensure to call the
     * superclass method.
     *
     * @return map of control descriptors by ID
     */
    protected Map<String, ControlDescriptor> extractControls() {
        return extControls;
    }

    /**
     * Called by the CodeContext to access all processed port descriptors.
     * Subclasses may override to extend, but should ensure to call the
     * superclass method.
     *
     * @return map of port descriptors by ID
     */
    protected Map<String, PortDescriptor> extractPorts() {
        return extPorts;
    }

    /**
     * Called by the CodeContext to access all processed reference descriptors.
     * Subclasses may override to extend, but should ensure to call the
     * superclass method.
     *
     * @return map of reference descriptors by ID
     */
    protected Map<String, ReferenceDescriptor> extractRefs() {
        return extRefs;
    }

    /**
     * Called by the CodeContext to access the generated {@link ComponentInfo}
     * for the delegate.
     *
     * @return component info
     */
    protected ComponentInfo extractInfo() {
        return info;
    }

    /**
     * Called by the CodeContext to control whether the context should be
     * attached to the execution clock. This method returns true if the delegate
     * has any fields of type {@link Property}. May be overridden.
     *
     * @return whether context should connect to clock
     */
    protected boolean requiresClock() {
        return hasPropertyField;
    }

    private void buildExternalData() {
        extControls = buildExternalControlMap();
        extPorts = buildExternalPortMap();
        extRefs = buildExternalRefsMap();
        info = buildComponentInfo(extControls, extPorts);
    }

    private Map<String, ControlDescriptor> buildExternalControlMap() {
        Map<String, ControlDescriptor> map = new LinkedHashMap<>();
        for (Map<Integer, ControlDescriptor> cat : controls.values()) {
            for (ControlDescriptor cd : cat.values()) {
                map.put(cd.getID(), cd);
            }
        }
        return map;
    }

    private Map<String, PortDescriptor> buildExternalPortMap() {
        Map<String, PortDescriptor> map = new LinkedHashMap<>();
        for (Map<Integer, PortDescriptor> cat : ports.values()) {
            for (PortDescriptor pd : cat.values()) {
                map.put(pd.getID(), pd);
            }
        }
        return map;
    }

    private Map<String, ReferenceDescriptor> buildExternalRefsMap() {
        if (refs.isEmpty()) {
            return Collections.EMPTY_MAP;
        } else {
            return new LinkedHashMap(refs);
        }
    }

    /**
     * Called during processing to generate the component info from the control
     * and port maps. May be overridden to configure / extend.
     *
     * @param controls map of control IDs and descriptors
     * @param ports map of port IDs and descriptors
     * @return component info
     */
    protected ComponentInfo buildComponentInfo(Map<String, ControlDescriptor> controls,
            Map<String, PortDescriptor> ports) {
        var cmp = Info.component();
        cmp.merge(ComponentProtocol.API_INFO);
        for (var e : controls.entrySet()) {
            if (!excludeFromInfo(e.getKey(), e.getValue())) {
                cmp.control(e.getKey(), e.getValue().getInfo());
            }
        }
        for (var e : ports.entrySet()) {
            if (!excludeFromInfo(e.getKey(), e.getValue())) {
                cmp.port(e.getKey(), e.getValue().getInfo());
            }
        }
        cmp.property(ComponentInfo.KEY_DYNAMIC, true);
        cmp.property(ComponentInfo.KEY_COMPONENT_TYPE, factory.getComponentType());
        return cmp.build();
    }

    private boolean excludeFromInfo(String id, ControlDescriptor desc) {
        return desc.getInfo() == null || id.startsWith("_");
    }

    private boolean excludeFromInfo(String id, PortDescriptor desc) {
        return id.startsWith("_");
    }

    /**
     * Add a control descriptor.
     *
     * @param ctl control descriptor
     */
    public void addControl(ControlDescriptor ctl) {
        controls.computeIfAbsent(ctl.getCategory(), c -> new TreeMap<>()).put(ctl.getIndex(), ctl);
    }

    /**
     * Add a port descriptor.
     *
     * @param port port descriptor
     */
    public void addPort(PortDescriptor port) {
        ports.computeIfAbsent(port.getCategory(), c -> new TreeMap<>()).put(port.getIndex(), port);
    }

    /**
     * Add a reference descriptor.
     *
     * @param ref reference descriptor
     */
    public void addReference(ReferenceDescriptor ref) {
        refs.put(ref.getID(), ref);
    }

    /**
     * Called during processing to create default controls. May be overridden to
     * configure or extend. By default this method adds the info and code
     * properties, and a hidden control used by logging support.
     */
    protected void addDefaultControls() {
        addControl(createInfoControl(getInternalIndex()));
        addControl(createCodeControl(getInternalIndex()));
        addControl(new LogControl.Descriptor(getInternalIndex()));
    }

    /**
     * Called to create the info property control.
     *
     * @param index position of control
     * @return info control descriptor
     */
    protected ControlDescriptor createInfoControl(int index) {
        return new InfoProperty.Descriptor(index);
    }

    /**
     * Called to create the code property control.
     *
     * @param index position of control
     * @return code control descriptor
     */
    protected ControlDescriptor createCodeControl(int index) {
        return new CodeProperty.Descriptor<>(factory, index);
    }

    /**
     * Called during processing to create default ports. May be overridden to
     * extend. By default this method does nothing.
     */
    protected void addDefaultPorts() {
        // no op hook
    }

    /**
     * Called during processing to analyse all discovered fields in the delegate
     * class. May be overridden, but usually better to override the more
     * specific analysis methods.
     *
     * @param fields discovered fields
     */
    protected void analyseFields(Field[] fields) {
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.getType() == Property.class) {
                hasPropertyField = true;
            }
            analyseField(f);
        }
    }

    /**
     * Called during processing to analyse all discovered methods in the
     * delegate class. May be overridden, but usually better to override the
     * more specific analysis methods.
     *
     * @param methods discovered methods
     */
    protected void analyseMethods(Method[] methods) {
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            analyseMethod(m);
        }
    }

    /**
     * Called during processing to analyse each discovered field. May be
     * overridden to extend. The default behaviour will first pass to available
     * plugins (see {@link Plugin}), then check for property, trigger, in,
     * aux-in, out, aux-out and inject annotations in that order. First valid
     * match wins.
     *
     * @param field discovered field
     */
    protected void analyseField(Field field) {

        for (Plugin p : plugins) {
            if (p.analyseField(this, field)) {
                return;
            }
        }

        P prop = field.getAnnotation(P.class);
        if (prop != null) {
            if (analyseResourcePropertyField(prop, field)) {
                return;
            }
            if (analysePropertyField(prop, field)) {
                return;
            }
            if (analyseTablePropertyField(prop, field)) {
                return;
            }
        }
        T trig = field.getAnnotation(T.class);
        if (trig != null && analyseTriggerField(trig, field)) {
            return;
        }
        In in = field.getAnnotation(In.class);
        if (in != null && analyseInputField(in, field)) {
            return;
        }
        AuxIn auxIn = field.getAnnotation(AuxIn.class);
        if (auxIn != null && analyseAuxInputField(auxIn, field)) {
            return;
        }
        Out out = field.getAnnotation(Out.class);
        if (out != null && analyseOutputField(out, field)) {
            return;
        }
        AuxOut aux = field.getAnnotation(AuxOut.class);
        if (aux != null && analyseAuxOutputField(aux, field)) {
            return;
        }
        Inject inject = field.getAnnotation(Inject.class);
        if (inject != null && analyseInjectField(inject, field)) {
            return;
        }
    }

    /**
     * Called during processing to analyse each discovered method. May be
     * overridden to extend. The default behaviour will first pass to available
     * plugins (see {@link Plugin}), then check for trigger, in and aux-in
     * annotations in that order. First valid match wins.
     *
     * @param method discovered method
     */
    protected void analyseMethod(Method method) {

        for (Plugin p : plugins) {
            if (p.analyseMethod(this, method)) {
                return;
            }
        }

        T trig = method.getAnnotation(T.class);
        if (trig != null && analyseTriggerMethod(trig, method)) {
            return;
        }
        In in = method.getAnnotation(In.class);
        if (in != null && analyseInputMethod(in, method)) {
            return;
        }
        AuxIn aux = method.getAnnotation(AuxIn.class);
        if (aux != null && analyseAuxInputMethod(aux, method)) {
            return;
        }
    }

    private boolean analyseInputField(In ann, Field field) {
        InputImpl.Descriptor odsc = InputImpl.createDescriptor(this, ann, field);
        if (odsc != null) {
            addPort(odsc);
            addControl(InputPortControl.Descriptor.createInput(odsc.getID(), odsc.getIndex(), odsc));
            return true;
        }

        DataPort.InputDescriptor din = DataPort.InputDescriptor.create(this, ann, field);
        if (din != null) {
            addPort(din);
            return true;
        }

        return false;
    }

    private boolean analyseAuxInputField(AuxIn ann, Field field) {
        InputImpl.Descriptor odsc = InputImpl.createDescriptor(this, ann, field);
        if (odsc != null) {
            addPort(odsc);
            addControl(InputPortControl.Descriptor.createAuxInput(odsc.getID(), odsc.getIndex(), odsc));
            return true;
        }

        DataPort.InputDescriptor din = DataPort.InputDescriptor.create(this, ann, field);
        if (din != null) {
            addPort(din);
            return true;
        }

        return false;

    }

    private boolean analyseOutputField(Out ann, Field field) {
        OutputImpl.Descriptor odsc = OutputImpl.createDescriptor(this, ann, field);
        if (odsc != null) {
            addPort(odsc);
            return true;
        }

        DataPort.OutputDescriptor dout = DataPort.OutputDescriptor.create(this, ann, field);
        if (dout != null) {
            addPort(dout);
            return true;
        }

        return false;
    }

    private boolean analyseAuxOutputField(AuxOut ann, Field field) {
        OutputImpl.Descriptor odsc = OutputImpl.createDescriptor(this, ann, field);
        if (odsc != null) {
            addPort(odsc);
            return true;
        }

        DataPort.OutputDescriptor dout = DataPort.OutputDescriptor.create(this, ann, field);
        if (dout != null) {
            addPort(dout);
            return true;
        }

        return false;

    }

    private boolean analyseTriggerField(T ann, Field field) {
        TriggerControl.Descriptor tdsc
                = TriggerControl.Descriptor.create(this, ann, field);
        if (tdsc != null) {
            addControl(tdsc);
            if (shouldAddPort(field)) {
                addPort(tdsc.createPortDescriptor());
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean analyseResourcePropertyField(P ann, Field field) {
        if (field.getAnnotation(Type.Resource.class) != null
                && String.class.equals(field.getType())) {
            ResourceProperty.Descriptor<String> rpd
                    = ResourceProperty.Descriptor.create(this, ann, field, ResourceProperty.getStringLoader());
            if (rpd != null) {
                addControl(rpd);
                if (shouldAddPort(field)) {
                    addPort(rpd.createPortDescriptor());
                }
                return true;
            }
        }
        return false;
    }

    private boolean analysePropertyField(P ann, Field field) {
        PropertyControl.Descriptor pdsc
                = PropertyControl.Descriptor.create(this, ann, field);
        if (pdsc != null) {
            addControl(pdsc);
            if (shouldAddPort(field)) {
                addPort(pdsc.createPortDescriptor());
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean analyseTablePropertyField(P ann, Field field) {
        TableProperty.Descriptor tpd
                = TableProperty.Descriptor.create(this, ann, field);
        if (tpd != null) {
            addControl(tpd);
            return true;
        }
        return false;
    }

    private boolean analyseInjectField(Inject ann, Field field) {

        if (Ref.class.equals(field.getType())) {
            RefImpl.Descriptor rdsc = RefImpl.Descriptor.create(this, field);
            if (rdsc != null) {
                addReference(rdsc);
                addControl(rdsc.getControlDescriptor());
                return true;
            } else {
                return false;
            }
        }

        if (Data.Sink.class.equals(field.getType())) {
            DataSink.Descriptor dsdsc = DataSink.Descriptor.create(this, field);
            if (dsdsc != null) {
                addReference(dsdsc);
                return true;
            } else {
                return false;
            }
        }

        PropertyControl.Descriptor pdsc
                = PropertyControl.Descriptor.create(this, ann, field);
        if (pdsc != null) {
            addControl(pdsc);
            return true;
        } else {
            return false;
        }
    }

    private boolean analyseTriggerMethod(T ann, Method method) {
        TriggerControl.Descriptor tdsc
                = TriggerControl.Descriptor.create(this, ann, method);
        if (tdsc != null) {
            addControl(tdsc);
            if (shouldAddPort(method)) {
                addPort(tdsc.createPortDescriptor());
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean analyseInputMethod(In ann, Method method) {
        MethodInput.Descriptor desc
                = MethodInput.createDescriptor(this, ann, method);
        if (desc != null) {
            addPort(desc);
            addControl(InputPortControl.Descriptor.createInput(desc.getID(), desc.getIndex(), desc));
            return true;
        } else {
            return false;
        }
    }

    private boolean analyseAuxInputMethod(AuxIn ann, Method method) {
        MethodInput.Descriptor desc
                = MethodInput.createDescriptor(this, ann, method);
        if (desc != null) {
            addPort(desc);
            addControl(InputPortControl.Descriptor.createAuxInput(desc.getID(), desc.getIndex(), desc));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find a suitable ID (control or port) for the provided field. Will first
     * look for {@link ID} annotation, and if not found convert the field name
     * from camelCase to dash-case.
     *
     * @param field field to find ID for
     * @return ID
     */
    public String findID(Field field) {
        ID ann = field.getAnnotation(ID.class);
        if (ann != null) {
            String id = ann.value();
            if (ControlAddress.isValidID(id)) {
                return id;
            }
        }
        return javaNameToID(field.getName());
    }

    /**
     * Find a suitable ID (control or port) for the provided method. Will first
     * look for {@link ID} annotation, and if not found convert the method name
     * from camelCase to dash-case.
     *
     * @param method method to find ID for
     * @return ID
     */
    public String findID(Method method) {
        ID ann = method.getAnnotation(ID.class);
        if (ann != null) {
            String id = ann.value();
            if (ControlAddress.isValidID(id)) {
                return id;
            }
        }
        return javaNameToID(method.getName());
    }

    /**
     * Convert a Java name in camelCase to an ID in dash-case.
     *
     * @param javaName Java name to convert
     * @return ID for Java name
     */
    protected String javaNameToID(String javaName) {
        String ret = idRegex.matcher(javaName).replaceAll("-");
        return ret.toLowerCase();
    }

    /**
     * Check whether a port should be added for provided element (field or
     * method). By default returns true unless the element is marked
     * {@link ReadOnly}, or with {@link Config.Port} and value false.
     *
     * @param element annotated element to analyse
     * @return whether to add a port
     */
    public boolean shouldAddPort(AnnotatedElement element) {
        if (element.isAnnotationPresent(ReadOnly.class)) {
            return false;
        }
        Config.Port port = element.getAnnotation(Config.Port.class);
        if (port != null) {
            return port.value();
        }
        return true;
    }

    /**
     * Return next index to use for synthetic descriptors. Increments and
     * returns an int on each call, not based on registered descriptors.
     *
     * @return next index
     */
    public int getSyntheticIndex() {
        return syntheticIdx++;
    }

    protected int getInternalIndex() {
        return internalIdx++;
    }
    
    ArgumentInfo infoFromType(Type typeAnnotation) {
        Class<? extends Value> valueCls = typeAnnotation.value();
        PMap properties = createPropertyMap(typeAnnotation.properties());
        return ArgumentInfo.of(valueCls, properties);
    }

    private PMap createPropertyMap(String... properties) {
        if (properties.length == 0) {
            return PMap.EMPTY;
        }
        if (properties.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        PMap.Builder bld = PMap.builder(properties.length / 2);
        for (int i = 0; i < properties.length; i += 2) {
            bld.put(properties[i], properties[i + 1]);
        }
        return bld.build();
    }

    Value defaultValueFromType(Type typeAnnotation) {
        Class<Value> valueCls = (Class<Value>) typeAnnotation.value();
        Value.Type<Value> valueType = Value.Type.of(valueCls);
        String defaultString = typeAnnotation.def();
        return valueType.converter().apply(PString.of(defaultString)).orElse(PString.EMPTY);
    }

    public static interface Plugin {

        default boolean analyseField(CodeConnector<?> connector, Field field) {
            return false;
        }

        default boolean analyseMethod(CodeConnector<?> connector, Method method) {
            return false;
        }

        default boolean isSupportedConnector(CodeConnector<?> connector) {
            return true;
        }

    }

}
