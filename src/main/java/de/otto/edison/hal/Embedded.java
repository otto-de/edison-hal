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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

/**
 * The embedded items of a HalResource.
 *
 * Provides access to embedded items by link-relation type.
 *
 * <code><pre>
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
 * </pre></code>
 *
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-4.1.2></a>
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
     * Used by Jackson to parse/create Embedded instances.
     *
     * @since 0.1.0
     */
    Embedded() {
        items =null;}

    /**
     * Create an Embedded instance from a Map of nested items.
     *
     * @param items The embedded items, mapped by link-relation type
     *
     * @since 0.1.0
     */
    private Embedded(final Map<String, List<HalRepresentation>> items) {
        this.items = items;
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
                                    final List<HalRepresentation> embeddedRepresentations) {
        return new Embedded(singletonMap(rel, embeddedRepresentations));
    }

    /**
     * Create a builder used to build Embedded instances with more than one link-relation type.
     *
     * @return EmbeddedBuilder
     *
     * @since 0.1.0
     */
    public static Builder embeddedBuilder() {
        return new Builder();
    }

    /**
     * Returns the embedded items by link-relation type.
     *
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
            return items.containsKey(rel) ? items.get(rel) : emptyList();
        } else {
            return emptyList();
        }
    }

    /**
     * Returns the embedded items by link-relation type.
     *
     * This method can be used if the Java type of the embedded representations is known, for example because the
     * {@link HalParser} is used to map the items to a specific HalRepresentation:
     *
     * <code><pre>
     * final String json = ...
     *
     * final FooHalRepresentation foo = HalParser
     *         .parse(json)
     *         .as(FooHalRepresentation.class, withEmbedded("bar", BarHalRepresentation.class));
     *
     * final List<BarHalRepresentation embeddedBars = foo
     *         .getEmbedded()
     *         .getItemsBy("bar", BarHalRepresentation.class);
     * </pre></code>
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
        if (items != null) {
            List<E> representations = new ArrayList<>();
            items.get(rel).forEach(i -> representations.add(asType.cast(i)));
            return items.containsKey(rel) ? representations : emptyList();
        } else {
            return emptyList();
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
     * A Builder used to build more complex Embedded objects.
     *
     * @since 0.1.0
     */
    public final static class Builder {
        private final Map<String,List<HalRepresentation>> _embedded = new LinkedHashMap<>();

        /**
         * Creates an EmbeddedBuilder initialized from a copy of an Embedded instance.
         *
         * This is used to add / replace lists of HAL representations for a link-relation type.
         *
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
         * @since 0.1.0
         */
        public Builder withEmbedded(final String rel, final List<HalRepresentation> embeddedRepresentations) {
            _embedded.put(rel, embeddedRepresentations);
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
        public Builder withoutEmbedded(final String rel) {
            _embedded.remove(rel);
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
            return _embedded.isEmpty() ? emptyEmbedded() : new Embedded(_embedded);
        }
    }

    /**
     * The Jackson JsonSerializer used to serialize Embedded instances to JSON.
     *
     * @since 0.1.0
     */
    static class EmbeddedSerializer extends JsonSerializer<Embedded> {

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
    static class EmbeddedDeserializer extends JsonDeserializer<Embedded> {

        private static final TypeReference<Map<String, List<HalRepresentation>>> TYPE_REF_LIST_OF_HAL_REPRESENTATIONS = new TypeReference<Map<String, List<HalRepresentation>>>() {};

        @Override
        public Embedded deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final Map<String,List<HalRepresentation>> items = p.readValueAs(TYPE_REF_LIST_OF_HAL_REPRESENTATIONS);
            return new Embedded(items);
        }
    }
}
