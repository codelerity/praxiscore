/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
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
package org.praxislive.script.commands;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.praxislive.core.Value;
import org.praxislive.core.ValueMapper;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PReference;
import org.praxislive.core.types.PString;
import org.praxislive.script.Command;
import org.praxislive.script.Env;
import org.praxislive.script.InlineCommand;
import org.praxislive.script.Namespace;

/**
 * Commands for interacting with the running JVM.
 */
class JavaCmds {

    private final static Map<String, Command> COMMANDS = Map.of(
            "jcall", new JCall(),
            "sys-env", new SysEnv(),
            "sys-prop", new SysProp()
    );

    private JavaCmds() {
    }

    static void install(Map<String, Command> commands) {
        commands.putAll(COMMANDS);
    }

    private static class JCall implements InlineCommand {

        @SuppressWarnings("unchecked")
        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            if (args.size() < 2) {
                throw new IllegalArgumentException("Incorrect number of arguments");
            }

            Class<?> cls = findClass(args.get(0).toString());
            PArray sig = PArray.from(args.get(1))
                    .orElseThrow(IllegalArgumentException::new);
            if (sig.isEmpty()) {
                throw new IllegalArgumentException();
            }
            String methodName = sig.get(0).toString();
            List<String> paramHints = sig.size() > 1 ? sig.stream()
                    .skip(1)
                    .map(Value::toString)
                    .toList() : List.of();

            Method method = findMethod(cls, methodName, paramHints, args.size() - 2);
            Object target;
            List<Value> paramArgs;
            if (Modifier.isStatic(method.getModifiers())) {
                target = null;
                paramArgs = args.stream().skip(2).toList();
            } else {
                target = fromValue(cls, args.get(2));
                paramArgs = args.stream().skip(3).toList();
            }

            Object result = method.invoke(target, toArgArray(method, paramArgs));

            if (method.getReturnType() == Void.TYPE) {
                return List.of();
            } else {
                return List.of(toValue(method.getGenericReturnType(), result));
            }

        }

        @Override
        public String description() {
            return CoreCommands.message("jcall.description");
        }

        private Class<?> findClass(String clsName) throws Exception {
            return Class.forName(clsName);
        }

        private Method findMethod(Class<?> cls, String methodName,
                List<String> paramHints, int argCount)
                throws Exception {
            List<Method> methods = Stream.of(cls.getMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .filter(m -> isMatch(m, paramHints, argCount))
                    .toList();

            if (methods.isEmpty()) {
                throw new IllegalArgumentException("No method found");
            } else if (methods.size() > 1) {
                throw new IllegalArgumentException("Ambiguous methods : " + methods);
            } else {
                return methods.getFirst();
            }

        }

        private boolean isMatch(Method method, List<String> paramHints, int argCount) {
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            int reqParams = isStatic ? argCount : (argCount - 1);
            if (method.getParameterCount() != reqParams) {
                return false;
            }
            if (paramHints.isEmpty()) {
                return true;
            }
            return isMatchedParameters(method.getParameters(), paramHints);
        }

        private boolean isMatchedParameters(Parameter[] parameters, List<String> paramHints) {
            int count = Math.min(parameters.length, paramHints.size());
            for (int i = 0; i < count; i++) {
                if (parameters[i].getParameterizedType().getTypeName()
                        .contains(paramHints.get(i))) {
                    continue;
                }
                return false;
            }
            return true;
        }

        private Object[] toArgArray(Executable executable, List<Value> args) {
            Object[] result = new Object[args.size()];
            List<Parameter> parameters = List.of(executable.getParameters());
            for (int i = 0; i < result.length; i++) {
                Parameter parameter = i < parameters.size()
                        ? parameters.get(i) : parameters.getLast();
                result[i] = fromValue(parameter.getParameterizedType(), args.get(i));
            }
            return result;
        }

        private Object fromValue(Type targetType, Value value) {
            if (targetType instanceof Class<?> targetClass) {
                if (value instanceof PReference ref) {
                    return ref.as(targetClass).orElseThrow(IllegalArgumentException::new);
                } else if (Object.class == targetClass) {
                    return value.toString();
                } else if (targetClass.isInstance(value)) {
                    return value;
                }
            }
            ValueMapper<?> mapper = ValueMapper.find(targetType);
            if (mapper != null) {
                return mapper.fromValue(value);
            }
            throw new IllegalArgumentException();
        }

        @SuppressWarnings("unchecked")
        private Value toValue(Type sourceType, Object source) {
            if (source instanceof Value value) {
                return value;
            }
            if (source == null) {
                return PString.of(source);
            }
            ValueMapper<Object> mapper = (ValueMapper<Object>) ValueMapper.find(source.getClass());
            if (mapper == null) {
                mapper = (ValueMapper<Object>) ValueMapper.find(sourceType);
            }
            if (mapper != null) {
                return mapper.toValue(source);
            } else {
                return PReference.of(source);
            }
        }

    }

    private static class SysEnv implements InlineCommand {

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            if (args.size() == 1) {
                String result = System.getenv(args.get(0).toString());
                if (result == null) {
                    return List.of(PString.EMPTY);
                } else {
                    return List.of(PString.of(result));
                }
            } else {
                throw new IllegalArgumentException("Incorrect number of arguments");
            }
        }

        @Override
        public String description() {
            return CoreCommands.message("sys-env.description");
        }

    }

    private static class SysProp implements InlineCommand {

        @Override
        public List<Value> process(Env context, Namespace namespace, List<Value> args) throws Exception {
            String result = switch (args.size()) {
                case 1 ->
                    System.getProperty(args.get(0).toString());
                case 2 ->
                    System.setProperty(args.get(0).toString(), args.get(1).toString());
                default ->
                    throw new IllegalArgumentException("Incorrect number of arguments");
            };
            if (result == null) {
                return List.of(PString.EMPTY);
            } else {
                return List.of(PString.of(result));
            }
        }

        @Override
        public String description() {
            return CoreCommands.message("sys-prop.description");
        }

    }

}
