package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Traverson.hops;
import static de.otto.edison.hal.Traverson.traverson;
import static de.otto.edison.hal.Traverson.withVars;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TraversonTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldNotCreateParametersWithUnevenNumberOfArguments() {
        withVars("foo", "bar", "foobar");
    }

    @Test
    public void shouldCreateParameters() {
        assertThat(withVars("foo", "bar").containsKey("foo"), is(true));
    }

    @Test
    public void shouldCreateMoreParameters() {
        assertThat(withVars("foo", "bar", "oof", "rab").values(), contains("bar", "rab"));
    }

    @Test
    public void shouldGetEmptyHalRepresentation() {
        final Traverson t = traverson(s->"{}").startWith("/");
        assertThat(t.get().getLinks(), is(emptyLinks()));
        assertThat(t.get().getEmbedded(), is(emptyEmbedded()));
    }

    @Test
    public void shouldGetHalRepresentationAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}",
                "{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}, \"someProperty\":\"bar\"}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .getAs(ExtendedHalRepresentation.class);
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/foo"));
        assertThat(hal.someProperty, is("bar"));
    }

    @Test
    public void shouldGetEmbeddedHalRepresentationAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{" +
                        "\"_embedded\":{\"foo\":[{\"someProperty\":\"bar\",\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}]}," +
                        "\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}" +
                        "}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .getAs(ExtendedHalRepresentation.class);
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/foo"));
        assertThat(hal.someProperty, is("bar"));
    }

    @Test
    public void shouldFollowLink() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}",
                "{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/foo"));
    }

    @Test
    public void shouldFollowMultipleLinks() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}",
                "{\"_links\":{\"bar\":{\"href\":\"/example/bar\"}}}",
                "{\"_links\":{\"foobar\":{\"href\":\"/example/foobar\"}}}",
                "{\"_links\":{\"barbar\":{\"href\":\"/example/barbar\"}}}",
                "{\"_links\":{\"self\":{\"href\":\"/example/barbar\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow(hops("foo", "bar", "foobar", "barbar"))
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/barbar"));
    }

    @Test
    public void shouldFollowLinksAfterGettingRepresentation() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}",
                "{\"_links\":{\"bar\":{\"href\":\"/example/bar\"}}}",
                "{\"_links\":{\"self\":{\"href\":\"/example/bar\"}}}");
        final Traverson traverson = traverson(mock)
                .startWith("/example");
        // when
        traverson.get();
        traverson.follow("foo");
        traverson.get();
        final HalRepresentation hal = traverson
                .follow(hops("bar"))
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/bar"));
    }

    @Test
    public void shouldGetCurrentNodeTwice() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}",
                "{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}");
        // when
        final Traverson traverson = traverson(mock)
                .startWith("/example")
                .follow("foo");

        // then
        assertThat(traverson.get(), is(traverson.get()));
    }

    @Test
    public void shouldFollowTemplatedLink() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply("/example")).thenReturn("{\"_links\":{\"foo\":{\"templated\":true,\"href\":\"/example/foo{?test}\"}}}");
        when(mock.apply("/example/foo?test=bar")).thenReturn("{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo", withVars("test", "bar"))
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/foo"));
    }

    @Test
    public void shouldFollowMultipleTemplatedLinks() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply("/example")).thenReturn("{\"_links\":{\"foo\":{\"templated\":true,\"href\":\"/example/foo{?param1}\"}}}");
        when(mock.apply("/example/foo?param1=value1")).thenReturn("{\"_links\":{\"bar\":{\"templated\":true,\"href\":\"/example/bar{?param2}\"}}}");
        when(mock.apply("/example/bar?param2=value2")).thenReturn("{\"_links\":{\"self\":{\"href\":\"/example/bar\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow(
                        hops("foo", "bar"),
                        withVars("param1", "value1", "param2", "value2"))
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/bar"));
    }

    @Test
    public void shouldFollowLinkWithEmbeddedObjects() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{" +
                        "\"_embedded\":{\"foo\":[{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}]}," +
                        "\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}" +
                "}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/foo"));
    }

    @Test
    public void shouldStreamLinkedObjects() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply("/example/foo")).thenReturn(
                "{" +
                        "\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}" +
                "}");
        when(mock.apply("/example/foo/1")).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/1\"}}" +
                "}");
        when(mock.apply("/example/foo/2")).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/2\"}}" +
                "}");
        // when
        final List<String> hrefs = traverson(mock)
                .startWith("/example/foo")
                .follow("foo")
                .stream()
                .map(r->r.getLinks().getLinkBy("self").get().getHref())
                .collect(toList());
        // then
        assertThat(hrefs, contains("/example/foo/1","/example/foo/2"));
    }

    @Test
    public void shouldStreamLinkedObjectsAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply("/example/foo")).thenReturn(
                "{" +
                        "\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}" +
                "}");
        when(mock.apply("/example/foo/1")).thenReturn(
                "{" +
                        "\"someProperty\":\"first\",\"_links\":{\"self\":{\"href\":\"/example/foo/1\"}}" +
                "}");
        when(mock.apply("/example/foo/2")).thenReturn(
                "{" +
                        "\"someProperty\":\"second\",\"_links\":{\"self\":{\"href\":\"/example/foo/2\"}}" +
                "}");
        // when
        final List<String> hrefs = traverson(mock)
                .startWith("/example/foo")
                .follow("foo")
                .streamAs(ExtendedHalRepresentation.class)
                .map(r->r.someProperty)
                .collect(toList());
        // then
        assertThat(hrefs, contains("first","second"));
    }

    @Test
    public void shouldStreamEmbeddedObjects() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{" +
                        "\"_embedded\":{\"foo\":[{\"_links\":{\"self\":{\"href\":\"/example/foo/1\"}}},{\"_links\":{\"self\":{\"href\":\"/example/foo/2\"}}}]}," +
                        "\"_links\":{\"foo\":[{\"href\":\"/example/foo#/1\"},{\"href\":\"/example/foo/2\"}]}" +
                "}");
        // when
        final List<String> hrefs = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .stream()
                .map(r->r.getLinks().getLinkBy("self").get().getHref())
                .collect(toList());
        // then
        assertThat(hrefs, contains("/example/foo/1","/example/foo/2"));
    }

    @Test
    public void shouldStreamEmbeddedObjectsAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<String,String> mock = mock(Function.class);
        when(mock.apply(anyString())).thenReturn(
                "{" +
                        "\"_embedded\":{\"foo\":[{\"someProperty\":\"first\"},{\"someProperty\":\"second\"}]}" +
                "}");
        // when
        final List<String> hrefs = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .streamAs(ExtendedHalRepresentation.class)
                .map(r->r.someProperty)
                .collect(toList());
        // then
        assertThat(hrefs, contains("first","second"));
    }

    static class ExtendedHalRepresentation extends HalRepresentation {
        @JsonProperty
        public String someProperty;
    }
}