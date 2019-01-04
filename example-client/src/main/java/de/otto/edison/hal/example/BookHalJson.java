package de.otto.edison.hal.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.edison.hal.HalRepresentation;

/**
 * <p>
 * Client-side HAL representation of a single product / book.
 * </p>
 * <p>
 * Note that this example does not use all properties from the server-side
 * {@code de.otto.edison.hal.example.web.ProductHalJson} HalRepresentation!
 * </p>
 * <p>
 * Book instances are only created by the parser, so we do not need to care about links.
 * </p>
 */
public class BookHalJson extends HalRepresentation {

    @JsonProperty
    public String title;
    @JsonProperty
    public long retailPrice;

}
