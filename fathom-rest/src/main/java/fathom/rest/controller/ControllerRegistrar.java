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

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Injector;
import fathom.conf.Settings;
import fathom.rest.RouteRegistration;
import fathom.utils.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects annotated controller routes.
 *
 * @author James Moger
 */
public class ControllerRegistrar extends ControllerScanner {

    private static final Logger log = LoggerFactory.getLogger(ControllerRegistrar.class);

    private final Injector injector;

    private final List<RouteRegistration> routeRegistrations;

    @Inject
    public ControllerRegistrar(Injector injector, Settings settings) {
        super(settings);
        this.injector = injector;
        this.routeRegistrations = new ArrayList<>();
    }

    public final void init(Package... packages) {
        List<String> names = new ArrayList<>();
        for (Package pkg : packages) {
            names.add(pkg.getName());
        }
        init(names.toArray(new String[names.size()]));
    }

    /**
     * Scans, identifies, and registers annotated controller methods for the
     * current runtime settings.
     *
     * @param packageNames
     */
    public final void init(String... packageNames) {

        Collection<Class<?>> classes = discoverClasses(packageNames);
        if (classes.isEmpty()) {
            log.warn("No annotated controllers found in package(s) '{}'", Arrays.toString(packageNames));
            return;
        }

        log.debug("Found {} controller classes in {} package(s)", classes.size(), packageNames.length);

        init(classes);
    }

    /**
     * Register all methods in the specified controller classes.
     *
     * @param controllers
     */
    public final void init(Class<? extends Controller>... controllers) {
        List<Class<?>> classes = Arrays.asList(controllers);
        init(classes);
    }

    private final void init(Collection<Class<?>> classes) {
        Map<Method, Class<? extends Annotation>> discoveredMethods = discoverMethods(classes);
        if (discoveredMethods.isEmpty()) {
            // if we are using the registrar we expect to discover controllers!
            log.warn("No annotated controller methods found in classes(s) '{}'", classes);
            return;
        }

        log.debug("Found {} annotated controller method(s)", discoveredMethods.size());

        registerControllerMethods(discoveredMethods);

        log.debug("Added {} annotated routes from '{}'", routeRegistrations.size(), classes);
    }

    /**
     * Return the collected route registrations.
     *
     * @return the route registrations
     */
    public List<RouteRegistration> getRouteRegistrations() {
        return routeRegistrations;
    }

    /**
     * Register the controller methods as Routes.
     *
     * @param discoveredMethods
     */
    private void registerControllerMethods(Map<Method, Class<? extends Annotation>> discoveredMethods) {

        Collection<Method> methods = sortMethods(discoveredMethods.keySet());

        Map<Class<? extends Controller>, Set<String>> controllers = new HashMap<>();
        for (Method method : methods) {
            Class<? extends Controller> controllerClass = (Class<? extends Controller>) method.getDeclaringClass();
            if (!controllers.containsKey(controllerClass)) {
                Set<String> paths = collectPaths(controllerClass);
                controllers.put(controllerClass, paths);
            }

            Class<? extends Annotation> httpMethodAnnotationClass = discoveredMethods.get(method);
            Annotation httpMethodAnnotation = method.getAnnotation(httpMethodAnnotationClass);

            String httpMethod = httpMethodAnnotation.annotationType().getAnnotation(HttpMethod.class).value();
            String[] methodPaths = ClassUtil.executeDeclaredMethod(httpMethodAnnotation, "value");

            Set<String> controllerPaths = controllers.get(controllerClass);
            if (controllerPaths.isEmpty()) {
                // add an empty string to allow controllerPaths iteration
                controllerPaths.add("");
            }

            for (String controllerPath : controllerPaths) {

                if (methodPaths.length == 0) {
                    // method does not specify a path, inherit from controller
                    String fullPath = StringUtils.addStart(StringUtils.removeEnd(controllerPath, "/"), "/");
                    ControllerHandler handler = new ControllerHandler(injector, controllerClass, method.getName());
                    handler.validateMethodArgs(fullPath);

                    RouteRegistration registration = new RouteRegistration(httpMethod, fullPath, handler);
                    configureRegistration(registration, method);
                    routeRegistrations.add(registration);
                } else {
                    // method specifies one or more paths, concatenate with controller paths
                    for (String methodPath : methodPaths) {
                        String path = Joiner.on("/").skipNulls().join(StringUtils.removeEnd(controllerPath, "/"), StringUtils.removeStart(methodPath, "/"));
                        String fullPath = StringUtils.addStart(StringUtils.removeEnd(path, "/"), "/");

                        ControllerHandler handler = new ControllerHandler(injector, controllerClass, method.getName());
                        handler.validateMethodArgs(fullPath);

                        RouteRegistration registration = new RouteRegistration(httpMethod, fullPath, handler);
                        configureRegistration(registration, method);
                        routeRegistrations.add(registration);
                    }
                }

            }

        }
    }

    private void configureRegistration(RouteRegistration registration, Method method) {
        // specify optional route name
        if (method.isAnnotationPresent(Named.class)) {
            Named named = method.getAnnotation(Named.class);
            registration.setName(named.value());
        }

        // specify optional or required content-type suffixes
        ContentTypeBySuffix bySuffix = ClassUtil.getAnnotation(method, ContentTypeBySuffix.class);
        if (bySuffix != null) {
            Collection<String> suffixes = ControllerUtil.getSuffixes(method);
            if (bySuffix.required()) {
                registration.requireContentTypeSuffixes(suffixes);
            } else {
                registration.contentTypeSuffixes(suffixes);
            }
        }
    }

}
