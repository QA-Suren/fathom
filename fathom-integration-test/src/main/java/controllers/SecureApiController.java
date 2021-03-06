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

package controllers;

import com.google.inject.Inject;
import dao.ItemDao;
import fathom.metrics.Metered;
import fathom.realm.Account;
import fathom.rest.controller.Auth;
import fathom.rest.controller.Controller;
import fathom.rest.controller.GET;
import fathom.rest.controller.Path;
import fathom.rest.controller.Produces;
import fathom.rest.controller.Return;
import fathom.rest.security.aop.RequirePermission;
import fathom.rest.swagger.Undocumented;
import models.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To be discoverable, a controller must be annotated with {@code @Path}
 */
@Path("/secure/")
@Undocumented
public class SecureApiController extends Controller {

    private final Logger log = LoggerFactory.getLogger(SecureApiController.class);

    @Inject
    ItemDao dao;

    /**
     * Responds to a GET request of an integer id like "/secure/1".
     * <p>
     * Notice that the {@code id} parameter is specified in the
     * {@link @GET} annotation and in the method signature.
     * </p>
     * <p>
     * This technique is relying on use of the Java 8 {@code -parameters}
     * flag passed to {@code javac}.  That flag preserves method parameter
     * names in the compiled class files.
     * </p>
     * <p>
     * This same technique is applied to the {@code @Subject} annotation which
     * references an object in the current session named "subject".
     * </p>
     *
     * @param id
     * @param account
     */
    @GET("{id: [0-9]+}")
    @Produces({Produces.JSON, Produces.XML})
    @Metered
    @RequirePermission("secure:view")
    @Return(code = 200, description = "Item retrieved", onResult = Item.class)
    @Return(code = 404, description = "Item does not exist")
    public Item get(int id, @Auth Account account) {

        // Enforce a required permission (see Components.java).
        // Alternatively we could skip @RequirePermission and
        // check manually:

        // account.checkPermission("secure:view");

        log.debug("GET item #{} for '{}'", id, account);
        Item item = dao.get(id);
        return item;
    }

}
