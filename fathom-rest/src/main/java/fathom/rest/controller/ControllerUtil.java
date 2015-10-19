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

import com.google.common.base.Strings;
import fathom.rest.Context;
import fathom.rest.controller.extractors.ArgumentExtractor;
import fathom.rest.controller.extractors.ExtractWith;
import fathom.rest.controller.extractors.ParamExtractor;
import fathom.utils.ClassUtil;
import ro.pippo.core.route.RouteHandler;
import ro.pippo.core.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author James Moger
 */
public class ControllerUtil {

    public static List<Class<? extends RouteHandler<Context>>> collectRouteInterceptors(Method method) {
        List<Class<? extends RouteHandler<Context>>> classList =
                ClassUtil.collectNestedAnnotation(method, RouteInterceptor.class).stream()
                        .map(RouteInterceptor::value)
                        .collect(Collectors.toList());
        return classList;
    }

    public static List<String> getConsumes(Method method) {
        Set<String> types = new LinkedHashSet<>();
        Consumes consumes = ClassUtil.getAnnotation(method, Consumes.class);
        if (consumes != null) {
            for (String value : consumes.value()) {
                types.add(value.trim());
            }
        }
        return new ArrayList<>(types);
    }

    public static List<String> getProduces(Method method) {
        Set<String> contentTypes = new LinkedHashSet<>();
        Produces produces = ClassUtil.getAnnotation(method, Produces.class);
        if (produces != null) {
            for (String value : produces.value()) {
                contentTypes.add(value.trim());
            }
        }
        return new ArrayList<>(contentTypes);
    }

    public static Collection<String> getSuffixes(Method method) {
        Set<String> suffixes = new LinkedHashSet<>();
        for (String produces : getProduces(method)) {
            int i = produces.lastIndexOf('/') + 1;
            String type = StringUtils.removeStart(produces.substring(i).toLowerCase(), "x-");
            suffixes.add(type);
        }
        return suffixes;
    }

    public static Collection<Return> getReturns(Method method) {
        Map<Integer, Return> returns = new TreeMap<>();
        if (method.getDeclaringClass().isAnnotationPresent(Returns.class)) {
            for (Return aReturn : method.getDeclaringClass().getAnnotation(Returns.class).value()) {
                returns.put(aReturn.code(), aReturn);
            }
        }
        if (method.getDeclaringClass().isAnnotationPresent(Return.class)) {
            Return aReturn = method.getDeclaringClass().getAnnotation(Return.class);
            returns.put(aReturn.code(), aReturn);
        }
        if (method.isAnnotationPresent(Returns.class)) {
            for (Return aReturn : method.getAnnotation(Returns.class).value()) {
                returns.put(aReturn.code(), aReturn);
            }
        }
        if (method.isAnnotationPresent(Return.class)) {
            Return aReturn = method.getAnnotation(Return.class);
            returns.put(aReturn.code(), aReturn);
        }
        return returns.values();
    }

    /**
     * Returns the name of a parameter.
     *
     * @param parameter
     * @return the name of a parameter.
     */
    public static String getParameterName(Parameter parameter) {
        // identify parameter name and pattern from method signature
        String methodParameterName = parameter.getName();
        if (parameter.isAnnotationPresent(Param.class)) {
            Param param = parameter.getAnnotation(Param.class);
            if (!Strings.isNullOrEmpty(param.value())) {
                methodParameterName = param.value();
            }
        }
        return methodParameterName;
    }

    /**
     * Returns the appropriate ArgumentExtractor to use for the controller method parameter.
     *
     * @param parameter
     * @return an argument extractor
     */
    public static Class<? extends ArgumentExtractor> getArgumentExtractor(Parameter parameter) {
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

    /**
     * Removes trailing wildcards from a content type as long as the content type is not a
     * universal wildcard content type like '*' or '*\*'.
     *
     * @param contentTypes
     * @return the list of content types
     */
    public static List<String> cleanupFuzzyContentTypes(List<String> contentTypes) {
        if (contentTypes == null || contentTypes.isEmpty()) {
            return contentTypes;
        }

        List<String> types = new ArrayList<>();
        for (String contentType : contentTypes) {
            if (contentType.equals("*") || contentType.equals("*/*")) {
                types.add(contentType);
                continue;
            }
            int i = contentType.indexOf('*');
            if (i > -1) {
                types.add(contentType.substring(0, i));
            } else {
                types.add(contentType);
            }
        }

        return types;
    }
}
