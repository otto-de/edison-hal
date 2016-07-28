package de.otto.edison.hal.example.web;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.example.shop.Product;
import de.otto.edison.hal.example.shop.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * REST controller for products.
 */
@RestController
public class ProductsController {

    @Autowired
    private ProductSearchService productSearch;

    /**
     * @return application/hal+json document containing links to the API.
     */
    @RequestMapping(
            path = "/api/products",
            method = RequestMethod.GET,
            produces = {"application/hal+json", "application/json"}
    )
    public HalRepresentation getProducts(@RequestParam(defaultValue = "false") final boolean embedded,
                                         @RequestParam(required = false) final String q) {
        return new ProductsHalJson(productSearch.searchFor(ofNullable(q)), embedded);
    }

    /**
     * @return application/hal+json document containing links to the API.
     */
    @RequestMapping(
            path = "/api/products/{productId}",
            method = RequestMethod.GET,
            produces = {"application/hal+json", "application/json"}
    )
    public HalRepresentation getProduct(@PathVariable final String productId,
                                        final HttpServletResponse response) throws IOException {
        Optional<Product> product = productSearch.findBy(productId);
        if (product.isPresent()) {
            return new ProductHalJson(product.get());
        } else {
            response.sendError(SC_NOT_FOUND, "Product " + productId + " not found");
            return null;
        }
    }

}
