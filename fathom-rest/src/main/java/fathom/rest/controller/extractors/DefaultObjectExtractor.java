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

import fathom.exception.FathomException;
import fathom.rest.Context;
import ro.pippo.core.ParameterValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author James Moger
 */
abstract class DefaultObjectExtractor implements CollectionExtractor {

    protected Class<? extends Collection> collectionType;

    protected Class<?> objectType;

    @Override
    public void setCollectionType(Class<? extends Collection> collectionType) {
        if (collectionType.isInterface() && !(Set.class == collectionType || List.class == collectionType)) {
            throw new FathomException("Collection type '{}' is not supported!", collectionType.getName());
        }

        if (Set.class == collectionType) {
            this.collectionType = HashSet.class;
        } else if (List.class == collectionType) {
            this.collectionType = ArrayList.class;
        } else {
            this.collectionType = collectionType;
        }
    }

    @Override
    public void setObjectType(Class<?> objectType) {
        ParameterValue testValue = new ParameterValue();
        testValue.to(objectType);
        this.objectType = objectType;
    }

}
