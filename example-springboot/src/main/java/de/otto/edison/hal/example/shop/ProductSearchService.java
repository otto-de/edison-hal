package de.otto.edison.hal.example.shop;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 *
 * A service used to search for products.
 *
 */
@Service
public class ProductSearchService {

    /**
     * Some random list of products, consisting of REST books.
     */
    private final List<Product> products = new ArrayList<Product>() {{
        add(new Product(
                "REST in Practice: Hypermedia and Systems Architecture",
                "Why don't typical enterprise projects go as smoothly as projects you develop for the Web? Does the REST architectural style really present a viable alternative for building distributed systems and enterprise-class applications?\n" +
                        "\n" +
                        "In this insightful book, three SOA experts provide a down-to-earth explanation of REST and demonstrate how you can develop simple and elegant distributed hypermedia systems by applying the Web's guiding principles to common enterprise computing problems.",
                2795));
        add(new Product(
                "RESTful Web APIs",
                "The popularity of REST in recent years has led to tremendous growth in almost-RESTful APIs that don’t include many of the architecture’s benefits. With this practical guide, you’ll learn what it takes to design usable REST APIs that evolve over time. By focusing on solutions that cross a variety of domains, this book shows you how to create powerful and secure applications, using the tools designed for the world’s most successful distributed computing system: the World Wide Web.",
                2995));
        add(new Product(
                "Spring REST",
                "Spring REST is a practical guide for designing and developing RESTful APIs using the Spring Framework. This book walks you through the process of designing and building a REST application while taking a deep dive into design principles and best practices for versioning, security, documentation, error handling, paging, and sorting.",
                2651));
        add(new Product(
                "RESTful Web API Design with Node.js",
                "Create a fully featured RESTful API solution from scratch.\n" +
                        "Learn how to leverage Node.JS, Express, MongoDB and NoSQL datastores to give an extra edge to your REST API design.\n" +
                        "Use this practical guide to integrate MongoDB in your Node.js application.",
                2887));
        add(new Product(
                "RESTful Web API Handbook",
                "This book is an exploration of the Restful web application-programming interface (API). The book begins by explaining what the API is, how it is used, and where it is used. The book then guides you on how to set up the various resources which are necessary for development in REST.",
                1276));
        add(new Product(
                "Building a RESTful Web Service with Spring",
                "Follow best practices and explore techniques such as clustering and caching to achieve a scalable web service\n" +
                        "Leverage the Spring Framework to quickly implement RESTful endpoints\n" +
                        "Learn to implement a client library for a RESTful web service using the Spring Framework",
                2887));
    }};

    /**
     * Searches for products using a case-insensitive search term.
     *
     * @param searchTerm expression to search for
     * @return List of matching products, or an empty list.
     */
    public List<Product> searchFor(final Optional<String> searchTerm) {
        if (searchTerm.isPresent()) {
            return products
                    .stream()
                    .filter(matchingProductsFor(searchTerm.get()))
                    .collect(toList());
        } else {
            return products;
        }

    }

    /**
     * Searches for the product identified by {@code productId}
     *
     * @param productId nomen est omen
     * @return Optional product
     */
    public Optional<Product> findBy(final String productId) {
        return products.stream().filter(p->p.id.equals(productId)).findAny();
    }

    /**
     * @param searchTerm some search term
     * @return a Predicate used to match products against search terms.
     */
    private Predicate<Product> matchingProductsFor(final String searchTerm) {
        return p ->
                p.title.toLowerCase().contains(searchTerm.toLowerCase())
                || p.description.toLowerCase().contains(searchTerm.toLowerCase());
    }
}
