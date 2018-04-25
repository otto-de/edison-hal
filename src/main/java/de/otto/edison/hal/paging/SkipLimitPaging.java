package de.otto.edison.hal.paging;

import com.damnhandy.uri.template.UriTemplate;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalInt;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.paging.PagingRel.*;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.max;

/**
 * A helper class used to create paging links for paged resources that are using page URIs with skip and limit paramters.
 * <p>
 *     By default, SkipLimitPaging is expecting an UriTemplate having the template variables
 *     'skip' and 'limit' to create links to 'self', 'first', 'next', 'prev' and 'last' pages.
 *     If you want to use different var names, you should derive from this class and override
 *     {@link #skipVar()} and/or {@link #limitVar()}.
 * </p>
 * <p>
 *     As specified in <a href="https://tools.ietf.org/html/draft-kelly-json-hal-06#section-4.1.1">Section 4.1.1</a>
 *     of the HAL specification, the {@code _links} object <em>"is an object whose property names are
 *     link relation types (as defined by [RFC5988]) and values are either a Link Object or an array
 *     of Link Objects"</em>.
 * </p>
 * <p>
 *     Paging links like 'first', 'next', and so on should generally be rendered as single Link Objects, so adding these
 *     links to an resource should be done using {@link Links.Builder#single(List)}.
 * </p>
 * <p>
 *     Usage:
 * </p>
 * <pre><code>
 * public class MyHalRepresentation extends HalRepresentation {
 *     public MyHalRepresentation(final SkipLimitPaging page, final List&lt;Stuff&gt; pagedStuff) {
 *          super(linkingTo()
 *              .single(page.links(
 *                      fromTemplate("http://example.com/api/stuff{?skip,limit}"),
 *                      EnumSet.allOf(PagingRel.class)))
 *              .array(pagedStuff
 *                      .stream()
 *                      .limit(page.limit)
 *                      .map(stuff -&gt; linkBuilder("item", stuff.href).withTitle(stuff.title).build())
 *                      .collect(toList()))
 *              .build()
 *          );
 *     }
 * }
 * </code></pre>
 */
public class SkipLimitPaging {

    /**
     * The default template-variable name used to identify the number of items to skip
     */
    public static final String SKIP_VAR = "skip";
    /**
     * The default template-variable name used to identify the number of items per page
     */
    public static final String LIMIT_VAR = "limit";

    /**
     * The number of items to skip.
     */
    private final int skip;
    /**
     * The number of items per page.
     */
    private final int limit;
    /**
     * There are more items beyond the current page - or not.
     */
    private final boolean hasMore;
    /**
     * Optional total number of available items. Used to calculate the number of items to skip for the last page.
     */
    private final OptionalInt total;

    /**
     * Creates a NumberedPage instance.
     *
     * @param skip the number of items to skip to come to this page.
     * @param limit the size of a page.
     * @param hasMore more items beyond this page?
     */
    protected SkipLimitPaging(final int skip, final int limit, final boolean hasMore) {
        if (skip < 0) {
            throw new IllegalArgumentException("Parameter 'skip' must not be less than zero");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Parameter 'limit' must be greater zero");
        }
        if (hasMore && limit == MAX_VALUE) {
            throw new IllegalArgumentException("Unable to calculate next page for unbounded page sizes.");
        }
        this.skip = skip;
        this.limit = limit;
        this.total = OptionalInt.empty();
        this.hasMore = hasMore;
    }

    /**
     * Creates a NumberedPage instance.
     *
     * @param skip the number of items to skip to come to this page.
     * @param limit the size of a page.
     * @param totalCount the total number of available items.
     */
    protected SkipLimitPaging(final int skip, final int limit, final int totalCount) {
        if (skip < 0) {
            throw new IllegalArgumentException("Parameter 'skip' must not be less than zero");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Parameter 'limit' must be greater zero");
        }
        if (totalCount < 0) {
            throw new IllegalArgumentException("Parameter 'totalCount' must be greater than or equal to zero");
        }
        if (totalCount < skip) {
            throw new IllegalArgumentException("Parameter 'totalCount' must be greater 'skip'");
        }
        this.skip = skip;
        this.limit = limit;
        this.total = OptionalInt.of(totalCount);
        this.hasMore = skip + limit < totalCount;
    }

    /**
     * Create a NumberedPaging instances for pages where it is known whether or not there are more
     * items beyond the current page.
     *
     * @param skip the number of items to skip before the current page
     * @param limit the number of items per page.
     * @param hasMore true if there are more items beyond the current page, false otherwise.
     * @return created SkipLimitPaging instance
     */
    public static SkipLimitPaging skipLimitPage(final int skip, final int limit, final boolean hasMore) {
        return new SkipLimitPaging(skip, limit, hasMore);
    }

    /**
     * Create a NumberedPaging instances for pages where it is known how many items are matching the initial query.
     *
     * @param skip the number of items to skip to come to this page.
     * @param limit the number of items per page.
     * @param totalCount the total number of items matching the initial query.
     * @return created SkipLimitPaging instance
     */
    public static SkipLimitPaging skipLimitPage(final int skip, final int limit, final int totalCount) {
        return new SkipLimitPaging(skip, limit, totalCount);
    }

    /**
     * Return the requested links for a paged resource were the link's hrefs are created using the given
     * {@link UriTemplate}.
     * <p>
     *     The variables used to identify the number of skipped items and page size must match the values returned
     *     by {@link #skipVar()} ()} and {@link #limitVar()}. Derive from this class, if other values than
     *     {@link #SKIP_VAR} or {@link #LIMIT_VAR} are required.
     * </p>
     * <p>
     *     If the provided template does not contain the required variable names. links can not be expanded.
     * </p>
     * @param pageUriTemplate the URI template used to create paging links.
     * @param rels the links expected to be created.
     * @return paging links
     */
    public final Links links(final UriTemplate pageUriTemplate, final EnumSet<PagingRel> rels) {
        final List<Link> links = new ArrayList<>();
        if (rels.contains(SELF)) {
            links.add(
                    self(pageUri(pageUriTemplate, skip, limit))
            );
        }
        if (rels.contains(FIRST)) {
            links.add(
                    link("first", pageUri(pageUriTemplate, 0, limit))
            );
        }
        if (skip > 0 && rels.contains(PREV)) {
            links.add(
                    link("prev", pageUri(pageUriTemplate, max(0, skip-limit), limit))
            );
        }
        if (hasMore && rels.contains(NEXT)) {
            links.add(
                    link("next", pageUri(pageUriTemplate, skip + limit, limit))
            );
        }
        if (total.isPresent() && rels.contains(LAST)) {
            final int skip = calcLastPageSkip(total.getAsInt(), this.skip, this.limit);
            links.add(
                    link("last", pageUri(pageUriTemplate, skip, limit))
            );
        }
        return linkingTo().single(links).build();
    }

    /**
     * Skipped number of items in the current selection.
     *
     * @return the number of skipped items
     */
    public int getSkip() {
        return skip;
    }

    /**
     * The limit for the current page size.
     *
     * @return number of items per page
     */
    public int getLimit() {
        return limit;
    }

    /**
     *
     * @return true if there are more pages, false otherwise.
     */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * Optional total number of items in the current selection.
     * <p>
     *     This number is used to calculate the link to the last page. If empty(), no
     *     last page is returned by {@link #links(UriTemplate, EnumSet)}
     * </p>
     * @return optional total number of items.
     */
    public OptionalInt getTotal() {
        return total;
    }

    /**
     * Return the name of the template variable used to specify the number of skipped items.
     * <p>
     *     Override this method if you want to use links with a different name than 'skip'
     *     for the number of skipped items.
     * </p>
     *
     * @return template variable for skipped items.
     */
    protected String skipVar() {
        return SKIP_VAR;
    }

    /**
     * Return the name of the template variable used to specify the size of the page.
     * <p>
     *     Override this methode if you want to use links with a different name than 'limit'
     *     for the number of items per page.
     * </p>
     *
     * @return template variable for the page size.
     */
    protected String limitVar() {
        return LIMIT_VAR;
    }

    /**
     * Calculate the number of items to skip for the last page.
     *
     * @param total total number of items.
     * @param skip number of items to skip for the current page.
     * @param limit page size
     * @return skipped items
     */
    private int calcLastPageSkip(int total, int skip, int limit) {
        if (skip > total - limit) {
            return skip;
        }
        if (total % limit > 0) {
            return total - total % limit;
        }
        return total - limit;
    }

    /**
     * Return the URI of the page with N skipped items and a page limitted to pages of size M.
     * @param uriTemplate the template used to create the link
     * @param skip the number of skipped items
     * @param limit the page size
     * @return href
     */
    private String pageUri(final UriTemplate uriTemplate, final int skip, final int limit) {
        if (limit == MAX_VALUE) {
            return uriTemplate.expand();
        }
        return uriTemplate.set(skipVar(), skip).set(limitVar(), limit).expand();
    }

}
