package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static java.lang.Boolean.TRUE;

/**
 *
 * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5"></a>
 * @since 0.1.0
 */
@JsonInclude(NON_ABSENT)
public class Link {

    @JsonIgnore
    private String rel;
    /**
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.1"></a>
     */
    @JsonProperty
    private String href;
    /**
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.2"></a>
     */
    @JsonProperty
    private Boolean templated;
    /**
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.3"></a>
     */
    @JsonProperty
    private String type;
    /**
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.8"></a>
     */
    @JsonProperty
    private String hreflang;
    /**
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.7"></a>
     */
    @JsonProperty
    private String title;
    /**
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.5"></a>
     */
    @JsonProperty
    private String name;
    /**
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.4"></a>
     */
    @JsonProperty
    private Boolean deprecation;
    /**
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.6"></a>
     */
    @JsonProperty
    private String profile;


    /**
     *
     * @since 0.1.0
     */
    private Link(final String rel, final String href) {
        this(rel, href, null, null, null, null, null, null, null);
    }

    /**
     *
     * @param rel
     * @param href
     * @param templated
     * @param type
     * @param hrefLang
     * @param title
     * @param name
     * @param profile
     * @param deprecation
     *
     * @since 0.1.0
     */
    private Link(final String rel,
                 final String href,
                 final Boolean templated,
                 final String type,
                 final String hrefLang,
                 final String title,
                 final String name,
                 final String profile,
                 final Boolean deprecation) {
        this.rel = rel;
        this.href = href;
        this.templated = templated;
        this.type = type;
        this.hreflang = hrefLang;
        this.title = title;
        this.name = name;
        this.profile = profile;
        this.deprecation = deprecation;
    }

    /**
     *
     * @param href
     * @return
     *
     * @since 0.1.0
     */
    public static Link self(final String href) {
        return new Link("self", href);
    }

    /**
     *
     * @param href
     * @return
     *
     * @since 0.1.0
     */
    public static Link profile(final String href) {
        return new Link("profile", href);
    }

    /**
     *
     * @param href
     * @return
     *
     * @since 0.1.0
     */
    public static Link item(final String href) {
        return new Link("item", href);
    }

    /**
     *
     * @param href
     * @return
     *
     * @since 0.1.0
     */
    public static Link collection(final String href) {
        return new Link("collection", href);
    }

    /**
     *
     * @param rel
     * @param href
     * @return
     *
     * @since 0.1.0
     */
    public static Link link(final String rel, final String href) {
        return new Link(rel, href);
    }

    /**
     *
     * @param rel
     * @param uriTemplate
     * @return
     *
     * @since 0.1.0
     */
    public static Link templated(final String rel, final String uriTemplate) {
        return new Link(rel, uriTemplate, TRUE, null, null, null, null, null, null);
    }

    /**
     * Create a Builder instance for templated links with mandatory link-relation type and uri template.
     *
     * @param rel  the link-relation type of the link
     * @param uriTemplate the URI template used to create the href of the linked resource
     * @return a Builder for a templated link.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6570">URI Template</a>
     * @since 0.1.0
     */
    public static Builder templatedBuilder(final String rel, final String uriTemplate) {
        return Builder.linkBuilderFor(rel, uriTemplate).beeingTemplated();
    }

    /**
     * Create a Builder instance with mandatory link-relation type and href
     *
     * @param rel  the link-relation type of the link
     * @param href the href of the linked resource
     * @return a Builder for a Link.
     *
     * @since 0.1.0
     */
    public static Builder linkBuilder(final String rel, final String href) {
        return Builder.linkBuilderFor(rel, href);
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
     *
     * @return href of the linked resource, or URI template, if the link is {@code templated}.
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.2"></a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getHref() {
        return href;
    }

    /**
     * Returns true, if the link is templated, false otherwise.
     *
     * @return boolean
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.2"></a>
     * @since 0.2.0
     */
    @JsonIgnore
    public boolean isTemplated() {
        return templated != null ? templated : false;
    }

    /**
     * Returns the type of the link, or an empty String if no type is specified.
     *
     * @return type
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.3"></a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getType() {
        return type != null ? type : "";
    }

    /**
     * Returns the hreflang of the link, or an empty String if no hreflang is specified.
     *
     * @return hreflang or empty string
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.8"></a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getHreflang() {
        return hreflang != null ? hreflang : "";
    }

    /**
     * Returns the title of the link, or an empty String if no title is specified.
     *
     * @return title or empty string
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.7"></a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getTitle() {
        return title != null ? title : "";
    }

    /**
     * Returns the name of the link, or an empty String if no name is specified.
     *
     * @return name or empty string
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.5"></a>
     * @since 0.2.0
     */
    @JsonIgnore
    public String getName() {
        return name != null ? name : "";
    }

    /**
     * Returns the profile of the link, or an empty String if no profile is specified.
     *
     * @return profile or empty string
     *
     * @since 0.2.0
     */
    @JsonIgnore
    public String getProfile() {
        return profile != null ? profile : "";
    }

    /**
     * Returns whether or not the link is deprecation.
     *
     * @return boolean
     *
     * @see <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08#section-5.4"></a>
     * @since 0.2.0
     */
    @JsonIgnore
    public boolean getDeprecation() {
        return deprecation != null ? deprecation : false;
    }

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
        private final String rel;
        private final String href;
        private String type;
        private String hrefLang;
        private String title;
        private String name;
        private String profile;
        private Boolean deprecated;
        private Boolean templated;

        /**
         * Create a Builder instance with mandatory link-relation type and href
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
         * Create a Builder instance with mandatory link-relation type and href
         *
         * @param rel  the link-relation type of the link
         * @param href the href of the linked resource
         *
         * @since 0.1.0
         */
        public static Builder linkBuilderFor(final String rel, final String href) {
            return new Builder(rel, href);
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
         *
         * @since 0.1.0
         */
        public Builder withHrefLang(final String hrefLang) {
            this.hrefLang = hrefLang;
            return this;
        }

        /**
         * Set the title attribute
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
         * Set deprecation attribute to true.
         *
         * @return this
         *
         * @since 0.1.0
         */
        public Builder beeingDeprecated() {
            this.deprecated = TRUE;
            return this;
        }

        /**
         * Set templated attribute to true.
         *
         * @return this
         *
         * @since 0.1.0
         */
        public Builder beeingTemplated() {
            this.templated = TRUE;
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
            return new Link(rel, href, templated, type, hrefLang, title, name, profile, deprecated);
        }
    }
}
