package de.otto.edison.hal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static de.otto.edison.hal.CuriTemplate.matchingCuriTemplateFor;

/**
 * <p>
 *     A parser used to parse application/hal+json representations of REST resources into Java objects.
 * </p>
 * <p>
 *     Simple HAL representations can be parsed using Jackson's ObjectMapper like this:
 * </p>
 * <pre><code>
 *     new ObjectMapper().readValue(json.getBytes(), MyHalRepresentation.class)
 * </code></pre>
 * <p>
 *     The same can be achieved by using a HalParser:
 * </p>
 * <pre><code>
 *     final MyHalRepresentation result = HalParser.parse(json).as(MyHalRepresentation.class);
 * </code></pre>
 * <p>
 *     However, if the representation contains embedded items, Jackson is unable to determine the Java type of
 *     the embedded items, because the HAL document itself does not contain type information. In this case, Jackson
 *     needs some help to identify the concrete types of embedded items. Using the HalParser, this is accomplished
 *     like this:
 * </p>
 * <pre><code>
 *     final FooHalRepresentation result = HalParser
 *             .parse(json)
 *             .as(FooHalRepresentation.class, with("bar", BarHalRepresentation.class));
 * </code></pre>
 * @since 0.1.0
 */
public final class HalParser {

    /** The Jackson ObjectMapper used to parse application/hal+json documents. */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    static {
        JSON_MAPPER.configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
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
     * @return T
     * @throws IOException if parsing the JSON fails for some reason.
     * @since 0.1.0
     */
    public <T extends HalRepresentation> T as(final Class<T> type, final EmbeddedTypeInfo typeInfo) throws IOException {
        final JsonNode jsonNode = JSON_MAPPER.readTree(json);
        final T halRepresentation = JSON_MAPPER.convertValue(jsonNode, type);

        resolveEmbeddedTypeInfo(typeInfo, jsonNode, halRepresentation);
        return halRepresentation;
    }

    private <T extends HalRepresentation> void resolveEmbeddedTypeInfo(EmbeddedTypeInfo typeInfo, JsonNode jsonNode, T halRepresentation) {
        if (!jsonNode.isMissingNode()) {
            final List<HalRepresentation> embeddedValues = new ArrayList<>();
            final JsonNode embeddedNodeForRel = findPossiblyCuriedEmbeddedNode(halRepresentation, jsonNode, typeInfo.rel);
            if (!embeddedNodeForRel.isMissingNode()) {
                resolveEmbeddedTypeInfo(typeInfo, halRepresentation, embeddedValues, embeddedNodeForRel);
            } else {
                // maybe there are some nested embeddeds which should be resolved based on typeInfo...
                Embedded embedded = halRepresentation.getEmbedded();
                embedded.getRels().forEach(rel -> {
                    embedded.getItemsBy(rel).forEach(embeddedItem -> {
                        final JsonNode embeddedNode = findEmbeddedNode(jsonNode, rel);
                        if (embeddedNode.isArray()) {
                            embeddedNode.iterator().forEachRemaining(node -> resolveEmbeddedTypeInfo(typeInfo, node, embeddedItem));
                        }
                    });
                });
            }
        }
    }

    private <T extends HalRepresentation> void resolveEmbeddedTypeInfo(EmbeddedTypeInfo typeInfo, T halRepresentation, List<HalRepresentation> embeddedValues, JsonNode embeddedNodeForRel) {
        if (embeddedNodeForRel.isArray()) {
            for (int i = 0; i < embeddedNodeForRel.size(); i++) {
                final JsonNode embeddedNode = embeddedNodeForRel.get(i);
                final HalRepresentation embedded = JSON_MAPPER.convertValue(embeddedNode, typeInfo.type);
                if (embedded != null) {
                    embeddedValues.add(embedded);
                }
            }
        } else {
            HalRepresentation embedded = JSON_MAPPER.convertValue(embeddedNodeForRel, typeInfo.type);
            if (embedded != null) {
                embeddedValues.add(embedded);
            }
        }
        halRepresentation.withEmbedded(typeInfo.rel, embeddedValues);
    }

    /**
     * Returns the JsonNode of the embedded items by link-relation type.
     *
     * @param jsonNode the JsonNode of the document
     * @param rel the link-relation type of interest
     * @return JsonNode
     * @since 1.0.0
     */
    private JsonNode findEmbeddedNode(final JsonNode jsonNode, final String rel) {
        return jsonNode.at("/_embedded/" + rel);
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
