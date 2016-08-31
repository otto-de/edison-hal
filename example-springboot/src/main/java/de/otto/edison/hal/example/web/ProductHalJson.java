package de.otto.edison.hal.example.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.example.shop.Product;

import static de.otto.edison.hal.Link.linkBuilder;
import static de.otto.edison.hal.Links.linkingTo;

/**
 * <p>
 *     HAL representation of a single product.
 * </p>
 * <p>
 *     This is an example on how to provide custom properties for a HAL document.
 * </p>
 */
class ProductHalJson extends HalRepresentation {

    @JsonProperty
    private String title;
    @JsonProperty
    private String description;
    @JsonProperty
    private long retailPrice;

    public ProductHalJson(final Product product) {
        super(
                linkingTo(
                        linkBuilder("self", "/api/products/" + product.id)
                                .withType("application/hal+json")
                                .withTitle(product.title)
                                .build(),
                        linkBuilder("collection", "/api/products")
                                .withTitle("All Products")
                                .withType("application/hal+json")
                                .build()
                )
        );

        title = product.title;
        description = product.description;
        retailPrice = product.retailPrice;
    }

}
