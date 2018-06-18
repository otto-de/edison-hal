package de.otto.edison.hal.traverson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.edison.hal.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static de.otto.edison.hal.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.HalParser.parse;
import static de.otto.edison.hal.Link.copyOf;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.LinkPredicates.always;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A Traverson is a utility that makes it easy to navigate REST APIs using HAL+JSON.
 * <p>
 *     {@link #startWith(String) Starting with} a href to an initial resource, you can {@link #follow(String) follow}
 *     one or more links identified by their link-relation type.
 * </p>
 * <p>
 *     {@link Link#isTemplated()}  Templated links} can be expanded using template
 *     {@link #withVars(String, Object, Object...) variables}.
 * </p>
 * <p>
 *     Paginated resources can be processed using the different {@link #paginateNext(PageHandler)},
 *     {@link #paginateNextAs(Class, PageHandler)}, etc. methods.
 * </p>
 * <p>
 *     Example:
 * </p>
 * <pre><code>
 *        final Optional&lt;HalRepresentation&gt; hal = traverson(this::myHttpGetFunction)
 *                .startWith("http://example.org")
 *                .follow("foobar")
 *                .follow(
 *                        hops("foo", "bar"),
 *                        withVars("param1", "value1", "param2", "value2"))
 *                .getResource();
 * </code></pre>
 * <p>
 *     Embedded resources are used instead of linked resources, if present.
 * </p>
 * @since 0.4.0
 */
public class Traverson {

    static class Hop {
        /** Link-relation type of the hop. */
        final String rel;
        final Predicate<Link> predicate;
        /** URI-template variables used when following the hop. */
        final Map<String,Object> vars;
        /** Ignore possibly embedded items. */
        final boolean ignoreEmbedded;

        Hop(final String rel,
            final Predicate<Link> predicate,
            final Map<String, Object> vars,
            final boolean ignoreEmbedded) {
            this.rel = rel;
            this.predicate = predicate;
            this.vars = vars;
            this.ignoreEmbedded = ignoreEmbedded;
        }
    }

    private static final Logger LOG = getLogger(Traverson.class);

    public static final ObjectMapper DEFAULT_JSON_MAPPER = new ObjectMapper();
    static {
        DEFAULT_JSON_MAPPER.configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        DEFAULT_JSON_MAPPER.findAndRegisterModules();
    }

    private final ObjectMapper objectMapper;
    private final LinkResolver linkResolver;
    private final List<Hop> hops = new ArrayList<>();
    private URL startWith;
    private URL contextUrl;
    private List<? extends HalRepresentation> lastResult;

    private Traverson(final LinkResolver linkResolver, final ObjectMapper objectMapper) {
        this.linkResolver = linkResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * <p>
     *     Create a Traverson that is using the given {@link Function} to resolve a Link and return a HAL+JSON document.
     * </p>
     * <p>
     *     The function will be called by the Traverson, whenever a Link must be followed. The Traverson will
     *     take care of URI templates (templated links), so the implementation of the function can rely on the
     *     Link parameter to be not {@link Link#isTemplated() templated}.
     * </p>
     * <p>
     *     Typical implementations of the Function will rely on some HTTP client. Especially in this case,
     *     the function should take care of the link's {@link Link#getType() type} and {@link Link#getProfile()},
     *     so the proper HTTP Accept header is used.
     * </p>
     * @param linkResolver A function that gets a Link of a resource and returns a HAL+JSON document.
     * @return Traverson
     */
    public static Traverson traverson(final LinkResolver linkResolver) {
        return new Traverson(linkResolver, DEFAULT_JSON_MAPPER);
    }

    /**
     * <p>
     *     Create a Traverson that is using the given {@link Function} to resolve a Link and return a HAL+JSON document.
     * </p>
     * <p>
     *     The function will be called by the Traverson, whenever a Link must be followed. The Traverson will
     *     take care of URI templates (templated links), so the implementation of the function can rely on the
     *     Link parameter to be not {@link Link#isTemplated() templated}.
     * </p>
     * <p>
     *     Typical implementations of the Function will rely on some HTTP client. Especially in this case,
     *     the function should take care of the link's {@link Link#getType() type} and {@link Link#getProfile()},
     *     so the proper HTTP Accept header is used.
     * </p>
     * @param linkResolver A function that gets a Link of a resource and returns a HAL+JSON document.
     * @param objectMapper The ObjectMapper instance used to parse documents.
     * @return Traverson
     */
    public static Traverson traverson(final LinkResolver linkResolver,
                                      final ObjectMapper objectMapper) {
        if (objectMapper.isEnabled(ACCEPT_SINGLE_VALUE_AS_ARRAY)) {
            return new Traverson(linkResolver, objectMapper);
        } else {
            return new Traverson(linkResolver, objectMapper
                    .copy()
                    .configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true));
        }
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
     *             .getResource();
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
     *     This method is only adding some semantic sugar, so you can write code like this:
     * </p>
     * <pre><code>
     *     traverson(this::httpGet)
     *             .startWith("http://example.org")
     *             .follow(
     *                     <strong>hops("foo", "bar"),</strong>
     *                     withVars("param1", "value1", "param2", "value2"))
     *             .getResource();
     * </code></pre>
     *
     * @param rel link-relation type
     * @param moreRels optionally more link-relation types
     * @return list of link-relation types
     */
    public static List<String> hops(final String rel, final String... moreRels) {
        if (moreRels != null) {
            return new ArrayList<String>() {{
                add(requireNonNull(rel));
                addAll(asList(moreRels));
            }};
        } else {
            return singletonList(requireNonNull(rel));
        }
    }

    /**
     * Start traversal at the application/hal+json resource idenfied by {@code uri}.
     *
     * @param uriTemplate the {@code URI} of the initial HAL resource.
     * @param vars uri-template variables used to build links.
     * @return Traverson initialized with the {@link HalRepresentation} identified by {@code uri}.
     */
    public Traverson startWith(final String uriTemplate, final Map<String, Object> vars) {
        return startWith(fromTemplate(uriTemplate).expand(vars));
    }

    /**
     * Start traversal at the application/hal+json resource idenfied by {@code uri}.
     *
     * @param uri the {@code URI} of the initial HAL resource.
     * @return Traverson initialized with the {@link HalRepresentation} identified by {@code uri}.
     */
    public Traverson startWith(final String uri) {
        startWith = hrefToUrl(uri);
        contextUrl = startWith;
        lastResult = null;
        return this;
    }

    /**
     * Start traversal at the given HAL resource.
     *
     * <p>
     *     It is expected, that the HalRepresentation has an absolute 'self' link, or that all links to other
     *     resources are absolute links. If this is not assured, {@link #startWith(URL, HalRepresentation)} must
     *     be used, so relative links can be resolved.
     * </p>
     *
     * @param resource the initial HAL resource.
     * @return Traverson initialized with the specified {@link HalRepresentation}.
     */
    public Traverson startWith(final HalRepresentation resource) {
        this.startWith = null;
        this.lastResult = singletonList(requireNonNull(resource));
        Optional<Link> self = resource.getLinks().getLinkBy("self");
        if (self.isPresent()) {
                this.contextUrl = linkToUrl(self.get());
        } else {
            resource
                    .getLinks()
                    .stream()
                    .filter(link -> !link.getHref().matches("http.*//.*"))
                    .findAny()
                    .ifPresent(link -> {throw new IllegalArgumentException("Unable to construct Traverson from HalRepresentation w/o self link but containing relative links. Please try Traverson.startWith(URL, HalRepresentation)");});
        }
        return this;
    }

    /**
     * Start traversal at the given HAL resource, using the {@code contextUrl} to resolve relative links.
     *
     * <p>
     *     If the {@code resource} is obtained from another Traverson, {@link Traverson#getCurrentContextUrl()}
     *     can be called to get this URL.
     * </p>
     *
     * @param contextUrl URL of the Traverson's current context, used to resolve relative links
     * @param resource the initial HAL resource.
     * @return Traverson initialized with the specified {@link HalRepresentation} and {@code contextUrl}.
     * @since 1.0.0
     */
    public Traverson startWith(final URL contextUrl, final HalRepresentation resource) {
        this.startWith = null;
        this.contextUrl = requireNonNull(contextUrl);
        this.lastResult = singletonList(requireNonNull(resource));
        return this;
    }

    /**
     * Follow the first {@link Link} of the current resource, selected by its link-relation type.
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of resolving the associated {@link Link}.
     * </p>
     * <p>
     *     Sometimes, only a subset of a linked resource is embedded into the resource. In this case,
     *     embedded items can be ignored by using {@link #followLink(String)} instead of this method.
     * </p>
     * @param rel the link-relation type of the followed link
     * @return this
     */
    public Traverson follow(final String rel) {
        return follow(rel, always(), emptyMap());
    }

    /**
     * Follow the first {@link Link} of the current resource, selected by its link-relation type.
     * <p>
     *     Other than {@link #follow(String)}, this method will ignore embedded resources, if a link with matching
     *     link-relation type is present, Only if the link is missing, an optional embedded resource is used.
     * </p>
     * @param rel the link-relation type of the followed link
     * @return this
     * @since 2.0.0
     */
    public Traverson followLink(final String rel) {
        return followLink(rel, always(), emptyMap());
    }

    /**
     * Follow the first {@link Link} of the current resource that is matching the link-relation type and
     * the {@link LinkPredicates predicate}.
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of resolving the associated {@link Link}.
     * </p>
     * <p>
     *     Sometimes, only a subset of a linked resource is embedded into the resource. In this case,
     *     embedded items can be ignored by using {@link #followLink(String,Predicate)} instead of this method.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param predicate the predicate used to select the link to follow
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final String rel,
                            final Predicate<Link> predicate) {
        return follow(rel, predicate, emptyMap());
    }

    /**
     * Follow the first {@link Link} of the current resource that is matching the link-relation type and
     * the {@link LinkPredicates predicate}.
     * <p>
     *     Other than {@link #follow(String, Predicate)}, this method will ignore embedded resources, if a link
     *     with matching link-relation type is present, Only if the link is missing, an optional embedded resource is
     *     used.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param predicate the predicate used to select the link to follow
     * @return this
     * @since 2.0.0
     */
    public Traverson followLink(final String rel,
                                final Predicate<Link> predicate) {
        return followLink(rel, predicate, emptyMap());
    }

    /**
     * Follow multiple link-relation types, one by one.
     * <p>
     *     Embedded items are used instead of resolving links, if present in the returned HAL documents.
     * </p>
     *
     * @param rels the link-relation types of the followed links
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final List<String> rels) {
        return follow(rels, always(), emptyMap());
    }

    /**
     * Follow multiple link-relation types, one by one, and select the links using the specified
     * {@link LinkPredicates predicate}.
     * <p>
     *     Embedded items are used instead of resolving links, if present in the returned HAL documents.
     * </p>
     *
     * @param rels the link-relation types of the followed links
     * @param predicate the predicated used to select the link to follow
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final List<String> rels, 
                            final Predicate<Link> predicate) {
        return follow(rels, predicate, emptyMap());
    }

    /**
     * Follow multiple link-relation types, one by one.
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     Embedded items are used instead of resolving links, if present in the returned HAL documents.
     * </p>
     *
     * @param rels the link-relation types of the followed links
     * @param vars uri-template variables used to build links.
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final List<String> rels, 
                            final Map<String, Object> vars) {
        for (String rel : rels) {
            follow(rel, always(), vars);
        }
        return this;
    }

    /**
     * Follow multiple link-relation types, one by one. The {@link LinkPredicates predicate} is used to select
     * the matching links, if there are more than one per link-relation type.
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     Embedded items are used instead of resolving links, if present in the returned HAL documents.
     * </p>
     *
     * @param rels the link-relation types of the followed links
     * @param predicate the predicate used to select the link to follow
     * @param vars uri-template variables used to build links.
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final List<String> rels, 
                            final Predicate<Link> predicate, 
                            final Map<String, Object> vars) {
        for (String rel : rels) {
            follow(rel, predicate, vars);
        }
        return this;
    }

    /**
     * Follow the first {@link Link} of the current resource, selected by its link-relation type.
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of resolving the associated {@link Link}.
     * </p>
     * <p>
     *     Sometimes, only a subset of a linked resource is embedded into the resource. In this case,
     *     embedded items can be ignored by using {@link #followLink(String,Map)} instead of this method.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param vars uri-template variables used to build links.
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final String rel, 
                            final Map<String, Object> vars) {
        return follow(rel, always(), vars);
    }

    /**
     * Follow the first {@link Link} of the current resource, selected by its link-relation type.
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     Other than {@link #follow(String, Map)}, this method will ignore embedded resources, if a link
     *     with matching link-relation type is present, Only if the link is missing, an optional embedded resource is
     *     used.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param vars uri-template variables used to build links.
     * @return this
     * @since 2.0.0
     */
    public Traverson followLink(final String rel, 
                                final Map<String, Object> vars) {
        return followLink(rel, always(), vars);
    }

    /**
     * Follow the first {@link Link} of the current resource, selected by its link-relation type. The
     * {@link LinkPredicates predicate} is used to select the matching link, if multiple links per link-relation type
     * are available.
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of resolving the associated {@link Link}.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param predicate the predicate used to select the link to follow.
     * @param vars uri-template variables used to build links.
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final String rel, 
                            final Predicate<Link> predicate, 
                            final Map<String, Object> vars) {
        checkState();
        hops.add(new Hop(rel, predicate, vars, false));
        return this;
    }

    /**
     * Follow the first {@link Link} of the current resource, selected by its link-relation type. The
     * {@link LinkPredicates predicate} is used to select the matching link, if multiple links are available for the
     * specified link-relation type.
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     Other than {@link #follow(String, Predicate, Map)}, this method will ignore embedded resources, if a link
     *     with matching link-relation type is present, Only if the link is missing, an optional embedded resource is
     *     used.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param predicate the predicate used to select the link to follow.
     * @param vars uri-template variables used to build links.
     * @return this
     * @since 2.0.0
     */
    public Traverson followLink(final String rel, 
                                final Predicate<Link> predicate, 
                                final Map<String, Object> vars) {
        checkState();
        hops.add(new Hop(rel, predicate, vars, true));
        return this;
    }

    /**
     * Iterates over pages by following 'next' links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
     *
     * <pre>
     *                              next
     *     ... --&gt; HalRepresentation --&gt; HalRepresentation
     *                     |                     |
     *                     v N item              v N item
     *              HalRepresentation     HalRepresentation
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a {@link HalRepresentation} with no type information. If pages may contain
     *     embedded items, and if a specific sub-type of HalRepresentation is required for items,
     *     {@link #paginateNext(EmbeddedTypeInfo,PageHandler)} or {@link #paginateNextAs(Class,EmbeddedTypeInfo,PageHandler)}
     *     should be used instead of this method.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param pageHandler the callback used to process pages of items.
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public void paginateNext(final PageHandler pageHandler) throws IOException {
        paginateAs("next", HalRepresentation.class, null, pageHandler);
    }

    /**
     * Iterates over pages by following 'next' links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
     *
     * <pre>
     *                              next
     *     ... --&gt; HalRepresentation --&gt; HalRepresentation
     *                     |                     |
     *                     v N item              v N item
     *               &lt;embedded type&gt;      &lt;embedded type&gt;
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a {@link HalRepresentation} with {@link EmbeddedTypeInfo}. This way it
     *     is possible to access items embedded into the page resources as specific subtypes of HalRepresentation.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param embeddedTypeInfo type information about possibly embedded items.
     * @param pageHandler the callback used to process pages of items.
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public void paginateNext(final EmbeddedTypeInfo embeddedTypeInfo,
                             final PageHandler pageHandler) throws IOException {
        paginateAs("next", HalRepresentation.class, embeddedTypeInfo, pageHandler);
    }

    /**
     * Iterates over pages by following 'next' links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
     *
     * <pre>
     *                        next
     *     ... --&gt; &lt;page type&gt; --&gt; &lt;page type&gt;
     *                 |                 |
     *                 v N item          v N item
     *         HalRepresentation  HalRepresentation
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a representation of {@code pageType}, but without
     *     {@link EmbeddedTypeInfo}. This way it
     *     is possible to access special attributes of the page by calling {@code pageTraverson.getResourceAs(pageType)}
     *     in the callback function. If the paged items need to be accessed as a subtype of HalRepresentation, you
     *     can call pageTraverson.follow(rel).streamAs(MyItemType.class) - but ONLY if the items are not embedded into
     *     the page.
     * </p>
     *
     * <p>
     *     For embedded items having a subtype of HalRepresentation, {@link #paginateNextAs(Class, EmbeddedTypeInfo, PageHandler)}
     *     must be used instead of this method, otherwise a {@code ClassCastException} will be thrown.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param pageType the subtype of HalRepresentation of the page resources
     * @param pageHandler callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public <T extends HalRepresentation> void paginateNextAs(final Class<T> pageType,
                                                             final PageHandler pageHandler) throws IOException {
        paginateAs("next", pageType, null, pageHandler);
    }

    /**
     * Iterates over pages by following 'next' links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
     *
     * <pre>
     *                        next
     *     ... --&gt; &lt;page type&gt; --&gt; &lt;page type&gt;
     *                  |               |
     *                  v N item        v N item
     *           &lt;embedded type&gt;  &lt;embedded type&gt;
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a representation of {@code pageType} with {@link EmbeddedTypeInfo}.
     *     This way it is possible to access items embedded into the page resources as specific subtypes of
     *     HalRepresentation.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     *
     * @param pageType the subtype of HalRepresentation of the page resources
     * @param embeddedTypeInfo type information of the (possibly embedded) items of a page
     * @param pageHandler callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public <T extends HalRepresentation> void paginateNextAs(final Class<T> pageType,
                                                             final EmbeddedTypeInfo embeddedTypeInfo,
                                                             final PageHandler pageHandler) throws IOException {
        paginateAs("next", pageType, embeddedTypeInfo, pageHandler);
    }

    /**
     * Iterates over pages by following 'prev' links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
     *
     * <pre>
     *                              prev
     *     ... --&gt; HalRepresentation --&gt; HalRepresentation
     *                     |                     |
     *                     v N item              v N item
     *              HalRepresentation     HalRepresentation
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a {@link HalRepresentation} with no type information. If pages may contain
     *     embedded items, and if a specific sub-type of HalRepresentation is required for items,
     *     {@link #paginateNext(EmbeddedTypeInfo,PageHandler)} or {@link #paginateNextAs(Class,EmbeddedTypeInfo,PageHandler)}
     *     should be used instead of this method.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param pageHandler the callback used to process pages of items.
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public void paginatePrev(final PageHandler pageHandler) throws IOException {
        paginateAs("prev", HalRepresentation.class, null, pageHandler);
    }

    /**
     * Iterates over pages by following 'prev' links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
     *
     * <pre>
     *                              prev
     *     ... --&gt; HalRepresentation --&gt; HalRepresentation
     *                     |                     |
     *                     v N item              v N item
     *               &lt;embedded type&gt;      &lt;embedded type&gt;
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a {@link HalRepresentation} with {@link EmbeddedTypeInfo}. This way it
     *     is possible to access items embedded into the page resources as specific subtypes of HalRepresentation.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param embeddedTypeInfo type information of the (possibly embedded) items of a page
     * @param pageHandler callback function called for every page
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public void paginatePrev(final EmbeddedTypeInfo embeddedTypeInfo,
                             final PageHandler pageHandler) throws IOException {
        paginateAs("prev", HalRepresentation.class, embeddedTypeInfo, pageHandler);
    }

    /**
     * Iterates over pages by following 'prev' links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
     *
     * <pre>
     *                         prev
     *     ... --&gt; &lt;page type&gt; --&gt; &lt;page type&gt;
     *                 |                 |
     *                 v N item          v N item
     *          HalRepresentation   HalRepresentation
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a representation of {@code pageType}, but without
     *     {@link EmbeddedTypeInfo}. This way it is possible to access special attributes of the page by
     *     calling {@code pageTraverson.getResourceAs(pageType)} in the callback function. If the paged items need to
     *     be accessed as a subtype of HalRepresentation, you can call pageTraverson.follow(rel).streamAs(MyItemType.class)
     *     - but ONLY if the items are not embedded into the page.
     * </p>
     *
     * <p>
     *     For embedded items having a subtype of HalRepresentation, {@link #paginatePrevAs(Class, EmbeddedTypeInfo, PageHandler)}
     *     must be used instead of this method, otherwise a {@code ClassCastException} will be thrown.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param pageType the subtype of HalRepresentation of the page resources
     * @param pageHandler callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public <T extends HalRepresentation> void paginatePrevAs(final Class<T> pageType,
                                                             final PageHandler pageHandler) throws IOException {
        paginateAs("prev", pageType, null, pageHandler);
    }

    /**
     * Iterates over pages by following 'prev' links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
     *
     * <pre>
     *                         prev
     *     ... --&gt; &lt;page type&gt; --&gt; &lt;page type&gt;
     *                  |               |
     *                  v N item        v N item
     *           &lt;embedded type&gt;  &lt;embedded type&gt;
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a representation of {@code pageType} with {@link EmbeddedTypeInfo}.
     *     This way it is possible to access items embedded into the page resources as specific subtypes of
     *     HalRepresentation.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     *
     * @param pageType the subtype of HalRepresentation of the page resources
     * @param embeddedTypeInfo type information of the (possibly embedded) items of a page
     * @param pageHandler callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public <T extends HalRepresentation> void paginatePrevAs(final Class<T> pageType,
                                                             final EmbeddedTypeInfo embeddedTypeInfo,
                                                             final PageHandler pageHandler) throws IOException {
        paginateAs("prev", pageType, embeddedTypeInfo, pageHandler);
    }

    /**
     * Iterates over pages by following {code rel} links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the {@link PageHandler} function.
     *
     * <pre>
     *                         &lt;rel&gt;
     *     ... --&gt; &lt;page type&gt; --&gt; &lt;page type&gt;
     *                  |                |
     *                  v N item         v N item
     *           &lt;embedded type&gt;   &lt;embedded type&gt;
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a {@link HalRepresentation} with {@link EmbeddedTypeInfo}.
     *     This way it is possible to access items embedded into the page resources as specific subtypes of
     *     HalRepresentation.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param rel link-relation type of the links used to traverse pages
     * @param embeddedTypeInfo type information of the (possibly embedded) items of a page
     * @param pageHandler callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 2.0.0
     */
    public <T extends HalRepresentation> void paginate(final String rel,
                                                       final EmbeddedTypeInfo embeddedTypeInfo,
                                                       final PageHandler pageHandler) throws IOException {
        paginateAs(rel, HalRepresentation.class, embeddedTypeInfo, pageHandler);
    }

    /**
     * Iterates over pages by following {code rel} links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the {@link PageHandler} function.
     *
     * <pre>
     *                         &lt;rel&gt;
     *     ... --&gt; &lt;page type&gt; --&gt; &lt;page type&gt;
     *                  |                |
     *                  v N item         v N item
     *           &lt;embedded type&gt;   &lt;embedded type&gt;
     * </pre>
     *
     * <p>
     *     The {@code Traverson} is backed by a representation of {@code pageType} with {@link EmbeddedTypeInfo}.
     *     This way it is possible to access items embedded into the page resources as specific subtypes of
     *     HalRepresentation.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param rel link-relation type of the links used to traverse pages
     * @param pageType the subtype of HalRepresentation of the page resources
     * @param embeddedTypeInfo type information of the (possibly embedded) items of a page
     * @param pageHandler callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 2.0.0
     */
    public <T extends HalRepresentation> void paginateAs(final String rel,
                                                         final Class<T> pageType,
                                                         final EmbeddedTypeInfo embeddedTypeInfo,
                                                         final PageHandler pageHandler) throws IOException {
        Optional<T> currentPage = getResourceAs(pageType, embeddedTypeInfo);
        while (currentPage.isPresent()
                && pageHandler.apply(traverson(linkResolver).startWith(contextUrl, currentPage.get()))
                && currentPage.get().getLinks().getRels().contains(rel)) {
            currentPage = follow(rel).getResourceAs(pageType, embeddedTypeInfo);
        }
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by its link-relation type and returns a {@link Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of resolving the associated {@link Link}.
     * </p>
     * <p>
     *     Many times, you do not need the HalRepresentations, but subtypes of HalRepresentation,
     *     so you are able to access custom attributes of your resources. In this case, you need
     *     to use {@link #streamAs(Class)} instead of this method.
     * </p>
     *
     * @return this
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public Stream<HalRepresentation> stream() throws IOException {
        return streamAs(HalRepresentation.class, null);
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by its link-relation type and returns a {@link Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of resolving the associated {@link Link}.
     * </p>
     * @param type the specific type of the returned HalRepresentations
     * @param <T> type of the returned HalRepresentations
     * @return this
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public <T extends HalRepresentation> Stream<T> streamAs(final Class<T> type) throws IOException {
        return streamAs(type, null);
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by its link-relation type and returns a {@link Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     The EmbeddedTypeInfo is used to define the specific type of embedded items.
     * </p>
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of resolving the associated {@link Link}.
     * </p>
     * @param type the specific type of the returned HalRepresentations
     * @param embeddedTypeInfo specification of the type of embedded items
     * @param moreEmbeddedTypeInfos more embedded type-infos
     * @param <T> type of the returned HalRepresentations
     * @return this
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public <T extends HalRepresentation> Stream<T> streamAs(final Class<T> type,
                                                            final EmbeddedTypeInfo embeddedTypeInfo,
                                                            final EmbeddedTypeInfo... moreEmbeddedTypeInfos) throws IOException {
        if (moreEmbeddedTypeInfos == null || moreEmbeddedTypeInfos.length == 0) {
            return streamAs(type, embeddedTypeInfo != null ? singletonList(embeddedTypeInfo) : emptyList());
        } else {
            final List<EmbeddedTypeInfo> typeInfos = new ArrayList<>();
            typeInfos.add(requireNonNull(embeddedTypeInfo));
            typeInfos.addAll(asList(moreEmbeddedTypeInfos));
            return streamAs(type, typeInfos);
        }
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by its link-relation type and returns a {@link Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     The EmbeddedTypeInfo is used to define the specific type of embedded items.
     * </p>
     * <p>
     *     Templated links are expanded to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of resolving the associated {@link Link}.
     * </p>
     * @param type the specific type of the returned HalRepresentations
     * @param embeddedTypeInfo specification of the type of embedded items
     * @param <T> type of the returned HalRepresentations
     * @return this
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public <T extends HalRepresentation> Stream<T> streamAs(final Class<T> type,
                                                            final List<EmbeddedTypeInfo> embeddedTypeInfo) throws IOException {
        checkState();
        try {
            if (startWith != null) {
                lastResult = traverseInitialResource(type, embeddedTypeInfo, true);
            } else if (!hops.isEmpty()) {
                lastResult = traverseHop(lastResult.get(0), type, embeddedTypeInfo, true);
            }
            return (Stream<T>) lastResult.stream();
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Return the selected resource as HalRepresentation.
     * <p>
     *     If there are multiple matching representations, the first node is returned.
     * </p>
     * <p>
     *     Many times, you do not need the HalRepresentation, but a subtype of HalRepresentation,
     *     so you are able to access custom attributes of your resource. In this case, you need
     *     to use {@link #getResourceAs(Class)} instead of this method.
     * </p>
     *
     * @return HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public Optional<HalRepresentation> getResource() throws IOException {
        return getResourceAs(HalRepresentation.class, null);
    }

    /**
     * Return the selected resource and return it in the specified type.
     * <p>
     *     If there are multiple matching representations, the first node is returned.
     * </p>
     * @param type the subtype of the HalRepresentation used to parse the resource.
     * @param <T> the subtype of HalRepresentation of the returned resource.
     * @return HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public <T extends HalRepresentation> Optional<T> getResourceAs(final Class<T> type) throws IOException {
        return getResourceAs(type, null);
    }

    /**
     * Return the selected resource and return it in the specified type.
     * <p>
     *     The EmbeddedTypeInfo is used to define the specific type of embedded items.
     * </p>
     * <p>
     *     If there are multiple matching representations, the first node is returned.
     * </p>
     * @param type the subtype of the HalRepresentation used to parse the resource.
     * @param embeddedTypeInfo specification of the type of embedded items
     * @param moreEmbeddedTypeInfos more type infors for embedded items
     * @param <T> the subtype of HalRepresentation of the returned resource.
     * @return HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public <T extends HalRepresentation> Optional<T> getResourceAs(final Class<T> type,
                                                                   final EmbeddedTypeInfo embeddedTypeInfo,
                                                                   final EmbeddedTypeInfo... moreEmbeddedTypeInfos) throws IOException {
        if (moreEmbeddedTypeInfos == null || moreEmbeddedTypeInfos.length == 0) {
            return getResourceAs(type, embeddedTypeInfo != null ? singletonList(embeddedTypeInfo) : emptyList());
        } else {
            final List<EmbeddedTypeInfo> typeInfos = new ArrayList<>();
            typeInfos.add(requireNonNull(embeddedTypeInfo));
            typeInfos.addAll(asList(moreEmbeddedTypeInfos));
            return getResourceAs(type, typeInfos);
        }
    }

    /**
     * Return the selected resource and return it in the specified type.
     * <p>
     *     The EmbeddedTypeInfo is used to define the specific type of embedded items.
     * </p>
     * <p>
     *     If there are multiple matching representations, the first node is returned.
     * </p>
     * @param type the subtype of the HalRepresentation used to parse the resource.
     * @param embeddedTypeInfos specification of the type of embedded items
     * @param <T> the subtype of HalRepresentation of the returned resource.
     * @return HalRepresentation
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     * @since 1.0.0
     */
    public <T extends HalRepresentation> Optional<T> getResourceAs(final Class<T> type, 
                                                                   final List<EmbeddedTypeInfo> embeddedTypeInfos) throws IOException {
        checkState();
        try {
            if (startWith != null) {
                lastResult = traverseInitialResource(type, embeddedTypeInfos, false);
            } else if (!hops.isEmpty()) {
                lastResult = traverseHop(lastResult.get(0), type, embeddedTypeInfos, false);
            }
            if (lastResult.size() > 0) {
                return Optional.of(type.cast(lastResult.get(0)));
            } else {
                return Optional.empty();
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the current contextUrl of the Traverson.
     * <p>
     *     The contextUrl is used to resolve relative links / HREFs to other resources.
     * </p>
     *
     * @return URL of the Traverson's current context.
     * @since 1.0.0
     */
    public URL getCurrentContextUrl() {
        return contextUrl;
    }

    /**
     *
     * @param resultType the Class of the returned HalRepresentation.
     * @param embeddedTypeInfo type information about embedded items
     * @param retrieveAll false if only a single resource is returned, true otherwise.
     * @param <T> type parameter of the returned HalRepresentation
     * @return list of zero or more HalRepresentations.
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     */
    private <T extends HalRepresentation> List<T> traverseInitialResource(final Class<T> resultType,
                                                                          final List<EmbeddedTypeInfo> embeddedTypeInfo,
                                                                          final boolean retrieveAll) throws IOException {
        /*
        #hops = N; N > 0
        max nesting-level in embeddedTypeInfo = M; M >= 0
        */
        final Link initial = self(startWith.toString());
        LOG.trace("Starting with {}", startWith);
        this.startWith = null;
        if (hops.isEmpty()) {
            /*
            0. N=0, M=0:
            getResource(startwith, pageType)
            */
            return singletonList(getResource(initial, resultType, embeddedTypeInfo));
        } else {
            final HalRepresentation firstHop;
            // Follow startWith URL, but have a look at the next hop, so we can parse the resource
            // with respect to pageType and embeddedTypeInfo:
            if (hops.size() == 1) {
                final Hop hop = hops.get(0);
                if (embeddedTypeInfo == null || embeddedTypeInfo.isEmpty()) {
                    /*
                    1. N=1, M=0 (keine TypeInfos):
                    Die zurückgegebene Representation soll vom Typ pageType sein.

                    startWith könnte hop 0 embedden, oder es könnten zwei Resourcen angefragt werden.

                    a) getResource(startwith, HalRepresentation.class, embeddedTypeInfo(hop-0-rel, pageType))
                    b) getResource(current, pageType)
                    */
                    firstHop = getResource(initial, HalRepresentation.class, withEmbedded(hop.rel, resultType));
                } else {
                    /*
                    2. N=1, M>0 (mit TypeInfos)
                    Die zurückgegebene Representation soll vom Typ pageType sein und eingebettete Items gemäss der embeddedTypeInfo enthalten.

                    startWith könnte hop 0 embedden, oder es könnten zwei Resourcen angefragt werden.

                    a) getResource(startwith, HalRepresentation.class, embeddedTypeInfo(hop-0-rel, pageType, embeddedTypeInfo))
                    b) getResource(current, pageType, embeddedTypeInfo)
                    */
                    //firstHop = getResource(current, pageType, embeddedTypeInfo);
                    firstHop = getResource(initial, HalRepresentation.class, withEmbedded(hop.rel, resultType, embeddedTypeInfo));
                }
            } else {
                /*
                3. N>=2, M=0
                Die zurückgegebene Representation soll vom Typ pageType sein.

                startWith könnte hop 0 und 1 embedden, oder es könnten zwei Resourcen angefragt werden, von denen die zweite
                den hop 1 embedded, oder es könnten drei Resource angefragt werden.

                a) getResource(startWith, HalRepresentation.class, embeddedTypeInfo(hop-0-rel, HalRepresentation.class, hop-1-rel, pageType,)
                b) getResource(current, HalRepresentation.class, embeddedTypeInfo(hop-1-rel, pageType)
                c) getResource(current, pageType)
                */
                firstHop = getResource(initial, HalRepresentation.class, embeddedTypeInfoFor(hops, resultType, embeddedTypeInfo));
            }
            return traverseHop(firstHop, resultType, embeddedTypeInfo, retrieveAll);
        }
    }

    /**
     *
     * @param current HalRepresentation of the current hop
     * @param resultType the Class of the returned HalRepresentation.
     * @param embeddedTypeInfo type information about embedded items
     * @param retrieveAll false if only a single resource is returned, true otherwise.
     * @param <T> type parameter of the returned HalRepresentation
     * @return list of zero or more HalRepresentations.
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     */
    @SuppressWarnings("unchecked")
    private <T extends HalRepresentation> List<T> traverseHop(final HalRepresentation current,
                                                              final Class<T> resultType,
                                                              final List<EmbeddedTypeInfo> embeddedTypeInfo,
                                                              final boolean retrieveAll) throws IOException {

        final List<T> response;
        final Hop currentHop = hops.remove(0);
        LOG.trace("Following {}", currentHop.rel);
        final List<Link> links = current
                .getLinks()
                .getLinksBy(currentHop.rel, currentHop.predicate);
        if (!currentHop.ignoreEmbedded || links.isEmpty()) {
            // the next hop could possibly be already available as an embedded object:
            final List<? extends HalRepresentation> embeddedItems = hops.isEmpty()
                    ? current.getEmbedded().getItemsBy(currentHop.rel, resultType)
                    : current.getEmbedded().getItemsBy(currentHop.rel);

            if (!embeddedItems.isEmpty()) {
                LOG.trace("Returning {} embedded {}", embeddedItems.size(), currentHop.rel);
                return hops.isEmpty()
                        ? (List<T>) embeddedItems
                        : traverseHop(embeddedItems.get(0), resultType, embeddedTypeInfo, retrieveAll);
            }
        }
        if (!links.isEmpty()) {
            final Link expandedLink = resolve(this.contextUrl, expand(links.get(0), currentHop.vars));
            if (hops.isEmpty()) { // last hop
                if (retrieveAll) {
                    LOG.trace("Following {} {} links", links.size(), currentHop.rel);
                    final List<T> representations = new ArrayList<>();
                    for (final Link link : links) {
                        representations.add(getResource(resolve(this.contextUrl, expand(link, currentHop.vars)), resultType, embeddedTypeInfo));
                    }
                    response = representations;
                } else {
                    this.contextUrl = linkToUrl(expandedLink);
                    response = singletonList(getResource(expandedLink, resultType, embeddedTypeInfo));
                }
            } else if (hops.size() == 1) { // one before the last hop:
                this.contextUrl = linkToUrl(expandedLink);
                final HalRepresentation resource = getResource(expandedLink, HalRepresentation.class, embeddedTypeInfoFor(hops, resultType, embeddedTypeInfo));
                response = traverseHop(resource, resultType, embeddedTypeInfo, retrieveAll);
            } else { // some more hops
                this.contextUrl = linkToUrl(expandedLink);
                final HalRepresentation resource = getResource(expandedLink, HalRepresentation.class, embeddedTypeInfoFor(hops, resultType, embeddedTypeInfo));
                response = traverseHop(resource, resultType, embeddedTypeInfo, retrieveAll);
            }
        } else {
            final String msg = format("Can not follow hop %s: no matching links found in resource %s", currentHop.rel, current);
            LOG.error(msg);
            response = emptyList();
        }
        return response;
    }

    private Link expand(final Link link, 
                        final Map<String,Object> vars) {
        if (link.isTemplated()) {
            final String href = fromTemplate(link.getHref()).expand(vars);
            return copyOf(link)
                    .withHref(href)
                    .withRel(link.getRel())
                    .build();
        } else {
            return link;
        }
    }

    /**
     * Retrieve the HAL resource identified by {@code uri} and return the representation as a HalRepresentation.
     *
     * @param link the Link of the resource to retrieve, or null, if the contextUrl should be resolved.
     * @throws IllegalArgumentException if resolving URLs is failing
     * @param resultType the Class of the returned HalRepresentation.
     * @param embeddedTypeInfo type information about embedded items
     * @param <T> type parameter of the returned HalRepresentation
     * @return list of zero or more HalRepresentations.
     */
    private <T extends HalRepresentation> T getResource(final Link link,
                                                        final Class<T> resultType,
                                                        final EmbeddedTypeInfo embeddedTypeInfo) throws IOException {
        return getResource(link, resultType, singletonList(embeddedTypeInfo));
    }

    /**
     * Retrieve the HAL resource identified by {@code uri} and return the representation as a HalRepresentation.
     *
     * @param link the Link of the resource to retrieve, or null, if the contextUrl should be resolved.
     * @param type the expected type of the returned resource
     * @param embeddedType type information to specify the type of embedded resources.
     * @param <T> the type of the returned HalRepresentation
     * @return HalRepresentation
     * @throws IllegalArgumentException if resolving URLs is failing
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     * @throws JsonParseException if the json document can not be parsed by Jackson's ObjectMapper
     * @throws JsonMappingException if the input JSON structure can not be mapped to the specified HalRepresentation type
     */
    private <T extends HalRepresentation> T getResource(final Link link, 
                                                        final Class<T> type, 
                                                        final List<EmbeddedTypeInfo> embeddedType) throws IOException {
        LOG.trace("Fetching resource href={} rel={} as type={} with embeddedType={}", link.getHref(), link.getRel(), type.getSimpleName(), embeddedType);
        final String json;
        try {
            json = linkResolver.apply(link);
        } catch (final IOException | RuntimeException e) {
            LOG.error("Failed to fetch resource href={}: {}", link.getHref(), e.getMessage());
            throw e;
        }
        try {
            return embeddedType != null && !embeddedType.isEmpty()
                    ? parse(json, objectMapper).as(type, embeddedType)
                    : parse(json, objectMapper).as(type);
        } catch (final RuntimeException e) {
            LOG.error("Failed to parse resource href={} rel={} as type={} with embeddedType={}", link.getHref(), link.getRel(), type.getSimpleName(), embeddedType);
            throw e;
        }
    }

    /**
     * Resolved a link using the URL of the current resource and returns it as an absolute Link.
     *
     * @param contextUrl the URL of the current context
     * @param link optional link to follow
     * @return absolute Link
     * @throws IllegalArgumentException if resolving the link is failing
     */
    private static Link resolve(final URL contextUrl, 
                                final Link link) {
        if (link != null && link.isTemplated()) {
            final String msg = "Link must not be templated";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }
        if (link == null) {
            return self(contextUrl.toString());
        } else {
            return copyOf(link).withHref(resolveHref(contextUrl, link.getHref()).toString()).build();
        }
    }

    static EmbeddedTypeInfo embeddedTypeInfoFor(final List<Hop> hops,
                                                final Class<? extends HalRepresentation> pageType,
                                                final List<EmbeddedTypeInfo> embeddedTypeInfo) {
        if (hops.isEmpty()) {
            throw new IllegalArgumentException("Hops must not be empty");
        }
        EmbeddedTypeInfo typeInfo = embeddedTypeInfo != null && !embeddedTypeInfo.isEmpty()
                ? withEmbedded(hops.get(hops.size()-1).rel, pageType, embeddedTypeInfo)
                : withEmbedded(hops.get(hops.size()-1).rel, pageType);
        for (int i = hops.size()-2; i >= 0; i--) {
            typeInfo = withEmbedded(hops.get(i).rel, HalRepresentation.class, typeInfo);
        }
        return typeInfo;
    }

    private static URL linkToUrl(final Link link) {
        if (link.isTemplated()) {
            final String msg = format("Unable to create URL from templated link %s", link);
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }
        return hrefToUrl(link.getHref());
    }

    private static URL hrefToUrl(final String href) {
        try {
            return new URL(href);
        } catch (final MalformedURLException e) {
            final String msg = format("Unable to create URL from href '%s': %s", href, e.getMessage());
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static URL resolveHref(final URL contextUrl, final String href) {
        try {
            return contextUrl == null ? new URL(href) : new URL(contextUrl, href);
        } catch (final MalformedURLException e) {
            final String msg = format("Unable to resolve URL from contextUrl %s and href '%s': %s", contextUrl, href, e.getMessage());
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    /**
     * Checks the current state of the Traverson.
     *
     * @throws IllegalStateException if some error occured during traversion
     */
    private void checkState() {
        if (startWith == null && lastResult == null) {
            final String msg = "Please call startWith(uri) first.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }
    }
}
