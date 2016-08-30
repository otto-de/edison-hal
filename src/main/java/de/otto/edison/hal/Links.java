package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static de.otto.edison.hal.CuriTemplate.curiTemplateFor;
import static de.otto.edison.hal.CuriTemplate.matchingCuriTemplateFor;
import static de.otto.edison.hal.Link.linkBuilder;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Representation of a number of HAL _links.
 *
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-4.1.1">draft-kelly-json-hal-08#section-4.1.1</a>
 * @since 0.1.0
 */
@JsonSerialize(using = Links.LinksSerializer.class)
@JsonDeserialize(using = Links.LinksDeserializer.class)
public class Links {

    private static final String CURIES_REL = "curies";

    private static final Links EMPTY_LINKS = new Links();

    private final Map<String, List<Link>> links = new LinkedHashMap<>();
    /**
     * Links in embedded items need to know about the CURIs of the embedding HalRepresentation, in order to resolve
     * compact URIs.
     */
    private volatile List<Link> curiesFromEmbedding = emptyList();

    /**
     *
     * @since 0.1.0
     */
    Links() {}

    /**
     * Creates a Links object from a map containing rel->List<Link>.
     *
     * If the links contain curies, the link-relation types are shortened to the curied format name:key.
     *
     * @param links a map with link-relation types as key and the list of links as value.
     * @since 0.1.0
     */
    private Links(final Map<String, List<Link>> links) {
        final List<Link> curies = links.get(CURIES_REL);
        if (curies == null || curies.isEmpty()) {
            this.links.putAll(links);
        } else {
            this.links.put(CURIES_REL, curies);
            links.keySet().forEach(rel -> {
                if (!rel.equals(CURIES_REL)) {
                    final Optional<CuriTemplate> curiTemplate = matchingCuriTemplateFor(curies, rel);
                    if (curiTemplate.isPresent()) {
                        this.links.put(curiTemplate.get().curiedRelFrom(rel), links.get(rel));
                    } else {
                        this.links.put(rel, links.get(rel));
                    }
                }
            });
        }
    }

    /**
     * Factory method used to create an empty Links instance.
     *
     * @return empty Links
     *
     * @since 0.1.0
     */
    public static Links emptyLinks() {
        return EMPTY_LINKS;
    }

    /**
     * Factory method used to build a Links instance from one or more {@link Link} objects.
     *
     * @param link a Link
     * @param more optionally more Links
     * @return Links
     *
     * @since 0.1.0
     */
    public static Links linkingTo(final Link link, final Link... more) {
        final Map<String,List<Link>> allLinks = new LinkedHashMap<>();
        allLinks.put(link.getRel(), new ArrayList<Link>(){{add(link);}});
        if (more != null) {
            Arrays.stream(more).forEach(l -> {
                if (!allLinks.containsKey(l.getRel())) {
                    allLinks.put(l.getRel(), new ArrayList<>());
                }
                allLinks.get(l.getRel()).add(l);
            });
        }
        return new Links(allLinks);
    }

    /**
     * Creates a Links object from a list of links.
     *
     * @param links the list of links.
     * @return Links
     *
     * @since 0.2.0
     */
    public static Links linkingTo(final List<Link> links) {
        final Map<String,List<Link>> allLinks = new LinkedHashMap<>();
        links.forEach(l -> {
            if (!allLinks.containsKey(l.getRel())) {
                allLinks.put(l.getRel(), new ArrayList<>());
            }
            allLinks.get(l.getRel()).add(l);
        });
        return new Links(allLinks);
    }

    /**
     * Factory method used to build a Links.Builder.
     *
     * @return Links.Builder
     */
    public static Builder linksBuilder() {
        return new Builder();
    }

    /**
     * Returns a Stream of links.
     */
    public Stream<Link> stream() {
        return links.values().stream()
                .flatMap(Collection::stream);
    }

    /**
     * Returns all link-relation types of the embedded items.
     *
     * @return list of link-relation types
     * @since 0.3.0
     */
    @JsonIgnore
    public Set<String> getRels() {
        return links.keySet();
    }

    /**
     * Returns the first (if any) link having the specified link-relation type.
     *
     * If CURIs are used to shorten custom link-relation types, it is possible to either use expanded link-relation types,
     * or the CURI of the link-relation type. Using CURIs to retrieve links is not recommended, because it
     * requires that the name of the CURI is known by clients.
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
     * Returns the list of links having the specified link-relation type.
     *
     * If CURIs are used to shorten custom link-relation types, it is possible to either use expanded link-relation types,
     * or the CURI of the link-relation type. Using CURIs to retrieve links is not recommended, because it
     * requires that the name of the CURI is known by clients.
     *
     * @param rel the link-relation type of the retrieved link.
     * @return list of matching link
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-8.2">draft-kelly-json-hal-08#section-8.2</a>
     * @since 0.1.0
     */
    public List<Link> getLinksBy(final String rel) {
        final List<Link> links = this.links.get(rel);

        if (links == null || links.isEmpty()) {
            return getCuriedLinks(rel);
        } else {
            return links;
        }
    }

    /**
     * Helper method used to retrieve Links with support for CURIs.
     *
     * @param rel a curied or full link-relation type.
     * @return List of matching links.
     */
    private List<Link> getCuriedLinks(String rel) {
        final List<Link> curies = getCuries();
        for (final Link curi : curies) {
            final CuriTemplate curiTemplate = curiTemplateFor(curi);
            if (curiTemplate.matches(rel)) {
                final String shortRel = curiTemplate.curiedRelFrom(rel);
                return this.links.containsKey(shortRel) ? this.links.get(shortRel) : emptyList();
            }
        }
        return emptyList();
    }

    /**
     * Helper method used to get the CURIs.
     *
     * @return List of CURI links, or empty list
     */
    private List<Link> getCuries() {
        final List<Link> curies = new ArrayList<>(curiesFromEmbedding);
        if (links.containsKey(CURIES_REL)) {
            curies.addAll(links.get(CURIES_REL));
        }
        return curies;
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

    void withParentCuries(final List<Link> curiesFromEmbedding) {
        this.curiesFromEmbedding = curiesFromEmbedding;
        if (this.links != null) {
            final List<Link> curies = getCuries();
            final Map<String, List<Link>> curiedLinks = new LinkedHashMap<>();
            this.links.keySet().forEach(rel->{
                final Optional<CuriTemplate> curiTemplate = matchingCuriTemplateFor(curies, rel);
                if (curiTemplate.isPresent()) {
                    curiedLinks.put(curiTemplate.get().curiedRelFrom(rel), links.get(rel));
                } else {
                    curiedLinks.put(rel, links.get(rel));
                }

            });
            this.links.clear();
            this.links.putAll(curiedLinks);
        }
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
        final Map<String,List<Link>> links = new LinkedHashMap<>();

        /**
         * Adds one or more Links.
         *
         * @param link a Link
         * @param more more links
         * @return this
         */
        public Builder with(final Link link, final Link... more) {
            links.put(link.getRel(), new ArrayList<Link>() {{
                add(link);
            }});
            if (more != null) {
                Arrays.stream(more).forEach(l -> {
                    if (!links.containsKey(l.getRel())) {
                        links.put(l.getRel(), new ArrayList<>());
                    }
                    links.get(l.getRel()).add(l);
                });
            }
            return this;
        }

        /**
         * Adds a list of links.
         *
         * @param links the list of links.
         * @return this
         *
         * @since 0.2.0
         */
        public Builder with(final List<Link> links) {
            links.forEach(l -> {
                if (!this.links.containsKey(l.getRel())) {
                    this.links.put(l.getRel(), new ArrayList<>());
                }
                this.links.get(l.getRel()).add(l);
            });
            return this;
        }

        /**
         * Creates a Links instance from all added links.
         *
         * @return Links
         */
        public Links build() {
            return new Links(links);
        }
    }

    /**
     * A Jackson JsonSerializer for Links. Used to render the _links part of HAL+JSON documents.
     *
     * @since 0.1.0
     */
    static class LinksSerializer extends JsonSerializer<Links> {

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public void serialize(final Links value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            for (final String rel : value.links.keySet()) {
                final List<Link> links = value.links.get(rel);
                // for some strange reason, CURI links obviously need to have an array of values:
                if (links.size() > 1 || rel.equals("curies")) {
                    gen.writeArrayFieldStart(rel);
                    for (final Link link : links) {
                        gen.writeObject(link);
                    }
                    gen.writeEndArray();
                } else {
                    gen.writeObjectField(rel, links.get(0));
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
    static class LinksDeserializer extends JsonDeserializer<Links> {

        private static final TypeReference<Map<String, ?>> TYPE_REF_LINK_MAP = new TypeReference<Map<String, ?>>() {};

        /**
         * {@inheritDoc}
         */
        @Override
        public Links deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            final Map<String,?> linksMap = p.readValueAs(TYPE_REF_LINK_MAP);
            return new Links(linksMap
                    .entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, e -> asListOfLinks(e.getKey(), e.getValue()))));
        }

        @SuppressWarnings("unchecked")
        private List<Link> asListOfLinks(final String rel, final Object value) {
            if (value instanceof Map) {
                return singletonList(asLink(rel, (Map)value));
            } else {
                return ((List<Map>)value).stream().map(o->asLink(rel, o)).collect(toList());
            }
        }

        private Link asLink(final String rel, final Map value) {
            Link.Builder builder = linkBuilder(rel, value.get("href").toString())
                    .withHrefLang((String) value.get("hreflang"))
                    .withName((String) value.get("name"))
                    .withTitle((String) value.get("title"))
                    .withType((String) value.get("type"))
                    .withProfile((String) value.get("profile"))
                    .withDeprecation((String) value.get("deprecation"));
            if (TRUE.equals(value.get("templated"))) {
                builder.beeingTemplated();
            }
            return builder.build();
        }
    }
}
