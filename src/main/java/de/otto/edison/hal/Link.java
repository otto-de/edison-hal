package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static java.lang.Boolean.TRUE;

/**
 * A link to a REST resource.
 *
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5">draft-kelly-json-hal-08#section-5</a>
 * @since 0.1.0
 */
@JsonInclude(NON_ABSENT)
public class Link {

    @JsonIgnore
    private String rel;
    @JsonProperty
    private String href;
    @JsonProperty
    private Boolean templated;
    @JsonProperty
    private String type;
    @JsonProperty
    private String hreflang;
    @JsonProperty
    private String title;
    @JsonProperty
    private String name;
    @JsonProperty
    private String deprecation;
    @JsonProperty
    private String profile;

    /**
     * Create a link having only rel and href.
     * <p>
     *     If href is an URI template, the created link {@link #isTemplated() is templated} and {@link #templated}
     *     will be true.
     * </p>
     *
     * @since 0.1.0
     */
    private Link(final String rel, final String href) {
        this(rel, href, null, null, null, null, null, null);
    }

    /**
     * Create a link with all attributes.
     * <p>
     *     If href is an URI template, the created link {@link #isTemplated() is templated} and {@link #templated}
     *     will be true.
     * </p>
     *
     * @param rel mandatory link-relation type
     * @param href mandatory href or URI template
     * @param type optional media type
     * @param hrefLang optional href language
     * @param title optional human-readable title
     * @param name optional name
     * @param profile profile of the linked resource
     * @param deprecation information about whether or not the link is deprecated
     *
     * @since 0.1.0
     */
    private Link(final String rel,
                 final String href,
                 final String type,
                 final String hrefLang,
                 final String title,
                 final String name,
                 final String profile,
                 final String deprecation) {
        this.rel = rel;
        this.href = href;
        this.type = type;
        this.hreflang = hrefLang;
        this.title = title;
        this.name = name;
        this.profile = profile;
        this.deprecation = deprecation;
        if (fromTemplate(href).getVariables().length > 0) {
            this.templated = TRUE;
        }
    }

    /**
     * Create a 'self' link from a href.
     *
     * @param href href of the linked resource
     * @return Link
     *
     * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
     * @since 0.1.0
     */
    public static Link self(final String href) {
        return new Link("self", href);
    }

    /**
     * <p>
     *     Create a 'curies' link (compact URI) with name and a URI template for the link-relation type.
     * </p>
     * <p>
     *     Curies may be used for brevity for custom link-relation type URIs. Curiess are established within a HAL document
     *     via a set of Link Objects with the relation type "curies" on the root Resource Object.
     *     These links contain a URI template with the token 'rel', and are named via the "name" property.
     * </p>
     * <pre><code>
     *     {
     *       "_links": {
     *         "self": { "href": "/orders" },
     *         "curies": [{
     *           "name": "acme",
     *           "href": "http://docs.acme.com/relations/{rel}",
     *           "templated": true
     *         }],
     *         "acme:widgets": { "href": "/widgets" }
     *       }
     *     }
     * </code></pre>
     *
     * @param name the short name of the CURI
     * @param relTemplate the template used to build link-relation types. Must contain a {rel} placeholder
     * @return Link
     *
     * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-8.2">draft-kelly-json-hal-08#section-8.2</a>
     * @since 0.3.0
     */
    public static Link curi(final String name, final String relTemplate) {
        if (!relTemplate.contains("{rel}")) {
            throw new IllegalArgumentException("Not a CURI template. Template is required to contain a {rel} placeholder");
        }
        return new Link("curies", relTemplate, null, null, null, name, null, null);
    }

    /**
     * Create a 'profile' link from a href
     *
     * @param href the linked profile
     * @return Link
     *
     * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
     * @since 0.1.0
     */
    public static Link profile(final String href) {
        return new Link("profile", href);
    }

    /**
     * Create a 'item' link from a href.
     * <p>
     *     If href is an URI template, the created link {@link #isTemplated() is templated} and {@link #templated}
     *     will be true.
     * </p>
     *
     * @param href the linked item
     * @return Link
     *
     * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
     * @since 0.1.0
     */
    public static Link item(final String href) {
        return new Link("item", href);
    }

    /**
     * Create a 'collection' link from a href
     * <p>
     *     If href is an URI template, the created link {@link #isTemplated() is templated} and {@link #templated}
     *     will be true.
     * </p>
     *
     * @param href the linked collection
     * @return Link
     *
     * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
     * @since 0.1.0
     */
    public static Link collection(final String href) {
        return new Link("collection", href);
    }

    /**
     * Create a link from a link-relation type and href.
     * <p>
     *     If href is an URI template, the created link {@link #isTemplated() is templated} and {@link #templated}
     *     will be true.
     * </p>
     *
     * @param rel registered link-relation type, or URI identifying a custom link-relation type.
     * @param href href of the linked resource
     * @return Link
     *
     * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
     * @since 0.1.0
     */
    public static Link link(final String rel, final String href) {
        return new Link(rel, href);
    }

    /**
     * Create a Builder instance with mandatory link-relation type and href
     * <p>
     *     If href is an URI template, the link created by the Builder will be {@link #isTemplated() templated}.
     * </p>
     *
     * @param rel  the link-relation type of the link
     * @param href the href of the linked resource
     * @return a Builder for a Link.
     *
     * @since 0.1.0
     */
    public static Builder linkBuilder(final String rel, final String href) {
        return new Builder(rel, href);
    }

    /**
     * Create a Builder instance and initialize it from a prototype Link.
     *
     * @param prototype the prototype link
     * @return a Builder for a Link.
     *
     * @since 0.1.0
     */
    public static Builder copyOf(final Link prototype) {
        return new Builder(prototype.rel, prototype.href)
                .withType(prototype.type)
                .withProfile(prototype.profile)
                .withTitle(prototype.title)
                .withName(prototype.name)
                .withDeprecation(prototype.deprecation)
                .withHrefLang(prototype.hreflang);
    }

    /**
     * Returns the link-relation type of the link.
     *
     * @return link-relation type
     *
     * @since 0.2.0
     */
    @JsonIgnore
    public String getRel() {
        return rel;
    }

    /**
     * Returns the href of the link.
     * <p>
     * The "href" property is REQUIRED.
     * </p>
     * <p>
     * Its value is either a URI [RFC3986] or a URI Template [RFC6570].
     * </p>
     * <p>
     * If the value is a URI Template then the Link Object SHOULD have a
     * "templated" attribute whose value is true.
     * </p>
     *
     * @return href of the linked resource, or URI template, if the link is {@code templated}.
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.1">draft-kelly-json-hal-08#section-5.1</a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getHref() {
        return href;
    }

    /**
     * Returns true, if the link is templated, false otherwise.
     * <p>
     * The "templated" property is OPTIONAL.
     * </p>
     * <p>
     * Its value is boolean and SHOULD be true when the Link Object's "href"
     * property is a URI Template.
     * </p>
     * <p>
     * Its value SHOULD be considered false if it is undefined or any other
     * value than true.
     * </p>
     *
     * @return boolean
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.2">draft-kelly-json-hal-08#section-5.2</a>
     * @since 0.2.0
     */
    @JsonIgnore
    public boolean isTemplated() {
        return templated != null ? templated : false;
    }

    /**
     * Returns the type of the link, or an empty String if no type is specified.
     * <p>
     * The "type" property is OPTIONAL.
     * </p>
     * <p>
     * Its value is a string used as a hint to indicate the media type
     * expected when dereferencing the target resource.
     * </p>
     *
     * @return type
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.3">draft-kelly-json-hal-08#section-5.3</a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getType() {
        return type != null ? type : "";
    }

    /**
     * Returns the hreflang of the link, or an empty String if no hreflang is specified.
     * <p>
     * The "hreflang" property is OPTIONAL.
     * </p>
     * <p>
     * Its value is a string and is intended for indicating the language of
     * the target resource (as defined by [RFC5988]).
     * </p>
     *
     * @return hreflang or empty string
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.8">draft-kelly-json-hal-08#section-5.8</a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getHreflang() {
        return hreflang != null ? hreflang : "";
    }

    /**
     * Returns the title of the link, or an empty String if no title is specified.
     * <p>
     * The "title" property is OPTIONAL.
     * </p>
     * <p>
     * Its value is a string and is intended for labelling the link with a
     * human-readable identifier (as defined by [RFC5988]).
     * </p>
     *
     * @return title or empty string
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.7">draft-kelly-json-hal-08#section-5.7</a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getTitle() {
        return title != null ? title : "";
    }

    /**
     * Returns the name of the link, or an empty String if no name is specified.
     * <p>
     * The "name" property is OPTIONAL.
     * </p>
     * <p>
     * Its value MAY be used as a secondary key for selecting Link Objects
     * which share the same relation type.
     * </p>
     *
     * @return name or empty string
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.5">draft-kelly-json-hal-08#section-5.5</a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getName() {
        return name != null ? name : "";
    }

    /**
     * Returns the profile of the link, or an empty String if no profile is specified.
     * <p>
     * The "profile" property is OPTIONAL.
     * </p>
     * <p>
     * Its value is a string which is a URI that hints about the profile of the target resource.
     * </p>
     *
     * @return profile or empty string
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.6">draft-kelly-json-hal-08#section-5.6</a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getProfile() {
        return profile != null ? profile : "";
    }

    /**
     * Returns the deprecation information, or an empty string, if the link is not deprecated.
     * <p>
     * The "deprecation" property is OPTIONAL.
     * </p>
     * <p>
     * Its presence indicates that the link is to be deprecated (i.e.
     * removed) at a future date.  Its value is a URL that SHOULD provide
     * further information about the deprecation.
     * </p>
     * <p>
     * A client SHOULD provide some notification (for example, by logging a
     * warning message) whenever it traverses over a link that has this
     * property.  The notification SHOULD include the deprecation property's
     * value so that a client manitainer can easily find information about
     * the deprecation.
     * </p>
     * @return URL
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.4">draft-kelly-json-hal-08#section-5.4</a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getDeprecation() {
        return deprecation != null ? deprecation : "";
    }

    /**
     * Two links are considerered equivalent, if they have the same link-relation type and are pointing to the same resource
     * in the same representation.
     *
     * @param other other link
     * @return true if the links are equivalent, false otherwise.
     */
    public boolean isEquivalentTo(final Link other) {
        return
                getRel().equals(other.getRel()) &&
                        getHref().equals(other.getHref()) &&
                        getType().equals(other.getType()) &&
                        getProfile().equals(other.getProfile());
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

        Link link = (Link) o;

        if (rel != null ? !rel.equals(link.rel) : link.rel != null) return false;
        if (href != null ? !href.equals(link.href) : link.href != null) return false;
        if (templated != null ? !templated.equals(link.templated) : link.templated != null) return false;
        if (type != null ? !type.equals(link.type) : link.type != null) return false;
        if (hreflang != null ? !hreflang.equals(link.hreflang) : link.hreflang != null) return false;
        if (title != null ? !title.equals(link.title) : link.title != null) return false;
        if (name != null ? !name.equals(link.name) : link.name != null) return false;
        if (profile != null ? !profile.equals(link.profile) : link.profile != null) return false;
        return deprecation != null ? deprecation.equals(link.deprecation) : link.deprecation == null;

    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public int hashCode() {
        int result = rel != null ? rel.hashCode() : 0;
        result = 31 * result + (href != null ? href.hashCode() : 0);
        result = 31 * result + (templated != null ? templated.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (hreflang != null ? hreflang.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (profile != null ? profile.hashCode() : 0);
        result = 31 * result + (deprecation != null ? deprecation.hashCode() : 0);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     */
    @Override
    public String toString() {
        return "Link{" +
                "rel='" + getRel() + '\'' +
                ", href='" + getHref() + '\'' +
                ", templated=" + isTemplated() +
                ", type='" + getType() + '\'' +
                ", hreflang='" + getHreflang() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", name='" + getName() + '\'' +
                ", profile='" + getProfile() + '\'' +
                ", deprecation=" + getDeprecation() +
                '}';
    }

    /**
     * A Builder used to build complex links.
     *
     * @since 0.1.0
     */
    public static class Builder {
        private String rel;
        private String href;
        private String type;
        private String hrefLang;
        private String title;
        private String name;
        private String profile;
        private String deprecation;

        /**
         * Create a Builder instance with mandatory link-relation type and href
         * <p>
         *     If href is an URI template, the created link {@link #isTemplated() is templated} and {@link #templated}
         *     will be true.
         * </p>
         *
         * @param rel  the link-relation type of the link
         * @param href the href of the linked resource
         *
         * @since 0.1.0
         */
        private Builder(final String rel, final String href) {
            this.rel = rel;
            this.href = href;
        }

        /**
         * Set the rel of the linked resource
         *
         * @param rel link-relation type
         * @return this
         *
         * @since 0.4.0
         */
        public Builder withRel(final String rel) {
            if (rel == null || rel.isEmpty()) {
                throw new IllegalArgumentException("The link-relation type is mandatory");
            }
            this.rel = rel;
            return this;
        }

        /**
         * Set the href of the linked resource
         * <p>
         *     If href is an URI template, the created link {@link #isTemplated() is templated} and {@link #templated}
         *     will be true.
         * </p>
         *
         * @param href href
         * @return this
         *
         * @since 0.4.0
         */
        public Builder withHref(final String href) {
            if (rel == null || rel.isEmpty()) {
                throw new IllegalArgumentException("The href parameter is mandatory");
            }
            this.href = href;
            return this;
        }

        /**
         * Set the media type of the linked resource
         *
         * @param type media type
         * @return this
         *
         * @since 0.1.0
         */
        public Builder withType(final String type) {
            this.type = type;
            return this;
        }

        /**
         * Set the language of the linked resource
         *
         * @param hrefLang the hreflang of the Link
         * @return this
         * @since 0.1.0
         */
        public Builder withHrefLang(final String hrefLang) {
            this.hrefLang = hrefLang;
            return this;
        }

        /**
         * Set the title attribute
         *
         * @param title the title of the linked resource.
         * @return this
         *
         * @since 0.1.0
         */
        public Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the name attribute.
         *
         * @param name the name of the linked resource.
         * @return this
         *
         * @since 0.1.0
         */
        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the profile attribute
         *
         * @param profile the profile of the representation
         * @return this
         *
         * @since 0.1.0
         */
        public Builder withProfile(final String profile) {
            this.profile = profile;
            return this;
        }

        /**
         * <p>
         *     Set deprecation attribute.
         * </p>
         * <p>
         *     Its presence indicates that the link is to be deprecated (i.e.
         *     removed) at a future date.  Its value is a URL that SHOULD provide
         *     further information about the deprecation.
         * </p>
         *
         * @param deprecation URL pointing to further information
         * @return this
         * @since 0.1.0
         */
        public Builder withDeprecation(final String deprecation) {
            this.deprecation = deprecation;
            return this;
        }

        /**
         * Builds the Link instance.
         *
         * @return Link
         *
         * @since 0.1.0
         */
        public Link build() {
            return new Link(rel, href, type, hrefLang, title, name, profile, deprecation);
        }
    }
}
