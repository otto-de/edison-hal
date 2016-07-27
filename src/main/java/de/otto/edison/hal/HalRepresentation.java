package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static de.otto.edison.hal.Embedded.Builder.copyOf;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Links.emptyLinks;

/**
 *
 *
 * @see <a href="http://stateless.co/hal_specification.html"></a>
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08"></a>
 *
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HalRepresentation {

    @JsonProperty(value = "_links")
    private Links links;
    @JsonProperty(value = "_embedded")
    private Embedded embedded;

    /**
     *
     * @since 0.1.0
     */
    public HalRepresentation() {
        this.links = null;
        embedded = null;
    }

    /**
     *
     * @since 0.1.0
     */
    public HalRepresentation(final Links links) {
        this.links = links;
        embedded = null;
    }

    /**
     *
     * @since 0.1.0
     */
    public HalRepresentation(final Links links, final Embedded embedded) {
        this.links = links;
        this.embedded = embedded;
    }

    /**
     *
     * @since 0.1.0
     */
    @JsonIgnore
    public Links getLinks() {
        return links != null ? links : emptyLinks();
    }

    @JsonIgnore
    public Embedded getEmbedded() {
        return embedded != null ? embedded : emptyEmbedded();
    }

    /**
     * This method is used by the HalParser to parse embedded items as a concrete sub-class of HalRepresentation
     * and replace these embedded items.
     *
     * @param rel the link-relation type of the embedded items that are replaced
     * @param embeddedValues the new values for the specified link-relation type
     *
     * @since 0.1.0
     */
    void withEmbedded(final String rel, final List<HalRepresentation> embeddedValues) {
        this.embedded = copyOf(this.embedded).with(rel, embeddedValues).build();
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
