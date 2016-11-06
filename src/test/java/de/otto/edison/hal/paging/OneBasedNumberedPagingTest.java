package de.otto.edison.hal.paging;

import com.damnhandy.uri.template.UriTemplate;
import de.otto.edison.hal.Links;
import org.junit.Test;

import java.util.EnumSet;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.paging.NumberedPaging.numberedPaging;
import static de.otto.edison.hal.paging.NumberedPaging.oneBasedNumberedPaging;
import static de.otto.edison.hal.paging.PagingRel.NEXT;
import static de.otto.edison.hal.paging.PagingRel.PREV;
import static de.otto.edison.hal.paging.PagingRel.SELF;
import static java.lang.Integer.MAX_VALUE;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;
import static java.util.EnumSet.range;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OneBasedNumberedPagingTest {

    public static final UriTemplate URI_TEMPLATE = fromTemplate("/{?page,pageSize}");

    public static final EnumSet<PagingRel> ALL_RELS = allOf(PagingRel.class);

    @Test
    public void shouldHandleEmptyPage() {
        final NumberedPaging p = oneBasedNumberedPaging(1, 100, 0);

        assertThat(p.getPageNumber(), is(1));
        assertThat(p.getPageSize(), is(100));
        assertThat(p.getTotal().getAsInt(), is(0));
        assertThat(p.getLastPage().getAsInt(), is(1));
    }

    @Test
    public void shouldHandleMostlyEmptySinglePage() {
        final NumberedPaging p = oneBasedNumberedPaging(1, 100, 1);

        assertThat(p.getPageNumber(), is(1));
        assertThat(p.getPageSize(), is(100));
        assertThat(p.getTotal().getAsInt(), is(1));
        assertThat(p.getLastPage().getAsInt(), is(1));
    }

    @Test
    public void shouldHandleFullSinglePage() {
        final NumberedPaging p = oneBasedNumberedPaging(1, 100, 100);

        assertThat(p.getPageNumber(), is(1));
        assertThat(p.getPageSize(), is(100));
        assertThat(p.getTotal().getAsInt(), is(100));
        assertThat(p.getLastPage().getAsInt(), is(1));
    }

    @Test
    public void shouldHandleMultiplePages() {
        final NumberedPaging p = oneBasedNumberedPaging(1, 100, 201);

        assertThat(p.getPageNumber(), is(1));
        assertThat(p.getPageSize(), is(100));
        assertThat(p.getTotal().getAsInt(), is(201));
        assertThat(p.getLastPage().getAsInt(), is(3));
    }

    @Test
    public void shouldBuildLinksForEmptyPage() {
        Links paging = linkingTo(oneBasedNumberedPaging(1, 100, 0).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?page=1&pageSize=100"));
        assertThat(hrefFrom(paging, "first"), is("/?page=1&pageSize=100"));
        assertThat(paging.getLinkBy("next").isPresent(), is(false));
        assertThat(paging.getLinkBy("prev").isPresent(), is(false));
        assertThat(hrefFrom(paging, "last"), is("/?page=1&pageSize=100"));
    }

    @Test
    public void shouldOnlyBuildWantedLinks() {
        Links paging = linkingTo(oneBasedNumberedPaging(2, 3, 10).links(URI_TEMPLATE, range(PREV, NEXT)));

        assertThat(isAbsent(paging, "self"), is(true));
        assertThat(isAbsent(paging, "first"), is(true));
        assertThat(isAbsent(paging, "next"), is(false));
        assertThat(isAbsent(paging, "prev"), is(false));
        assertThat(isAbsent(paging, "last"), is(true));
    }


    @Test
    public void shouldBuildUriWithoutParams() {
        Links paging = linkingTo(oneBasedNumberedPaging(1, MAX_VALUE, false).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/"));
        assertThat(hrefFrom(paging, "first"), is("/"));
        assertThat(paging.getLinkBy("next").isPresent(), is(false));
        assertThat(paging.getLinkBy("prev").isPresent(), is(false));
        assertThat(paging.getLinkBy("last").isPresent(), is(false));
    }

    @Test
    public void shouldBuildUrisForFirstPage() {
        Links paging = linkingTo(oneBasedNumberedPaging(1, 2, true).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?page=1&pageSize=2"));
        assertThat(hrefFrom(paging, "first"), is("/?page=1&pageSize=2"));
        assertThat(hrefFrom(paging, "next"), is("/?page=2&pageSize=2"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForMiddlePage() {
        Links paging = linkingTo(oneBasedNumberedPaging(4, 2, true).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?page=4&pageSize=2"));
        assertThat(hrefFrom(paging, "first"), is("/?page=1&pageSize=2"));
        assertThat(hrefFrom(paging, "next"), is("/?page=5&pageSize=2"));
        assertThat(hrefFrom(paging, "prev"), is("/?page=3&pageSize=2"));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForLastPage() {
        Links paging = linkingTo(oneBasedNumberedPaging(4, 3, false).links(URI_TEMPLATE, ALL_RELS));
        assertThat(hrefFrom(paging, "self"), is("/?page=4&pageSize=3"));
        assertThat(hrefFrom(paging, "first"), is("/?page=1&pageSize=3"));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(hrefFrom(paging, "prev"), is("/?page=3&pageSize=3"));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForFirstPageWithKnownTotalCount() {
        Links paging = linkingTo(oneBasedNumberedPaging(1, 3, 10).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?page=1&pageSize=3"));
        assertThat(hrefFrom(paging, "first"), is("/?page=1&pageSize=3"));
        assertThat(hrefFrom(paging, "next"), is("/?page=2&pageSize=3"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(hrefFrom(paging, "last"), is("/?page=4&pageSize=3"));
    }

    @Test
    public void shouldBuildUrisForMiddlePageWithKnownTotalCount() {
        Links paging = linkingTo(oneBasedNumberedPaging(3, 3, 10).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?page=3&pageSize=3"));
        assertThat(hrefFrom(paging, "first"), is("/?page=1&pageSize=3"));
        assertThat(hrefFrom(paging, "next"), is("/?page=4&pageSize=3"));
        assertThat(hrefFrom(paging, "prev"), is("/?page=2&pageSize=3"));
        assertThat(hrefFrom(paging, "last"), is("/?page=4&pageSize=3"));
    }

    @Test
    public void shouldBuildUrisForLastPageWithKnownTotalCount() {
        Links paging = linkingTo(oneBasedNumberedPaging(5, 3, 10).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?page=5&pageSize=3"));
        assertThat(hrefFrom(paging, "first"), is("/?page=1&pageSize=3"));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(hrefFrom(paging, "prev"), is("/?page=4&pageSize=3"));
        assertThat(hrefFrom(paging, "last"), is("/?page=4&pageSize=3"));
    }

    static class TestNumberedPaging extends NumberedPaging {

        TestNumberedPaging(final int page, final int pageSize, final boolean hasMore) {
            super(1, page, pageSize, hasMore);
        }

        @Override
        protected String pageNumberVar() {
            return "p";
        }

        @Override
        protected String pageSizeVar() {
            return "num";
        }
    }

    @Test
    public void shouldBeAbleToOverrideTemplateVariables() {
        Links paging = linkingTo(new TestNumberedPaging(8, 3, false).links(fromTemplate("/{?p,num}"), of(SELF)));

        assertThat(hrefFrom(paging, "self"), is("/?p=8&num=3"));
    }

    private boolean isAbsent(Links links, String rel) {
        return !links.getLinkBy(rel).isPresent();
    }

    private String hrefFrom(Links links, String rel) {
        return links
                .getLinkBy(rel)
                .orElseThrow(()->new IllegalStateException(rel + " does not exist!"))
                .getHref();
    }
}