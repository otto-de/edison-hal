package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static de.otto.edison.hal.Embedded.Builder.copyOf;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Links.copyOf;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linksBuilder;

/**
 * Representation used to parse and create HAL+JSON documents from Java classes.
 *
 * @see <a href="http://stateless.co/hal_specification.html">hal_specification.html</a>
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08">draft-kelly-json-hal-08</a>
 *
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class HalRepresentation {

    @JsonProperty(value = "_links")
    private volatile Links links;
    @JsonProperty(value = "_embedded")
    private volatile Embedded embedded;
    @JsonAnySetter
    public Map<String,JsonNode> attributes = new LinkedHashMap<>();

    /**
     *
     * @since 0.1.0
     */
    public HalRepresentation() {
        this(null, null);
    }

    /**
     * Creates a HalRepresentation having {@link Links}
     *
     * @param links the Links of the HalRepresentation
     * @since 0.1.0
     */
    public HalRepresentation(final Links links) {
        this(links, null);
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
        this.links = links == null || links.isEmpty()
                ? null
                : links;
        this.embedded = embedded == null || embedded.isEmpty()
                ? null
                : embedded.using(getLinks().getRelRegistry());
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
     * @return this
     */
    protected HalRepresentation withLinks(final Link link, final Link... moreLinks) {
        this.links = this.links != null
                ? copyOf(this.links).with(link, moreLinks).build()
                : linksBuilder().with(link, moreLinks).build();
        if (embedded != null) {
            embedded = embedded.using(this.links.getRelRegistry());
        }
        return this;
    }

    /**
     * Add links to the HalRepresentation.
     * <p>
     *     Links are only added if they are not {@link Link#isEquivalentTo(Link) equivalent}
     *     to already existing links.
     * </p>
     * @param links added links
     * @return this
     */
    protected HalRepresentation withLinks(final List<Link> links) {
        this.links = this.links != null
                ? copyOf(this.links).with(links).build()
                : linksBuilder().with(links).build();
        if (embedded != null) {
            embedded = embedded.using(this.links.getRelRegistry());
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
        embedded = copyOf(embedded).with(rel, embeddedItems).using(getLinks().getRelRegistry()).build();
        return this;
    }

    protected HalRepresentation using(final RelRegistry relRegistry) {
        if (links != null) {
            links = links.using(relRegistry);
            if (embedded != null) {
                embedded = embedded.using(links.getRelRegistry());
            }
        } else {
            if (embedded != null) {
                embedded = embedded.using(relRegistry);
            }
        }
        return this;
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
