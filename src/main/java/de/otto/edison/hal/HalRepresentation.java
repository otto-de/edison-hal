package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static de.otto.edison.hal.Curies.emptyCuries;
import static de.otto.edison.hal.Embedded.Builder.copyOf;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Links.copyOf;
import static de.otto.edison.hal.Links.emptyLinks;

/**
 * Representation used to parse and create HAL+JSON documents from Java classes.
 *
 * @see <a href="http://stateless.co/hal_specification.html">hal_specification.html</a>
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08">draft-kelly-json-hal-08</a>
 *
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HalRepresentation {

    @JsonProperty(value = "_links")
    @JsonInclude(NON_NULL)
    private volatile Links links;
    @JsonProperty(value = "_embedded")
    @JsonInclude(NON_NULL)
    private volatile Embedded embedded;
    @JsonAnySetter
    private Map<String,JsonNode> attributes = new LinkedHashMap<>();
    @JsonIgnore
    private volatile Curies curies;

    /**
     *
     * @since 0.1.0
     */
    public HalRepresentation() {
        this(null, null, emptyCuries());
    }

    /**
     * Creates a HalRepresentation having {@link Links}
     *
     * @param links the Links of the HalRepresentation
     * @since 0.1.0
     */
    public HalRepresentation(final Links links) {
        this(links, null, emptyCuries());
    }

    /**
     * Creates a HalRepresentation having {@link Links} and a Curies that can be used
     * to configure the link-relation types that should always be rendered as an array of links.
     *
     * @param links the Links of the HalRepresentation
     * @param curies the Curies used to resolve curies
     * @since 1.0.0
     * @deprecated This method will most likely not be required by any users of edison-hal. Please contact me,
     * if you need this, otherwise the constructor will be removed in 3.0.0
     */
    @Deprecated
    public HalRepresentation(final Links links, final Curies curies) {
        this(links, null, curies);
    }

    /**
     * <p>
     *     Creates a HalRepresentation with {@link Links} and {@link Embedded} objects.
     * </p>
     * <p>
     *     If the Links do contain CURIs, the link-relation types of the embedded objects are shortened.
     * </p>
     *
     * @param links the Links of the HalRepresentation
     * @param embedded the Embedded items of the HalRepresentation
     * @since 0.1.0
     */
    @JsonCreator
    public HalRepresentation(final @JsonProperty("_links") Links links,
                             final @JsonProperty("_embedded") Embedded embedded) {
        this.curies = emptyCuries();
        this.links = links == null || links.isEmpty()
                ? null
                : links.using(this.curies);
        this.embedded = embedded == null || embedded.isEmpty()
                ? null
                : embedded.using(curies);
    }

    /**
     * <p>
     *     Creates a HalRepresentation with {@link Links}, {@link Embedded} objects and a Curies used to
     *     resolve curies from parent representations.
     * </p>
     * <p>
     *     If the Links do contain CURIs, the matching link-relation types of links and embedded objects are shortened.
     * </p>
     *
     * @param links the Links of the HalRepresentation
     * @param embedded the Embedded items of the HalRepresentation
     * @param curies the Curies used to resolve curies
     * @since 1.0.0
     * @deprecated This method will most likely not be required by any users of edison-hal. Please contact me,
     * if you need this, otherwise the constructor will be removed in 3.0.0
     */
    @Deprecated
    public HalRepresentation(final Links links,
                             final Embedded embedded,
                             final Curies curies) {
        this.curies = curies;
        this.links = links == null || links.isEmpty()
                ? null
                : links.using(this.curies);
        this.embedded = embedded == null || embedded.isEmpty()
                ? null
                : embedded.using(this.curies);
    }

    /**
     *
     * @return the Curies used by this HalRepresentation.
     */
    Curies getCuries() {
        return curies;
    }

    /**
     * Returns the Links of the HalRepresentation.
     *
     * @return Links
     * @since 0.1.0
     */
    @JsonIgnore
    public Links getLinks() {
        return links != null ? links : emptyLinks();
    }

    /**
     * Add links to the HalRepresentation.
     * <p>
     *     Links are only added if they are not {@link Link#isEquivalentTo(Link) equivalent}
     *     to already existing links.
     * </p>
     * @param links links that are added to this HalRepresentation
     * @return this
     */
    protected HalRepresentation add(final Links links) {
        this.links = this.links != null
                ? copyOf(this.links).with(links).build()
                : links.using(this.curies);
        if (embedded != null) {
            embedded = embedded.using(this.curies);
        }
        return this;
    }

    /**
     * Returns the Embedded objects of the HalRepresentation.
     *
     * @return Embedded, possibly beeing {@link Embedded#isEmpty() empty}
     */
    @JsonIgnore
    public Embedded getEmbedded() {
        return embedded != null ? embedded : emptyEmbedded();
    }

    /**
     * Returns extra attributes that were not mapped to properties of the HalRepresentation.
     *
     * @return map containing unmapped attributes
     */
    @JsonIgnore
    public Map<String, JsonNode> getAttributes() {
        return attributes;
    }

    /**
     * Returns the value of an extra attribute as a JsonNode, or null if no such attribute is present.
     *
     * @param name the name of the attribute
     * @return JsonNode or null
     */
    @JsonIgnore
    public JsonNode getAttribute(final String name) {
        return attributes.get(name);
    }

    /**
     * Adds embedded items for a link-relation type to the HalRepresentation.
     * <p>
     *     If {@code rel} is already present, it is replaced by the new embedded items.
     * </p>
     *
     * @param rel the link-relation type of the embedded items that are added or replaced
     * @param embeddedItems the new values for the specified link-relation type
     * @return this
     * @since 0.5.0
     */
    protected HalRepresentation withEmbedded(final String rel, final List<? extends HalRepresentation> embeddedItems) {
        embedded = copyOf(embedded).with(rel, embeddedItems).using(curies).build();
        return this;
    }

    /**
     * Adds an embedded item for a link-relation type to the HalRepresentation.
     * <p>
     *     The embedded item will be rendered as a single resource object.
     * </p>
     * <p>
     *     If {@code rel} is already present, it is replaced by the new embedded items.
     * </p>
     *
     * @param rel the link-relation type of the embedded item that is added or replaced
     * @param embeddedItem the new value for the specified link-relation type
     * @return this
     *
     * @since 2.0.0
     */
    protected HalRepresentation withEmbedded(final String rel, final HalRepresentation embeddedItem) {
        embedded = copyOf(embedded).with(rel, embeddedItem).using(curies).build();
        return this;
    }

    /**
     * Merges the Curies of an embedded resource with the Curies of this resource and updates
     * link-relation types in _links and _embedded items.
     *
     * @param curies the Curies of the embedding resource
     * @return this
     */
    HalRepresentation mergeWithEmbedding(final Curies curies) {
        this.curies = this.curies.mergeWith(curies);
        if (this.links != null) {

            removeDuplicateCuriesFromEmbedding(curies);

            this.links = this.links.using(this.curies);
            if (embedded != null) {
                embedded = embedded.using(this.curies);
            }
        } else {
            if (embedded != null) {
                embedded = embedded.using(curies);
            }
        }
        return this;
    }

    private void removeDuplicateCuriesFromEmbedding(final Curies curies) {
        if (this.links.hasLink("curies")) {
            final List<Link> curiLinks = new ArrayList<>(this.links.getLinksBy("curies"));
            curies.getCuries().forEach(curi -> {
                curiLinks.removeIf((link -> link.isEquivalentTo(curi)));
            });
            this.links = copyOf(this.links).replace("curies", curiLinks).build();
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

        HalRepresentation that = (HalRepresentation) o;

        if (links != null ? !links.equals(that.links) : that.links != null) return false;
        return embedded != null ? embedded.equals(that.embedded) : that.embedded == null;

    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public int hashCode() {
        int result = links != null ? links.hashCode() : 0;
        result = 31 * result + (embedded != null ? embedded.hashCode() : 0);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public String toString() {
        return "HalRepresentation{" +
                "links=" + links +
                ", embedded=" + embedded +
                '}';
    }
}
