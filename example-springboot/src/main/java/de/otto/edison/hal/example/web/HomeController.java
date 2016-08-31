package de.otto.edison.hal.example.web;

import de.otto.edison.hal.HalRepresentation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Link.templatedBuilder;
import static de.otto.edison.hal.Links.linkingTo;

@RestController
public class HomeController {

    /**
     * Entry point for the products REST API.
     *
     * @param request current request
     * @return application/hal+json document containing links to the API.
     */
    @RequestMapping(
            path = "/api",
            method = RequestMethod.GET,
            produces = {"application/hal+json", "application/json"}
    )
    public HalRepresentation getHomeDocument(final HttpServletRequest request) {
        final String homeUrl = request.getRequestURL().toString();
        return new HalRepresentation(
                linkingTo(
                        self(homeUrl),
                        templatedBuilder("search", "/api/products{?q,embedded}")
                                .withTitle("Search Products")
                                .withType("application/hal+json")
                                .beeingTemplated()
                                .build()
                )
        );
    }
}
