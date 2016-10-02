package de.otto.edison.hal.paging;

import de.otto.edison.hal.Links;

/**
 * Link-relation types used in paged resources.
 *
 * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
 */
public enum PagingRel {
    /** Conveys an identifier for the link's context. */
    SELF,
    /** An IRI that refers to the furthest preceding resource in a series of resources. */
    FIRST,
    /** Indicates that the link's context is a part of a series, and that the previous in the series is the link target. */
    PREV,
    /** Indicates that the link's context is a part of a series, and that the next in the series is the link target. */
    NEXT,
    /** An IRI that refers to the furthest following resource in a series of resources. */
    LAST;

    /**
     * Returns a link-relation type in lower-case format so it is usable in 'rel' attributes of {@link Links}
     *
     * @return link-relation type conforming to IANA
     * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA link-relations</a>
     */
    public String toString() {
        return name().toLowerCase();
    }
}
