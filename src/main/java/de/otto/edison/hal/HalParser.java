package de.otto.edison.hal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static java.util.Arrays.asList;

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
     * @param moreTypeInfo more type information of embedded items.
     * @param <T> The type used to parse the HAL document
     * @return T
     * @throws IOException if parsing the JSON fails for some reason.
     * @since 0.1.0
     */
    public <T extends HalRepresentation> T as(final Class<T> type,
                                              final EmbeddedTypeInfo typeInfo,
                                              final EmbeddedTypeInfo... moreTypeInfo) throws IOException {
        final List<EmbeddedTypeInfo> typeInfos = new ArrayList<>();
        typeInfos.add(typeInfo);
        if (moreTypeInfo != null) {
            typeInfos.addAll(asList(moreTypeInfo));
        }
        return as(type, typeInfos);
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
    public <T extends HalRepresentation> T as(final Class<T> type,
                                              final List<EmbeddedTypeInfo> typeInfo) throws IOException {
        final JsonNode jsonNode = JSON_MAPPER.readTree(json);
        final T halRepresentation = JSON_MAPPER.convertValue(jsonNode, type);
        resolveEmbeddedTypeInfo(typeInfo, jsonNode, halRepresentation);
        return halRepresentation;
    }

    /**
     * Resolves the types of the embedded items based on {@code typeInfos}
     *
     * @param typeInfos information about the expected types of embedded items
     * @param itemNode the json node of the item
     * @param item the item with possible embedded child items.
     * @param <T> the java type of the item
     */
    private <T extends HalRepresentation> void resolveEmbeddedTypeInfo(final List<EmbeddedTypeInfo> typeInfos,
                                                                       final JsonNode itemNode,
                                                                       final T item) {
        if (!itemNode.isMissingNode()) {
            typeInfos.forEach(typeInfo -> {
                final Optional<JsonNode> embeddedNodeForRel = findPossiblyCuriedEmbeddedNode(item, itemNode, typeInfo.getRel());
                embeddedNodeForRel.ifPresent(node -> resolveEmbeddedTypeInfoForRel(typeInfo, item, node));
            });
        }
    }

    /**
     * @param typeInfo info about the java type for a rel
     * @param parent the parent HalRepresentation that is embedding items for rel
     * @param embeddedNodeForRel the JsonNode of the rel with one or many embedded items as children
     * @param <T>
     */
    private <T extends HalRepresentation> void resolveEmbeddedTypeInfoForRel(final EmbeddedTypeInfo typeInfo,
                                                                             final T parent,
                                                                             final JsonNode embeddedNodeForRel) {
        final List<HalRepresentation> embeddedValues = new ArrayList<>();
        if (embeddedNodeForRel.isArray()) {
            for (int i = 0; i < embeddedNodeForRel.size(); i++) {
                final JsonNode embeddedNode = embeddedNodeForRel.get(i);
                final HalRepresentation embedded = JSON_MAPPER.convertValue(embeddedNode, typeInfo.getType())
                        .mergeWithEmbedding(parent.getRelRegistry());
                if (embedded != null) {
                    typeInfo.getNestedTypeInfo().forEach(nestedTypeInfo -> {
                        findPossiblyCuriedEmbeddedNode(embedded, embeddedNode, nestedTypeInfo.getRel())
                                .ifPresent(nestedNodeForRel -> {
                                    resolveEmbeddedTypeInfoForRel(nestedTypeInfo, embedded, nestedNodeForRel);
                                });
                    });
                    embeddedValues.add(embedded);
                }
            }
        } else {
            HalRepresentation embedded = JSON_MAPPER.convertValue(embeddedNodeForRel, typeInfo.getType());
            if (embedded != null) {
                embeddedValues.add(embedded);
            }
        }
        parent.withEmbedded(typeInfo.getRel(), embeddedValues);
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
    private Optional<JsonNode> findPossiblyCuriedEmbeddedNode(final HalRepresentation halRepresentation,
                                                              final JsonNode jsonNode,
                                                              final String rel) {
        final JsonNode embedded = jsonNode.get("_embedded");
        if (embedded != null) {
            final RelRegistry relRegistry = halRepresentation.getRelRegistry();
            final JsonNode curiedNode = embedded.get(relRegistry.resolve(rel));
            if (curiedNode == null) {
                return Optional.ofNullable(embedded.get(relRegistry.expand(rel)));
            } else {
                return Optional.of(curiedNode);
            }
        } else {
            return Optional.empty();
        }
    }
}
