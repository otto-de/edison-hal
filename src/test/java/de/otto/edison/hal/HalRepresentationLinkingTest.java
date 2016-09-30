package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static de.otto.edison.hal.Link.collection;
import static de.otto.edison.hal.Link.item;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.linkBuilder;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class HalRepresentationLinkingTest {

    @Test
    public void shouldRenderSimpleHalRepresentationWithoutLinks() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation() {
            public final String first = "foo";
            public final String second = "bar";
        };
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"first\":\"foo\",\"second\":\"bar\"}"));

    }

    @Test
    public void shouldNotRenderEmptyLinks() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(emptyLinks()) {
            public final String first = "foo";
            public final String second = "bar";
        };
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"first\":\"foo\",\"second\":\"bar\"}"));

    }

    @Test
    public void shouldRenderSelfLinkAndProperty() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        self("http://example.org/test/foo"))
        ) {
            public final String test = "foo";
        };
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"test\":\"foo\",\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}}}"));
    }

    @Test
    public void shouldRenderMultipleLinks() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        self("http://example.org/test/foo"),
                        collection("http://example.org/test"))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"},\"collection\":{\"href\":\"http://example.org/test\"}}}"));
    }

    @Test
    public void shouldRenderMultipleLinksForSingleRel() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        link("test", "http://example.org/test/foo"),
                        link("test", "http://example.org/test/bar"))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"test\":[{\"href\":\"http://example.org/test/foo\"},{\"href\":\"http://example.org/test/bar\"}]}}"));
    }

    @Test
    public void shouldRenderTemplatedLink() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        link("search", "/test{?bar}"))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"search\":{\"href\":\"/test{?bar}\",\"templated\":true}}}"));
    }

    @Test
    public void shouldRenderEvenMoreComplexLinks() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        linkBuilder("search", "/test{?bar}")
                                .withType("application/hal+json")
                                .withHrefLang("de-DE")
                                .withTitle("Some Title")
                                .withName("Foo")
                                .withProfile("http://example.org/profiles/test-profile")
                                .withDeprecation("http://example.org/deprecations/4711.html")
                                .build(),
                        linkBuilder("foo", "/test/bar")
                                .withType("application/hal+json")
                                .withHrefLang("de-DE")
                                .withTitle("Some Title")
                                .withName("Foo")
                                .withProfile("http://example.org/profiles/test-profile")
                                .withDeprecation("http://example.org/deprecations/4711.html")
                                .build()
        ));
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{" + "" +
                "\"search\":{\"href\":\"/test{?bar}\",\"templated\":true,\"type\":\"application/hal+json\",\"hreflang\":\"de-DE\",\"title\":\"Some Title\",\"name\":\"Foo\",\"deprecation\":\"http://example.org/deprecations/4711.html\",\"profile\":\"http://example.org/profiles/test-profile\"}," +
                "\"foo\":{\"href\":\"/test/bar\",\"type\":\"application/hal+json\",\"hreflang\":\"de-DE\",\"title\":\"Some Title\",\"name\":\"Foo\",\"deprecation\":\"http://example.org/deprecations/4711.html\",\"profile\":\"http://example.org/profiles/test-profile\"}" +
                "}}"));
    }

    @Test
    public void shouldBeAbleToAddLinksAfterConstruction() {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        self("/"),
                        item("/i/1")
                )
        );
        // when
        representation.withLinks(link("foo", "/foo/1"));
        representation.withLinks(asList(item("/i/2"),item("/i/3")));
        // then
        assertThat(representation.getLinks().getRels(), hasSize(3));
        assertThat(representation.getLinks().getLinkBy("foo").isPresent(), is(true));
        assertThat(representation.getLinks().getLinksBy("item"), contains(item("/i/1"),item("/i/2"),item("/i/3")));
    }
}
