package de.otto.edison.hal;

import java.util.*;

import static de.otto.edison.hal.CuriTemplate.matchingCuriTemplateFor;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;

/**
 * Helper class used to resolve CURIs in links and embedded items.
 */
public class RelRegistry {

    public static final Set<String> DEFAULT_ARRAY_LINK_RELATIONS = unmodifiableSet(new HashSet<String>() {{
        add("curies");
        add("item");
        add("items");
    }});

    /**
     * The list of registered CURI links
     */
    private final List<Link> curies;
    /**
     * The set of link-relation types that is always rendered as an array, even if there is only a single link.
     */
    private volatile Set<String> arrayRels;

    private RelRegistry() {
        this.curies = new ArrayList<>();
        this.arrayRels = DEFAULT_ARRAY_LINK_RELATIONS;
    }

    /**
     * Creates a RelRegistry from a list of CURI links and a collection of link-relation types that
     * should be rendered as an array of links.
     *
     * @param curies list of {@link Link links} with link-relation type 'curies'.
     * @param arrayRels link-relation types that should be rendered as an array of links.
     * @throws IllegalArgumentException if the list contains non-CURI links.
     * @since 1.0.0
     */
    private RelRegistry(final List<Link> curies, final Collection<String> arrayRels) {
        this.arrayRels = new LinkedHashSet<>(arrayRels);
        this.arrayRels.add("curies");
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
        this.arrayRels = new LinkedHashSet<>(other.arrayRels);
    }

    /**
     * Creates default RelRegistry with no curi links but 'curies', 'item' and 'items' registered as
     * {@link #getArrayRels() array rels}
     *
     * @return default RelRegistry
     */
    public static RelRegistry defaultRelRegistry() {
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
     * Creates RelRegistry from some {@link Links}. CURIes contained in the Links are
     * {@link #register(Link) registered}.
     *
     * @param links Links possibly containing CURIes
     * @param arrayRels link-relation types that should be rendered as an array of links.
     * @return RelRegistry
     */
    public static RelRegistry relRegistry(final Links links,
                                          final Collection<String> arrayRels) {
        final List<Link> curies = links.getLinksBy("curies");
        return relRegistry(curies, arrayRels);
    }

    /**
     * Creates RelRegistry with no curies, but a collection of link-relation types that
     * should be rendered as an array of links.
     *
     * @param arrayRels link-relation types that should be rendered as an array of links.
     * @return RelRegistry
     */
    public static RelRegistry relRegistry(final Collection<String> arrayRels) {
        return new RelRegistry(emptyList(), arrayRels);
    }

    /**
     * Creates RelRegistry from a list of CURI links.
     *
     * @param curies list of {@link Link links} with link-relation type 'curies'.
     * @return RelRegistry
     * @throws IllegalArgumentException if the list contains non-CURI links.
     */
    public static RelRegistry relRegistry(final List<Link> curies) {
        return new RelRegistry(curies, DEFAULT_ARRAY_LINK_RELATIONS);
    }

    /**
     * Creates RelRegistry from a list of CURI links and a collection of link-relation types that
     * should be rendered as an array of links.
     *
     * @param curies list of {@link Link links} with link-relation type 'curies'.
     * @param arrayRels link-relation types that should be rendered as an array of links.
     * @return RelRegistry
     * @throws IllegalArgumentException if the list contains non-CURI links.
     */
    public static RelRegistry relRegistry(final List<Link> curies,
                                          final Collection<String> arrayRels) {
        return new RelRegistry(curies, arrayRels);
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
        arrayRels = arrayRels.stream().map(this::resolve).collect(toSet());
    }

    /**
     * Returns true if the given link-relation type is configured to be rendered as an array, false if not.
     *
     * @param rel the link-relation type in curied or expanded format
     * @return boolean
     */
    public boolean isArrayRel(final String rel) {
        return arrayRels.contains(resolve(rel));
    }

    /**
     * Returns the set of link-relation types (in curied format) that is configured to be rendered as an array of links.
     *
     * @return set of link-relation types
     */
    public Set<String> getArrayRels() {
        return arrayRels;
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
        return Objects.equals(curies, that.curies) &&
                Objects.equals(arrayRels, that.arrayRels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(curies, arrayRels);
    }

    @Override
    public String toString() {
        return "RelRegistry{" +
                "curies=" + curies +
                ", arrayRels=" + arrayRels +
                '}';
    }
}
