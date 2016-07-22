package de.otto.edison.hal;

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

import static de.otto.edison.hal.Link.linkBuilder;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-4.1.1">draft-kelly-json-hal-08#section-4.1.1</a>
 * @since 0.1.0
 */
@JsonSerialize(using = Links.LinksSerializer.class)
@JsonDeserialize(using = Links.LinksDeserializer.class)
public class Links {

    private static final Links EMPTY_LINKS = new Links();

    private final Map<String, List<Link>> links = new LinkedHashMap<>();

    /**
     *
     * @since 0.1.0
     */
    Links() {}

    /**
     *
     * @param links
     * @since 0.1.0
     */
    private Links(final Map<String, List<Link>> links) {
        this.links.putAll(links);
    }

    /**
     * @return
     *
     * @since 0.1.0
     */
    public static Links emptyLinks() {
        return EMPTY_LINKS;
    }

    /**
     *
     * @param link
     * @param more
     * @return
     *
     * @since 0.1.0
     */
    public static Links linkingTo(final Link link, final Link... more) {
        final Map<String,List<Link>> allLinks = new LinkedHashMap<>();
        allLinks.put(link.getRel(), new ArrayList<Link>(){{add(link);}});
        if (more != null) {
            stream(more).forEach(l -> {
                if (!allLinks.containsKey(l.getRel())) {
                    allLinks.put(l.getRel(), new ArrayList<>());
                }
                allLinks.get(l.getRel()).add(l);
            });
        }
        return new Links(allLinks);
    }

    /**
     *
     * @param rel
     * @return
     *
     * @since 0.1.0
     */
    public Optional<Link> getLinkBy(final String rel) {
        List<Link> links = this.links.get(rel);
        return links == null || links.isEmpty()
                ? Optional.empty()
                : Optional.of(links.get(0));
    }

    /**
     *
     * @param rel
     * @return
     *
     * @since 0.1.0
     */
    public List<Link> getLinksBy(final String rel) {
        List<Link> links = this.links.get(rel);
        return links == null
                ? emptyList()
                : links;
    }

    /**
     *
     * @return
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
     *
     * @since 0.1.0
     */
    static class LinksSerializer extends JsonSerializer<Links> {

        @Override
        @SuppressWarnings("unchecked")
        public void serialize(final Links value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            for (final String rel : value.links.keySet()) {
                final List<Link> links = value.links.get(rel);
                if (links.size() > 1) {
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
     *
     * @since 0.1.0
     */
    static class LinksDeserializer extends JsonDeserializer<Links> {

        private static final TypeReference<Map<String, ?>> TYPE_REF_LINK_MAP = new TypeReference<Map<String, ?>>() {};

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
