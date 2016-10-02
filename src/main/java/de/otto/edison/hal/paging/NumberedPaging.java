package de.otto.edison.hal.paging;

import com.damnhandy.uri.template.UriTemplate;
import de.otto.edison.hal.Link;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalInt;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.self;
import static java.lang.Integer.MAX_VALUE;

/**
 * A helper class used to create paging links for paged resources that are using numbered page URIs.
 * <p>
 *     Usage:
 * </p>
 * <pre><code>
 * public class MyHalRepresentation extends HalRepresentation {
 *    public MyHalRepresentation(final NumberedPaging page, final List&lt;Stuff&gt; pagedStuff) {
 *          super(linksBuilder()
 *                  .with(page.links(
 *                          fromTemplate(contextPath + "/api/stuff{?pageNumber,pageSize}"),
 *                          allOf(PagingRel.class)
 *                   ))
 *                   .with(pagedStuff.stream()
 *                          .limit(page.pageNumber*pageSize)
 *                          .map(stuff -&gt; linkBuilder("item", stuff.selfHref)
 *                                  .withTitle(stuff.title)
 *                                  .build())
 *                          .collect(toList()))
 *                   .build()
 *          );
 *    }
 * </code></pre>
 */
public class NumberedPaging {

    /**
     * The default template-variable name used to identify the number of the page.
     */
    public static final String PAGE_NUMBER_VAR = "page";
    /**
     * The default template-variable name used to identify the size of the page.
     */
    public static final String PAGE_SIZE_VAR = "pageSize";

    /**
     * The page number of the current page.
     */
    private final int pageNumber;
    /**
     * The size of the pages.
     */
    private final int pageSize;
    /**
     * More items beyond the current page - or not.
     */
    private final boolean hasMore;
    /**
     * Optionally, the total number of available items. Used to generate the number of the last page
     */
    private final OptionalInt total;

    /**
     * Creates a NumberedPage instance.
     *
     * @param pageNumber the current page number.
     * @param pageSize the size of a page.
     * @param hasMore more items beyond this page?
     */
    private NumberedPaging(final int pageNumber, final int pageSize, final boolean hasMore) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Parameter 'pageNumber' must not be less than zero");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Parameter 'pageSize' must be greater zero");
        }
        if (hasMore && pageSize == MAX_VALUE) {
            throw new IllegalArgumentException("Unable to calculate next page for unbounded page sizes.");
        }
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Parameter 'pageNumber' must not be less than zero");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Parameter 'pageSize' must be greater zero");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.hasMore = hasMore;
        this.total = OptionalInt.empty();
    }

    /**
     * Creates a NumberedPage instance.
     *
     * @param pageNumber the current page number.
     * @param pageSize the size of a page.
     * @param total the total number of available items.
     */
    private NumberedPaging(final int pageNumber, final int pageSize, final int total) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Parameter 'pageNumber' must not be less than zero");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Parameter 'pageSize' must be greater zero");
        }
        if (total <= 0) {
            throw new IllegalArgumentException("Parameter 'total' must be greater zero");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.hasMore = pageNumber*pageSize < total;
        this.total = OptionalInt.of(total);
    }

    /**
     * Create a NumberedPaging instances for pages where it is known whether or not there are more
     * items beyond the current page.
     *
     * @param pageNumber the page number of the current page.
     * @param pageSize the number of items per page.
     * @param hasMore true if there are more items beyond the current page, false otherwise.
     * @return created NumberedPaging instance
     */
    public static NumberedPaging numberedPaging(final int pageNumber, final int pageSize, final boolean hasMore) {
        return new NumberedPaging(pageNumber, pageSize, hasMore);
    }

    /**
     * Create a NumberedPaging instances for pages where it is known how many items are matching the initial query.
     *
     * @param pageNumber the page number of the current page.
     * @param pageSize the number of items per page.
     * @param totalCount the total number of items matching the initial query.
     * @return created NumberedPaging instance
     */
    public static NumberedPaging numberedPaging(final int pageNumber, final int pageSize, final int totalCount) {
        return new NumberedPaging(pageNumber, pageSize, totalCount);
    }

    /**
     * Return the requested links for a paged resource were the link's hrefs are created using the given
     * {@link UriTemplate}.
     * <p>
     *     The variables used to identify the current page number and page size must match the values returned
     *     by {@link #pageNumberVar()} and {@link #pageSizeVar()}. Derive from this class, if other values than
     *     {@link #PAGE_NUMBER_VAR} or {@link #PAGE_SIZE_VAR} are required.
     * </p>
     * <p>
     *     If the provided template does not contain the required variable names. links can not be expanded.
     * </p>
     * @param pageUriTemplate the URI template used to create paging links.
     * @param rels the links expected to be created.
     * @return List of links
     */
    public final List<Link> links(final UriTemplate pageUriTemplate, final EnumSet<PagingRel> rels) {
        final List<Link> links = new ArrayList<>();
        if (rels.contains(PagingRel.SELF)) {
            links.add(
                    self(pageUri(pageUriTemplate, pageNumber, pageSize))
            );
        }
        if (rels.contains(PagingRel.FIRST)) {
            links.add(
                    link("first", pageUri(pageUriTemplate, 0, pageSize))
            );
        }
        if (pageNumber > 0 && rels.contains(PagingRel.PREV)) {
            links.add(
                    link("prev", pageUri(pageUriTemplate, pageNumber-1, pageSize))
            );
        }
        if (hasMore && rels.contains(PagingRel.NEXT)) {
            links.add(
                    link("next", pageUri(pageUriTemplate, pageNumber+1, pageSize))
            );
        }
        if (total.isPresent() && rels.contains(PagingRel.LAST)) {
            links.add(
                    link("last", pageUri(pageUriTemplate, calcLastPage(this.total.getAsInt(), this.pageSize), pageSize))
            );
        }
        return links;
    }

    /**
     * The name of the uri-template variable used to identify the current page number. By default,
     * 'page' is used.
     * @return uri-template variable for the current page-number.
     */
    protected String pageNumberVar() {
        return PAGE_NUMBER_VAR;
    }

    /**
     * The name of the uri-template variable used to specify the current page size. By default,
     * 'pageSize' is used.
     * @return uri-template variable for the current page-size.
     */
    protected String pageSizeVar() {
        return PAGE_SIZE_VAR;
    }

    /**
     * Returns the number of the last page, if the total number of items is known.
     *
     * @param total total number of items
     * @param pageSize the current page size
     * @return page number of the last page
     */
    private int calcLastPage(int total, int pageSize) {
        return total % pageSize > 0
                ? total / pageSize + 1
                : total / pageSize;
    }

    /**
     * Return the HREF of the page specified by UriTemplate, pageNumber and pageSize.
     *
     * @param uriTemplate the template used to build hrefs.
     * @param pageNumber the number of the linked page.
     * @param pageSize the size of the pages.
     * @return href of the linked page.
     */
    private String pageUri(final UriTemplate uriTemplate, final int pageNumber, final int pageSize) {
        if (pageNumber == 0 && pageSize == MAX_VALUE) {
            return uriTemplate.expand();
        }
        if (pageNumber > 0 && pageSize == MAX_VALUE) {
            return uriTemplate.set(pageNumberVar(), pageNumber).expand();
        }
        return uriTemplate.set(pageNumberVar(), pageNumber).set(pageSizeVar(), pageSize).expand();
    }
}
