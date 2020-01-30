package de.otto.edison.hal.paging;

import com.damnhandy.uri.template.UriTemplate;
import de.otto.edison.hal.Links;
import org.junit.Test;

import java.util.EnumSet;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static de.otto.edison.hal.paging.NumberedPaging.zeroBasedNumberedPaging;
import static de.otto.edison.hal.paging.PagingRel.*;
import static java.lang.Integer.MAX_VALUE;
import static java.util.EnumSet.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ZeroBasedNumberedPagingTest {

    public static final UriTemplate URI_TEMPLATE = fromTemplate("/{?page,pageSize}");

    public static final EnumSet<PagingRel> ALL_RELS = allOf(PagingRel.class);

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToSkipNegativePageNum1() {
        zeroBasedNumberedPaging(-1, 2, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToSkipNegativePageNum2() {
        zeroBasedNumberedPaging(-1, 2, 42);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToLimitPageSize1() {
        zeroBasedNumberedPaging(0, 0, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToLimitPageSize2() {
        zeroBasedNumberedPaging(0, 0, 42);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToProvideMoreElements() {
        zeroBasedNumberedPaging(0, Integer.MAX_VALUE, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToHaveTotalCountLessThenZero() {
        zeroBasedNumberedPaging(0, 4, -1);
    }

    @Test
    public void shouldHandleEmptyPage() {
        final NumberedPaging p = zeroBasedNumberedPaging(0, 100, 0);

        assertThat(p.getPageNumber(), is(0));
        assertThat(p.getPageSize(), is(100));
        assertThat(p.getTotal().getAsInt(), is(0));
        assertThat(p.getLastPage().getAsInt(), is(0));
        assertThat(p.hasMore(), is(false));
    }

    @Test
    public void shouldHandleMostlyEmptySinglePage() {
        final NumberedPaging p = zeroBasedNumberedPaging(0, 100, 1);

        assertThat(p.getPageNumber(), is(0));
        assertThat(p.getPageSize(), is(100));
        assertThat(p.getTotal().getAsInt(), is(1));
        assertThat(p.getLastPage().getAsInt(), is(0));
        assertThat(p.hasMore(), is(false));
    }

    @Test
    public void shouldHandleFullSinglePage() {
        final NumberedPaging p = zeroBasedNumberedPaging(0, 100, 100);

        assertThat(p.getPageNumber(), is(0));
        assertThat(p.getPageSize(), is(100));
        assertThat(p.getTotal().getAsInt(), is(100));
        assertThat(p.getLastPage().getAsInt(), is(0));
        assertThat(p.hasMore(), is(false));
    }

    @Test
    public void shouldHandleMultiplePages() {
        final NumberedPaging p = zeroBasedNumberedPaging(0, 100, 201);

        assertThat(p.getPageNumber(), is(0));
        assertThat(p.getPageSize(), is(100));
        assertThat(p.getTotal().getAsInt(), is(201));
        assertThat(p.getLastPage().getAsInt(), is(2));
        assertThat(p.hasMore(), is(true));
    }

    @Test
    public void shouldBuildLinksForEmptyPage() {
        Links paging = zeroBasedNumberedPaging(0, 100, 0).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?page=0&pageSize=100"));
        assertThat(hrefFrom(paging, "first"), is("/?page=0&pageSize=100"));
        assertThat(paging.getLinkBy("next").isPresent(), is(false));
        assertThat(paging.getLinkBy("prev").isPresent(), is(false));
        assertThat(hrefFrom(paging, "last"), is("/?page=0&pageSize=100"));
    }

    @Test
    public void shouldOnlyBuildWantedLinks() {
        Links paging = zeroBasedNumberedPaging(3, 3, 10).links(URI_TEMPLATE, range(PREV, NEXT));

        assertThat(isAbsent(paging, "self"), is(true));
        assertThat(isAbsent(paging, "first"), is(true));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(isAbsent(paging, "prev"), is(false));
        assertThat(isAbsent(paging, "last"), is(true));
    }


    @Test
    public void shouldBuildUriWithoutParams() {
        Links paging = zeroBasedNumberedPaging(0, MAX_VALUE, false).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/"));
        assertThat(hrefFrom(paging, "first"), is("/"));
        assertThat(paging.getLinkBy("next").isPresent(), is(false));
        assertThat(paging.getLinkBy("prev").isPresent(), is(false));
        assertThat(paging.getLinkBy("last").isPresent(), is(false));
    }

    @Test
    public void shouldBuildUrisForFirstPage() {
        Links paging = zeroBasedNumberedPaging(0, 2, true).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?page=0&pageSize=2"));
        assertThat(hrefFrom(paging, "first"), is("/?page=0&pageSize=2"));
        assertThat(hrefFrom(paging, "next"), is("/?page=1&pageSize=2"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForMiddlePage() {
        Links paging = zeroBasedNumberedPaging(3, 2, true).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?page=3&pageSize=2"));
        assertThat(hrefFrom(paging, "first"), is("/?page=0&pageSize=2"));
        assertThat(hrefFrom(paging, "next"), is("/?page=4&pageSize=2"));
        assertThat(hrefFrom(paging, "prev"), is("/?page=2&pageSize=2"));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForLastPage() {
        Links paging = zeroBasedNumberedPaging(3, 3, false).links(URI_TEMPLATE, ALL_RELS);
        assertThat(hrefFrom(paging, "self"), is("/?page=3&pageSize=3"));
        assertThat(hrefFrom(paging, "first"), is("/?page=0&pageSize=3"));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(hrefFrom(paging, "prev"), is("/?page=2&pageSize=3"));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForFirstPageWithKnownTotalCount() {
        Links paging = zeroBasedNumberedPaging(0, 3, 10).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?page=0&pageSize=3"));
        assertThat(hrefFrom(paging, "first"), is("/?page=0&pageSize=3"));
        assertThat(hrefFrom(paging, "next"), is("/?page=1&pageSize=3"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(hrefFrom(paging, "last"), is("/?page=3&pageSize=3"));
    }

    @Test
    public void shouldBuildUrisForMiddlePageWithKnownTotalCount() {
        Links paging = zeroBasedNumberedPaging(2, 3, 10).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?page=2&pageSize=3"));
        assertThat(hrefFrom(paging, "first"), is("/?page=0&pageSize=3"));
        assertThat(hrefFrom(paging, "next"), is("/?page=3&pageSize=3"));
        assertThat(hrefFrom(paging, "prev"), is("/?page=1&pageSize=3"));
        assertThat(hrefFrom(paging, "last"), is("/?page=3&pageSize=3"));
    }

    @Test
    public void shouldBuildUrisForLastPageWithKnownTotalCount() {
        Links paging = zeroBasedNumberedPaging(4, 3, 10).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?page=4&pageSize=3"));
        assertThat(hrefFrom(paging, "first"), is("/?page=0&pageSize=3"));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(hrefFrom(paging, "prev"), is("/?page=3&pageSize=3"));
        assertThat(hrefFrom(paging, "last"), is("/?page=3&pageSize=3"));
    }

    static class TestNumberedPaging extends NumberedPaging {

        TestNumberedPaging(final int page, final int pageSize, final boolean hasMore) {
            super(0, page, pageSize, hasMore);
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
        Links paging = new TestNumberedPaging(8, 3, false).links(fromTemplate("/{?p,num}"), of(SELF));

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