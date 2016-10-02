package de.otto.edison.hal.paging;

import com.damnhandy.uri.template.UriTemplate;
import de.otto.edison.hal.Links;
import org.junit.Test;

import java.util.EnumSet;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.paging.PagingRel.NEXT;
import static de.otto.edison.hal.paging.PagingRel.PREV;
import static de.otto.edison.hal.paging.PagingRel.SELF;
import static java.lang.Integer.MAX_VALUE;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;
import static java.util.EnumSet.range;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SkipLimitPagingTest {

    public static final UriTemplate URI_TEMPLATE = fromTemplate("/{?skip,limit}");

    public static final EnumSet<PagingRel> ALL_RELS = allOf(PagingRel.class);

    @Test
    public void shouldOnlyBuildWantedLinks() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(3, 3, 10).links(URI_TEMPLATE, range(PREV, NEXT)));

        assertThat(isAbsent(paging, "self"), is(true));
        assertThat(isAbsent(paging, "first"), is(true));
        assertThat(isAbsent(paging, "next"), is(false));
        assertThat(isAbsent(paging, "prev"), is(false));
        assertThat(isAbsent(paging, "last"), is(true));
    }


    @Test
    public void shouldBuildUriWithoutParams() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(0, MAX_VALUE, false).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/"));
        assertThat(hrefFrom(paging, "first"), is("/"));
        assertThat(paging.getLinkBy("next").isPresent(), is(false));
        assertThat(paging.getLinkBy("prev").isPresent(), is(false));
        assertThat(paging.getLinkBy("last").isPresent(), is(false));
    }

    @Test
    public void shouldBuildUrisForFirstPage() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(0, 2, true).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?skip=0&limit=2"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=2"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=2&limit=2"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForMiddlePage() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(1, 2, true).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?skip=1&limit=2"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=2"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=3&limit=2"));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=0&limit=2"));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForLastPage() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(6, 3, false).links(URI_TEMPLATE, ALL_RELS));
        assertThat(hrefFrom(paging, "self"), is("/?skip=6&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=3&limit=3"));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForFirstPageWithKnownTotalCount() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(0, 3, 10).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=3&limit=3"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(hrefFrom(paging, "last"), is("/?skip=9&limit=3"));
    }

    @Test
    public void shouldBuildUrisForMiddlePageWithKnownTotalCount() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(5, 3, 10).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?skip=5&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=8&limit=3"));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=2&limit=3"));
        assertThat(hrefFrom(paging, "last"), is("/?skip=9&limit=3"));
    }

    @Test
    public void shouldBuildUrisForMiddlePageWithKnownTotalCount2() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(4, 3, 10).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?skip=4&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=7&limit=3"));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=1&limit=3"));
        assertThat(hrefFrom(paging, "last"), is("/?skip=9&limit=3"));
    }

    @Test
    public void shouldBuildUrisForMiddlePageWithKnownTotalCount3() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(3, 3, 10).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?skip=3&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=6&limit=3"));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "last"), is("/?skip=9&limit=3"));
    }

    @Test
    public void shouldBuildUrisForLastPageWithKnownTotalCount() {
        Links paging = Links.linkingTo(SkipLimitPaging.skipLimitPage(8, 3, 10).links(URI_TEMPLATE, ALL_RELS));

        assertThat(hrefFrom(paging, "self"), is("/?skip=8&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=5&limit=3"));
        assertThat(hrefFrom(paging, "last"), is("/?skip=8&limit=3"));
    }

    static class TestSkipLimitPaging extends SkipLimitPaging {

        TestSkipLimitPaging(final int skip, final int limit, final boolean hasMore) {
            super(skip, limit, hasMore);
        }

        @Override
        protected String skipVar() {
            return "s";
        }

        @Override
        protected String limitVar() {
            return "num";
        }
    }

    @Test
    public void shouldBeAbleToOverrideTemplateVariables() {
        Links paging = linkingTo(new TestSkipLimitPaging(8, 3, false).links(fromTemplate("/{?s,num}"), of(SELF)));

        assertThat(hrefFrom(paging, "self"), is("/?s=8&num=3"));
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