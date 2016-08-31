package de.otto.edison.hal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.otto.edison.hal.CuriTemplate.matchingCuriTemplateFor;

/**
 * A parser used to parse application/hal+json representations of REST resources into Java objects.
 * <p>
 * Simple HAL representations can be parsed using Jackson's ObjectMapper like this:
 * <pre><code>
 *     new ObjectMapper().readValue(json.getBytes(), MyHalRepresentation.class)
 * </code></pre>
 *
 * The same can be achieved by using a HalParser:
 * <pre><code>
 * final MyHalRepresentation result = HalParser.parse(json).as(MyHalRepresentation.class);
 * </code></pre>
 * <p>
 * However, if the representation contains embedded items, Jackson is unable to determine the Java type of
 * the embedded items, because the HAL document itself does not contain type information. In this case, Jackson
 * needs some help to identify the concrete types of embedded items. Using the HalParser, this is accomplished
 * like this:
 * <pre><code>
 * final FooHalRepresentation result = HalParser
 *         .parse(json)
 *         .as(FooHalRepresentation.class, with("bar", BarHalRepresentation.class));
 * </code></pre>
 *
 * @since 0.1.0
 */
public final class HalParser {

    /** The Jackson ObjectMapper used to parse application/hal+json documents. */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Type information for embedded items. This is required if more complex embedded items should be parsed
     * into sub classes of HalRepresentation.
     * @param <T> The type of the HalRepresentation used for a single link-relation type.
     *
     * @since 0.1.0
     */
    public static class EmbeddedTypeInfo<T extends HalRepresentation> {

        /** The link-relation type used to identify items of the embedded type. */
        final String rel;

        /** The Java class used to deserialize the embedded items for the link-relation type */
        final Class<T> type;

        /** Creates a new EmbeddedTypeInfo by link-relation type and Java class of embedded items. */
        EmbeddedTypeInfo(final String rel, final Class<T> type) {
            this.rel = rel;
            this.type = type;
        }

        public static <E extends HalRepresentation> EmbeddedTypeInfo<E> withEmbedded(final String rel, final Class<E> embeddedType) {
            return new EmbeddedTypeInfo<>(rel, embeddedType);
        }

    }

    /** The JSON documents that is going to be parsed */
    private final String json;

    /**
     * Creates a HalParser for a given JSON document.
     *
     * @since 0.1.0
     */
    private HalParser(final String json) {
        this.json = json;
    }

    /**
     * Create a new HalParser for a JSON document.
     * @param json the application/hal+json document to be parsed.
     * @return HalParser instance.
     *
     * @since 0.1.0
     */
    public static HalParser parse(final String json) {
        return new HalParser(json);
    }

    /**
     * Specify the type that is used to parse and map the json.
     *
     * @param type the java type used to parse the JSON
     * @param <T> the type of the class, extending HalRepresentation
     * @return instance of T, containing the data of the parsed HAL document.
     * @throws IOException if parsing JSOn failed for some reason.
     *
     * @since 0.1.0
     */
    public <T extends HalRepresentation> T as(final Class<T> type) throws IOException {
        return JSON_MAPPER.readValue(json, type);
    }

    /**
     * Parse embedded items of a given link-relation type not as HalRepresentation, but as a sub-type of
     * HalRepresentation, so extra attributes of the embedded items can be accessed.
     *
     * @param type the Java class used to map JSON to.
     * @param typeInfo type information of the embedded items.
     * @param <T> The type used to parse the HAL document
     * @param <E> The type used to parse the embedded HAL documents
     * @return T
     * @throws IOException if parsing the JSON fails for some reason.
     *
     * @since 0.1.0
     */
    public <T extends HalRepresentation, E extends HalRepresentation> T as(final Class<T> type, final EmbeddedTypeInfo<E> typeInfo) throws IOException {
        final JsonNode jsonNode = JSON_MAPPER.readTree(json);
        final T halRepresentation = JSON_MAPPER.convertValue(jsonNode, type);

        final List<HalRepresentation> embeddedValues = new ArrayList<>();
        final JsonNode listOfHalRepresentations = findPossiblyCuriedEmbeddedNode(halRepresentation, jsonNode, typeInfo.rel);
        for (int i = 0; i < listOfHalRepresentations.size(); i++) {
            JsonNode embeddedRepresentation = listOfHalRepresentations.get(i);
            embeddedValues.add(JSON_MAPPER.convertValue(embeddedRepresentation, typeInfo.type));
        }
        halRepresentation.withEmbedded(typeInfo.rel, embeddedValues);
        return halRepresentation;
    }

    /**
     * Returns the JsonNode of the embedded items by link-relation type and resolves possibly curied rels.
     *
     * @param halRepresentation the HAL document including the 'curies' links.
     * @param jsonNode the JsonNode of the document
     * @param rel the link-relation type of interest
     * @return JsonNode
     * @since 0.3.0
     */
    private JsonNode findPossiblyCuriedEmbeddedNode(final HalRepresentation halRepresentation, final JsonNode jsonNode, final String rel) {
        final JsonNode listOfHalRepresentations = jsonNode.at("/_embedded/" + rel);
        if (listOfHalRepresentations.isMissingNode()) {
            // Try it with curied links:
            final List<Link> curies = halRepresentation.getLinks().getLinksBy("curies");
            final Optional<CuriTemplate> curiTemplate = matchingCuriTemplateFor(curies, rel);
            if (curiTemplate.isPresent()) {
                return jsonNode.at("/_embedded/" + curiTemplate.get().curiedRelFrom(rel));
            }
        }
        return listOfHalRepresentations;
    }
}
