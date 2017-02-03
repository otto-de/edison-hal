package de.otto.edison.hal.traverson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.linkBuilder;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.LinkPredicates.havingName;
import static de.otto.edison.hal.LinkPredicates.havingType;
import static de.otto.edison.hal.LinkPredicates.optionallyHavingType;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.traverson.TraversionError.Type.INVALID_JSON;
import static de.otto.edison.hal.traverson.TraversionError.Type.NOT_FOUND;
import static de.otto.edison.hal.traverson.TraversionError.traversionError;
import static de.otto.edison.hal.traverson.Traverson.hops;
import static de.otto.edison.hal.traverson.Traverson.traverson;
import static de.otto.edison.hal.traverson.Traverson.withVars;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TraversonTest {


    ////////////////////////////////////
    // Basics:
    ////////////////////////////////////

    @Test(expected = IllegalArgumentException.class)
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
        assertThat(t.getResource().get().getLinks(), is(emptyLinks()));
        assertThat(t.getResource().get().getEmbedded(), is(emptyEmbedded()));
    }

    ////////////////////////////////////
    // Following and getting resources:
    ////////////////////////////////////

    @Test
    public void shouldFollowLink() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}");
        when(mock.apply(link("foo", "/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/foo"));
    }

    @Test
    public void shouldFollowLinkMatchingPredicate() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("/example"))).thenReturn(
                "{\"_links\":{\"item\":[{\"href\":\"/example/foo\"},{\"href\":\"/example/foo\",\"type\":\"text/plain\"}]}}");
        when(mock.apply(linkBuilder("item", "/example/foo").withType("text/plain").build())).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/foo?type=text\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("item", havingType("text/plain"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.get().getHref(), is("/example/foo?type=text"));

    }

    @Test
    public void shouldFollowLinkStartingWithHalRepresentation() {
        // given
        final HalRepresentation existingRepresentation = new HalRepresentation(
                linkingTo(link("search", "/example/foo"))
        );
        // and
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("search", "/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}");

        // when
        final Optional<HalRepresentation> hal = traverson(mock)
                .startWith(existingRepresentation)
                .follow("search")
                .getResource();
        // then
        final Optional<Link> self = hal.get().getLinks().getLinkBy("self");
        assertThat(self.get().getHref(), is("/example/foo"));
    }

    @Test
    public void shouldFollowMultipleLinks() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}");
        when(mock.apply(link("foo", "/example/foo"))).thenReturn(
                "{\"_links\":{\"bar\":{\"href\":\"/example/bar\"}}}");
        when(mock.apply(link("bar", "/example/bar"))).thenReturn(
                "{\"_links\":{\"foobar\":{\"href\":\"/example/foobar\"}}}");
        when(mock.apply(link("foobar", "/example/foobar"))).thenReturn(
                "{\"_links\":{\"barbar\":{\"href\":\"/example/barbar\"}}}");
        when(mock.apply(link("barbar", "/example/barbar"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/barbar\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow(hops("foo", "bar", "foobar", "barbar"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/barbar"));
    }

    @Test
    public void shouldFollowMultipleLinksMatchingPredicate() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}");
        when(mock.apply(link("foo", "/example/foo"))).thenReturn(
                "{\"_links\":{\"bar\":[{\"href\":\"/example/bar1\",\"name\":\"First\"},{\"href\":\"/example/bar2\",\"name\":\"Second\"}]}}");
        when(mock.apply(linkBuilder("bar", "/example/bar2").withName("Second").build())).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/bar2\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .follow("bar", havingName("Second"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/bar2"));
    }

    @Test
    public void shouldFollowLinksAfterGettingRepresentation() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}");
        when(mock.apply(link("foo", "/example/foo"))).thenReturn(
                "{\"_links\":{\"bar\":{\"href\":\"/example/bar\"}}}");
        when(mock.apply(link("bar", "/example/bar"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/bar\"}}}");

        // when
        final Traverson traverson = traverson(mock)
                .startWith("/example");
        traverson.getResource();
        // and
        traverson.follow("foo");
        traverson.getResource();
        final HalRepresentation hal = traverson
                .follow(hops("bar"))
                .getResource()
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
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}");
        when(mock.apply(link("foo", "/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}");
        // when
        final Traverson traverson = traverson(mock)
                .startWith("/example")
                .follow("foo");

        // then
        assertThat(traverson.getResource(), is(traverson.getResource()));
    }

    @Test
    public void shouldFollowTemplatedLink() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self","/example"))).thenReturn("{\"_links\":{\"foo\":{\"templated\":true,\"href\":\"/example/foo{?test}\"}}}");
        when(mock.apply(link("foo", "/example/foo?test=bar"))).thenReturn("{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo", withVars("test", "bar"))
                .getResource()
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
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example"))).thenReturn("{\"_links\":{\"foo\":{\"templated\":true,\"href\":\"/example/foo{?param1}\"}}}");
        when(mock.apply(link("foo", "/example/foo?param1=value1"))).thenReturn("{\"_links\":{\"bar\":{\"templated\":true,\"href\":\"/example/bar{?param2}\"}}}");
        when(mock.apply(link("bar", "/example/bar?param2=value2"))).thenReturn("{\"_links\":{\"self\":{\"href\":\"/example/bar\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow(
                        hops("foo", "bar"),
                        withVars("param1", "value1", "param2", "value2"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/bar"));
    }

    @Test
    public void shouldFollowTemplatedLinksWithPredicates() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example")))
                .thenReturn("{\"_links\":{\"foo\":[" +
                        "{\"templated\":true,\"type\":\"text/plain\",\"href\":\"/example/foo1{?param1}\"}," +
                        "{\"templated\":true,\"type\":\"text/html\",\"href\":\"/example/foo2{?param1}\"}]}}");
        when(mock.apply(linkBuilder("foo", "/example/foo2?param1=value1").withType("text/html").build()))
                .thenReturn("{\"_links\":{\"bar\":{\"templated\":true,\"href\":\"/example/bar{?param2}\"}}}");
        when(mock.apply(link("bar", "/example/bar?param2=value2")))
                .thenReturn("{\"_links\":{\"self\":{\"href\":\"/example/bar\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow(
                        hops("foo", "bar"),
                        optionallyHavingType("text/html"),
                        withVars("param1", "value1", "param2", "value2"))
                .getResource()
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
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                        "\"_embedded\":{\"foo\":[{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}]}" +
                "}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/foo"));
    }

    //////////////////////////
    // Streaming
    //////////////////////////

    @Test
    public void shouldStreamLinkedObjects() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}" +
                "}");
        when(mock.apply(link("foo", "/example/foo/1"))).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/1\"}}" +
                "}");
        when(mock.apply(link("foo", "/example/foo/2"))).thenReturn(
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
    public void shouldStreamEmbeddedObjects() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
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

    //////////////////////////////////
    // Error Handling
    //////////////////////////////////

    @Test
    public void shouldReportErrorIfResourceDoesNotExist() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        final RuntimeException expectedException = new RuntimeException("Kawummmm!");
        when(mock.apply(any(Link.class))).thenThrow(expectedException);
        // when
        final Traverson traverson = traverson(mock);
        final Optional<HalRepresentation> hal = traverson
                .startWith("/example")
                .follow("foo")
                .getResource();
        // then
        assertThat(hal.isPresent(), is(false));
        assertThat(traverson.getLastError(), is(traversionError(NOT_FOUND, "Kawummmm!", expectedException)));
    }

    @Test
    public void shouldReportErrorOnMissingLinkedResource() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn("{{}");
        // when
        final Traverson traverson = traverson(mock);
        final Optional<HalRepresentation> hal = traverson
                .startWith("/example")
                .follow("foo")
                .getResource();
        // then
        assertThat(hal.isPresent(), is(false));
        assertThat(traverson.getLastError().getType(), is(INVALID_JSON));
        assertThat(traverson.getLastError().getMessage(), startsWith("Document returned from /example is not in application/hal+json format"));
        assertThat(traverson.getLastError().getCause().get(), instanceOf(JsonParseException.class));
    }

    @Test
    public void shouldReportErrorOnInvalidHalLinkFormat() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                        "\"_links\":{\"href\":\"/example/foo/1\"}" +
                "}");
        // when
        final Traverson traverson = traverson(mock);
        final Optional<HalRepresentation> hal = traverson
                .startWith("/example")
                .getResource();
        // then
        assertThat(hal.isPresent(), is(false));
        assertThat(traverson.getLastError().getType(), is(INVALID_JSON));
        assertThat(traverson.getLastError().getMessage(), startsWith("Document returned from /example is not in application/hal+json format"));
        assertThat(traverson.getLastError().getCause().get(), instanceOf(JsonMappingException.class));
    }

    @Test
    public void shouldReportErrorOnBrokenHalEmbeddedFormat() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                        "\"_embedded\":{\"count\":42}" +
                "}");
        // when
        final Traverson traverson = traverson(mock);
        final Optional<HalRepresentation> hal = traverson
                .startWith("/example")
                .getResource();
        // then
        assertThat(hal.isPresent(), is(false));
        assertThat(traverson.getLastError().getType(), is(INVALID_JSON));
        assertThat(traverson.getLastError().getMessage(), startsWith("Document returned from /example is not in application/hal+json format"));
        assertThat(traverson.getLastError().getCause().get(), instanceOf(JsonMappingException.class));
    }

    //////////////////////////////////
    // Subtypes of HalRepresentation
    //////////////////////////////////

    @Test
    public void shouldGetHalRepresentationAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}",
                "{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}, \"someProperty\":\"bar\"}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .getResourceAs(ExtendedHalRepresentation.class)
                .get();
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
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                        "\"_embedded\":{\"foo\":[{\"someProperty\":\"bar\",\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}]}," +
                        "\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}" +
                        "}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("/example")
                .follow("foo")
                .getResourceAs(ExtendedHalRepresentation.class)
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("/example/foo"));
        assertThat(hal.someProperty, is("bar"));
    }

    @Test
    public void shouldGetSingleEmbeddedHalRepresentationAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{\"_embedded\":{\"foo\":[{\"someProperty\":\"bar\"}]}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("/example")
                .getResourceAs(HalRepresentation.class, withEmbedded("foo", ExtendedHalRepresentation.class))
                .get();
        // then
        assertThat(hal.getEmbedded().getItemsBy("foo", ExtendedHalRepresentation.class).get(0).someProperty, is("bar"));
    }

    @Test
    public void shouldGetSingleExtendedHalWithEmbeddedHalRepresentationAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{\"someProperty\":\"42\", \"_embedded\":{\"foo\":[{\"someOtherProperty\":\"0815\"}]}}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("/example")
                .getResourceAs(ExtendedHalRepresentation.class, withEmbedded("foo", OtherExtendedHalRepresentation.class))
                .get();
        // then
        assertThat(hal.someProperty, is("42"));
        assertThat(hal.getEmbedded().getItemsBy("foo", OtherExtendedHalRepresentation.class).get(0).someOtherProperty, is("0815"));
    }

    @Test
    public void shouldStreamLinkedObjectsAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}" +
                        "}");
        when(mock.apply(link("foo","/example/foo/1"))).thenReturn(
                "{" +
                        "\"someProperty\":\"first\",\"_links\":{\"self\":{\"href\":\"/example/foo/1\"}}" +
                        "}");
        when(mock.apply(link("foo","/example/foo/2"))).thenReturn(
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
    public void shouldStreamLinkedObjectsAsExtendedSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "/example/foo"))).thenReturn(
                "{\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}}");
        when(mock.apply(link("foo","/example/foo/1"))).thenReturn(
                "{\"someProperty\":\"first\",\"_embedded\":{\"bar\":{\"someOtherProperty\":\"firstBar\"}}}");
        when(mock.apply(link("foo","/example/foo/2"))).thenReturn(
                "{\"someProperty\":\"second\",\"_embedded\":{\"bar\":{\"someOtherProperty\":\"secondBar\"}}}");
        // when
        final List<String> hrefs = new ArrayList<>();
        final List<String> embeddedBars = new ArrayList<>();
        traverson(mock)
                .startWith("/example/foo")
                .follow("foo")
                .streamAs(ExtendedHalRepresentation.class, withEmbedded("bar", OtherExtendedHalRepresentation.class))
                .forEach(e -> {
                    hrefs.add(e.someProperty);
                    embeddedBars.add(e.getEmbedded().getItemsBy("bar", OtherExtendedHalRepresentation.class).get(0).someOtherProperty);
                });
        // then
        assertThat(hrefs, contains("first","second"));
        assertThat(embeddedBars, contains("firstBar","secondBar"));
    }

    @Test
    public void shouldStreamEmbeddedObjectsAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
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

    static class OtherExtendedHalRepresentation extends HalRepresentation {
        @JsonProperty
        public String someOtherProperty;
    }
}