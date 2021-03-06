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

package fathom.rest.controller.extractors;

import fathom.rest.controller.Session;
import fathom.rest.Context;

/**
 * @author James Moger
 */
public class SessionExtractor implements NamedExtractor, ConfigurableExtractor<Session> {

    private String name;

    @Override
    public Class<Session> getAnnotationClass() {
        return Session.class;
    }

    @Override
    public void configure(Session param) {
        setName(param.value());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object extract(Context context) {
        Object o = context.getSession(name);
        return o;
    }
}
