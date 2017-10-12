package de.otto.edison.hal.traverson;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.edison.hal.EmbeddedTypeInfo;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
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
import static de.otto.edison.hal.traverson.Traverson.embeddedTypeInfoFor;
import static de.otto.edison.hal.traverson.Traverson.hops;
import static de.otto.edison.hal.traverson.Traverson.traverson;
import static de.otto.edison.hal.traverson.Traverson.withVars;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

public class TraversonTest {

    private static final Logger LOG = getLogger(TraversonTest.class);

    @Rule
    public TestName testName = new TestName();

    @Before
    public void logTestName() throws Throwable {
        LOG.trace("============== {} ===================", testName.getMethodName());
    }

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
        final Traverson t = traverson(s->"{}").startWith("http://example.com/");
        assertThat(t.getResource().get().getLinks(), is(emptyLinks()));
        assertThat(t.getResource().get().getEmbedded(), is(emptyEmbedded()));
    }

    ////////////////////////////////////
    // Creating Traversons:
    ////////////////////////////////////

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void shouldNotCreateTraversonFromHalRepresentationWithoutSelfLinkButWithRelativeLinks() {
        // given
        final HalRepresentation existingRepresentation = new HalRepresentation(
                linkingTo(link("search", "/example/foo"))
        );

        // when
        traverson(mock(Function.class))
                .startWith(existingRepresentation);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCreateTraversonFromContextUrlAndHalRepresentationWithoutSelfLinkButWithRelativeLinks() throws MalformedURLException {
        // given
        final HalRepresentation existingRepresentation = new HalRepresentation(
                linkingTo(link("search", "/example/foo"))
        );

        // when
        Traverson traverson = traverson(mock(Function.class))
                .startWith(new URL("http://example.com"), existingRepresentation);

        // then
        assertThat(traverson.getCurrentContextUrl(), is(new URL("http://example.com")));

        // and when
        Optional<HalRepresentation> resource = traverson.getResource();

        // then
        assertThat(resource.isPresent(), is(true));
        assertThat(traverson.getCurrentContextUrl(), is(new URL("http://example.com")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCreateTraversonFromHalRepresentationWithoutSelfLinkButWithAbsoluteLinks() throws MalformedURLException {
        // given
        final HalRepresentation existingRepresentation = new HalRepresentation(
                linkingTo(link("search", "http://example.com/example/"))
        );
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("search", "http://example.com/example/"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"http://example.com/example/foo\"}}}");

        // when
        final Traverson traverson = traverson(mock)
                .startWith(existingRepresentation)
                .follow("search");

        // then
        assertThat(traverson.getCurrentContextUrl(), is(nullValue()));

        // and when
        Optional<HalRepresentation> resource = traverson.getResource();

        // then
        assertThat(resource.isPresent(), is(true));
        assertThat(traverson.getCurrentContextUrl(), is(new URL("http://example.com/example/")));
    }


    ////////////////////////////////////
    // Following and getting resources:
    ////////////////////////////////////

    @Test
    public void shouldFollowLink() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("http://example.com/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"http://example.com/example/foo\"}}}");
        when(mock.apply(link("foo", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("foo")
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("http://example.com/example/foo"));
    }

    @Test
    public void shouldFollowRelativeLink() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("http://example.com/example/"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"foo\"}}}");
        when(mock.apply(link("foo", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example/")
                .follow("foo")
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("http://example.com/example/foo"));
    }

    @Test
    public void shouldFollowLinkMatchingPredicate() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("http://example.com/example"))).thenReturn(
                "{\"_links\":{\"item\":[{\"href\":\"http://example.com/example/foo\"},{\"href\":\"http://example.com/example/foo\",\"type\":\"text/plain\"}]}}");
        when(mock.apply(linkBuilder("item", "http://example.com/example/foo").withType("text/plain").build())).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo?type=text\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("item", havingType("text/plain"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.get().getHref(), is("http://example.com/example/foo?type=text"));
    }

    @Test
    public void shouldFollowRelativeLinkMatchingPredicate() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("http://example.com/example"))).thenReturn(
                "{\"_links\":{\"item\":[{\"href\":\"/example/foo\"},{\"href\":\"/example/foo\",\"type\":\"text/plain\"}]}}");
        when(mock.apply(linkBuilder("item", "http://example.com/example/foo").withType("text/plain").build())).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo?type=text\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("item", havingType("text/plain"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.get().getHref(), is("http://example.com/example/foo?type=text"));
    }

    @Test
    public void shouldFollowLinkStartingWithHalRepresentationHavingAbsoluteLinks() {
        // given
        final HalRepresentation existingRepresentation = new HalRepresentation(
                linkingTo(link("search", "http://example.com/example/foo"))
        );
        // and
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("search", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo\"}}}");

        // when
        final Optional<HalRepresentation> hal = traverson(mock)
                .startWith(existingRepresentation)
                .follow("search")
                .getResource();
        // then
        final Optional<Link> self = hal.get().getLinks().getLinkBy("self");
        assertThat(self.get().getHref(), is("http://example.com/example/foo"));
    }

    @Test
    public void shouldFollowLinkStartingWithHalRepresentationHavingSelfLink() {
        // given
        final HalRepresentation existingRepresentation = new HalRepresentation(
                linkingTo(
                        link("search", "/example/foo"),
                        self("http://example.com")
                )
        );
        // and
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("search", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo\"}}}");

        // when
        final Optional<HalRepresentation> hal = traverson(mock)
                .startWith(existingRepresentation)
                .follow("search")
                .getResource();
        // then
        final Optional<Link> self = hal.get().getLinks().getLinkBy("self");
        assertThat(self.get().getHref(), is("http://example.com/example/foo"));
    }

    @Test
    public void shouldFollowLinkStartingWithHalRepresentationAndContextUrl() throws MalformedURLException {
        // given
        final HalRepresentation existingRepresentation = new HalRepresentation(
                linkingTo(link("search", "/example/foo")));
        // and
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("search", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo\"}}}");

        // when
        final Optional<HalRepresentation> hal = traverson(mock)
                .startWith(new URL("http://example.com"), existingRepresentation)
                .follow("search")
                .getResource();
        // then
        final Optional<Link> self = hal.get().getLinks().getLinkBy("self");
        assertThat(self.get().getHref(), is("http://example.com/example/foo"));
    }

    @Test
    public void shouldFollowMultipleLinks() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("http://example.com/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"http://example.com/example/foo\"}}}");
        when(mock.apply(link("foo", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"bar\":{\"href\":\"/example/bar\"}}}");
        when(mock.apply(link("bar", "http://example.com/example/bar"))).thenReturn(
                "{\"_links\":{\"foobar\":{\"href\":\"/example/foobar\"}}}");
        when(mock.apply(link("foobar", "http://example.com/example/foobar"))).thenReturn(
                "{\"_links\":{\"barbar\":{\"href\":\"http://example.com/example/barbar\"}}}");
        when(mock.apply(link("barbar", "http://example.com/example/barbar"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/barbar\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow(hops("foo", "bar", "foobar", "barbar"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("http://example.com/example/barbar"));
    }

    @Test
    public void shouldFollowMultipleLinksMatchingPredicate() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("http://example.com/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}");
        when(mock.apply(link("foo", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"bar\":[{\"href\":\"/example/bar1\",\"name\":\"First\"},{\"href\":\"/example/bar2\",\"name\":\"Second\"}]}}");
        when(mock.apply(linkBuilder("bar", "http://example.com/example/bar2").withName("Second").build())).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"http://example.com/example/bar2\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("foo")
                .follow("bar", havingName("Second"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("http://example.com/example/bar2"));
    }

    @Test
    public void shouldFollowLinksAfterGettingRepresentation() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(self("http://example.com/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"http://example.com/example/foo\"}}}");
        when(mock.apply(link("foo", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"bar\":{\"href\":\"/example/bar\"}}}");
        when(mock.apply(link("bar", "http://example.com/example/bar"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/bar\"}}}");

        // when
        final Traverson traverson = traverson(mock)
                .startWith("http://example.com/example");
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
        when(mock.apply(self("http://example.com/example"))).thenReturn(
                "{\"_links\":{\"foo\":{\"href\":\"/example/foo\"}}}");
        when(mock.apply(link("foo", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"self\":{\"href\":\"/example/foo\"}}}");
        // when
        final Traverson traverson = traverson(mock)
                .startWith("http://example.com/example")
                .follow("foo");

        // then
        assertThat(traverson.getResource(), is(traverson.getResource()));
    }

    @Test
    public void shouldFollowTemplatedLink() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self","http://example.com/example"))).thenReturn("{\"_links\":{\"foo\":{\"templated\":true,\"href\":\"/example/foo{?test}\"}}}");
        when(mock.apply(link("foo", "http://example.com/example/foo?test=bar"))).thenReturn("{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("foo", withVars("test", "bar"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("http://example.com/example/foo"));
    }

    @Test
    public void shouldFollowMultipleTemplatedLinks() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "http://example.com/example"))).thenReturn("{\"_links\":{\"foo\":{\"templated\":true,\"href\":\"/example/foo{?param1}\"}}}");
        when(mock.apply(link("foo", "http://example.com/example/foo?param1=value1"))).thenReturn("{\"_links\":{\"bar\":{\"templated\":true,\"href\":\"/example/bar{?param2}\"}}}");
        when(mock.apply(link("bar", "http://example.com/example/bar?param2=value2"))).thenReturn("{\"_links\":{\"self\":{\"href\":\"http://example.com/example/bar\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow(
                        hops("foo", "bar"),
                        withVars("param1", "value1", "param2", "value2"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("http://example.com/example/bar"));
    }

    @Test
    public void shouldFollowTemplatedLinksWithPredicates() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "http://example.com/example")))
                .thenReturn("{\"_links\":{\"foo\":[" +
                        "{\"templated\":true,\"type\":\"text/plain\",\"href\":\"/example/foo1{?param1}\"}," +
                        "{\"templated\":true,\"type\":\"text/html\",\"href\":\"/example/foo2{?param1}\"}]}}");
        when(mock.apply(linkBuilder("foo", "http://example.com/example/foo2?param1=value1").withType("text/html").build()))
                .thenReturn("{\"_links\":{\"bar\":{\"templated\":true,\"href\":\"/example/bar{?param2}\"}}}");
        when(mock.apply(link("bar", "http://example.com/example/bar?param2=value2")))
                .thenReturn("{\"_links\":{\"self\":{\"href\":\"http://example.com/example/bar\"}}}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow(
                        hops("foo", "bar"),
                        optionallyHavingType("text/html"),
                        withVars("param1", "value1", "param2", "value2"))
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("http://example.com/example/bar"));
    }

    @Test
    public void shouldFollowLinkWithEmbeddedObjects() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                        "\"_embedded\":{\"foo\":[{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo\"}}}]}" +
                "}");
        // when
        final HalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("foo")
                .getResource()
                .get();
        // then
        final Optional<Link> self = hal.getLinks().getLinkBy("self");
        assertThat(self.isPresent(), is(true));
        assertThat(self.get().getHref(), is("http://example.com/example/foo"));
    }

    //////////////////////////
    // Streaming
    //////////////////////////

    @Test
    public void shouldStreamLinkedObjects() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}" +
                "}");
        when(mock.apply(link("foo", "http://example.com/example/foo/1"))).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/1\"}}" +
                "}");
        when(mock.apply(link("foo", "http://example.com/example/foo/2"))).thenReturn(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"/example/foo/2\"}}" +
                "}");
        // when
        final List<String> hrefs = traverson(mock)
                .startWith("http://example.com/example/foo")
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
                .startWith("http://example.com/example")
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
                .startWith("http://example.com/example")
                .follow("foo")
                .getResource();
        // then
        assertThat(hal.isPresent(), is(false));
        assertThat(traverson.getLastError(), is(traversionError(NOT_FOUND, "Error fetching json response from http://example.com/example: Kawummmm!", expectedException)));
    }

    @Test
    public void shouldReportErrorOnBrokenJsonFormat() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn("{{}");
        // when
        final Traverson traverson = traverson(mock);
        final Optional<HalRepresentation> hal = traverson
                .startWith("http://example.com/example")
                .follow("foo")
                .getResource();
        // then
        assertThat(hal.isPresent(), is(false));
        assertThat(traverson.getLastError().getType(), is(INVALID_JSON));
        assertThat(traverson.getLastError().getMessage(), startsWith("Unable to parse {{} returned from http://example.com/example"));
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
                .startWith("http://example.com/example")
                .getResource();
        // then
        assertThat(hal.isPresent(), is(false));
        assertThat(traverson.getLastError().getType(), is(INVALID_JSON));
        assertThat(traverson.getLastError().getMessage(), startsWith("Unable to parse {\"_links\":{\"href\":\"/example/foo/1\"}} returned from"));
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
                .startWith("http://example.com/example")
                .getResource();
        // then
        assertThat(hal.isPresent(), is(false));
        assertThat(traverson.getLastError().getType(), is(INVALID_JSON));
        assertThat(traverson.getLastError().getMessage(), startsWith("Unable to parse {\"_embedded\":{\"count\":42}} returned from http://example.com/example"));
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
                .startWith("http://example.com/example")
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
                .startWith("http://example.com/example")
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
    public void shouldGetDeeplyNestedEmbeddedHalRepresentationWithSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                "   \"_embedded\":{" +
                "       \"foo\":[{" +
                "           \"someProperty\":\"foo value\"," +
                "           \"_embedded\":{" +
                "               \"bar\":[{" +
                "                   \"someProperty\":\"bar value\"" +
                "               }]" +
                "           }" +
                "       }]" +
                "   }" +
                "}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("foo")
                .getResourceAs(ExtendedHalRepresentation.class, withEmbedded("bar", ExtendedHalRepresentation.class))
                .get();
        // then
        assertThat(hal.someProperty, is("foo value"));
        assertThat(hal.getEmbedded().getItemsBy("bar", ExtendedHalRepresentation.class).get(0).someProperty, is("bar value"));
    }

    @Test
    public void shouldGetDeeplyNestedEmbeddedHalRepresentationWithMultipleSubtypes() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                "   \"_embedded\":{" +
                "       \"foo\":[{" +
                "           \"someProperty\":\"foo value\"," +
                "           \"_embedded\":{" +
                "               \"bar\":{" +
                "                   \"someProperty\":\"bar value\"" +
                "               }," +
                "               \"foobar\":[{" +
                "                   \"someOtherProperty\":\"foobar value\"" +
                "               }]" +
                "           }" +
                "       }]" +
                "   }" +
                "}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("foo")
                .getResourceAs(ExtendedHalRepresentation.class,
                        withEmbedded("bar", ExtendedHalRepresentation.class),
                        withEmbedded("foobar", OtherExtendedHalRepresentation.class)
                )
                .get();
        // then
        assertThat(hal.someProperty, is("foo value"));
        assertThat(hal.getEmbedded().getItemsBy("bar", ExtendedHalRepresentation.class).get(0).someProperty, is("bar value"));
        assertThat(hal.getEmbedded().getItemsBy("foobar", OtherExtendedHalRepresentation.class).get(0).someOtherProperty, is("foobar value"));
    }

    @Test
    public void shouldGetDeeplyNestedEmbeddedHalRepresentationWithCuriesWithSubtypeFromSingleDocument() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                "   \"_links\":{\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]}," +
                "   \"_embedded\":{" +
                "       \"x:foo\":[{" +
                "           \"someProperty\":\"foo value\"," +
                "           \"_embedded\":{" +
                "               \"http://example.com/rels/bar\":[{" +
                "                   \"someProperty\":\"bar value\"" +
                "               }]" +
                "           }" +
                "       }]" +
                "   }" +
                "}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("http://example.com/rels/foo")
                .getResourceAs(ExtendedHalRepresentation.class, withEmbedded("x:bar", ExtendedHalRepresentation.class))
                .get();
        // then
        assertThat(hal.someProperty, is("foo value"));
        assertThat(hal.getEmbedded().getItemsBy("http://example.com/rels/bar", ExtendedHalRepresentation.class).get(0).someProperty, is("bar value"));
    }

    @Test
    public void shouldGetDeeplyNestedEmbeddedHalRepresentationWithCuriesWithMultipleSubtypesFromSingleDocument() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                "   \"_links\":{\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]}," +
                "   \"_embedded\":{" +
                "       \"x:foo\":[{" +
                "           \"someProperty\":\"foo value\"," +
                "           \"_embedded\":{" +
                "               \"http://example.com/rels/bar\":[{" +
                "                   \"someProperty\":\"bar value\"" +
                "               }]," +
                "               \"x:foobar\":{" +
                "                   \"someProperty\":\"foobar value\"" +
                "               }" +
                "           }" +
                "       }]" +
                "   }" +
                "}");
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow("http://example.com/rels/foo")
                .getResourceAs(ExtendedHalRepresentation.class,
                        withEmbedded("x:bar", ExtendedHalRepresentation.class),
                        withEmbedded("http://example.com/rels/foobar", ExtendedHalRepresentation.class))
                .get();
        // then
        assertThat(hal.someProperty, is("foo value"));
        assertThat(hal.getEmbedded().getItemsBy("http://example.com/rels/bar", ExtendedHalRepresentation.class).get(0).someProperty, is("bar value"));
        assertThat(hal.getEmbedded().getItemsBy("http://example.com/rels/foobar", ExtendedHalRepresentation.class).get(0).someProperty, is("foobar value"));
    }

    @Test
    public void shouldGetDeeplyNestedEmbeddedHalRepresentationWithCuriesWithSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                "   \"_links\":{" +
                        "\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]," +
                        "\"x:foo\":{\"href\":\"http://example.com/foo\"}" +
                "   }" +
                "}",
        "{" +
                "   \"_links\":{\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]}," +
                "   \"_embedded\":{" +
                "       \"x:bar\":[{" +
                "           \"someProperty\":\"bar value\"," +
                "           \"_embedded\":{" +
                "               \"http://example.com/rels/foobar\":[{" +
                "                   \"someProperty\":\"foobar value\"" +
                "               }]" +
                "           }" +
                "       }]" +
                "   }" +
                "}"
        );
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow(asList("http://example.com/rels/foo", "http://example.com/rels/bar"))
                .getResourceAs(ExtendedHalRepresentation.class, withEmbedded("x:foobar", ExtendedHalRepresentation.class))
                .get();
        // then
        assertThat(hal.someProperty, is("bar value"));
        assertThat(hal.getEmbedded().getItemsBy("http://example.com/rels/foobar", ExtendedHalRepresentation.class).get(0).someProperty, is("foobar value"));
    }

    @Test
    public void shouldGetDeeplyNestedEmbeddedHalRepresentationWithCuriesWithMultipleSubtypes() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                "   \"_links\":{" +
                        "\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]," +
                        "\"x:foo\":{\"href\":\"http://example.com/foo\"}" +
                "   }" +
                "}",
        "{" +
                "   \"_links\":{\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]}," +
                "   \"_embedded\":{" +
                "       \"x:bar\":[{" +
                "           \"someProperty\":\"bar value\"," +
                "           \"_embedded\":{" +
                "               \"http://example.com/rels/foobar\":[{" +
                "                   \"someProperty\":\"foobar value\"" +
                "               }]," +
                "               \"http://example.com/rels/barbar\":[{" +
                "                   \"someOtherProperty\":\"barbar value\"" +
                "               }]" +
                "           }" +
                "       }]" +
                "   }" +
                "}"
        );
        // when
        final ExtendedHalRepresentation hal = traverson(mock)
                .startWith("http://example.com/example")
                .follow(asList("http://example.com/rels/foo", "http://example.com/rels/bar"))
                .getResourceAs(ExtendedHalRepresentation.class,
                        withEmbedded("x:foobar", ExtendedHalRepresentation.class),
                        withEmbedded("x:barbar", OtherExtendedHalRepresentation.class)
                )
                .get();
        // then
        assertThat(hal.someProperty, is("bar value"));
        assertThat(hal.getEmbedded().getItemsBy("http://example.com/rels/foobar", ExtendedHalRepresentation.class).get(0).someProperty, is("foobar value"));
        assertThat(hal.getEmbedded().getItemsBy("http://example.com/rels/barbar", OtherExtendedHalRepresentation.class).get(0).someOtherProperty, is("barbar value"));
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
                .startWith("http://example.com/example")
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
                .startWith("http://example.com/example")
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
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{" +
                        "\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}" +
                        "}");
        when(mock.apply(link("foo","http://example.com/example/foo/1"))).thenReturn(
                "{" +
                        "\"someProperty\":\"first\",\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo/1\"}}" +
                        "}");
        when(mock.apply(link("foo","http://example.com/example/foo/2"))).thenReturn(
                "{" +
                        "\"someProperty\":\"second\",\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo/2\"}}" +
                        "}");
        // when
        final List<String> hrefs = traverson(mock)
                .startWith("http://example.com/example/foo")
                .follow("foo")
                .streamAs(ExtendedHalRepresentation.class)
                .map(r->r.someProperty)
                .collect(toList());
        // then
        assertThat(hrefs, contains("first","second"));
    }

    @Test
    public void shouldStreamLinkedObjectsWithExtendedSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}}");
        when(mock.apply(link("foo","http://example.com/example/foo/1"))).thenReturn(
                "{\"someProperty\":\"first\",\"_embedded\":{\"bar\":{\"someOtherProperty\":\"firstBar\"}}}");
        when(mock.apply(link("foo","http://example.com/example/foo/2"))).thenReturn(
                "{\"someProperty\":\"second\",\"_embedded\":{\"bar\":{\"someOtherProperty\":\"secondBar\"}}}");
        // when
        final List<String> hrefs = new ArrayList<>();
        final List<String> embeddedBars = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
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
    public void shouldStreamLinkedObjectsWithMultipleExtendedSubtypes() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(link("self", "http://example.com/example/foo"))).thenReturn(
                "{\"_links\":{\"foo\":[{\"href\":\"/example/foo/1\"},{\"href\":\"/example/foo/2\"}]}}");
        when(mock.apply(link("foo","http://example.com/example/foo/1"))).thenReturn(
                "{\"someProperty\":\"first\",\"_embedded\":{\"bar\":{\"someOtherProperty\":\"firstBar\"}, \"foobar\":{\"someProperty\":\"firstFooBar\"}}}");
        when(mock.apply(link("foo","http://example.com/example/foo/2"))).thenReturn(
                "{\"someProperty\":\"second\",\"_embedded\":{\"bar\":{\"someOtherProperty\":\"secondBar\"}, \"foobar\":{\"someProperty\":\"secondFooBar\"}}}");
        // when
        final List<String> hrefs = new ArrayList<>();
        final List<String> embeddedBars = new ArrayList<>();
        traverson(mock)
                .startWith("http://example.com/example/foo")
                .follow("foo")
                .streamAs(ExtendedHalRepresentation.class,
                        withEmbedded("bar", OtherExtendedHalRepresentation.class),
                        withEmbedded("foobar", ExtendedHalRepresentation.class))
                .forEach(e -> {
                    hrefs.add(e.someProperty);
                    embeddedBars.add(e.getEmbedded().getItemsBy("bar", OtherExtendedHalRepresentation.class).get(0).someOtherProperty);
                    embeddedBars.add(e.getEmbedded().getItemsBy("foobar", ExtendedHalRepresentation.class).get(0).someProperty);
                });
        // then
        assertThat(hrefs, contains("first","second"));
        assertThat(embeddedBars, contains("firstBar", "firstFooBar", "secondBar", "secondFooBar"));
    }

    @Test
    public void shouldStreamEmbeddedObjectsWithSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                        "\"_embedded\":{\"foo\":[{\"someProperty\":\"first\"},{\"someProperty\":\"second\"}]}" +
                        "}");
        // when
        final List<String> hrefs = traverson(mock)
                .startWith("http://example.com/example")
                .follow("foo")
                .streamAs(ExtendedHalRepresentation.class)
                .map(r->r.someProperty)
                .collect(toList());
        // then
        assertThat(hrefs, contains("first","second"));
    }

    @Test
    public void shouldStreamDeeplyNestedEmbeddedObjectsWithCuriesAsSubtype() {
        // given
        @SuppressWarnings("unchecked")
        final Function<Link,String> mock = mock(Function.class);
        when(mock.apply(any(Link.class))).thenReturn(
                "{" +
                        "   \"_links\":{" +
                        "\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]," +
                        "\"x:foo\":{\"href\":\"http://example.com/foo\"}" +
                        "   }" +
                        "}",
                "{" +
                        "   \"_links\":{\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]}," +
                        "   \"_embedded\":{" +
                        "       \"x:bar\":[" +
                        "           {" +
                        "               \"someProperty\":\"foo\"," +
                        "               \"_embedded\":{" +
                        "                   \"http://example.com/rels/foobar\":[" +
                        "                       {\"someOtherProperty\":\"value1\"}" +
                        "                   ]" +
                        "               }" +
                        "           }," +
                        "           {" +
                        "               \"someProperty\":\"bar\"," +
                        "               \"_embedded\":{" +
                        "                   \"x:foobar\":[" +
                        "                       {\"someOtherProperty\":\"value2\"}" +
                        "                   ]" +
                        "               }" +
                        "           }" +
                        "       ]" +
                        "   }" +
                        "}"
        );
        // when
        final List<String> barValues = traverson(mock)
                .startWith("http://example.com/example")
                .follow(asList("http://example.com/rels/foo", "http://example.com/rels/bar"))
                .streamAs(ExtendedHalRepresentation.class, withEmbedded("x:foobar", OtherExtendedHalRepresentation.class))
                .map(r->r.someProperty + " " + r.getEmbedded().getItemsBy("http://example.com/rels/foobar", OtherExtendedHalRepresentation.class).get(0).someOtherProperty)
                .collect(toList());
        // then
        assertThat(barValues, contains("foo value1", "bar value2"));
    }


    @Test
    public void shouldBuildTypeInfoForSingleHopWithoutEmbeddedTypeInfo() {
        final EmbeddedTypeInfo typeInfo = embeddedTypeInfoFor(
                asList(hop("foo")),
                ExtendedHalRepresentation.class, null);
        assertThat(typeInfo.getRel(), is("foo"));
        assertThat(typeInfo.getType().getSimpleName(), is("ExtendedHalRepresentation"));
        assertThat(typeInfo.getNestedTypeInfo(), is(empty()));
    }

    @Test
    public void shouldBuildTypeInfoForSingleHop() {
        final EmbeddedTypeInfo typeInfo = embeddedTypeInfoFor(
                asList(hop("foo")),
                ExtendedHalRepresentation.class,
                singletonList(
                        withEmbedded("bar", OtherExtendedHalRepresentation.class)
                ));
        assertThat(typeInfo.getRel(), is("foo"));
        assertThat(typeInfo.getType().getSimpleName(), is("ExtendedHalRepresentation"));
        assertThat(typeInfo.getNestedTypeInfo().get(0).getRel(), is("bar"));
        assertThat(typeInfo.getNestedTypeInfo().get(0).getType().getSimpleName(), is("OtherExtendedHalRepresentation"));
    }

    @Test
    public void shouldBuildTypeInfoForSingleHopWithMultipleNestedTypeInfos() {
        final EmbeddedTypeInfo typeInfo = embeddedTypeInfoFor(
                asList(hop("foo")),
                ExtendedHalRepresentation.class,
                asList(
                        withEmbedded("bar", ExtendedHalRepresentation.class),
                        withEmbedded("foobar", OtherExtendedHalRepresentation.class)
                ));
        assertThat(typeInfo.getRel(), is("foo"));
        assertThat(typeInfo.getType().getSimpleName(), is("ExtendedHalRepresentation"));
        assertThat(typeInfo.getNestedTypeInfo().get(0).getRel(), is("bar"));
        assertThat(typeInfo.getNestedTypeInfo().get(0).getType().getSimpleName(), is("ExtendedHalRepresentation"));
        assertThat(typeInfo.getNestedTypeInfo().get(1).getRel(), is("foobar"));
        assertThat(typeInfo.getNestedTypeInfo().get(1).getType().getSimpleName(), is("OtherExtendedHalRepresentation"));
    }

    @Test
    public void shouldBuildTypeInfoForMultipleHopsWithoutEmbeddedTypeInfo() {
        final EmbeddedTypeInfo typeInfo = embeddedTypeInfoFor(
                asList(hop("foo"), hop("bar"), hop("foobar")),
                ExtendedHalRepresentation.class, null);
        assertThat(typeInfo.getRel(), is("foo"));
        assertThat(typeInfo.getType().getSimpleName(), is("HalRepresentation"));
        EmbeddedTypeInfo nested = typeInfo.getNestedTypeInfo().get(0);
        assertThat(nested.getRel(), is("bar"));
        assertThat(nested.getType().getSimpleName(), is("HalRepresentation"));
        nested = nested.getNestedTypeInfo().get(0);
        assertThat(nested.getRel(), is("foobar"));
        assertThat(nested.getType().getSimpleName(), is("ExtendedHalRepresentation"));
        assertThat(nested.getNestedTypeInfo(), is(empty()));
    }

    @Test
    public void shouldBuildTypeInfoForMultipleHops() {
        final EmbeddedTypeInfo typeInfo = embeddedTypeInfoFor(
                asList(hop("foo"), hop("bar"), hop("foobar")),
                ExtendedHalRepresentation.class,
                singletonList(withEmbedded("barbar", OtherExtendedHalRepresentation.class)));
        assertThat(typeInfo.getRel(), is("foo"));
        assertThat(typeInfo.getType().getSimpleName(), is("HalRepresentation"));
        EmbeddedTypeInfo nested = typeInfo.getNestedTypeInfo().get(0);
        assertThat(nested.getRel(), is("bar"));
        assertThat(nested.getType().getSimpleName(), is("HalRepresentation"));
        nested = nested.getNestedTypeInfo().get(0);
        assertThat(nested.getRel(), is("foobar"));
        assertThat(nested.getType().getSimpleName(), is("ExtendedHalRepresentation"));
        nested = nested.getNestedTypeInfo().get(0);
        assertThat(nested.getRel(), is("barbar"));
        assertThat(nested.getType().getSimpleName(), is("OtherExtendedHalRepresentation"));
    }

    private Traverson.Hop hop(final String rel) {
        return new Traverson.Hop(rel, null, null);
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