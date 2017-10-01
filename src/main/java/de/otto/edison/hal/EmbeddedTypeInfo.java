package de.otto.edison.hal;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * Type information for embedded items. This is required if more complex embedded items should be parsed
 * into sub classes of HalRepresentation.
 *
 * @since 0.1.0
 */
public class EmbeddedTypeInfo {

    private final String rel;

    private final Class<? extends HalRepresentation> type;

    /** Creates a new EmbeddedTypeInfo by link-relation type and Java class of embedded items. */
    private EmbeddedTypeInfo(final String rel, final Class<? extends HalRepresentation> type) {
        this.rel = rel;
        this.type = type;
    }

    public static EmbeddedTypeInfo withEmbedded(final String rel,
                                                      final Class<? extends HalRepresentation> embeddedType) {
        return new EmbeddedTypeInfo(rel, embeddedType);
    }

    /**
     * @return The link-relation type used to identify items of the embedded type.
     */
    public String getRel() {
        return rel;
    }

    /**
     * @return The Java class used to deserialize the embedded items for the link-relation type
     */
    public Class<? extends HalRepresentation> getType() {
        return type;
    }
}
