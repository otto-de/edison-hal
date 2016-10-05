package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static de.otto.edison.hal.Embedded.Builder.copyOf;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;

/**
 * Representation used to parse and create HAL+JSON documents from Java classes.
 *
 * @see <a href="http://stateless.co/hal_specification.html"></a>
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08"></a>
 *
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class HalRepresentation {

    public static final String CURIES = "curies";
    @JsonProperty(value = "_links")
    private volatile Links links;
    @JsonProperty(value = "_embedded")
    private volatile Embedded embedded;

    /**
     *
     * @since 0.1.0
     */
    public HalRepresentation() {
        this.links = null;
        embedded = null;
    }

    /**
     * Creates a HalRepresentation having {@link Links}
     *
     * @param links the Links of the HalRepresentation
     * @since 0.1.0
     */
    public HalRepresentation(final Links links) {
        this.links = links.isEmpty() ? null : links;
        embedded = null;
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
    public HalRepresentation(final Links links, final Embedded embedded) {
        this(links);
        this.embedded = embedded.isEmpty() ? null : embedded.withCuries(getLinks().getLinksBy(CURIES));
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
     * @param link a link
     * @param moreLinks optionally more links
     */
    protected void withLinks(final Link link, final Link... moreLinks) {
        this.links = this.links != null
                ? Links.copyOf(this.links).with(link, moreLinks).build()
                : linkingTo(link, moreLinks);
        updateCuriesInEmbeddedItems();
    }

    /**
     * Add links to the HalRepresentation.
     * <p>
     *     Links are only added if they are not {@link Link#isEquivalentTo(Link) equivalent}
     *     to already existing links.
     * </p>
     * @param links added links
     */
    protected void withLinks(final List<Link> links) {
        this.links = this.links != null
                ? Links.copyOf(this.links).with(links).build()
                : linkingTo(links);
        updateCuriesInEmbeddedItems();
    }

    /**
     * Returns the Embedded objects of the HalRepresentation.
     *
     * @return Embedded, possibly beeing {@link Embedded#isEmpty() empty}
     */
    @JsonIgnore
    public Embedded getEmbedded() {
        return embedded != null ? embedded.withCuries(getLinks().getLinksBy(CURIES)) : emptyEmbedded();
    }

    /**
     * Adds embedded items for a link-relation type to the HalRepresentation.
     * <p>
     *     If {@code rel} is already present, it is replaced by the new embedded items.
     * </p>
     *
     * @param rel the link-relation type of the embedded items that are added or replaced
     * @param embeddedItems the new values for the specified link-relation type
     *
     * @since 0.5.0
     */
    protected void withEmbedded(final String rel, final List<HalRepresentation> embeddedItems) {
        embedded = copyOf(embedded).with(rel, embeddedItems).build().withCuries(getLinks().getLinksBy(CURIES));
    }

    /**
     * This method is used by embedded HalRepresentations to get the CURIs from the embedding
     * representation so curied links can be resolved.
     * <p>
     *     Only to be used internally.
     * </p>
     *
     * @param curiesFromEmbedding the curies from the embedding representation.
     */
    void withParentCuries(final List<Link> curiesFromEmbedding) {
        if (links != null) {
            links.withParentCuries(curiesFromEmbedding);
        }
    }

    private void updateCuriesInEmbeddedItems() {
        if (embedded != null && this.links.getRels().contains(CURIES)) {
            embedded = embedded.withCuries(getLinks().getLinksBy(CURIES));
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
