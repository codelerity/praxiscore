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
package org.praxislive.code;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.praxislive.core.ComponentType;
import org.praxislive.core.Lookup;
import org.praxislive.core.services.LogBuilder;

/**
 * A CodeFactory wraps configuration and task creation for creating code
 * components and contexts for a given delegate base type.
 *
 * @param <D> base delegate type
 */
public class CodeFactory<D extends CodeDelegate> {

    /**
     * Key for use in control info properties with the name of the base delegate
     * type (class) as the value.
     */
    public static final String BASE_CLASS_KEY = "base-class";

    /**
     * Key for use in control info properties with the base imports that should
     * be available in source code. The value should be a PArray of import
     * lines, each containing a full import declaration.
     */
    public static final String BASE_IMPORTS_KEY = "base-imports";

    private final Class<D> baseClass;
    private final List<String> baseImports;
    private final ComponentType type;
    private final String template;
    private final Class<? extends D> defaultDelegateClass;
    private final Lookup lookup;
    private final Supplier<? extends CodeComponent<D>> componentCreator;
    private final BiFunction<CodeFactory.Task<D>, D, CodeContext<D>> contextCreator;

    /**
     * Construct CodeFactory for a type extending the base code delegate type.
     * This constructor allows for a precompiled default delegate class to be
     * provided. This must correspond to the code compiled from wrapping the
     * provided class body template in the provided class body context.
     *
     * @param cbc class body context that will wrap source code
     * @param type the component type
     * @param defaultCls precompiled default delegate
     * @param template code template reflecting default delegate
     */
    @Deprecated
    protected CodeFactory(
            ClassBodyContext<D> cbc,
            ComponentType type,
            Class<? extends D> defaultCls,
            String template) {
        this.baseClass = cbc.getExtendedClass();
        this.baseImports = List.of(cbc.getDefaultImports());
        this.lookup = Lookup.of(cbc);
        this.type = type;
        this.defaultDelegateClass = defaultCls;
        this.template = template;
        this.componentCreator = CodeComponent::new;
        this.contextCreator = null;
    }

    private CodeFactory(Base<D> base, ComponentType type, Class<? extends D> defaultCls, String template) {
        this.baseClass = base.baseClass;
        this.baseImports = base.baseImports;
        this.componentCreator = base.componentCreator;
        this.contextCreator = base.contextCreator;
        this.lookup = base.lookup;
        this.type = Objects.requireNonNull(type);
        this.defaultDelegateClass = Objects.requireNonNull(defaultCls);
        this.template = Objects.requireNonNull(template);
    }

    /**
     * Construct CodeFactory for a type extending the base code delegate type.
     * This constructor is used where the default delegate is compiled from the
     * template at runtime.
     *
     * @param cbc class body context that will wrap source code
     * @param type the component type
     * @param template code template reflecting default delegate
     */
    @Deprecated
    protected CodeFactory(
            ClassBodyContext<D> cbc,
            ComponentType type,
            String template) {
        this(cbc, type, null, template);
    }

    /**
     * Get the component type.
     *
     * @return component type
     */
    @Deprecated
    public final ComponentType getComponentType() {
        return type;
    }

    /**
     * Get the component type.
     *
     * @return component type
     */
    public final ComponentType componentType() {
        return type;
    }

    /**
     * Class body context used to create wrapping class for source code.
     *
     * @return class body context
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public final ClassBodyContext<D> getClassBodyContext() {
        return (ClassBodyContext<D>) lookup.find(ClassBodyContext.class)
                .orElseGet(() -> new ClassBodyContext(baseClass) {
            @Override
            public String[] getDefaultImports() {
                return baseImports.toArray(String[]::new);
            }

        });
    }

    final String getClassBodyContextName() {
        return lookup.find(ClassBodyContext.class)
                .map(cbc -> cbc.getClass().getName())
                .orElse(ClassBodyContext.Default.class.getName());
    }

    /**
     * The source template corresponding to the default delegate class.
     *
     * @return source template
     */
    @Deprecated
    public final String getSourceTemplate() {
        return template;
    }

    /**
     * The source template corresponding to the default delegate class.
     *
     * @return source template
     */
    public final String sourceTemplate() {
        return template;
    }

    /**
     * Optional precompiled version of the default delegate class.
     *
     * @return optional precompiled default delegate
     */
    @Deprecated
    public final Optional<Class<? extends D>> getDefaultDelegateClass() {
        return Optional.ofNullable(defaultDelegateClass);
    }

    /**
     * Query the default delegate class.
     *
     * @return default delegate class
     */
    public final Class<? extends D> defaultDelegateClass() {
        return defaultDelegateClass;
    }

    /**
     * Query the base delegate class. This is the superclass for the source
     * template and any derived code body.
     *
     * @return base delegate class
     */
    public final Class<D> baseClass() {
        return baseClass;
    }

    /**
     * Query the base imports to be automatically added to the source body.
     *
     * @return base imports
     */
    public final List<String> baseImports() {
        return baseImports;
    }

    /**
     * Create a task for constructing a context or component from a delegate
     * class. This will return a suitable Task subclass that should be
     * configured and used to create a context or component.
     *
     * @return code factory task
     */
    public Task<D> task() {
        return new Task<>(this, false);
    }

    /**
     * Create a component {@link CodeFactory.Base} for the given base delegate
     * class, from which can be created individual CodeFactory instances. The
     * base class and default imports will be used to wrap user sources passed
     * across to the compiler. The context creator function is used to wrap the
     * compiled delegate in a {@link CodeContext}, and will usually correspond
     * to <code>(task, delegate) -> new XXXCodeContext(new
     * XXXCodeConnector(task, delegate))</code>
     *
     * @param <B> base delegate type
     * @param baseClass base delegate superclass
     * @param baseImports default base imports
     * @param contextCreator create context for delegate
     * @return code factory base
     */
    public static <B extends CodeDelegate> Base<B> base(Class<B> baseClass,
            List<String> baseImports,
            BiFunction<CodeFactory.Task<B>, B, CodeContext<B>> contextCreator) {
        return new Base<>(baseClass, baseImports, CodeComponent::new, contextCreator, Lookup.EMPTY);
    }

    /**
     * Create a container {@link CodeFactory.Base} for the given base delegate
     * class, from which can be created individual CodeFactory instances. The
     * base class and default imports will be used to wrap user sources passed
     * across to the compiler. The context creator function is used to wrap the
     * compiled delegate in a {@link CodeContext}, and will usually correspond
     * to <code>(task, delegate) -> new XXXCodeContext(new
     * XXXCodeConnector(task, delegate))</code>
     *
     * @param <B> base delegate type
     * @param baseClass base delegate superclass
     * @param baseImports default base imports
     * @param contextCreator create context for delegate
     * @return code factory base
     */
    public static <B extends CodeContainerDelegate> Base<B> containerBase(Class<B> baseClass,
            List<String> baseImports,
            BiFunction<CodeFactory.Task<B>, B, CodeContext<B>> contextCreator) {
        return new Base<>(baseClass, baseImports, CodeContainer::new, contextCreator, Lookup.EMPTY);
    }

    /**
     * Create a root component {@link CodeFactory.Base} for the given base
     * delegate class, from which can be created individual CodeFactory
     * instances. The base class and default imports will be used to wrap user
     * sources passed across to the compiler. The context creator function is
     * used to wrap the compiled delegate in a {@link CodeContext}, and will
     * usually correspond to <code>(task, delegate) -> new XXXCodeContext(new
     * XXXCodeConnector(task, delegate))</code>
     *
     * @param <B> base delegate type
     * @param baseClass base delegate superclass
     * @param baseImports default base imports
     * @param contextCreator create context for delegate
     * @return code factory base
     */
    public static <B extends CodeRootDelegate> Base<B> rootBase(Class<B> baseClass,
            List<String> baseImports,
            BiFunction<CodeFactory.Task<B>, B, CodeContext<B>> contextCreator) {
        return new Base<>(baseClass, baseImports, CodeRoot::new, contextCreator, Lookup.EMPTY);
    }

    /**
     * Create a root container {@link CodeFactory.Base} for the given base
     * delegate class, from which can be created individual CodeFactory
     * instances. The base class and default imports will be used to wrap user
     * sources passed across to the compiler. The context creator function is
     * used to wrap the compiled delegate in a {@link CodeContext}, and will
     * usually correspond to <code>(task, delegate) -> new XXXCodeContext(new
     * XXXCodeConnector(task, delegate))</code>
     *
     * @param <B> base delegate type
     * @param baseClass base delegate superclass
     * @param baseImports default base imports
     * @param contextCreator create context for delegate
     * @return code factory base
     */
    public static <B extends CodeRootContainerDelegate> Base<B> rootContainerBase(Class<B> baseClass,
            List<String> baseImports,
            BiFunction<CodeFactory.Task<B>, B, CodeContext<B>> contextCreator) {
        return new Base<>(baseClass, baseImports, CodeRootContainer::new, contextCreator, Lookup.EMPTY);
    }

    /**
     * A task for creating a component or context for a given delegate.
     *
     * @param <D> delegate base type
     */
    public static class Task<D extends CodeDelegate> {

        private final CodeFactory<D> factory;

        private LogBuilder log;
        private Class<D> previous;

        /**
         * Construct a task for the given factory.
         *
         * @param factory
         */
        public Task(CodeFactory<D> factory) {
            this(factory, true);
        }

        private Task(CodeFactory<D> factory, boolean verify) {
            this.factory = Objects.requireNonNull(factory);
        }

        /**
         * Attach a log builder to the task.
         *
         * @param log log builder
         * @return this for chaining
         */
        public Task<D> attachLogging(LogBuilder log) {
            this.log = log;
            return this;
        }

        /**
         * Attach the previous iteration of delegate class, if available, that
         * is being replaced. May be null.
         *
         * @param previous previous iteration of delegate class
         * @return this for chaining
         */
        public Task<D> attachPrevious(Class<D> previous) {
            this.previous = previous;
            return this;
        }

        /**
         * Create a CodeComponent for the provided delegate. By default, this
         * calls {@link #createContext(org.praxislive.code.CodeDelegate)} and
         * installs the context on a new instance of CodeComponent.
         *
         * @param delegate delegate to create component for
         * @return code component
         */
        public CodeComponent<D> createComponent(D delegate) {
            CodeComponent<D> cmp = factory.componentCreator.get();
            cmp.install(createContext(delegate));
            return cmp;
        }

        /**
         * Create a CodeContext for the provided delegate, for installation in
         * an existing component. By default just calls through to
         * {@link #createCodeContext(org.praxislive.code.CodeDelegate)}.
         *
         * @param delegate delegate to create context for
         * @return code context
         */
        public CodeContext<D> createContext(D delegate) {
            return createCodeContext(delegate);
        }

        /**
         * Get the log for reporting messages during context creation.
         *
         * @return log builder
         */
        protected LogBuilder getLog() {
            return log;
        }

        /**
         * Get the previous delegate class installed on the component. May be
         * null.
         *
         * @return previous delegate class
         */
        protected Class<D> getPrevious() {
            return previous;
        }

        /**
         * Get access to the CodeFactory this task was created for.
         *
         * @return code factory
         */
        protected CodeFactory<D> getFactory() {
            return factory;
        }

        /**
         * Create the code context for the given delegate. A typical
         * implementation is <code>return new XXXCodeContext(new
         * XXXCodeConnector(this, delegate));</code>.
         *
         * @param delegate delegate to create context for
         * @return code context
         */
        protected CodeContext<D> createCodeContext(D delegate) {
            return factory.contextCreator.apply(this, delegate);
        }

    }

    /**
     * Base code factory for a given base delegate class. Encompasses shared
     * configuration, component and context creation. Create specific
     * CodeFactory instances with the create methods. See
     * {@link #base(java.lang.Class, java.util.List, java.util.function.BiFunction)}
     * and
     * {@link #containerBase(java.lang.Class, java.util.List, java.util.function.BiFunction)}
     *
     * @param <B> base delegate type
     */
    public static final class Base<B extends CodeDelegate> {

        private final Class<B> baseClass;
        private final List<String> baseImports;
        private final Supplier<? extends CodeComponent<B>> componentCreator;
        private final BiFunction<CodeFactory.Task<B>, B, CodeContext<B>> contextCreator;
        private final Lookup lookup;

        Base(Class<B> baseClass,
                List<String> baseImports,
                Supplier<? extends CodeComponent<B>> componentCreator,
                BiFunction<CodeFactory.Task<B>, B, CodeContext<B>> contextCreator,
                Lookup lookup) {
            this.baseClass = Objects.requireNonNull(baseClass);
            this.baseImports = List.copyOf(baseImports);
            this.componentCreator = Objects.requireNonNull(componentCreator);
            this.contextCreator = Objects.requireNonNull(contextCreator);
            this.lookup = Objects.requireNonNull(lookup);
        }

        /**
         * Create a CodeFactory with the given component type, default
         * precompiled delegate class, and source class body corresponding to
         * the compiled delegate.
         *
         * @param type component type as String, passed to
         * {@link ComponentType#of(java.lang.String)}
         * @param defaultDelegate default delegate class
         * @param defaultSource default source class body
         * @return code factory
         */
        public CodeFactory<B> create(String type, Class<? extends B> defaultDelegate, String defaultSource) {
            return create(ComponentType.of(type), defaultDelegate, defaultSource);
        }

        /**
         * Create a CodeFactory with the given component type, default
         * precompiled delegate class, and source class body corresponding to
         * the compiled delegate.
         *
         * @param type component type
         * @param defaultDelegate default delegate class
         * @param defaultSource default source class body
         * @return code factory
         */
        public CodeFactory<B> create(ComponentType type, Class<? extends B> defaultDelegate, String defaultSource) {
            return new CodeFactory<>(this, type, defaultDelegate, defaultSource);
        }

    }

}
