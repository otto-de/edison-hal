package de.otto.edison.hal.example.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Returns HTML documentation for a single link-relation type.
 */
@Controller
public class RelsController {

    @RequestMapping(
            path = "/rels/{rel}",
            produces = {"text/html", "*/*"}
    )
    public String getRel(@PathVariable String rel) {
        return "/doc/" + rel + ".html";
    }

}
