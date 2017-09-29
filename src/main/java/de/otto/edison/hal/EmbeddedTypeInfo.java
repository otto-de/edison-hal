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

    public static List<EmbeddedTypeInfo> withEmbedded(final String rel1,
                                                      final Class<? extends HalRepresentation> embeddedType1,
                                                      final String rel2,
                                                      final Class<? extends HalRepresentation> embeddedType2) {
        return asList(
                new EmbeddedTypeInfo(rel1, embeddedType1),
                new EmbeddedTypeInfo(rel2, embeddedType2)
        );
    }

    public static List<EmbeddedTypeInfo> withEmbedded(final String rel1,
                                                      final Class<? extends HalRepresentation> embeddedType1,
                                                      final String rel2,
                                                      final Class<? extends HalRepresentation> embeddedType2,
                                                      final String rel3,
                                                      final Class<? extends HalRepresentation> embeddedType3) {
        return asList(
                new EmbeddedTypeInfo(rel1, embeddedType1),
                new EmbeddedTypeInfo(rel2, embeddedType2),
                new EmbeddedTypeInfo(rel3, embeddedType3)
        );
    }

    public static List<EmbeddedTypeInfo> withEmbedded(final String rel1,
                                                      final Class<? extends HalRepresentation> embeddedType1,
                                                      final String rel2,
                                                      final Class<? extends HalRepresentation> embeddedType2,
                                                      final String rel3,
                                                      final Class<? extends HalRepresentation> embeddedType3,
                                                      final String rel4,
                                                      final Class<? extends HalRepresentation> embeddedType4) {
        return asList(
                new EmbeddedTypeInfo(rel1, embeddedType1),
                new EmbeddedTypeInfo(rel2, embeddedType2),
                new EmbeddedTypeInfo(rel3, embeddedType3),
                new EmbeddedTypeInfo(rel4, embeddedType4)
        );
    }

    public static List<EmbeddedTypeInfo> withEmbedded(final String rel1,
                                                      final Class<? extends HalRepresentation> embeddedType1,
                                                      final String rel2,
                                                      final Class<? extends HalRepresentation> embeddedType2,
                                                      final String rel3,
                                                      final Class<? extends HalRepresentation> embeddedType3,
                                                      final String rel4,
                                                      final Class<? extends HalRepresentation> embeddedType4,
                                                      final String rel5,
                                                      final Class<? extends HalRepresentation> embeddedType5) {
        return asList(
                new EmbeddedTypeInfo(rel1, embeddedType1),
                new EmbeddedTypeInfo(rel2, embeddedType2),
                new EmbeddedTypeInfo(rel3, embeddedType3),
                new EmbeddedTypeInfo(rel4, embeddedType4),
                new EmbeddedTypeInfo(rel5, embeddedType5)
        );
    }

    public static List<EmbeddedTypeInfo> withEmbedded(final Map<String,Class<? extends HalRepresentation>> typeInfos) {
        return typeInfos
                .entrySet()
                .stream()
                .map(entry -> new EmbeddedTypeInfo(entry.getKey(), entry.getValue()))
                .collect(toList());
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
