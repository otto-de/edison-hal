package de.otto.edison.hal.paging;

import com.damnhandy.uri.template.UriTemplate;
import de.otto.edison.hal.Links;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.EnumSet;
import java.util.OptionalInt;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.paging.PagingRel.*;
import static de.otto.edison.hal.paging.SkipLimitPaging.skipLimitPage;
import static java.lang.Integer.MAX_VALUE;
import static java.util.EnumSet.*;
import static java.util.OptionalInt.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SkipLimitPagingTest {

    public static final UriTemplate URI_TEMPLATE = fromTemplate("/{?skip,limit}");

    public static final EnumSet<PagingRel> ALL_RELS = allOf(PagingRel.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToSkipNegativeNumElements1() {
        skipLimitPage(-1, 2, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToSkipNegativeNumElements2() {
        skipLimitPage(-1, 2, 42);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToLimitNegativeNumElements1() {
        skipLimitPage(0, -1, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToLimitNegativeNumElements2() {
        skipLimitPage(0, -1, 42);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToProvideMoreElements() {
        skipLimitPage(0, Integer.MAX_VALUE, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToHaveTotalCountLessThenZero() {
        skipLimitPage(0, 4, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToSkipBehindLastPage() {
        skipLimitPage(6, 4, 5);
    }

    @Test
    public void shouldInitHasMoreFromTotal() {
        final SkipLimitPaging paging = skipLimitPage(1, 2, 4);
        assertThat(paging.getLimit(), is(2));
        assertThat(paging.getSkip(), is(1));
        assertThat(paging.getTotal(), is(OptionalInt.of(4)));
        assertThat(paging.hasMore(), is(true));
    }

    @Test
    public void shouldInitTotalAsEmpty() {
        final SkipLimitPaging paging = skipLimitPage(1, 2, true);
        assertThat(paging.getLimit(), is(2));
        assertThat(paging.getSkip(), is(1));
        assertThat(paging.getTotal(), is(empty()));
        assertThat(paging.hasMore(), is(true));
    }

    @Test
    public void shouldCreateLinksForEmptyPage() {
        Links paging = skipLimitPage(0, 3, 0).links(URI_TEMPLATE, ALL_RELS);
        assertThat(hrefFrom(paging, "self"), is("/?skip=0&limit=3"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(hrefFrom(paging, "last"), is("/?skip=0&limit=3"));
    }

    @Test
    public void shouldNotPageAfterLastPage() {
        expectedException.expect(IllegalArgumentException.class);
        skipLimitPage(1, 3, 0);
    }

    @Test
    public void shouldOnlyBuildWantedLinks() {
        Links paging = linkingTo().with(skipLimitPage(3, 3, 10).links(URI_TEMPLATE, range(PREV, NEXT))).build();

        assertThat(isAbsent(paging, "self"), is(true));
        assertThat(isAbsent(paging, "first"), is(true));
        assertThat(isAbsent(paging, "next"), is(false));
        assertThat(isAbsent(paging, "prev"), is(false));
        assertThat(isAbsent(paging, "last"), is(true));
    }


    @Test
    public void shouldBuildUriWithoutParams() {
        Links paging = skipLimitPage(0, MAX_VALUE, false).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/"));
        assertThat(hrefFrom(paging, "first"), is("/"));
        assertThat(paging.getLinkBy("next").isPresent(), is(false));
        assertThat(paging.getLinkBy("prev").isPresent(), is(false));
        assertThat(paging.getLinkBy("last").isPresent(), is(false));
    }

    @Test
    public void shouldBuildUrisForFirstPage() {
        Links paging = skipLimitPage(0, 2, true).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?skip=0&limit=2"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=2"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=2&limit=2"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForMiddlePage() {
        Links paging = skipLimitPage(1, 2, true).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?skip=1&limit=2"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=2"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=3&limit=2"));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=0&limit=2"));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForLastPage() {
        Links paging = skipLimitPage(6, 3, false).links(URI_TEMPLATE, ALL_RELS);
        assertThat(hrefFrom(paging, "self"), is("/?skip=6&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(isAbsent(paging, "next"), is(true));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=3&limit=3"));
        assertThat(isAbsent(paging, "last"), is(true));
    }

    @Test
    public void shouldBuildUrisForFirstPageWithKnownTotalCount() {
        Links paging = skipLimitPage(0, 3, 10).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=3&limit=3"));
        assertThat(isAbsent(paging, "prev"), is(true));
        assertThat(hrefFrom(paging, "last"), is("/?skip=9&limit=3"));
    }

    @Test
    public void shouldBuildUrisForLagePageWithTotalAsMultipleOfSkip() {
        Links paging = skipLimitPage(0, 5, 10).links(URI_TEMPLATE, of(LAST));

        assertThat(hrefFrom(paging, "last"), is("/?skip=5&limit=5"));
    }

    @Test
    public void shouldBuildUrisForMiddlePageWithKnownTotalCount() {
        Links paging = skipLimitPage(5, 3, 10).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?skip=5&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=8&limit=3"));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=2&limit=3"));
        assertThat(hrefFrom(paging, "last"), is("/?skip=9&limit=3"));
    }

    @Test
    public void shouldBuildUrisForMiddlePageWithKnownTotalCount2() {
        Links paging = skipLimitPage(4, 3, 10).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?skip=4&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=7&limit=3"));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=1&limit=3"));
        assertThat(hrefFrom(paging, "last"), is("/?skip=9&limit=3"));
    }

    @Test
    public void shouldBuildUrisForMiddlePageWithKnownTotalCount3() {
        Links paging = skipLimitPage(3, 3, 10).links(URI_TEMPLATE, ALL_RELS);

        assertThat(hrefFrom(paging, "self"), is("/?skip=3&limit=3"));
        assertThat(hrefFrom(paging, "first"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "next"), is("/?skip=6&limit=3"));
        assertThat(hrefFrom(paging, "prev"), is("/?skip=0&limit=3"));
        assertThat(hrefFrom(paging, "last"), is("/?skip=9&limit=3"));
    }

    @Test
    public void shouldBuildUrisForLastPageWithKnownTotalCount() {
        Links paging = skipLimitPage(8, 3, 10).links(URI_TEMPLATE, ALL_RELS);

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
        Links paging = new TestSkipLimitPaging(8, 3, false).links(fromTemplate("/{?s,num}"), of(SELF));

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