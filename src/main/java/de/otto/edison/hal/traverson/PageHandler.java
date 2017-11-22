/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package de.otto.edison.hal.traverson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import de.otto.edison.hal.EmbeddedTypeInfo;

import java.io.IOException;

/**
 * Functional interface used to traverse pages of linked or embedded resources.
 * <p>
 *     The different {@link Traverson#paginate(String, Class, EmbeddedTypeInfo, PageHandler) pagination} methods
 *     of the {@link Traverson} make use of PageHandlers to traverse a single page.
 * </p>
 * <pre><code>
 *     Traverson.traverson(this::fetchJson)
 *          .startWith("http://example.com/example/collection")
 *          .paginateNext( (Traverson pageTraverson) -&gt; {
 *              pageTraverson
 *                      .follow("item")
 *                      .streamAs(OtherExtendedHalRepresentation.class)
 *                      .forEach(x -&gt; values.add(x.someOtherProperty));
 *              return true;
 *          });
 * </code></pre>
 * @since 2.0.0
 */
@FunctionalInterface
public interface PageHandler {

    /**
     * Processes a single page and decides whether or not to proceed to the following page.
     *
     * @param traverson the Traverson used to process the current page.
     * @return true if traversion should continue, false if it should be aborted.
     *
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     */
    boolean apply(Traverson traverson) throws IOException;

}
