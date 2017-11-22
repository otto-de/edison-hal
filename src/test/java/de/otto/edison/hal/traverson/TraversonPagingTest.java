package de.otto.edison.hal.traverson;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.otto.edison.hal.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.traverson.Traverson.traverson;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

public class TraversonPagingTest {

    private static final Logger LOG = getLogger(TraversonPagingTest.class);

    @Rule
    public TestName testName = new TestName();

    @Before
    public void logTestName() throws Throwable {
        LOG.trace("============== {} ===================", testName.getMethodName());
    }

    @Test
    public void shouldPageOverLinksUsingNext() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"next\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");
        when(mock.apply(link("next", "http://example.com/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/3\"}," +
                                "{\"href\":\"/example/foo/4\"}]," +
                            "\"prev\":" +
                                "{\"href\":\"/example/foo\"}" +
                        "}" +
                "}");

        // when
        Optional<HalRepresentation> optionalPage = traverson(mock)
                .startWith("http://example.com/example/foo")
                .follow("next")
                .getResource();

        // then
        assertThat(optionalPage.isPresent(), is(true));
    }

    @Test
    public void shouldStreamLinkedItemsOfNextPage() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"next\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");
        when(mock.apply(link("next", "http://example.com/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/3\"}," +
                                "{\"href\":\"/example/foo/4\"}]," +
                            "\"prev\":" +
                                "{\"href\":\"/example/foo\"}" +
                        "}" +
                "}");
        when(mock.apply(link("foo", "http://example.com/example/foo/3"))).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/3\"}}" +
                "}");
        when(mock.apply(link("foo", "http://example.com/example/foo/4"))).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/4\"}}" +
                "}");

        // when
        final Traverson traverson = traverson(mock);
        traverson
                .startWith("http://example.com/example/foo")
                .follow("next")
                .getResource();
        // then
        assertThat(traverson.follow("foo").stream().count(), is(2L));
    }

    @Test
    public void shouldNotPageAfterLastPage() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn("{}");

        // when we getResource the next page
        final Optional<HalRepresentation> optionalPage = traverson(mock)
                .startWith("http://example.com/example/foo")
                .follow("next")
                .getResource();

        assertThat(optionalPage.isPresent(), is(false));
    }

    @Test
    public void shouldPageOverLinksUsingPrev() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/3\"}," +
                                "{\"href\":\"/example/foo/4\"}]," +
                            "\"prev\":" +
                                "{\"href\":\"/example/foo\"}" +
                        "}" +
                "}");
        when(mock.apply(link("prev", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"next\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");

        // when
        final Optional<HalRepresentation> optionalPage = traverson(mock)
                .startWith("http://example.com/example/foo?page=2")
                .follow("prev")
                .getResource();

        // then
        assertThat(optionalPage.isPresent(), is(true));
    }

    @Test
    public void shouldNotPageBeforeFirstPage() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]" +
                        "}" +
                "}");

        // when
        final Optional<HalRepresentation> optionalPage = traverson(mock)
                .startWith("http://example.com/example/foo")
                .follow("prev")
                .getResource();
        // then
        assertThat(optionalPage.isPresent(), is(false));
    }

    @Test
    public void shouldPageOverLinksUsingFirstAndLast() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"last\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");
        when(mock.apply(link("first", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"last\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");
        when(mock.apply(link("last", "http://example.com/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/3\"}," +
                                "{\"href\":\"/example/foo/4\"}]," +
                            "\"first\":" +
                                "{\"href\":\"/example/foo\"}" +
                        "}" +
                "}");

        final Traverson traverson = traverson(mock);

        // when we getResource the next page
        Optional<HalRepresentation> optionalPage = traverson
                .startWith("http://example.com/example/foo")
                .follow("last")
                .getResource();

        assertThat(optionalPage.isPresent(), is(true));
        List<String> hrefs = optionalPage.get().getLinks().getLinksBy("foo")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        // then
        assertThat(hrefs, contains("/example/foo/3","/example/foo/4"));

        // when we return to previous page
        optionalPage = traverson.follow("first").getResource();
        hrefs = optionalPage.get().getLinks().getLinksBy("foo")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        // then
        assertThat(hrefs, contains("/example/foo/1","/example/foo/2"));
    }

    @Test
    public void shouldIterateOverAllItemsOfMultiplePages() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{\"someProperty\":\"firstPage\",\"_links\":{" +
                        "\"self\":{\"href\":\"http://example.com/example/foo?page=1\"}," +
                        "\"item\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]," +
                        "\"next\":{\"href\":\"/example/foo?page=2\"}}}");
        when(mock.apply(link("next", "http://example.com/example/foo?page=2"))).thenReturn(
                "{\"someProperty\":\"secondPage\"," +
                        "\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/3\"},{\"href\":\"/example/foo/4\"}]," +
                        "\"prev\":{\"href\":\"/example/foo\"}}" +
                        "}");
        when(mock.apply(link("item", "http://example.com/example/foo/1"))).thenReturn("{\"someOtherProperty\":\"one\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/2"))).thenReturn("{\"someOtherProperty\":\"two\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/3"))).thenReturn("{\"someOtherProperty\":\"three\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/4"))).thenReturn("{\"someOtherProperty\":\"four\"}");

        // when
        final List<String> values = new ArrayList<>();
        final Traverson pager = traverson(mock);
        Optional<ExtendedHalRepresentation> currentPage = pager
                .startWith("http://example.com/example/foo")
                .getResourceAs(ExtendedHalRepresentation.class);
        while (currentPage.isPresent()) {
            traverson(mock)
                    .startWith(pager.getCurrentContextUrl(), currentPage.get())
                    .follow("item")
                    .streamAs(OtherExtendedHalRepresentation.class)
                    .forEach(x -> values.add(x.someOtherProperty));
            if (currentPage.get().getLinks().hasLink("next")) {
                currentPage = pager
                        .follow("next")
                        .getResourceAs(ExtendedHalRepresentation.class);
            } else {
                currentPage = Optional.empty();
            }
        }

        // then
        assertThat(values, contains("one", "two", "three", "four"));
    }

    @Test
    public void shouldIterateOverAllItemsOfMultiplePagesUsingPaginateNext() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]," +
                        "\"next\":{\"href\":\"/example/foo?page=2\"}}}");
        when(mock.apply(link("next", "http://example.com/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/3\"},{\"href\":\"/example/foo/4\"}]," +
                        "\"prev\":{\"href\":\"/example/foo\"}}" +
                        "}");
        when(mock.apply(link("item", "http://example.com/example/foo/1"))).thenReturn("{\"someOtherProperty\":\"one\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/2"))).thenReturn("{\"someOtherProperty\":\"two\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/3"))).thenReturn("{\"someOtherProperty\":\"three\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/4"))).thenReturn("{\"someOtherProperty\":\"four\"}");

        // when
        final List<String> values = new ArrayList<>();
        final Traverson traverson = traverson(mock);
                traverson.startWith("http://example.com/example/foo")
                .paginateNext((Traverson pageTraverson) -> {
                    pageTraverson
                            .follow("item")
                            .streamAs(OtherExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someOtherProperty));
                    return true;
                });

        // then
        assertThat(values, contains("one", "two", "three", "four"));
        assertThat(traverson.getCurrentContextUrl(), is(new URL("http://example.com/example/foo?page=2")));
    }

    @Test
    public void shouldIterateOverAllItemsOfMultiplePagesUsingPaginatePrev() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]," +
                        "\"prev\":{\"href\":\"/example/foo?page=2\"}}}");
        when(mock.apply(link("prev", "http://example.com/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/3\"},{\"href\":\"/example/foo/4\"}]," +
                        "\"next\":{\"href\":\"/example/foo\"}}" +
                        "}");
        when(mock.apply(link("item", "http://example.com/example/foo/1"))).thenReturn("{\"someOtherProperty\":\"one\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/2"))).thenReturn("{\"someOtherProperty\":\"two\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/3"))).thenReturn("{\"someOtherProperty\":\"three\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/4"))).thenReturn("{\"someOtherProperty\":\"four\"}");

        // when
        final List<String> values = new ArrayList<>();
        final Traverson traverson = traverson(mock);
                traverson.startWith("http://example.com/example/foo")
                .paginatePrev((Traverson pageTraverson) -> {
                    pageTraverson
                            .follow("item")
                            .streamAs(OtherExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someOtherProperty));
                    return true;
                });

        // then
        assertThat(values, contains("one", "two", "three", "four"));
        assertThat(traverson.getCurrentContextUrl(), is(new URL("http://example.com/example/foo?page=2")));
    }

    @Test
    public void shouldIterateOverAllEmbeddedItemsOfMultiplePagesUsingPaginateNext() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{\"next\":{\"href\":\"http://example.com/example/foo?page=2\"}}," +
                        "\"_embedded\":{\"item\":[{\"someProperty\":\"one\"},{\"someProperty\":\"two\"}]" +
                        "}}");
        when(mock.apply(link("next", "http://example.com/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{\"prev\":{\"href\":\"/example/foo\"}}," +
                        "\"_embedded\":{\"item\":[{\"someProperty\":\"three\"},{\"someProperty\":\"four\"}]" +
                        "}}");

        // when
        final List<String> values = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
                .paginateNext(withEmbedded("item", ExtendedHalRepresentation.class), (Traverson pageTraverson) -> {
                    pageTraverson
                            .follow("item")
                            .streamAs(ExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someProperty));
                    return true;
                });

        // then
        assertThat(values, contains("one", "two", "three", "four"));
    }

    @Test
    public void shouldIterateOverAllEmbeddedItemsOfMultiplePagesUsingPaginatePrev() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{\"prev\":{\"href\":\"http://example.com/example/foo?page=2\"}}," +
                        "\"_embedded\":{\"item\":[{\"someProperty\":\"one\"},{\"someProperty\":\"two\"}]" +
                        "}}");
        when(mock.apply(link("prev", "http://example.com/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{\"next\":{\"href\":\"/example/foo\"}}," +
                        "\"_embedded\":{\"item\":[{\"someProperty\":\"three\"},{\"someProperty\":\"four\"}]" +
                        "}}");

        // when
        final List<String> values = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
                .paginatePrev(withEmbedded("item", ExtendedHalRepresentation.class), (Traverson pageTraverson) -> {
                    pageTraverson
                            .follow("item")
                            .streamAs(ExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someProperty));
                    return true;
                });

        // then
        assertThat(values, contains("one", "two", "three", "four"));
    }

    @Test
    public void shouldStopPagination() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{\"someProperty\":\"firstPage\",\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]," +
                        "\"next\":{\"href\":\"/example/foo?page=2\"}}}");
        when(mock.apply(link("item", "http://example.com/example/foo/1"))).thenReturn("{\"someOtherProperty\":\"one\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/2"))).thenReturn("{\"someOtherProperty\":\"two\"}");

        // when
        final List<String> values = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
                .paginateNext((Traverson pageTraverson) -> {
                    pageTraverson
                            .follow("item")
                            .streamAs(OtherExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someOtherProperty));
                    return values.size() < 2;
                });

        // then
        assertThat(values, contains("one", "two"));
    }

    @Test
    public void shouldIterateOverAllItemsOfMultiplePagesUsingPaginateNextAs() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{\"someProperty\":\"firstPage\",\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]," +
                        "\"next\":{\"href\":\"/example/foo?page=2\"}}}");
        when(mock.apply(link("next", "http://example.com/example/foo?page=2"))).thenReturn(
                "{\"someProperty\":\"secondPage\"," +
                        "\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/3\"},{\"href\":\"/example/foo/4\"}]," +
                        "\"prev\":{\"href\":\"/example/foo\"}}" +
                        "}");
        when(mock.apply(link("item", "http://example.com/example/foo/1"))).thenReturn("{\"someOtherProperty\":\"one\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/2"))).thenReturn("{\"someOtherProperty\":\"two\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/3"))).thenReturn("{\"someOtherProperty\":\"three\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/4"))).thenReturn("{\"someOtherProperty\":\"four\"}");

        // when
        final List<String> pageValues = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
                .paginateNextAs(ExtendedHalRepresentation.class, (Traverson pageTraverson) -> {
                    pageTraverson
                            .getResourceAs(ExtendedHalRepresentation.class)
                            .ifPresent((page) -> pageValues.add(page.someProperty));
                    pageTraverson
                            .follow("item")
                            .streamAs(OtherExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someOtherProperty));
                    return true;
                });

        // then
        assertThat(pageValues, contains("firstPage", "secondPage"));
        assertThat(values, contains("one", "two", "three", "four"));
    }

    @Test
    public void shouldIterateOverAllItemsOfMultiplePagesUsingPaginatePrevAs() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{\"someProperty\":\"firstPage\",\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]," +
                        "\"prev\":{\"href\":\"/example/foo?page=2\"}}}");
        when(mock.apply(link("prev", "http://example.com/example/foo?page=2"))).thenReturn(
                "{\"someProperty\":\"secondPage\"," +
                        "\"_links\":{" +
                        "\"item\":[{\"href\":\"/example/foo/3\"},{\"href\":\"/example/foo/4\"}]" +
                        "}" +
                        "}");
        when(mock.apply(link("item", "http://example.com/example/foo/1"))).thenReturn("{\"someOtherProperty\":\"one\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/2"))).thenReturn("{\"someOtherProperty\":\"two\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/3"))).thenReturn("{\"someOtherProperty\":\"three\"}");
        when(mock.apply(link("item", "http://example.com/example/foo/4"))).thenReturn("{\"someOtherProperty\":\"four\"}");

        // when
        final List<String> pageValues = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
                .paginatePrevAs(ExtendedHalRepresentation.class, (Traverson pageTraverson) -> {
                    pageTraverson
                            .getResourceAs(ExtendedHalRepresentation.class)
                            .ifPresent((page) -> pageValues.add(page.someProperty));
                    pageTraverson
                            .follow("item")
                            .streamAs(OtherExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someOtherProperty));
                    return true;
                });

        // then
        assertThat(pageValues, contains("firstPage", "secondPage"));
        assertThat(values, contains("one", "two", "three", "four"));
    }

    @Test
    public void shouldIterateOverAllEmbeddedItemsOfMultiplePagesUsingPaginateNextAs() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{\"someProperty\":\"firstPage\"," +
                        "\"_links\":{\"next\":{\"href\":\"/example/foo?page=2\"}}," +
                        "\"_embedded\":{\"item\":[{\"someOtherProperty\":\"one\"},{\"someOtherProperty\":\"two\"}]" +
                        "}}");
        when(mock.apply(link("next", "http://example.com/example/foo?page=2"))).thenReturn(
                "{\"someProperty\":\"secondPage\"," +
                        "\"_links\":{\"prev\":{\"href\":\"/example/foo\"}}," +
                        "\"_embedded\":{\"item\":[{\"someOtherProperty\":\"three\"},{\"someOtherProperty\":\"four\"}]" +
                        "}}");

        // when
        final List<String> pageValues = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
                .paginateNextAs(ExtendedHalRepresentation.class, withEmbedded("item", OtherExtendedHalRepresentation.class), (Traverson pageTraverson) -> {
                    pageTraverson
                            .getResourceAs(ExtendedHalRepresentation.class)
                            .ifPresent((page) -> pageValues.add(page.someProperty));
                    pageTraverson
                            .follow("item")
                            .streamAs(OtherExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someOtherProperty));
                    return true;
                });

        // then
        assertThat(pageValues, contains("firstPage", "secondPage"));
        assertThat(values, contains("one", "two", "three", "four"));
    }

    @Test
    public void shouldIterateOverAllEmbeddedItemsOfMultiplePagesUsingPaginatePrevAs() throws IOException {
        // given
        @SuppressWarnings("unchecked")
        final LinkResolver mock = mock(LinkResolver.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{\"someProperty\":\"firstPage\"," +
                        "\"_links\":{\"prev\":{\"href\":\"/example/foo?page=2\"}}," +
                        "\"_embedded\":{\"item\":[{\"someOtherProperty\":\"one\"},{\"someOtherProperty\":\"two\"}]" +
                        "}}");
        when(mock.apply(link("prev", "http://example.com/example/foo?page=2"))).thenReturn(
                "{\"someProperty\":\"secondPage\"," +
                        "\"_links\":{\"next\":{\"href\":\"/example/foo\"}}," +
                        "\"_embedded\":{\"item\":[{\"someOtherProperty\":\"three\"},{\"someOtherProperty\":\"four\"}]" +
                        "}}");

        // when
        final List<String> pageValues = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
                .paginatePrevAs(ExtendedHalRepresentation.class, withEmbedded("item", OtherExtendedHalRepresentation.class), (Traverson pageTraverson) -> {
                    pageTraverson
                            .getResourceAs(ExtendedHalRepresentation.class)
                            .ifPresent((page) -> pageValues.add(page.someProperty));
                    pageTraverson
                            .follow("item")
                            .streamAs(OtherExtendedHalRepresentation.class)
                            .forEach(x -> values.add(x.someOtherProperty));
                    return true;
                });

        // then
        assertThat(pageValues, contains("firstPage", "secondPage"));
        assertThat(values, contains("one", "two", "three", "four"));
    }

    static class ExtendedHalRepresentation extends HalRepresentation {
        @JsonProperty
        public String someProperty;
    }

    static class OtherExtendedHalRepresentation extends HalRepresentation {
        @JsonProperty
        public String someOtherProperty;
    }
}