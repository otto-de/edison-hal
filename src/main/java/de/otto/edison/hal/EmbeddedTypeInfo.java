package de.otto.edison.hal;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Type information for embedded items. This is required if more complex embedded items should be parsed
 * into sub classes of HalRepresentation.
 *
 * @since 0.1.0
 */
public class EmbeddedTypeInfo {

    private final String rel;

    private final Class<? extends HalRepresentation> type;

    private final List<EmbeddedTypeInfo> nestedTypeInfo;

    /** Creates a new EmbeddedTypeInfo by link-relation type and Java class of embedded items. */
    private EmbeddedTypeInfo(final String rel,
                             final Class<? extends HalRepresentation> type,
                             final List<EmbeddedTypeInfo> nestedTypeInfo) {
        this.rel = rel;
        this.type = type;
        this.nestedTypeInfo = nestedTypeInfo;
    }

    public static EmbeddedTypeInfo withEmbedded(final String rel,
                                                final Class<? extends HalRepresentation> embeddedType,
                                                final EmbeddedTypeInfo... nestedTypeInfo) {
        if (nestedTypeInfo == null) {
            return new EmbeddedTypeInfo(rel, embeddedType, emptyList());
        } else {
            return new EmbeddedTypeInfo(rel, embeddedType, asList(nestedTypeInfo));
        }
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

    public List<EmbeddedTypeInfo> getNestedTypeInfo() {
        return nestedTypeInfo;
    }
}
