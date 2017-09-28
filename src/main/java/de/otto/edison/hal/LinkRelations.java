package de.otto.edison.hal;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static de.otto.edison.hal.CuriTemplate.matchingCuriTemplateFor;
import static java.util.stream.Collectors.toSet;

/**
 * Helper class used to resolve CURIs in links and embedded items.
 */
public class LinkRelations {

    /**
     * The list of registered CURI links
     */
    private final List<Link> curies;

    private LinkRelations() {
        this.curies = new ArrayList<>();
    }

    /**
     * Creates a clone from another LinkRelations instance.
     *
     * @param other cloned LinkRelations
     */
    private LinkRelations(final LinkRelations other) {
        this.curies = new ArrayList<>(other.curies);
    }

    /**
     * Creates an empty LinkRelations instance.
     *
     * @return empty LinkRelations
     */
    public static LinkRelations emptyLinkRelations() {
        return new LinkRelations();
    }

    /**
     * Creates LinkRelations from some {@link Links}. CURIes contained in the Links are
     * {@link #register(Link) registered}.
     *
     * @param links Links possibly containing CURIes
     * @return LinkRelations
     */
    public static LinkRelations linkRelations(final Links links) {
        if (links != null) {
            List<Link> curies = links.getLinksBy("curies");
            return linkRelations(curies);
        } else {
            return emptyLinkRelations();
        }
    }

    /**
     * Creates LinkRelations from a list of CURI links.
     *
     * @param curies list of {@link Link links} with link-relation type 'curies'.
     * @return LinkRelations
     * @throws IllegalArgumentException if the list contains non-CURI links.
     */
    public static LinkRelations linkRelations(final List<Link> curies) {
        final LinkRelations registry = new LinkRelations();
        curies.forEach(registry::register);
        return registry;
    }

    /**
     * Returns a copy of another LinkRelations instance.
     *
     * @param registry copied LinkRelations
     * @return copied LinkRelations
     */
    public static LinkRelations copyOf(final LinkRelations registry) {
        return new LinkRelations(registry);
    }

    /**
     * Registers a CURI link in the LinkRelations instance.
     *
     * @param curi the CURI
     * @throws IllegalArgumentException if the link-relation type of the link is not equal to 'curies'
     */
    public void register(final Link curi) {
        if (!curi.getRel().equals("curies")) {
            throw new IllegalArgumentException("Link must be a CURI");
        }

        final boolean alreadyRegistered = curies
                .stream()
                .anyMatch(link -> link.getHref().equals(curi.getHref()));
        if (alreadyRegistered) {
            curies.removeIf(link -> link.getName().equals(curi.getName()));
            curies.replaceAll(link -> link.getName().equals(curi.getName()) ? curi : link);
        }
        curies.add(curi);
    }

    /**
     * Merges this LinkRelations with curies from the Links parameter.
     *
     * @param links merged Links
     * @return merged LinkRelations
     */
    public LinkRelations mergeWith(final Links links) {
        final LinkRelations merged = copyOf(this);
        links.getLinksBy("curies").forEach(merged::register);
        return merged;
    }

    /**
     * Merges this LinkRelations with another instance of LinkRelations and returns the merged instance.
     *
     * @param other merged LinkRelations
     * @return a merged copy of this and other
     */
    public LinkRelations mergeWith(final LinkRelations other) {
        final LinkRelations merged = copyOf(this);
        other.curies.forEach(merged::register);
        return merged;
    }

    /**
     * Resolves a link-relation type (curied or full rel) and returns the curied form, or
     * the unchanged rel, if no matching CURI is registered.
     *
     * @param rel link-relation type
     * @return curied link-relation type
     */
    public String resolve(final String rel) {
        final Optional<CuriTemplate> curiTemplate = matchingCuriTemplateFor(curies, rel);
        return curiTemplate.map(t -> t.curiedRelFrom(rel)).orElse(rel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkRelations that = (LinkRelations) o;
        return Objects.equals(curies, that.curies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(curies);
    }

    @Override
    public String toString() {
        return "RelRegistry{" +
                "curies=" + curies +
                '}';
    }
}
