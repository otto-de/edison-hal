package de.otto.edison.hal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.otto.edison.hal.CuriTemplate.matchingCuriTemplateFor;

/**
 * Helper class used to resolve CURIs in links and embedded items.
 */
public class RelRegistry {

    /**
     * The list of registered CURI links
     */
    private final List<Link> curies;

    private RelRegistry() {
        this.curies = new ArrayList<>();
    }

    /**
     * Creates a RelRegistry from a list of CURI links.
     *
     * @param curies list of {@link Link links} with link-relation type 'curies'.
     * @throws IllegalArgumentException if the list contains non-CURI links.
     * @since 1.0.0
     */
    private RelRegistry(final List<Link> curies) {
        this.curies = new ArrayList<>();
        curies.forEach(this::register);
    }

    /**
     * Creates a clone from another RelRegistry instance.
     *
     * @param other cloned RelRegistry
     */
    private RelRegistry(final RelRegistry other) {
        this.curies = new ArrayList<>(other.curies);
    }

    /**
     * Creates an empty RelRegistry without curi links.
     *
     * @return default RelRegistry
     */
    public static RelRegistry emptyRelRegistry() {
        return new RelRegistry();
    }

    /**
     * Creates RelRegistry from some {@link Links}. CURIes contained in the Links are
     * {@link #register(Link) registered}.
     *
     * @param links Links possibly containing CURIes
     * @return RelRegistry
     */
    public static RelRegistry relRegistry(final Links links) {
        List<Link> curies = links.getLinksBy("curies");
        return relRegistry(curies);
    }

    /**
     * Creates RelRegistry from a list of CURI links.
     *
     * @param curies list of {@link Link links} with link-relation type 'curies'.
     * @return RelRegistry
     * @throws IllegalArgumentException if the list contains non-CURI links.
     */
    public static RelRegistry relRegistry(final List<Link> curies) {
        return new RelRegistry(curies);
    }

    /**
     * Returns a copy of another RelRegistry instance.
     *
     * @param registry copied RelRegistry
     * @return copied LinkReRelRegistrylations
     */
    public static RelRegistry copyOf(final RelRegistry registry) {
        return new RelRegistry(registry);
    }

    /**
     * Registers a CURI link in the RelRegistry instance.
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
     * Merges this RelRegistry with another instance of RelRegistry and returns the merged instance.
     *
     * @param other merged RelRegistry
     * @return a merged copy of this and other
     */
    public RelRegistry mergeWith(final RelRegistry other) {
        final RelRegistry merged = copyOf(this);
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

    public String expand(final String rel) {
        if (rel.contains(":")) {
            final String name = rel.substring(0, rel.indexOf(":"));
            Optional<Link> curi = curies.stream().filter(c -> c.getName().equals(name)).findAny();
            return curi.map(c -> c.getHrefAsTemplate()
                    .set("rel", rel.substring(rel.indexOf(":")+1))
                    .expand()).orElse(rel);
        } else {
            return rel;
        }
    }

    public List<Link> getCuries() {
        return this.curies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelRegistry that = (RelRegistry) o;
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
