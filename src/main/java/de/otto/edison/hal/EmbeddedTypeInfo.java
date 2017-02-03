package de.otto.edison.hal;

/**
 * Type information for embedded items. This is required if more complex embedded items should be parsed
 * into sub classes of HalRepresentation.
 *
 * @since 0.1.0
 */
public class EmbeddedTypeInfo {

    /** The link-relation type used to identify items of the embedded type. */
    public final String rel;

    /** The Java class used to deserialize the embedded items for the link-relation type */
    public final Class<? extends HalRepresentation> type;

    /** Creates a new EmbeddedTypeInfo by link-relation type and Java class of embedded items. */
    private EmbeddedTypeInfo(final String rel, final Class<? extends HalRepresentation> type) {
        this.rel = rel;
        this.type = type;
    }

    public static <E extends HalRepresentation> EmbeddedTypeInfo withEmbedded(final String rel, final Class<E> embeddedType) {
        return new EmbeddedTypeInfo(rel, embeddedType);
    }

}
