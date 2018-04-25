package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static de.otto.edison.hal.Link.linkBuilder;
import static de.otto.edison.hal.Curies.emptyCuries;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Representation of a number of HAL _links.
 * <p>
 *     Links can be created using the {@link Links.Builder} using the factory-method {@link Links#linkingTo()}:
 * </p>
 * <pre><code>
 *     final Links someLinks = Links.linkingTo()
 *              .self("http://example.com/shopping-cart/42")
 *              .curi("ex", "http://example.com/rels/{rel}")
 *              .item("http://example.com/products/1"),
 *              .item("http://example.com/products/2"),
 *              .single(
 *                      Link.link("ex:customer", "http://example.com/customers/4711"))
 *              .build()
 *
 * </code></pre>
 *
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-4.1.1">draft-kelly-json-hal-08#section-4.1.1</a>
 * @since 0.1.0
 */
@JsonSerialize(using = Links.LinksSerializer.class)
@JsonDeserialize(using = Links.LinksDeserializer.class)
public class Links {

    private static final String CURIES_REL = "curies";

    private final Map<String, Object> links = new LinkedHashMap<>();
    private final Curies curies;


    /**
     *
     * @since 0.1.0
     */
    Links() {
        this.curies = emptyCuries();
    }

    /**
     * <p>
     *     Creates a Links object from a map containing rel->List<Link>.
     * </p>
     * <p>
     *     If the links contain curies, the link-relation types are shortened to the curied format name:key.
     * </p>
     * <p>
     *     The list of links for a link-relation type must have the same {@link Link#rel}
     * </p>
     * <p>
     *     The {@link Curies} may contain CURIs from the root resource, so curied rels can be resolved.
     * </p>
     *
     * @param links a map with link-relation types as key and the list of links as value.
     * @param curies the Curies used to CURI the link-relation types of the links.
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    private Links(final Map<String, Object> links, final Curies curies) {
        this.curies = curies;
        final List<Link> curiLinks = (List<Link>)links.getOrDefault(CURIES_REL, emptyList());
        curiLinks.forEach(this.curies::register);
        links.keySet().forEach(rel -> {
                this.links.put(curies.resolve(rel), links.get(rel));
        });
    }

    /**
     * Returns a copy of this Links instance and replaces link-relation types with CURIed form, if applicable.
     * <p>
     *     All CURIes are registered in the given Curies, so the HalRepresentation can forward these
     *     CURIes to embedded items.
     * </p>
     * @param curies Curies used to replace CURIed rels
     * @return Links having a reference to the given Curies.
     */
    Links using(final Curies curies) {
        return new Links(links, curies);
    }

    /**
     * Factory method used to create an empty Links instance.
     *
     * @return empty Links
     *
     * @since 0.1.0
     */
    public static Links emptyLinks() {
        return new Links();
    }

    public static Builder linkingTo() {
        return new Builder();
    }

    /**
     * Factory method used to build a Links.Builder that is initialized from a prototype Links instance.
     *
     * @param prototype the prototype used to initialize the builder
     * @return Links.Builder
     */
    public static Builder copyOf(final Links prototype) {
        return new Builder().with(prototype);
    }

    /**
     * Returns a Stream of links.
     *
     * @return Stream of Links
     */
    @SuppressWarnings("rawtypes")
    public Stream<Link> stream() {
        return links.values()
                .stream()
                .map(obj -> {
                    if (obj instanceof List) {
                        return (List) obj;
                    } else {
                        return singletonList(obj);
                    }
                })
                .flatMap(Collection::stream);
    }

    /**
     * Returns all link-relation types of the embedded items.
     *
     * @return set of link-relation types
     * @since 0.3.0
     */
    @JsonIgnore
    public Set<String> getRels() {
        return links.keySet();
    }

    /**
     * <p>
     *     Returns the first (if any) link having the specified link-relation type.
     * </p>
     * <p>
     *     If CURIs are used to shorten custom link-relation types, it is possible to either use expanded link-relation types,
     *     or the CURI of the link-relation type. Using CURIs to retrieve links is not recommended, because it
     *     requires that the name of the CURI is known by clients.
     * </p>
     *
     * @param rel the link-relation type of the retrieved link.
     * @return optional link
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-8.2">draft-kelly-json-hal-08#section-8.2</a>
     * @since 0.1.0
     */
    public Optional<Link> getLinkBy(final String rel) {
        final List<Link> links = getLinksBy(rel);
        return links.isEmpty()
                ? Optional.empty()
                : Optional.of(links.get(0));
    }

    /**
     * <p>
     *     Returns the first (if any) link having the specified link-relation type and matching the given predicate.
     * </p>
     * <p>
     *     If CURIs are used to shorten custom link-relation types, it is possible to either use expanded link-relation types,
     *     or the CURI of the link-relation type. Using CURIs to retrieve links is not recommended, because it
     *     requires that the name of the CURI is known by clients.
     * </p>
     * <p>
     *     The Predicate is used to select one of possibly several links having the same link-relation type. See
     *     {@link LinkPredicates} for typical selections.
     * </p>
     *
     * @param rel the link-relation type of the retrieved link.
     * @param selector a predicate used to select one of possibly several links having the same link-relation type
     * @return optional link
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-8.2">draft-kelly-json-hal-08#section-8.2</a>
     * @since 1.0.0
     */
    public Optional<Link> getLinkBy(final String rel, final Predicate<Link> selector) {
        final List<Link> links = getLinksBy(rel, selector);
        return links.isEmpty()
                ? Optional.empty()
                : Optional.of(links.get(0));
    }

    /**
     * <p>
     *     Returns the list of links having the specified link-relation type.
     * </p>
     * <p>
     *     If CURIs are used to shorten custom link-relation types, it is possible to either use expanded link-relation types,
     *     or the CURI of the link-relation type. Using CURIs to retrieve links is not recommended, because it
     *     requires that the name of the CURI is known by clients.
     * </p>
     *
     * @param rel the link-relation type of the retrieved link.
     * @return list of matching link
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-8.2">draft-kelly-json-hal-08#section-8.2</a>
     * @since 0.1.0
     */
    @SuppressWarnings("unchecked")
    public List<Link> getLinksBy(final String rel) {
        final String curiedRel = curies.resolve(rel);
        if (this.links.containsKey(curiedRel)) {
            final Object links = this.links.get(curiedRel);
            return links instanceof List
                    ? (List<Link>) links
                    : singletonList((Link)links);
        } else {
            return emptyList();
        }
    }

    /**
     * <p>
     *     Returns the list of links having the specified link-relation type and matching the given predicate.
     * </p>
     * <p>
     *     If CURIs are used to shorten custom link-relation types, it is possible to either use expanded link-relation types,
     *     or the CURI of the link-relation type. Using CURIs to retrieve links is not recommended, because it
     *     requires that the name of the CURI is known by clients.
     * </p>
     * <p>
     *     The Predicate is used to select some of possibly several links having the same link-relation type. See
     *     {@link LinkPredicates} for typical selections.
     * </p>
     *
     * @param rel the link-relation type of the retrieved link.
     * @param selector a predicate used to select some of the links having the specified link-relation type
     * @return list of matching link
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-8.2">draft-kelly-json-hal-08#section-8.2</a>
     * @since 1.0.0
     */
    public List<Link> getLinksBy(final String rel, final Predicate<Link> selector) {
        return getLinksBy(rel).stream().filter(selector).collect(toList());
    }

    /**
     * Removes the links with link-relation type {@code rel} from the Links object.
     * @param rel link-relation type
     */
    public void remove(final String rel) {
        links.remove(rel);
    }

    /**
     * Checks the existence of a link with link-relation type {@code rel}
     * @param rel the link-relation type
     * @return true, if links exists, false otherwise
     *
     * @since 1.0.0
     */
    public boolean hasLink(final String rel) {
        return links.containsKey(rel);
    }

    /**
     * Returns true if there is at least one link with link-relation type {@code rel}, and if the link will
     * be rendered as an array of link-objects.
     *
     * @param rel the link-relation type
     * @return boolean
     *
     * @since 2.0.0
     */
    public boolean isArray(final String rel) {
        return hasLink(rel) && (links.get(rel) instanceof List);
    }

    /**
     *
     * @return true if Links is empty, false otherwise.
     *
     * @since 0.1.0
     */
    public boolean isEmpty() {
        return links.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Links links1 = (Links) o;

        return links != null ? links.equals(links1.links) : links1.links == null;

    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public int hashCode() {
        return links != null ? links.hashCode() : 0;
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public String toString() {
        return "Links{" +
                "links=" + links +
                '}';
    }

    /**
     * A Builder used to build Links instances.
     *
     * @since 0.2.0
     */
    public static class Builder {
        private final Map<String,Object> links = new LinkedHashMap<>();
        private Curies curies = emptyCuries();



        /**
         * Adds a 'self' link and returns the Builder.
         * <p>
         *     Using this method is equivalent to {@link #single(Link, Link...) single(Link.self(href))}
         * </p>
         *
         * @param href href of the linked resource
         * @return Builder
         *
         * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
         * @since 2.0.0
         */
        public Builder self(final String href) {
            single(Link.self(href));
            return this;
        }

        /**
         * <p>
         *     Adds a 'curies' link (compact URI) with name and a URI template for the link-relation type.
         * </p>
         * <p>
         *     Curies may be used for brevity for custom link-relation type URIs. Curies are established within a HAL document
         *     via a set of Link Objects with the relation type "curies" on the root Resource Object.
         *     These links contain a URI template with the token 'rel', and are named via the "name" property.
         * </p>
         * <pre><code>
         *     {
         *       "_links": {
         *         "self": { "href": "/orders" },
         *         "curies": [{
         *           "name": "acme",
         *           "href": "http://docs.acme.com/relations/{rel}",
         *           "templated": true
         *         }],
         *         "acme:widgets": { "href": "/widgets" }
         *       }
         *     }
         * </code></pre>
         * <p>
         *     Using this method is equivalent to {@link #array(Link, Link...) array(Link.curi(name, relTemplate))}
         * </p>
         *
         * @param name the short name of the CURI
         * @param relTemplate the template used to build link-relation types. Must contain a {rel} placeholder
         * @return Link
         *
         * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
         * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-8.2">draft-kelly-json-hal-08#section-8.2</a>
         * @since 2.0.0
         */
        public Builder curi(final String name, final String relTemplate) {
            array(Link.curi(name, relTemplate));
            return this;
        }

        /**
         * Adds an 'item' link to the builder.
         * <p>
         *     If href is an URI template, the added link {@link Link#isTemplated() is templated}.
         * </p>
         * <p>
         *     Using this method is equivalent to {@link #array(Link, Link...) array(Link.item(href))}
         * </p>
         *
         * @param href the linked item
         * @return this
         *
         * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
         * @since 2.0.0
         */
        public Builder item(final String href) {
            array(Link.item(href));
            return this;
        }

        /**
         * Adds one or more link to the builder that will be rendered as a single link-object instead of an array of link-objects.
         * <p>
         *     The links must have different {@link Link#getRel() Link-Relation Types}, otherwise it would not be
         *     possible to render them as single link-objects. If two or more links have the same Link-Relation Type,
         *     an IllegalArgumentException is thrown.
         * </p>
         * <p>
         *     Because curies must always be {@link #array(List) array} links, it is not possible to add links with
         *     {@code rel='curies'} to the builder using {@link #single(Link, Link...)} or {@link #single(List)}.
         * </p>
         * <p>
         *     As specified in <a href="https://tools.ietf.org/html/draft-kelly-json-hal-06#section-4.1.1">Section 4.1.1</a>
         *     of the HAL specification, the {@code _links} object <em>"is an object whose property names are
         *     link relation types (as defined by [RFC5988]) and values are either a Link Object or an array
         *     of Link Objects"</em>.
         * </p>
         * <p>
         *     Adding a link using {@code single(Link)} will result in a representation, where the link is rendered
         *     as a Link Object.
         * </p>
         * <p>
         *     Calling {@code single(Link)} with a {@link Link#getRel() link-relation type} that is already present, an
         *     {@link IllegalStateException} is thrown.
         * </p>
         *
         * @param link the added link. The Link-Relation Type of the link must not yet be added to the builder.
         * @param more optionally more links having different Link-Relation Types
         * @return this
         * @throws IllegalStateException if the Link-Relation Type of the link is already associated with another Link.
         *
         * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
         * @since 2.0.0
         */
        public Builder single(final Link link, final Link... more) {
            if (link.getRel().equals(CURIES_REL)) {
                throw new IllegalArgumentException("According to the spec, curies must always be arrays of links, not single links.");
            } else {
                if (!this.links.containsKey(link.getRel())) {
                    this.links.put(link.getRel(), link);
                } else {
                    throw new IllegalStateException("The Link-Relation Type '" + link.getRel() + "' of the Link is already present.");
                }
                if (more != null) {
                    Arrays.stream(more).forEach(this::single);
                }
            }
            return this;
        }

        /**
         * Adds a list of links to the builder that will be rendered as a single link-object instead of an array of
         * link-objects.
         * <p>
         *     As specified in <a href="https://tools.ietf.org/html/draft-kelly-json-hal-06#section-4.1.1">Section 4.1.1</a>
         *     of the HAL specification, the {@code _links} object <em>"is an object whose property names are
         *     link relation types (as defined by [RFC5988]) and values are either a Link Object or an array
         *     of Link Objects"</em>.
         * </p>
         * <p>
         *     Adding a link using {@code single(Link)} will result in a representation, where the link is rendered
         *     as a Link Object.
         * </p>
         * <p>
         *     The List of links must not contain multiple links having the same link-relation type, otherwise an
         *     IllegalArgumentException is thrown.
         * </p>
         * <p>
         *     Because curies must always be {@link #array(List) array} links, it is not possible to add links with
         *     {@code rel='curies'} to the builder using {@link #single(Link, Link...)} or {@link #single(List)}.
         * </p>
         * <p>
         *     Calling {@code single(List<Link>)} with {@link Link#getRel() link-relation types} that are already
         *     present, an {@link IllegalStateException} is thrown.
         * </p>
         *
         * @param singleLinkObjects the added link. The Link-Relation Type of the link must not yet be added to the builder.
         * @return this
         * @throws IllegalArgumentException if the list contains multiple links having the same Link-Relation Type.
         * @throws IllegalStateException if the Link-Relation Type of the link is already associated with another Link.
         *
         * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
         * @since 2.0.0
         */
        public Builder single(final List<Link> singleLinkObjects) {
            if (singleLinkObjects.stream().map(Link::getRel).count() != singleLinkObjects.size()) {
                throw new IllegalArgumentException("Unable to add links as single link objects as there are multiple links having the same link-relation type.");
            }
            singleLinkObjects.forEach(this::single);
            return this;
        }

        /**
         * Adds one or more Links.
         * <p>
         *     {@link Link#isEquivalentTo(Link) Equivalent} links are NOT added but silently ignored.
         * </p>
         * <p>
         *     As specified in <a href="https://tools.ietf.org/html/draft-kelly-json-hal-06#section-4.1.1">Section 4.1.1</a>
         *     of the HAL specification, the {@code _links} object <em>"is an object whose property names are
         *     link relation types (as defined by [RFC5988]) and values are either a Link Object or an array
         *     of Link Objects"</em>.
         * </p>
         * <p>
         *     Adding a link using {@code array(Link, Link...)} will result in a representation, where the links are
         *     rendered as an array of Link Objects, even if there are only single links for a given Link-Relation Type.
         * </p>
         *
         * @param link a Link
         * @param more more links
         * @return this
         *
         * @since 2.0.0
         */
        public Builder array(final Link link, final Link... more) {
            array(new ArrayList<Link>() {{
                add(link);
                if (more != null) {
                    addAll(asList(more));
                }
            }});
            return this;
        }

        /**
         * Adds a list of links to the builder that will  be rendered as an array of link-objects
         * <p>
         *     {@link Link#isEquivalentTo(Link) Equivalent} links are NOT added but silently ignored.
         * </p>
         * <p>
         *     As specified in <a href="https://tools.ietf.org/html/draft-kelly-json-hal-06#section-4.1.1">Section 4.1.1</a>
         *     of the HAL specification, the {@code _links} object <em>"is an object whose property names are
         *     link relation types (as defined by [RFC5988]) and values are either a Link Object or an array
         *     of Link Objects"</em>.
         * </p>
         * <p>
         *     Adding a link using {@code array(List<Link>)} will result in a representation, where the links are
         *     rendered as an array of Link Objects, even if there are only single links for a given Link-Relation Type.
         * </p>
         *
         * @param links the list of links.
         * @return this
         *
         * @since 2.0.0
         */
        @SuppressWarnings("unchecked")
        public Builder array(final List<Link> links) {
            links.forEach(link -> {
                if (!this.links.containsKey(link.getRel())) {
                    this.links.put(link.getRel(), new ArrayList<>());
                }
                final Object linkOrList = this.links.get(link.getRel());
                if (linkOrList instanceof List) {
                    final List<Link> linksForRel = (List<Link>) linkOrList;
                    final boolean equivalentLinkExists = linksForRel
                            .stream()
                            .anyMatch(l -> l.isEquivalentTo(link));
                    if (!equivalentLinkExists) {
                        linksForRel.add(link);
                    }
                } else {
                    throw new IllegalStateException("Unable to add links with rel=" + link.getRel() + " as there is already a single Link Object added for this link-relation type");
                }
            });
            return this;
        }

        /**
         * Adds all links from {@link Links} to the builder.
         * <p>
         *     {@link Link#isEquivalentTo(Link) Equivalent} links are NOT added but silently ignored in order to
         *     avoid duplicate links.
         * </p>
         *
         * @param moreLinks the added links.
         * @return this
         *
         * @since 2.0.0
         */
        public Builder with(final Links moreLinks) {
            for (final String rel : moreLinks.getRels()) {
                if (moreLinks.isArray(rel)) {
                    array(moreLinks.getLinksBy(rel));
                } else {
                    single(moreLinks.getLinksBy(rel));
                }
            }
            return this;
        }

        /**
         * Removes registered links from the builder.
         *
         * @param rel Link-Relation Type of the links that should be removed.
         * @return this
         *
         * @since 2.0.0
         */
        public Builder without(final String rel) {
            this.links.remove(rel);
            return this;
        }

        public Builder using(final Curies curies) {
            this.curies = this.curies != null ? this.curies.mergeWith(curies) : curies;
            return this;
        }

        /**
         * Creates a Links instance from all added links.
         *
         * @return Links
         */
        public Links build() {
            return new Links(links, curies);
        }
    }

    /**
     * A Jackson JsonSerializer for Links. Used to render the _links part of HAL+JSON documents.
     *
     * @since 0.1.0
     */
    public static class LinksSerializer extends JsonSerializer<Links> {

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public void serialize(final Links links, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            for (final String rel : links.links.keySet()) {
                if (links.isArray(rel)) {
                    gen.writeArrayFieldStart(rel);
                    for (final Link link : links.getLinksBy(rel)) {
                        gen.writeObject(link);
                    }
                    gen.writeEndArray();
                } else {
                    gen.writeObjectField(rel, links.getLinkBy(rel).orElseThrow(IllegalStateException::new));
                }
            }
            gen.writeEndObject();
        }
    }

    /**
     * A Jackson JsonDeserializer for Links. Used to parse the _links part of HAL+JSON documents.
     *
     * @since 0.1.0
     */
    public static class LinksDeserializer extends JsonDeserializer<Links> {


        private static final TypeReference<Map<String, ?>> TYPE_REF_LINK_MAP = new TypeReference<Map<String, ?>>() {};

        /**
         * {@inheritDoc}
         */
        @Override
        public Links deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final Map<String,?> linksMap = p.readValueAs(TYPE_REF_LINK_MAP);
            final Map<String, Object> links = linksMap
                    .entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, e -> asArrayOrObject(e.getKey(), e.getValue())));

            fixPossibleIssueWithCuriesAsSingleLinkObject(links);

            return new Links(
                    links,
                    emptyCuries()
            );
        }

        private void fixPossibleIssueWithCuriesAsSingleLinkObject(Map<String, Object> links) {
            // CURIES should always have a List of Links. Because this might not aways be the case, we have to fix this:
            if (links.containsKey(CURIES_REL)) {
                if (links.get(CURIES_REL) instanceof Link) {
                    links.put(CURIES_REL, new ArrayList<Link>() {{
                        add((Link) links.get(CURIES_REL));
                    }});
                }
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Object asArrayOrObject(final String rel, final Object value) {
            if (value instanceof Map) {
                return asLink(rel, (Map)value);
            } else {
                try {
                    return ((List<Map>) value).stream().map(o -> asLink(rel, o)).collect(toList());
                } catch (final ClassCastException e) {
                    throw new IllegalStateException("Expected a single Link or a List of Links: rel=" + rel + " value=" + value);
                }
            }
        }

        @SuppressWarnings("rawtypes")
        private Link asLink(final String rel, final Map value) {
            Link.Builder builder = linkBuilder(rel, value.get("href").toString())
                    .withHrefLang((String) value.get("hreflang"))
                    .withName((String) value.get("name"))
                    .withTitle((String) value.get("title"))
                    .withType((String) value.get("type"))
                    .withProfile((String) value.get("profile"))
                    .withDeprecation((String) value.get("deprecation"));
            return builder.build();
        }
    }
}
