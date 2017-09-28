package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.*;

import static de.otto.edison.hal.RelRegistry.defaultRelRegistry;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

/**
 * <p>
 *     The embedded items of a HalResource.
 * </p>
 * <p>
 *     Provides access to embedded items by link-relation type.
 * </p>
 * <pre><code>
 *     {
 *         "_embedded": {
 *             "item": [
 *                  {
 *                      "_links": {
 *                          ...
 *                      },
 *                      "_embedded": {
 *                          ...
 *                      },
 *                      "someAttribute" : "Foo"
 *                  },
 *                  {
 *                      ...
 *                  },
 *             ]
 *         }
 *
 *     }
 * </code></pre>
 *
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-4.1.2">draft-kelly-json-hal-08#section-4.1.2</a>
 * @since 0.1.0
 */
@JsonSerialize(using = Embedded.EmbeddedSerializer.class)
@JsonDeserialize(using = Embedded.EmbeddedDeserializer.class)
public class Embedded {
    /**
     * The embedded items, mapped by link-relation type.
     */
    private final Map<String,List<HalRepresentation>> items;
    /**
     * The RelRegistry used to resolve curies
     */
    private final RelRegistry relRegistry;

    /**
     * Used by Jackson to parse/create Embedded instances.
     *
     * @since 0.1.0
     */
    Embedded() {
        items = null;
        relRegistry = defaultRelRegistry();
    }

    /**
     * Create an Embedded instance from a Map of nested items.
     *
     * @param items The embedded items, mapped by link-relation type
     *
     * @since 0.1.0
     */
    private Embedded(final Map<String, List<HalRepresentation>> items) {
        this.items = items;
        this.relRegistry = defaultRelRegistry();
    }

    private Embedded(final Map<String, List<HalRepresentation>> items, final RelRegistry relRegistry) {
        final Map<String, List<HalRepresentation>> curiedItems = new LinkedHashMap<>();
        for (final String rel : items.keySet()) {
            curiedItems.put(relRegistry.resolve(rel), items.get(rel)
                    .stream()
                    .map(halRepresentation -> halRepresentation.using(relRegistry))
                    .collect(toList()));
        }
        this.items = curiedItems;
        this.relRegistry = relRegistry;
    }

    /**
     * Create an empty embedded instance, without embedded items.
     *
     * @return empty Embedded
     *
     * @since 0.1.0
     */
    public static Embedded emptyEmbedded() {
        return new Embedded(null);
    }

    /**
     * Create an Embedded instance with a list of nested HalRepresentations for a single link-relation type.
     *
     * @param rel the link-relation type of the embedded representations
     * @param embeddedRepresentations the list of embedded representations
     * @return Embedded
     *
     * @since 0.1.0
     */
    public static Embedded embedded(final String rel,
                                    final List<? extends HalRepresentation> embeddedRepresentations) {
        return new Embedded(singletonMap(rel, new ArrayList<>(embeddedRepresentations)));
    }

    /**
     * Create a linksBuilder used to build Embedded instances with more than one link-relation type.
     *
     * @return EmbeddedBuilder
     *
     * @since 0.1.0
     */
    public static Builder embeddedBuilder() {
        return new Builder();
    }

    protected Embedded using(final RelRegistry relRegistry) {
        return new Embedded(items, relRegistry);
    }

    /**
     * Returns all link-relation types of the embedded items.
     *
     * @return list of link-relation types
     * @since 0.3.0
     */
    @JsonIgnore
    public Set<String> getRels() {
        return items != null ? items.keySet() : emptySet();
    }

    /**
     * Returns the embedded items by link-relation type.
     * <p>
     * If no items with this type are embedded, an empty list is returned.
     *
     * @param rel the link-relation type
     * @return list of embedded HAL representations for the link-relation type
     *
     * @since 0.1.0
     */
    @JsonIgnore
    public List<HalRepresentation> getItemsBy(final String rel) {
        if (items != null) {
            return items.getOrDefault(relRegistry.resolve(rel), emptyList());
        } else {
            return emptyList();
        }
    }

    /**
     * Returns the embedded items by link-relation type.
     * <p>
     *     This method can be used if the Java type of the embedded representations is known, for example because the
     *     {@link HalParser} is used to map the items to a specific HalRepresentation:
     * </p>
     * <pre><code>
     * final String json = ...
     *
     * final FooHalRepresentation foo = HalParser
     *         .parse(json)
     *         .as(FooHalRepresentation.class, with("bar", BarHalRepresentation.class));
     *
     * final List&lt;BarHalRepresentation embeddedBars = foo
     *         .getEmbedded()
     *         .getItemsBy("bar", BarHalRepresentation.class);
     * </code></pre>
     *
     * @param rel the link-relation type
     * @param asType the expected class of the embedded items.
     * @param <E> the specific type of the embedded HalRepresentations
     * @return List of E
     * @throws ClassCastException if the expected type does not fit the actual type of the embedded items.
     *
     * @since 0.1.0
     */
    @JsonIgnore
    public <E extends HalRepresentation> List<E> getItemsBy(final String rel, final Class<E> asType) {
        return getItemsBy(rel).stream().map(asType::cast).collect(toList());
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

        Embedded embedded = (Embedded) o;

        return this.items != null ? this.items.equals(embedded.items) : embedded.items == null;

    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public int hashCode() {
        return items != null ? items.hashCode() : 0;
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public String toString() {
        return "Embedded{" +
                "items=" + items +
                '}';
    }

    /**
     * @return true if there are no embedded items, false otherwise.
     *
     * @since 0.2.0
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public final static class Builder {
        private final Map<String,List<HalRepresentation>> _embedded = new LinkedHashMap<>();
        private RelRegistry relRegistry = defaultRelRegistry();

        /**
         * <p>
         *     Creates an EmbeddedBuilder initialized from a copy of an Embedded instance.
         * </p>
         * <p>
         *     This is used to add / replace lists of HAL representations for a link-relation type.
         * </p>
         * @param embedded the Embedded instance to be copied.
         * @return EmbeddedBuilder that is initialized using {@code embedded}.
         *
         * @since 0.1.0
         */
        public static Builder copyOf(final Embedded embedded) {
            final Builder builder = new Builder();
            if (embedded != null && embedded.items != null) {
                builder._embedded.putAll(embedded.items);
            }
            return builder;
        }

        /**
         * Adds / replaces the embedded representations for a link-relation type.
         *
         * @param rel the link-relation type
         * @param embeddedRepresentations the embedded items
         * @return EmbeddedBuilder
         *
         * @since 0.2.0
         */
        public Builder with(final String rel, final List<? extends HalRepresentation> embeddedRepresentations) {
            _embedded.put(rel, new ArrayList<>(embeddedRepresentations));
            return this;
        }

        /**
         * Removes the embedded representations for a link-relation type.
         *
         * @param rel the link-relation type
         * @return EmbeddedBuilder
         *
         * @since 0.2.0
         */
        public Builder without(final String rel) {
            _embedded.remove(rel);
            return this;
        }

        public Builder using(final RelRegistry relRegistry) {
            this.relRegistry = relRegistry;
            return this;
        }

        /**
         * Builds an Embedded instance.
         *
         * @return Embedded
         *
         * @since 0.1.0
         */
        public Embedded build() {
            return _embedded.isEmpty() ? emptyEmbedded() : new Embedded(_embedded, relRegistry);
        }
    }

    /**
     * The Jackson JsonSerializer used to serialize Embedded instances to JSON.
     *
     * @since 0.1.0
     */
    public static class EmbeddedSerializer extends JsonSerializer<Embedded> {

        @Override
        public void serialize(Embedded value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeObject(value.items);
        }
    }

    /**
     * The Jackson JsonDeserializer used to deserialize JSON into Embedded instances.
     *
     * @since 0.1.0
     */
    public static class EmbeddedDeserializer extends JsonDeserializer<Embedded> {

        private static final TypeReference<Map<String, List<HalRepresentation>>> TYPE_REF_LIST_OF_HAL_REPRESENTATIONS = new TypeReference<Map<String, List<HalRepresentation>>>() {};

        @Override
        public Embedded deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                final Map<String, List<HalRepresentation>> items = p.readValueAs(TYPE_REF_LIST_OF_HAL_REPRESENTATIONS);
                return new Embedded(items);
            } catch (final JsonMappingException e) {
                if (e.getMessage().contains("Can not deserialize instance of java.util.ArrayList out of START_OBJECT token")) {
                    throw new JsonMappingException(p, "Can not deserialize single embedded items for a link-relation type. Try using the HalParser, or configure your ObjectMapper: 'objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)'.", e);
                } else {
                    throw e;
                }
            }
        }
    }
}
