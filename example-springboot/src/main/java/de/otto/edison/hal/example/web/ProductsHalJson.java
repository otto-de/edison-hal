package de.otto.edison.hal.example.web;

import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.example.shop.Product;

import java.util.List;

import static de.otto.edison.hal.Embedded.embeddedBuilder;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Link.linkBuilder;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Link.templatedBuilder;
import static de.otto.edison.hal.Links.linksBuilder;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;

/**
 * HAL reprensentation of a list of products.
 */
public class ProductsHalJson extends HalRepresentation {

    public ProductsHalJson(final Product product, final boolean embedded) {
        this(singletonList(product), embedded);
    }

    public ProductsHalJson(final List<Product> products, final boolean embedded) {
        super(
                linksBuilder()
                        .with(self(fromCurrentRequestUri().toUriString()))
                        .with(templatedBuilder("search", "/api/products{?q,embedded}")
                                .withTitle("Search Products")
                                .withType("application/hal+json")
                                .beeingTemplated()
                                .build())
                        .with(products
                                .stream()
                                .map(b -> linkBuilder("product", "/api/products/" + b.id)
                                        .withTitle(b.title)
                                        .withType("application/hal+json")
                                        .build())
                                .collect(toList()))
                        .build(),
                embedded ? withEmbedded(products) : emptyEmbedded()
        );
    }

    private static Embedded withEmbedded(final List<Product> products) {
        return embeddedBuilder()
                .with("product", products
                        .stream()
                        .map(ProductHalJson::new)
                        .collect(toList()))
                .build();
    }
}
