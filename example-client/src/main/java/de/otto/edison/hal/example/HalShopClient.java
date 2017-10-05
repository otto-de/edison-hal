package de.otto.edison.hal.example;

import de.otto.edison.hal.Link;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
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

    private static final String HOME_URI = "http://localhost:8080/api";
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
     * <p>
     *     Traverses the API and searches for all products matching the query term.
     *     </p>
     *
     * @param query some query term
     * @param embeddedProducts query for embedded products
     * @see <a href="https://github.com/damnhandy/Handy-URI-Templates">Handy-URI-Templates</a>
     */
    public void traverse(final String query, final boolean embeddedProducts) {
        System.out.println("\n\n");
        System.out.println("---------- Traverson Example ----------");
        traverson(this::getHalJson)
                .startWith(HOME_URI)
                .follow(REL_SEARCH, withVars("q", query, "embedded", embeddedProducts))
                .follow(REL_PRODUCT)
                .streamAs(BookHalJson.class)
                .forEach(product->{
                    System.out.println("|   * " + product.title + ": " + format("%.2f€", product.retailPrice/100.0));
                });
        System.out.println("----------------------------------------");
    }

    /**
     * Returns the REST resource identified by {@code uri} as a JSON string.
     *
     * @param link the non-templated Link of the resource
     * @return json
     */
    private String getHalJson(final Link link) {
        try {
            final HttpGet httpget = new HttpGet(link.getHref());
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
