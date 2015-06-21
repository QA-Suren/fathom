/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fathom.rest.controller;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import fathom.exception.FathomException;
import fathom.rest.Context;
import fathom.rest.controller.exceptions.RequiredException;
import fathom.rest.controller.exceptions.RangeException;
import fathom.rest.controller.extractors.ArgumentExtractor;
import fathom.rest.controller.extractors.CollectionExtractor;
import fathom.rest.controller.extractors.ConfigurableExtractor;
import fathom.rest.controller.extractors.ExtractWith;
import fathom.rest.controller.extractors.FileItemExtractor;
import fathom.rest.controller.extractors.NamedExtractor;
import fathom.rest.controller.extractors.ParamExtractor;
import fathom.rest.controller.extractors.TypedExtractor;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.FileItem;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.RouteHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * ControllerHandler executes controller methods.
 *
 * @author James Moger
 */
public class ControllerHandler implements RouteHandler<Context> {

    private static final Logger log = LoggerFactory.getLogger(ControllerHandler.class);

    protected final Class<? extends Controller> controllerClass;
    protected final Provider<? extends Controller> controllerProvider;
    protected final Method method;
    protected ArgumentExtractor[] extractors;
    protected String[] patterns;
    protected List<String> declaredProduces;
    protected Collection<Return> declaredReturns;

    public ControllerHandler(Injector injector, Class<? extends Controller> controllerClass, String methodName) {
        if (controllerClass.isAnnotationPresent(Singleton.class)
                || controllerClass.isAnnotationPresent(javax.inject.Singleton.class)) {
            throw new FathomException("Controller '{}' may not be annotated as a Singleton!", controllerClass.getName());
        }

        this.controllerClass = controllerClass;
        this.controllerProvider = injector.getProvider(controllerClass);
        this.method = findMethod(injector, controllerClass, methodName);

        Preconditions.checkNotNull(method, "Failed to find method '%s'", Util.toString(controllerClass, methodName));
        log.trace("Obtained method for '{}'", Util.toString(method));

        this.declaredProduces = ControllerUtil.collectProduces(method);
        this.declaredReturns = ControllerUtil.collectReturns(method);
    }

    public Class<? extends Controller> getControllerClass() {
        return controllerClass;
    }

    public Method getControllerMethod() {
        return method;
    }

    public List<String> getDeclaredProduces() {
        return declaredProduces;
    }

    public Collection<Return> getDeclaredReturns() {
        return declaredReturns;
    }

    @Override
    public void handle(Context context) {

        try {

            log.trace("Preparing '{}' arguments from request", Util.toString(method));
            Object[] args = prepareMethodArgs(context);

            log.trace("Invoking '{}'", Util.toString(method));
            Controller controller = controllerProvider.get();
            controller.setContext(context);

            // set the response content type and optionally negotiate
            if (!declaredProduces.isEmpty()) {
                String defaultContentType = declaredProduces.get(0);
                context.getResponse().contentType(defaultContentType);

                if (declaredProduces.size() > 1) {
                    context.negotiateContentType();
                }
            }

            Object result = method.invoke(controller, args);
            if (Void.class != method.getReturnType()) {
                // method declares a return value
                if (result == null) {
                    // prepare a NOT FOUND (404)
                    context.getResponse().notFound();

                    for (Return declaredReturn : declaredReturns) {
                        if (declaredReturn.code() == HttpConstants.StatusCode.NOT_FOUND) {
                            if (!Strings.isNullOrEmpty(declaredReturn.description())) {
                                context.setLocal("message", declaredReturn.description());
                            }
                            break;
                        }
                    }
                } else {
                    // prepare declared Return
                    Class<?> resultClass = result.getClass();
                    for (Return declaredReturn : declaredReturns) {
                        if (declaredReturn.onResult().isAssignableFrom(resultClass)) {
                            context.status(declaredReturn.code());
                            break;
                        }
                    }

                    context.send(result);
                }
            }

            context.next();

        } catch (InvocationTargetException e) {
            // handles exceptions thrown within the proxied controller method
            Throwable t = e.getTargetException();
            if (t instanceof Exception) {
                Exception target = (Exception) t;
                handleDeclaredReturnException(target, method, context);
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                log.error("Failed to handle controller method exception", t);
            }
        } catch (Exception e) {
            // handles exceptions thrown within this handle() method
            handleDeclaredReturnException(e, method, context);
        }
    }

    protected Method findMethod(Injector injector, Class<?> controllerClass, String name) {
        // identify first method which matches the name
        Method controllerMethod = null;
        for (Method method : controllerClass.getMethods()) {
            if (method.getName().equals(name)) {
                if (controllerMethod == null) {
                    controllerMethod = method;

                    // mapped parameters
                    Class<?>[] types = method.getParameterTypes();
                    extractors = new ArgumentExtractor[types.length];
                    patterns = new String[types.length];
                    for (int i = 0; i < types.length; i++) {
                        final Parameter parameter = method.getParameters()[i];
                        final Class<? extends Collection> collectionType;
                        final Class<?> objectType;
                        if (Collection.class.isAssignableFrom(types[i])) {
                            collectionType = (Class<? extends Collection>) types[i];
                            objectType = getParameterGenericType(parameter);
                        } else {
                            collectionType = null;
                            objectType = types[i];
                        }

                        // determine the appropriate extractor
                        Class<? extends ArgumentExtractor> extractorType;
                        if (FileItem.class == objectType) {
                            extractorType = FileItemExtractor.class;
                        } else {
                            extractorType = getArgumentExtractor(parameter);
                        }

                        // instantiate the extractor
                        extractors[i] = injector.getInstance(extractorType);

                        // configure the extractor
                        if (extractors[i] instanceof ConfigurableExtractor<?>) {
                            ConfigurableExtractor extractor = (ConfigurableExtractor) extractors[i];
                            Annotation annotation = ClassUtil.getAnnotation(parameter, extractor.getAnnotationClass());
                            if (annotation != null) {
                                extractor.configure(annotation);
                            }
                        }

                        if (collectionType != null) {
                            if (extractors[i] instanceof CollectionExtractor) {
                                CollectionExtractor extractor = (CollectionExtractor) extractors[i];
                                extractor.setCollectionType(collectionType);
                            } else {
                                throw new FathomException(
                                        "Controller method '{}' parameter {} of type '{}' does not specify an argument extractor that supports collections!",
                                        Util.toString(method), i + 1, describeType(collectionType, objectType));
                            }
                        }

                        if (extractors[i] instanceof TypedExtractor) {
                            TypedExtractor extractor = (TypedExtractor) extractors[i];
                            extractor.setObjectType(objectType);
                        }

                        if (extractors[i] instanceof NamedExtractor) {
                            // ensure that the extractor has a proper name
                            NamedExtractor namedExtractor = (NamedExtractor) extractors[i];
                            if (Strings.isNullOrEmpty(namedExtractor.getName())) {
                                // parameter is not named via annotation
                                // try looking for the parameter name in the compiled .class file
                                if (parameter.isNamePresent()) {
                                    namedExtractor.setName(parameter.getName());
                                } else {
                                    log.error("Properly annotate your controller methods OR specify the '-parameters' flag for your Java compiler!");
                                    throw new FathomException(
                                            "Controller method '{}' parameter {} of type '{}' does not specify a name!",
                                            Util.toString(method), i + 1, describeType(collectionType, objectType));
                                }
                            }
                        }
                    }
                } else {
                    throw new FathomException("Found overloaded controller method '{}'. Method names must be unique!",
                            Util.toString(method));
                }
            }
        }

        return controllerMethod;
    }

    protected Object[] prepareMethodArgs(Context context) {
        Parameter[] parameters = method.getParameters();

        if (parameters.length == 0) {
            return new Object[]{};
        }

        Object[] args = new Object[parameters.length];
        for (int i = 0; i < args.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();

            ArgumentExtractor extractor = extractors[i];
            Object value = extractor.extract(context);

            validateParameterValue(parameter, value);

            if (value == null || ClassUtil.isAssignable(value, type)) {
                args[i] = value;
            } else {
                String parameterName = ControllerUtil.getParameterName(parameter);
                throw new FathomException("Type for '{}' is actually '{}' but was specified as '{}'!",
                        parameterName, value.getClass().getName(), type.getName());
            }
        }

        return args;
    }

    protected void validateParameterValue(Parameter parameter, Object value) {
        if (value == null && parameter.isAnnotationPresent(Required.class)) {
            throw new RequiredException("'{}' is a required parameter!", ControllerUtil.getParameterName(parameter));
        }

        if (value != null && value instanceof Number) {
            Number number = (Number) value;

            if (parameter.isAnnotationPresent(Min.class)) {
                // validate required minimum value
                Min min = parameter.getAnnotation(Min.class);
                if (number.longValue() < min.value()) {
                    throw new RangeException("'{}' must be >= {}", ControllerUtil.getParameterName(parameter), min.value());
                }
            }

            if (parameter.isAnnotationPresent(Max.class)) {
                // validate required maximum value
                Max max = parameter.getAnnotation(Max.class);
                if (number.longValue() > max.value()) {
                    throw new RangeException("'{}' must be <= {}", ControllerUtil.getParameterName(parameter), max.value());
                }
            }

            if (parameter.isAnnotationPresent(Range.class)) {
                Range range = parameter.getAnnotation(Range.class);
                if (number.longValue() < range.min()) {
                    throw new RangeException("'{}' must be >= {}", ControllerUtil.getParameterName(parameter), range.min());
                }
                if (number.longValue() > range.max()) {
                    throw new RangeException("'{}' must be <= {}", ControllerUtil.getParameterName(parameter), range.max());
                }
            }
        }
    }

    protected Class<?> getParameterGenericType(Parameter parameter) {
        Type parameterType = parameter.getParameterizedType();
        if (!ParameterizedType.class.isAssignableFrom(parameterType.getClass())) {
            throw new FathomException("Please specify a generic parameter type for '{}' of '{}'",
                    ControllerUtil.getParameterName(parameter), Util.toString(method));
        }

        ParameterizedType parameterizedType = (ParameterizedType) parameterType;
        try {
            Class<?> genericClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            return genericClass;
        } catch (ClassCastException e) {
            throw new FathomException("Please specify a generic parameter type for '{}' of '{}'",
                    ControllerUtil.getParameterName(parameter), Util.toString(method));
        }
    }

    protected Class<? extends ArgumentExtractor> getArgumentExtractor(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(ExtractWith.class)) {
                ExtractWith with = annotation.annotationType().getAnnotation(ExtractWith.class);
                Class<? extends ArgumentExtractor> extractorClass = with.value();
                return extractorClass;
            }
        }
        // if unspecified we use the ParamExtractor
        return ParamExtractor.class;
    }

    protected String describeType(Class<? extends Collection> collectionType, Class<?> objectType) {
        if (collectionType == null) {
            return objectType.getSimpleName();
        }
        return collectionType.getSimpleName() + "<" + objectType.getSimpleName() + ">";
    }

    protected void handleDeclaredReturnException(Exception e, Method method, Context context) {
        Class<? extends Exception> exceptionClass = e.getClass();
        for (Return declaredReturn : declaredReturns) {
            if (exceptionClass.isAssignableFrom(declaredReturn.onResult())) {
                context.status(declaredReturn.code());
                if (Strings.isNullOrEmpty(declaredReturn.description())) {
                    context.setLocal("message", e.getMessage());
                } else {
                    context.setLocal("message", declaredReturn.description());
                }
                log.warn("Handling declared return exception '{}' for '{}'", e.getMessage(), Util.toString(method));
                return;
            }
        }

        if (e instanceof FathomException) {
            // pass-through the thrown exception
            throw (FathomException) e;
        }

        // undeclared exception, wrap & throw
        throw new FathomException(e);
    }
}
