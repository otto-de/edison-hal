package de.otto.edison.hal;

import java.util.function.Predicate;

/**
 * Predicates used to select links matching some criteria.
 */
public final class LinkPredicates {

    private LinkPredicates() {}

    public static Predicate<Link> havingType(final String type) {
        return link -> type.equals(link.getType());
    }

    public static Predicate<Link> havingProfile(final String profile) {
        return link -> profile.equals(link.getProfile());
    }

    public static Predicate<Link> havingName(final String name) {
        return link -> name.equals(link.getName());
    }
}
