package de.otto.edison.hal.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.edison.hal.HalRepresentation;

/**
 * Client-side HAL representation of a single product / book.
 *
 * Note that this example does not use all properties from the server-side
 * {@link de.otto.edison.hal.example.web.ProductHalJson} HalRepresentation!
 *
 * Book instances are only created by the parser, so we do not need to care about links.
 *
 * Created by guido on 27.07.16.
 */
public class BookHalJson extends HalRepresentation {

    @JsonProperty
    public String title;
    @JsonProperty
    public long retailPrice;

}
