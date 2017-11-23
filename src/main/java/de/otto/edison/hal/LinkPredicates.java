package de.otto.edison.hal;

import java.util.function.Predicate;

/**
 * Predicates used to select links matching some criteria.
 *
 * @since 1.0.0
 */
public final class LinkPredicates {

    private LinkPredicates() {}

    /**
     * Returns a Predicate that is matching every link.
     *
     * @return Predicate used to select links
     */
    public static Predicate<Link> always() {
        return link -> true;
    }

    /**
     * Returns a Predicate that is matching links having the specified type {@link Link#getType() type}
     *
     * @param type the expected media type of the link
     * @return Predicate used to select links
     */
    public static Predicate<Link> havingType(final String type) {
        return link -> type.equals(link.getType());
    }

    /**
     * Returns a Predicate that is matching links having the specified type {@link Link#getType() type}, or no type
     * at all.
     *
     * @param type the expected media type of the link
     * @return Predicate used to select links
     */
    public static Predicate<Link> optionallyHavingType(final String type) {
        return havingType(type).or(havingType(""));
    }

    /**
     * Returns a Predicate that is matching links having the specified profile {@link Link#getProfile() profile}
     *
     * @param profile the expected profile of the link
     * @return Predicate used to select links
     */
    public static Predicate<Link> havingProfile(final String profile) {
        return link -> profile.equals(link.getProfile());
    }

    /**
     * Returns a Predicate that is matching links having the specified profile {@link Link#getProfile() profile}, or
     * no profile at all.
     *
     * @param profile the expected profile of the link
     * @return Predicate used to select links
     */
    public static Predicate<Link> optionallyHavingProfile(final String profile) {
        return havingProfile(profile).or(havingProfile(""));
    }

    /**
     * Returns a Predicate that is matching links having the specified name {@link Link#getName() name}
     *
     * @param name the expected name of the link
     * @return Predicate used to select links
     */
    public static Predicate<Link> havingName(final String name) {
        return link -> name.equals(link.getName());
    }

    /**
     * Returns a Predicate that is matching links having the specified name {@link Link#getName() name}, or
     * no name at all.
     *
     * @param name the expected name of the link
     * @return Predicate used to select links
     */
    public static Predicate<Link> optionallyHavingName(final String name) {
        return havingName(name).or(havingName(""));
    }
}
