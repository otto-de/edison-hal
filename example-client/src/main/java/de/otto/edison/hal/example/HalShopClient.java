package de.otto.edison.hal.example;

import com.damnhandy.uri.template.UriTemplate;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static de.otto.edison.hal.HalParser.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.HalParser.parse;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.traverson.Traverson.traverson;
import static de.otto.edison.hal.traverson.Traverson.withVars;
import static java.lang.String.format;
import static org.apache.http.impl.client.HttpClients.createDefault;

/**
 * <p>
 * A client for the API of example-springboot.
 * </p>
 * <p>
 * The client is using Apache HttpClient to access the /api endpoint of the example server, in order
 * to retrieve the entry-point of the products collection resource. It is then following the links to
 * get the product information.
 * </p>
 */
public class HalShopClient implements AutoCloseable {

    private static final String HOST = "http://localhost:8080";
    private static final String HOME_URI = "/api";
    private static final String REL_SEARCH = "search";
    private static final String REL_PRODUCT = "http://localhost:8080/rels/product";

    private final CloseableHttpClient httpclient;

    /**
     * Create a HalShopClient with a default HttpClient.
     */
    public HalShopClient() {
        httpclient = createDefault();
    }

    /**
     * Traverses the API and renders all product links.
     *
     */
    public void printAllBooks() {
        System.out.println("\n\n");
        System.out.println("------------- All Books ----------------");
        final String uri = findSearchUriTemplate()
                .expand();
        final List<Link> productLinks = getHalRepresentation(link(REL_SEARCH, uri))
                .getLinks()
                .getLinksBy(REL_PRODUCT);
        System.out.println("|");
        productLinks
                .forEach(link -> {
                    System.out.println("|   * " + link.getTitle());
                });
        System.out.println("-----------------------------------------");
    }

    /**
     * <p>
     *     Traverses the API and searches for all products matching the query term. Products are mapped
     *     to BookHalJson instances and used to render additional information like prices that are not
     *     part of HAL+JSON format.
 *     </p>
     * <p>
     *     The 'search' Link used to retrieve products is a UriTemplate. Using this template,
     *     parameters like 'q' or 'embedded' can be set.
     * </p>
     * @param query some query term
     * @see <a href="https://github.com/damnhandy/Handy-URI-Templates">Handy-URI-Templates</a>
     */
    public void printPricesOfAllBooksAbout(final String query) {
        System.out.println("\n\n");
        System.out.println("------------- All Books about ----------");
        final String uri = findSearchUriTemplate()
                .set("q", query)
                .set("embedded", true)
                .expand();
        final List<BookHalJson> products = getHalRepresentation(link(REL_SEARCH, uri))
                .getEmbedded()
                .getItemsBy(REL_PRODUCT, BookHalJson.class);
        System.out.println("|");
        products
                .forEach(product -> {
                    System.out.println("|   * " + product.title + ": " + format("%.2f€", product.retailPrice/100.0));
                });
        System.out.println("----------------------------------------");
    }

    /**
     * <p>
     *     Traverses the API and searches for all products matching the query term.
 *     </p>
     * <p>
     *     This is similar to {@link #printPricesOfAllBooksAbout(String)}, but is using a Traverson to access
     *     the products.
     * </p>
     * @param query some query term
     * @see <a href="https://github.com/damnhandy/Handy-URI-Templates">Handy-URI-Templates</a>
     */
    public void traverseLinksUsingTraverson(final String query) {
        System.out.println("\n\n");
        System.out.println("---------- Traverson Example ----------");
        traverson(this::getHalJson)
                .startWith(HOME_URI)
                .follow(REL_SEARCH, withVars("q", query, "embedded", false))
                .follow(REL_PRODUCT)
                .streamAs(BookHalJson.class)
                .forEach(product->{
                    System.out.println("|   * " + product.title + ": " + format("%.2f€", product.retailPrice/100.0));
                });
        System.out.println("----------------------------------------");
    }

    /**
     * Retrieves the entry-point of the example server and returns the UriTemplate of the search link.
     *
     * @return UriTemplate
     * @see <a href="https://github.com/damnhandy/Handy-URI-Templates">Handy-URI-Templates</a>
     */
    private UriTemplate findSearchUriTemplate() {
        final Link searchLink = getHalRepresentation(self(HOME_URI))
                .getLinks()
                .getLinksBy(REL_SEARCH)
                .get(0);
        return fromTemplate(searchLink.getHref());
    }

    /**
     * Returns the REST resource identified by {@code uri} as a HalRepresentation.
     *
     * @param link the non-templated Link of the resource
     * @return HalRepresentation with optionally embedded BookHalJson items.
     */
    private HalRepresentation getHalRepresentation(final Link link) {
        try {
            return parse(getHalJson(link)).as(HalRepresentation.class, withEmbedded(REL_PRODUCT, BookHalJson.class));
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns the REST resource identified by {@code uri} as a JSON string.
     *
     * @param link the non-templated Link of the resource
     * @return json
     */
    private String getHalJson(final Link link) {
        try {
            final HttpGet httpget = new HttpGet(HOST + link.getHref());
            if (link.getType().isEmpty()) {
                httpget.addHeader("Accept", "application/hal+json");
            } else {
                httpget.addHeader("Accept", link.getType());
            }

            System.out.println("|   ------------- Request --------------");
            System.out.println("|   " + httpget.getRequestLine());

            final HttpEntity entity = httpclient.execute(httpget).getEntity();
            final String json = EntityUtils.toString(entity);
            System.out.println("|   ------------- Response -------------");
            System.out.println("|   " + json);
            return json;
        } catch (final IOException e) {
            System.out.println("\nPlease start example-springboot Server before you are running the Client.\n");
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Closes the Apache HttpClient.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        httpclient.close();
    }
}
