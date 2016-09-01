package de.otto.edison.hal;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

/**
 * A Traverson is a utility that makes it easy to navigate REST APIs using HAL+JSON.
 * <p>
 *     {@link #startWith(String) Starting with} an URI to an initial resource, you can {@link #follow(String)} one or more
 *     links identified by their link-relation type. {@link Link#templated(String, String) Templated links} can be
 *     expanded using template {@link #withVars(String, Object, Object...) variables}.
 * </p>
 * <p>
 *     Example:
 * </p>
 * <pre><code>
 *        final HalRepresentation hal = traverson(this::myHttpGetFunction)
 *                .startWith("/example")
 *                .follow("foobar")
 *                .follow(
 *                        hops("foo", "bar"),
 *                        withVars("param1", "value1", "param2", "value2"))
 *                .get();
 * </code></pre>
 * <p>
 *     Embedded resources are used instead of linked resources, if present.
 * </p>
 * @since 0.4.0
 */
public class Traverson {


    private final Function<String, String> uriToJsonFunc;
    private volatile HalRepresentation current;

    private Traverson(final Function<String,String> uriToJsonFunc) {
        this.uriToJsonFunc = uriToJsonFunc;
    }

    /**
     * <p>
     *     Create a Traverson that is using the given {@link Function} to resolve an URI and return a HAL+JSON document.
     * </p>
     * <p>
     *     Typical implementations of the Function will rely on some HTTP client.
     * </p>
     * @param getJsonFromUri A Function that gets an URI of a resource and returns a HAL+JSON document.
     * @return Traverson
     */
    public static Traverson traverson(final Function<String,String> getJsonFromUri) {
        return new Traverson(getJsonFromUri);
    }

    /**
     * Creates a Map containing key-value pairs used as parameters for UriTemplate variables.
     * <p>
     *     Basically, this method is only adding some semantic sugar, so you can write code like this:
     * </p>
     * <pre><code>
     *     traverson(this::httpGet)
     *             .startWith("http://example.org")
     *             .follow("foo", withVars("param1", "value1", "param2", "value2"))
     *             .get();
     * </code></pre>
     *
     * @param key the first key
     * @param value the value associated with the first key
     * @param more Optionally more key-value pairs. The number of parameters is must even, keys must be Strings.
     * @return Map
     */
    public static Map<String, Object> withVars(final String key, final Object value, final Object... more) {
        if (more != null && more.length % 2 != 0) {
            throw new IllegalArgumentException("The number of template variables (key-value pairs) must be even");
        }
        final Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        if (more != null) {
            for (int i=0; i<more.length/2; i+=2) {
                map.put(more[i].toString(), more[i+1]);
            }
        }
        return map;
    }

    /**
     * Creates a list of link-relation types used to {@link #follow(List) follow} multiple hops, optionally
     * using {@link #follow(List, Map) uri template variables}.
     * <p>
     *     Basically, this method is only adding some semantic sugar, so you can write code like this:
     * </p>
     * <pre><code>
     *     traverson(this::httpGet)
     *             .startWith("http://example.org")
     *             .follow(
     *                     hops("foo", "bar"),
     *                     withVars("param1", "value1", "param2", "value2"))
     *             .get();
     * </code></pre>
     *
     * @param rel link-relation type
     * @param moreRels optionally more link-relation types
     * @return list of link-relation types
     */
    public static List<String> hops(final String rel, final String... moreRels) {
        if (moreRels != null) {
            return new ArrayList<String>() {{
                add(rel);
                addAll(asList(moreRels));
            }};
        } else {
            return singletonList(rel);
        }
    }

    /**
     * Start traversal at the application/hal+json resource idenfied by {@code uri}.
     *
     * @param uri the {@code URI} of the initial HAL resource.
     * @return Traverson initialized with the {@link HalRepresentation} identified by {@code uri}.
     */
    public Traverson startWith(final String uri) {
        this.current = getAndParse(uri, HalRepresentation.class);
        return this;
    }

    /**
     * Follow the first {@link Link} of the current resource, selected by it's link-relation type.
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param rel the link-relation type of the followed link
     * @return this
     */
    public Traverson follow(final String rel) {
        return follow(rel, emptyMap());
    }

    /**
     * Follow multiple link-relation types, one by one.
     * <p>
     *     Embedded items are used instead of resolving links, if present in the returned HAL documents.
     * </p>
     *
     * @param rels the link-relation types of the followed links
     * @return this
     */
    public Traverson follow(final List<String> rels) {
        return follow(rels, emptyMap());
    }

    /**
     * Follow multiple link-relation types, one by one.
     * <p>
     *     Templated links are resolved to URIs using the specified template variables.
     * </p>
     * <p>
     *     Embedded items are used instead of resolving links, if present in the returned HAL documents.
     * </p>
     *
     * @param rels the link-relation types of the followed links
     * @param vars uri-template variables used to build links.
     * @return this
     */
    public Traverson follow(final List<String> rels, final Map<String, Object> vars) {
        for (String rel : rels) {
            follow(rel, vars);
        }
        return this;
    }

    /**
     * Follow the first {@link Link} of the current resource, selected by it's link-relation type.
     * <p>
     *     Templated links are resolved to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param rel the link-relation type of the followed link
     * @return this
     */
    public Traverson follow(final String rel, final Map<String, Object> vars) {
        checkState();
        final List<HalRepresentation> items = this.current.getEmbedded().getItemsBy(rel);
        if (items.size() > 0) {
            this.current = items.get(0);
        } else {
            final Optional<Link> optionalLink = this.current.getLinks().getLinkBy(rel);
            if (optionalLink.isPresent()) {
                final Link link = optionalLink.get();
                if (link.isTemplated()) {
                    this.current = getAndParse(fromTemplate(link.getHref()).expand(vars), HalRepresentation.class);
                } else {
                    this.current = getAndParse(link.getHref(), HalRepresentation.class);
                }
            } else {
                throw new IllegalStateException("Link with rel=" + rel + " not found in resource.");
            }
        }
        return this;
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by it's link-relation type and returns a {@Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param rel the link-relation type of the followed link
     * @return this
     */
    public Stream<HalRepresentation> stream(final String rel) {
        return stream(rel, emptyMap(), HalRepresentation.class);
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by it's link-relation type and returns a {@Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     Templated links are resolved to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param rel the link-relation type of the followed link
     * @return this
     */
    public Stream<HalRepresentation> stream(final String rel, final Map<String, Object> vars) {
        return stream(rel, vars, HalRepresentation.class);
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by it's link-relation type and returns a {@Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param type the type of the returned HalRepresentations
     * @return this
     */
    public <T extends HalRepresentation> Stream<T> stream(final String rel, final Class<T> type) {
        return stream(rel, emptyMap(), type);
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by it's link-relation type and returns a {@Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     Templated links are resolved to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param type the type of the returned HalRepresentations
     * @return this
     */
    public <T extends HalRepresentation> Stream<T> stream(final String rel, final Map<String, Object> vars, final Class<T> type) {
        checkState();

        // TODO: Get embedded items as Stream. If type is something different than HalRepresentation.class, this will fail,
        // because we would have to get the embedded items as type...
        final List<HalRepresentation> items = this.current.getEmbedded().getItemsBy(rel);
        if (items.size() > 0) {
            return items.stream().map(type::cast);
        } else {
            final List<Link> links = this.current.getLinks().getLinksBy(rel);
            return links.stream().map(link->{
                if (link.isTemplated()) {
                    return getAndParse(fromTemplate(link.getHref()).expand(vars), type);
                } else {
                    return getAndParse(link.getHref(), type);
                }
            });
        }
    }

    /**
     * Return the current HalRepresentation.
     *
     * @return HalRepresentation
     */
    public HalRepresentation get() {
        checkState();
        return current;
    }

    /**
     * Retrieve the HAL resource identified by {@code uri} and return the representation as a subtype of HalRepresentation
     *
     * @param uri the URI of the resource to retrieve
     * @param type the type of the HalRepresentation
     * @param <T> the concrete type of the returned representation object
     * @return T
     */
    private <T extends HalRepresentation> T getAndParse(final String uri, final Class<T> type) {
        try {
            return HalParser
                    .parse(uriToJsonFunc.apply(uri))
                    .as(type);
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Checks the current state of the Traverson.
     *
     * @throws IllegalStateException if some error occured during traversion
     */
    private void checkState() {
        if (this.current == null) {
            throw new IllegalStateException("Please call startWith(uri) first.");
        }
    }
}
