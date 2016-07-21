package de.otto.edison.hal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by guido on 16.07.16.
 */
public final class HalParser {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static class EmbeddedTypeInfo<T> {

        final String rel;

        final Class<T> type;
        EmbeddedTypeInfo(final String rel, final Class<T> type) {
            this.rel = rel;
            this.type = type;
        }

        public static <E extends HalRepresentation> EmbeddedTypeInfo<E> withEmbedded(final String rel, final Class<E> embeddedType) {
            return new EmbeddedTypeInfo<>(rel, embeddedType);
        }

    }

    private final String json;

    private HalParser(final String json) {
        this.json = json;
    }

    public static HalParser parse(final String json) {
        return new HalParser(json);
    }

    public <T extends HalRepresentation> T as(final Class<T> type) throws IOException {
        return JSON_MAPPER.readValue(json, type);
    }

    public <T extends HalRepresentation, E extends HalRepresentation> T as(final Class<T> type, final EmbeddedTypeInfo<E> typeInfo) throws IOException {
        final JsonNode jsonNode = JSON_MAPPER.readTree(json);
        final T representation = JSON_MAPPER.convertValue(jsonNode, type);

        final List<HalRepresentation> embeddedValues = new ArrayList<>();
        final JsonNode listOfHalRepresentations = jsonNode.at("/_embedded/" + typeInfo.rel);
        for (int i = 0; i < listOfHalRepresentations.size(); i++) {
            JsonNode embeddedRepresentation = listOfHalRepresentations.get(i);
            embeddedValues.add(JSON_MAPPER.convertValue(embeddedRepresentation, typeInfo.type));
        }
        representation.withEmbedded(typeInfo.rel, embeddedValues);
        return representation;
    }

}
