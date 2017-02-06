package de.otto.edison.hal.traverson;

import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;

import de.otto.edison.hal.EmbeddedTypeInfo;
import static de.otto.edison.hal.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.HalParser.parse;
import static de.otto.edison.hal.Link.copyOf;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.traverson.TraversionError.Type;
import static de.otto.edison.hal.traverson.TraversionError.Type.INVALID_JSON;
import static de.otto.edison.hal.traverson.TraversionError.Type.MISSING_LINK;
import static de.otto.edison.hal.traverson.TraversionError.Type.NOT_FOUND;
import static de.otto.edison.hal.traverson.TraversionError.traversionError;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;

/**
 * A Traverson is a utility that makes it easy to navigate REST APIs using HAL+JSON.
 * <p>
 *     {@link #startWith(String) Starting with} an URI to an initial resource, you can {@link #follow(String) follow}
 *     one or more links identified by their link-relation type.
 * </p>
 * <p>
 *     {@link Link#isTemplated()}  Templated links} can be expanded using template
 *     {@link #withVars(String, Object, Object...) variables}.
 * </p>
 * <p>
 *     Example:
 * </p>
 * <pre><code>
 *        final HalRepresentation hal = traverson(this::myHttpGetFunction)
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

    private static class Hop {
        /** Link-relation type of the hop. */
        final String rel;
        final Predicate<Link> predicate;
        /** URI-template variables used when following the hop. */
        final Map<String,Object> vars;

        private Hop(final String rel,
                    final Predicate<Link> predicate,
                    final Map<String, Object> vars) {
            this.rel = rel;
            this.predicate = predicate;
            this.vars = vars;
        }
    }

    private final Function<Link, String> linkToJsonFunc;
    private final List<Hop> hops = new ArrayList<>();
    private String startWith;
    private List<? extends HalRepresentation> lastResult;
    private TraversionError lastError;
    private Traverson(final Function<Link,String> linkToJsonFunc) {
        this.linkToJsonFunc = linkToJsonFunc;
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
     * @param linkToJsonFunc A Function that gets a ) Link of a resource and returns a HAL+JSON document.
     * @return Traverson
     */
    public static Traverson traverson(final Function<Link,String> linkToJsonFunc) {
        return new Traverson(linkToJsonFunc);
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

    public TraversionError getLastError() {
        return lastError;
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
        startWith = uri;
        lastResult = null;
        return this;
    }

    /**
     * Start traversal at the given HAL resource.
     *
     * @param resource the initial HAL resource.
     * @return Traverson initialized with the specified {@link HalRepresentation}.
     */
    public Traverson startWith(final HalRepresentation resource) {
        startWith = null;
        lastResult = singletonList(resource);
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
     * Follow the first {@link Link} of the current resource that is matching the link-relation type and
     * the predicate.
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param rel the link-relation type of the followed link
     * @param predicate the predicate used to select the link to follow
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final String rel, final Predicate<Link> predicate) {
        return follow(rel, predicate, emptyMap());
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
        return follow(rels, (link)->true, emptyMap());
    }

    /**
     * Follow multiple link-relation types, one by one, and select the links using the specified predicate.
     * <p>
     *     Embedded items are used instead of resolving links, if present in the returned HAL documents.
     * </p>
     *
     * @param rels the link-relation types of the followed links
     * @param predicate the predicated used to select the link to follow
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final List<String> rels, final Predicate<Link> predicate) {
        return follow(rels, predicate, emptyMap());
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
            follow(rel, (link)->true, vars);
        }
        return this;
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
     * @param predicate the predicate used to select the link to follow
     * @param vars uri-template variables used to build links.
     * @return this
     * @since 1.0.0
     */
    public Traverson follow(final List<String> rels, final Predicate<Link> predicate, final Map<String, Object> vars) {
        for (String rel : rels) {
            follow(rel, predicate, vars);
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
     * @param vars uri-template variables used to build links.
     * @return this
     */
    public Traverson follow(final String rel, final Map<String, Object> vars) {
        return follow(rel, (link)->true, vars);
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
     * @param predicate the predicate used to select the link to follow.
     * @param vars uri-template variables used to build links.
     * @return this
     */
    public Traverson follow(final String rel, final Predicate<Link> predicate, final Map<String, Object> vars) {
        checkState();
        hops.add(new Hop(rel, predicate, vars));
        return this;
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by it's link-relation type and returns a {@link Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * <p>
     *     Many times, you do not need the HalRepresentations, but subtypes of HalRepresentation,
     *     so you are able to access custom attributes of your resources. In this case, you need
     *     to use {@link #streamAs(Class)} instead of this method.
     * </p>
     *
     * @return this
     */
    public Stream<HalRepresentation> stream() {
        return streamAs(HalRepresentation.class, null);
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by it's link-relation type and returns a {@link Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     Templated links are resolved to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param type the specific type of the returned HalRepresentations
     * @param <T> type of the returned HalRepresentations
     * @return this
     */
    public <T extends HalRepresentation> Stream<T> streamAs(final Class<T> type) {
        return streamAs(type, null);
    }

    /**
     * Follow the {@link Link}s of the current resource, selected by it's link-relation type and returns a {@link Stream}
     * containing the returned {@link HalRepresentation HalRepresentations}.
     * <p>
     *     The EmbeddedTypeInfo is used to define the specific type of embedded items.
     * </p>
     * <p>
     *     Templated links are resolved to URIs using the specified template variables.
     * </p>
     * <p>
     *     If the current node has {@link Embedded embedded} items with the specified {@code rel},
     *     these items are used instead of following the associated {@link Link}.
     * </p>
     * @param type the specific type of the returned HalRepresentations
     * @param embeddedTypeInfo specification of the type of embedded items
     * @param <T> type of the returned HalRepresentations
     * @return this
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public <T extends HalRepresentation> Stream<T> streamAs(final Class<T> type, final EmbeddedTypeInfo embeddedTypeInfo) {
        checkState();
        try {
            if (startWith != null) {
                lastResult = traverseInitialResource(type, embeddedTypeInfo, true);
                lastError = null;
            } else if (!hops.isEmpty()) {
                lastResult = traverseHop(lastResult.get(0), type, embeddedTypeInfo, true);
                lastError = null;
            }
            return (Stream<T>) lastResult.stream();
        } catch (final TraversionException e) {
            lastError = e.getError();
            return empty();
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
     */
    public Optional<HalRepresentation> getResource() {
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
     */
    public <T extends HalRepresentation> Optional<T> getResourceAs(final Class<T> type) {
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
     * @param <T> the subtype of HalRepresentation of the returned resource.
     * @return HalRepresentation
     * @since 1.0.0
     */
    public <T extends HalRepresentation> Optional<T> getResourceAs(final Class<T> type, final EmbeddedTypeInfo embeddedTypeInfo) {
        checkState();
        try {
            if (startWith != null) {
                lastResult = traverseInitialResource(type, embeddedTypeInfo, false);
                lastError = null;
            } else if (!hops.isEmpty()) {
                lastResult = traverseHop(lastResult.get(0), type, embeddedTypeInfo, false);
                lastError = null;
            }
            return Optional.of(type.cast(lastResult.get(0)));
        } catch (final TraversionException e) {
            lastError = e.getError();
            return Optional.empty();
        }
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
     *     {@link #paginateNext(EmbeddedTypeInfo,Function)} or {@link #paginateNextAs(Class,EmbeddedTypeInfo,Function)}
     *     should be used instead of this method.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param pageCallback the callback used to process pages of items.
     * @since 1.0.0
     */
    public void paginateNext(final Function<Traverson, Boolean> pageCallback) {
        paginate("next", HalRepresentation.class, null, pageCallback);
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
     * @param pageCallback the callback used to process pages of items.
     * @since 1.0.0
     */
    public void paginateNext(final EmbeddedTypeInfo embeddedTypeInfo,
                             final Function<Traverson, Boolean> pageCallback) {
        paginate("next", HalRepresentation.class, embeddedTypeInfo, pageCallback);
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
     *     For embedded items having a subtype of HalRepresentation, {@link #paginateNextAs(Class, EmbeddedTypeInfo, Function)}
     *     must be used instead of this method, otherwise a {@code ClassCastException} will be thrown.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param pageType the subtype of HalRepresentation of the page resources
     * @param pageCallback callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @since 1.0.0
     */
    public <T extends HalRepresentation> void paginateNextAs(final Class<T> pageType,
                                                             final Function<Traverson, Boolean> pageCallback) {
        paginate("next", pageType, null, pageCallback);
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
     * @param pageCallback callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @since 1.0.0
     */
    public <T extends HalRepresentation> void paginateNextAs(final Class<T> pageType,
                                                             final EmbeddedTypeInfo embeddedTypeInfo,
                                                             final Function<Traverson, Boolean> pageCallback) {
        paginate("next", pageType, embeddedTypeInfo, pageCallback);
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
     *     {@link #paginateNext(EmbeddedTypeInfo,Function)} or {@link #paginateNextAs(Class,EmbeddedTypeInfo,Function)}
     *     should be used instead of this method.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param pageCallback the callback used to process pages of items.
     * @since 1.0.0
     */
    public void paginatePrev(final Function<Traverson, Boolean> pageCallback) {
        paginate("prev", HalRepresentation.class, null, pageCallback);
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
     * @param pageCallback callback function called for every page
     * @since 1.0.0
     */
    public void paginatePrev(final EmbeddedTypeInfo embeddedTypeInfo,
                             final Function<Traverson, Boolean> pageCallback) {
        paginate("prev", HalRepresentation.class, embeddedTypeInfo, pageCallback);
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
     *     For embedded items having a subtype of HalRepresentation, {@link #paginatePrevAs(Class, EmbeddedTypeInfo, Function)}
     *     must be used instead of this method, otherwise a {@code ClassCastException} will be thrown.
     * </p>
     *
     * <p>
     *     Iteration stops if the callback returns {@code false}, or if the last page is processed.
     * </p>
     *
     * @param pageType the subtype of HalRepresentation of the page resources
     * @param pageCallback callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @since 1.0.0
     */
    public <T extends HalRepresentation> void paginatePrevAs(final Class<T> pageType,
                                                             final Function<Traverson, Boolean> pageCallback) {
        paginate("next", pageType, null, pageCallback);
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
     * @param pageCallback callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @since 1.0.0
     */
    public <T extends HalRepresentation> void paginatePrevAs(final Class<T> pageType,
                                                             final EmbeddedTypeInfo embeddedTypeInfo,
                                                             final Function<Traverson, Boolean> pageCallback) {
        paginate("next", pageType, null, pageCallback);
    }

    /**
     * Iterates over pages by following {code rel} links. For every page, a {@code Traverson} is created and provided as a
     * parameter to the callback function.
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
     * @param pageCallback callback function called for every page
     * @param <T> subtype of HalRepresentation
     * @since 1.0.0
     */
    private <T extends HalRepresentation> void paginate(final String rel,
                                                        final Class<T> pageType,
                                                        final EmbeddedTypeInfo embeddedTypeInfo,
                                                        final Function<Traverson, Boolean> pageCallback) {
        Optional<T> currentPage = getResourceAs(pageType, embeddedTypeInfo);
        while (currentPage.isPresent() && pageCallback.apply(traverson(linkToJsonFunc).startWith(currentPage.get()))) {
            currentPage = follow(rel).getResourceAs(pageType, embeddedTypeInfo);
        }
    }

    private <T extends HalRepresentation> List<T> traverseInitialResource(final Class<T> type, final EmbeddedTypeInfo embeddedTypeInfo, final boolean retrieveAll) {
        final Link link = self(this.startWith.toString());
        this.startWith = null;
        if (hops.isEmpty()) {
            return singletonList(getResource(link, type, embeddedTypeInfo));
        } else {
            final HalRepresentation firstHop;
            if (hops.size() == 1) {
                final Hop hop = hops.get(0);
                if (embeddedTypeInfo == null || !hop.rel.equals(embeddedTypeInfo.rel)) {
                    firstHop = getResource(link, HalRepresentation.class, withEmbedded(hop.rel, type));
                } else {
                    firstHop = getResource(link, type, embeddedTypeInfo);
                }
            } else {
                firstHop = getResource(link, HalRepresentation.class, null);
            }
            return traverseHop(firstHop, type, embeddedTypeInfo, retrieveAll);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends HalRepresentation> List<T> traverseHop(final HalRepresentation current, final Class<T> resultType, final EmbeddedTypeInfo embeddedTypeInfo, boolean retrieveAll) {

        final Hop currentHop = hops.remove(0);

        // the next hop could possibly be already available as an embedded object:
        final List<? extends HalRepresentation> items = hops.isEmpty()
                ? current.getEmbedded().getItemsBy(currentHop.rel, resultType)
                : current.getEmbedded().getItemsBy(currentHop.rel);

        if (!items.isEmpty()) {
            return hops.isEmpty()
                    ? (List<T>) items
                    : traverseHop(items.get(0), resultType, embeddedTypeInfo, retrieveAll);
        }

        final List<Link> links = current
                .getLinks()
                .getLinksBy(currentHop.rel, currentHop.predicate);
        if (links.isEmpty()) {
            throw new TraversionException(traversionError(
                    MISSING_LINK,
                    format("Can not follow hop %s: no matching links found in resource %s", currentHop.rel, current))
            );
        }
        final Link expandedLink = expand(links.get(0), currentHop.vars);

        if (hops.isEmpty()) { // last hop
            if (retrieveAll) {
                return links
                        .stream()
                        .map(link-> getResource(expand(link, currentHop.vars), resultType, embeddedTypeInfo))
                        .collect(toList());
            } else {
                return singletonList(getResource(expandedLink, resultType, embeddedTypeInfo));
            }
        }

        if (hops.size() == 1) { // one before the last hop:
            final Hop nextHop = hops.get(0);
            if (embeddedTypeInfo != null) {
                final EmbeddedTypeInfo embeddedType = nextHop.rel.equals(embeddedTypeInfo.rel)
                        ? embeddedTypeInfo
                        : withEmbedded(nextHop.rel, resultType);
                return traverseHop(getResource(expandedLink, resultType, embeddedType), resultType, embeddedTypeInfo, retrieveAll);
            } else {
                return traverseHop(getResource(expandedLink, resultType, withEmbedded(nextHop.rel, resultType)), resultType, null, retrieveAll);
            }
        } else { // some more hops
            return traverseHop(getResource(expandedLink, HalRepresentation.class, null), resultType, embeddedTypeInfo, retrieveAll);
        }
    }

    private Link expand(final Link link, final Map<String,Object> vars) {
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
     * Retrieve the HAL resource identified by {@code uri} and return the representation as a HopResponse.
     *
     * @param link the Link of the resource to retrieve
     * @return HopResponse
     * @throws TraversionException thrown if getting or parsing the resource failed for some reason
     */
    private <T extends HalRepresentation> T getResource(final Link link, final Class<T> type, final EmbeddedTypeInfo embeddedType) {
        final String json = getJson(link);
        try {
            return embeddedType != null
                    ? parse(json).as(type, embeddedType)
                    : parse(json).as(type);
        } catch (final Exception e) {
            throw new TraversionException(traversionError(
                    Type.INVALID_JSON,
                    format("Document returned from %s is not in application/hal+json format: %s", link.getHref(), e.getMessage()),
                    e));
        }
    }

    private String getJson(Link link) {
        final String json;
        try {
            json = linkToJsonFunc.apply(link);
        } catch (final RuntimeException e) {
            throw new TraversionException(traversionError(
                    NOT_FOUND, e.getMessage(), e
            ));
        }
        if (json == null) {
            throw new TraversionException(traversionError(
                    INVALID_JSON,
                    format("Did not get JSON response from %s", linkToJsonFunc.toString())));
        }
        return json;
    }

    /**
     * Checks the current state of the Traverson.
     *
     * @throws IllegalStateException if some error occured during traversion
     */
    private void checkState() {
        if (startWith == null && lastResult == null) {
            throw new IllegalStateException("Please call startWith(uri) first.");
        }
    }
}
