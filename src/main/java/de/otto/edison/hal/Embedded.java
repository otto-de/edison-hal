package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.*;

import static de.otto.edison.hal.Curies.emptyCuries;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

/**
 * <p>
 *     The embedded items of a HalResource.
 * </p>
 * <pre><code>
 * {
 *      "_links": {
 *          ...
 *      },
 *      "_embedded": {
 *          "item" : [
 *              {"description" : "first embedded item (resource object)"},
 *              {"description" : "second embedded item (resource object"}
 *          ],
 *          "example" : {
 *              "description" : "A single resource object"
 *          }
 *      },
 *      "someAttribute" : "Foo"
 * }
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
     *
     * <p>
     *     The values are either List&lt;HalRepresentation&gt; or single HalRepresentation instances.
     * </p>
     */
    private final Map<String,Object> items;
    /**
     * The Curies instance used to resolve curies
     */
    private final Curies curies;

    /**
     * Used by Jackson to parse/create Embedded instances.
     *
     * @since 0.1.0
     */
    Embedded() {
        items = null;
        curies = emptyCuries();
    }

    /**
     * Create an Embedded instance from a Map of nested items.
     *
     * @param items The embedded items, mapped by link-relation type. The values of the map must be of type
     *              HalRepresentation or List&lt;HalRepresentation&gt;
     *
     * @since 0.1.0
     */
    private Embedded(final Map<String, Object> items) {
        this.items = items;
        this.curies = emptyCuries();
    }

    @SuppressWarnings("unchecked")
    private Embedded(final Map<String, Object> items, final Curies curies) {
        final Map<String, Object> curiedItems = new LinkedHashMap<>();
        for (final String rel : items.keySet()) {
            final Object itemOrListOfItems = items.get(rel);
            if (itemOrListOfItems instanceof List) {
                curiedItems.put(curies.resolve(rel), ((List<HalRepresentation>) itemOrListOfItems)
                        .stream()
                        .map(halRepresentation -> halRepresentation.mergeWithEmbedding(curies))
                        .collect(toList()));
            } else {
                curiedItems.put(curies.resolve(rel), ((HalRepresentation) itemOrListOfItems).mergeWithEmbedding(curies));
            }
        }
        this.items = curiedItems;
        this.curies = curies;
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
     * Create an Embedded instance with a single embedded HalRepresentations that will be rendered as a single
     * item instead of an array of embedded items.
     *
     * @param rel the link-relation type of the embedded items
     * @param embeddedItem the single embedded item
     * @return Embedded
     *
     * @since 2.0.0
     */
    public static Embedded embedded(final String rel,
                                    final HalRepresentation embeddedItem) {
        return new Embedded(singletonMap(rel, embeddedItem));
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
     * Checks the existence of embedded items with link-relation type {@code rel}
     *
     * @param rel the link-relation type
     * @return true, if item(s) exists, false otherwise
     *
     * @since 2.0.0
     */
    public boolean hasItem(final String rel) {
        final String resolvedRel = curies.resolve(rel);
        return items != null && items.containsKey(resolvedRel);
    }

    /**
     * Returns true if there is at least one embedded item with link-relation type {@code rel}, and if the item will
     * be rendered as an array of embedded items instead of a single object.
     *
     * @param rel the link-relation type
     * @return boolean
     *
     * @since 2.0.0
     */
    public boolean isArray(final String rel) {
        final String resolvedRel = curies.resolve(rel);
        return hasItem(rel) && (items.get(resolvedRel) instanceof List);
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

    protected Embedded using(final Curies curies) {
        return new Embedded(items, curies);
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
    @SuppressWarnings("unchecked")
    public List<HalRepresentation> getItemsBy(final String rel) {
        if (!hasItem(rel)) {
            return emptyList();
        } else {
            final Object item = items.get(curies.resolve(rel));
            return item instanceof List
                    ? (List<HalRepresentation>) item
                    : singletonList((HalRepresentation) item);
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
        private final Map<String,Object> _embedded = new LinkedHashMap<>();
        private Curies curies = emptyCuries();

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
         * <p>
         *     The HalRepresentations added using this method will be rendered as an array of object instead of
         *     a {@link #with(String, HalRepresentation) single object}:
         * </p>
         * <pre><code>
         *     {
         *         "_embedded" : {
         *             "foo" : [
         *                 {
             *                 "_links" : {
             *                     "self" : { "href" : "http://example.com/a-single-embedded-foo-item}
             *                 }
         *                 }
         *             ]
         *         }
         *     }
         * </code></pre>
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
         * Adds / replaces the embedded representation for a link-relation type.
         * <p>
         *     The single HalRepresentation added using this method will be rendered as a single object instead of
         *     an {@link #with(String, List) array of objects}:
         * </p>
         * <pre><code>
         *     {
         *         "_embedded" : {
         *             "foo" : {
         *                 "_links" : {
         *                     "self" : { "href" : "http://example.com/a-single-embedded-foo-item"}
         *                 }
         *             }
         *         }
         *     }
         * </code></pre>
         *
         * @param rel the link-relation type
         * @param embeddedRepresentation the single embedded item
         * @return EmbeddedBuilder
         *
         * @since 0.2.0
         */
        public Builder with(final String rel, final HalRepresentation embeddedRepresentation) {
            _embedded.put(rel, embeddedRepresentation);
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

        public Builder using(final Curies curies) {
            this.curies = curies;
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
            return _embedded.isEmpty() ? emptyEmbedded() : new Embedded(_embedded, curies);
        }
    }

    /**
     * The Jackson JsonSerializer used to serialize Embedded instances to JSON.
     *
     * @since 0.1.0
     */
    public static class EmbeddedSerializer extends JsonSerializer<Embedded> {

        @Override
        public void serialize(Embedded value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
                final Map<String, Object> items = p.readValueAs(TYPE_REF_LIST_OF_HAL_REPRESENTATIONS);
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
