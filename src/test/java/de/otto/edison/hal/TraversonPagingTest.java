package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Traverson.traverson;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TraversonPagingTest {

    @Test
    public void shouldPageOverLinksUsingNext() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"next\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");
        when(mock.apply(link("next", "/example/foo?page=2"))).thenReturn(
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
                .startWith("/example/foo")
                .follow("next")
                .getResource();

        // then
        assertThat(optionalPage.isPresent(), is(true));
    }

    @Test
    public void shouldStreamLinkedItemsOfNextPage() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"next\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");
        when(mock.apply(link("next", "/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/3\"}," +
                                "{\"href\":\"/example/foo/4\"}]," +
                            "\"prev\":" +
                                "{\"href\":\"/example/foo\"}" +
                        "}" +
                "}");
        when(mock.apply(link("foo", "/example/foo/3"))).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/3\"}}" +
                "}");
        when(mock.apply(link("foo", "/example/foo/4"))).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/4\"}}" +
                "}");

        // when
        final Traverson traverson = traverson(mock);
        final Optional<HalRepresentation> optionalPage = traverson
                .startWith("/example/foo")
                .follow("next")
                .getResource();
        // then
        assertThat(traverson.follow("foo").stream().count(), is(2L));
    }

    @Test
    public void shouldNotPageAfterLastPage() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]" +
                        "}" +
                "}");

        // when we getResource the next page
        final Optional<HalRepresentation> optionalPage = traverson(mock)
                .startWith("/example/foo")
                .follow("next")
                .getResource();

        assertThat(optionalPage.isPresent(), is(false));
    }

    @Test
    public void shouldPageOverLinksUsingPrev() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo?page=2"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/3\"}," +
                                "{\"href\":\"/example/foo/4\"}]," +
                            "\"prev\":" +
                                "{\"href\":\"/example/foo\"}" +
                        "}" +
                "}");
        when(mock.apply(link("prev", "/example/foo"))).thenReturn(
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
                .startWith("/example/foo?page=2")
                .follow("prev")
                .getResource();

        // then
        assertThat(optionalPage.isPresent(), is(true));
    }

    @Test
    public void shouldNotPageBeforeFirstPage() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]" +
                        "}" +
                "}");

        // when
        final Optional<HalRepresentation> optionalPage = traverson(mock)
                .startWith("/example/foo")
                .follow("prev")
                .getResource();
        // then
        assertThat(optionalPage.isPresent(), is(false));
    }

    @Test
    public void shouldPageOverLinksUsingFirstAndLast() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"last\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");
        when(mock.apply(link("first", "/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{" +
                            "\"foo\":[" +
                                "{\"href\":\"/example/foo/1\"}," +
                                "{\"href\":\"/example/foo/2\"}]," +
                            "\"last\":" +
                                "{\"href\":\"/example/foo?page=2\"}" +
                        "}" +
                "}");
        when(mock.apply(link("last", "/example/foo?page=2"))).thenReturn(
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
                .startWith("/example/foo")
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

    static class ExtendedHalRepresentation extends HalRepresentation {
        @JsonProperty
        public String someProperty;
    }
}