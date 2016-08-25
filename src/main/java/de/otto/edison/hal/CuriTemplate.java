package de.otto.edison.hal;

import java.util.List;
import java.util.Optional;

/**
 * A utility class used to handle custom link-relation type URIs and CURI templates.
 *
 * Example usage:
 * <pre><code>
 *     Link curi = Link.curi("x", "http://example.org/rels/{rel}");
 *     String rel = "http://example.org/rels/product";
 *
 *     curiTemplateFor(curi).matches(rel) => true
 *
 *     curiTemplateFor(curi).curiedRelFrom(rel) => "x:product"
 *
 *     curiTemplateFor(curi).relPlaceHolderFrom(rel) => "product"
 *
 * </code></pre>
 * @since 0.3.0
 */
public class CuriTemplate {

    private static final String REL_PLACEHOLDER = "{rel}";

    private final String relPrefix;
    private final String relSuffix;
    private final Link curi;

    private CuriTemplate(final Link curi) {
        if (!curi.getRel().equals("curies")) {
            throw new IllegalArgumentException("Parameter is not a CURI link.");
        }
        if (!curi.getHref().contains(REL_PLACEHOLDER)) {
            throw new IllegalArgumentException("Href of the CURI does not contain the required {rel} placeholder.");
        }
        final String curiHref = curi.getHref();
        relPrefix = curi.getHref().substring(0, curiHref.indexOf(REL_PLACEHOLDER));
        relSuffix = curi.getHref().substring(curiHref.indexOf(REL_PLACEHOLDER) + REL_PLACEHOLDER.length(), curiHref.length());
        this.curi = curi;
    }

    public static CuriTemplate curiTemplateFor(final Link curi) {
        return new CuriTemplate(curi);
    }

    /**
     * Returns a CuriTemplate that is {@link #matches matching} the rel parameter, or empty if no matching CURI is found.
     *
     * @param curies a List of curies Links
     * @param rel the link-relation type to check against the curies
     * @return optional CuriTemplate
     */
    public static Optional<CuriTemplate> matchingCuriTemplateFor(final List<Link> curies, final String rel) {
        return curies.stream().map(CuriTemplate::curiTemplateFor).filter(t->t.matches(rel)).findAny();
    }

    /**
     * Returns true, if the given link-relation type is matching the CuriTemplate pattern, false if not.
     *
     * @param rel a link-relation type.
     * @return boolean
     */
    public boolean matches(final String rel) {
        return rel.startsWith(relPrefix) && rel.endsWith(relSuffix);
    }

    public String relPlaceHolderFrom(final String rel) {
        if (matches(rel)) {
            return rel.substring(relPrefix.length(), rel.length() - relSuffix.length());
        } else {
            throw new IllegalArgumentException("Rel is not matching the CURI template.");
        }

    }

    public String curiedRelFrom(final String rel) {
        if (matches(rel)) {
            return curi.getName() + ":" + relPlaceHolderFrom(rel);
        } else {
            throw new IllegalArgumentException("Rel is not matching the CURI template.");
        }
    }
}
