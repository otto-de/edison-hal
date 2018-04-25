package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static de.otto.edison.hal.Embedded.embedded;
import static de.otto.edison.hal.Link.*;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
                linkingTo()
                        .self("http://example.org/test/foo")
                        .build()
        ) {
            public final String test = "foo";
        };
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"test\":\"foo\",\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}}}"));
    }

    @Test
    public void shouldRenderSingleCuriAsArray() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}")
                        .build()
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}]}}"));
    }

    @Test
    public void shouldRenderSingleItemAsArray() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .item("http://example.org/items/1")
                        .build()
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"item\":[{\"href\":\"http://example.org/items/1\"}]}}"));
    }

    @Test
    public void shouldRenderConfiguredRelAsArray() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .array(link("foo", "http://example.org/items/1"))
                        .single(link("bar", "http://example.org/items/2"))
                        .build()
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"foo\":[{\"href\":\"http://example.org/items/1\"}],\"bar\":{\"href\":\"http://example.org/items/2\"}}}"));
    }

    @Test
    public void shouldRenderCuriedRelAsArray() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("ex", "http://example.org/rels/{rel}")
                        .array(
                                link("ex:foo", "http://example.org/items/1"),
                                link("http://example.org/rels/bar", "http://example.org/items/2"))
                        .build()
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{" +
                "\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"ex\"}]," +
                "\"ex:foo\":[{\"href\":\"http://example.org/items/1\"}]," +
                "\"ex:bar\":[{\"href\":\"http://example.org/items/2\"}]}}"));
    }

    @Test
    public void shouldRenderCuriedRelAsLinkObject() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("ex", "http://example.org/rels/{rel}")
                        .single(
                                link("ex:foo", "http://example.org/items/1"),
                                link("http://example.org/rels/bar", "http://example.org/items/2"))
                        .build()
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{" +
                "\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"ex\"}]," +
                "\"ex:foo\":{\"href\":\"http://example.org/items/1\"}," +
                "\"ex:bar\":{\"href\":\"http://example.org/items/2\"}}}"));
    }

    @Test
    public void shouldRenderNestedCuriedRelAsArray() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                emptyLinks(),
                embedded("http://example.org/rels/nested", asList(new HalRepresentation(
                        linkingTo()
                                .curi("ex", "http://example.org/rels/{rel}")
                                .array(
                                        link("ex:foo", "http://example.org/items/1"),
                                        link("http://example.org/rels/bar", "http://example.org/items/2"))
                                .build()
                )))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{" +
                "\"_embedded\":{\"http://example.org/rels/nested\":[{\"_links\":{" +
                "\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"ex\"}]," +
                "\"ex:foo\":[{\"href\":\"http://example.org/items/1\"}]," +
                "\"ex:bar\":[{\"href\":\"http://example.org/items/2\"}]}}" +
                "]}}"));
    }

    @Test
    public void shouldRenderNestedCuriedRelAsLinkObject() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                emptyLinks(),
                embedded("http://example.org/rels/nested", asList(new HalRepresentation(
                        linkingTo()
                                .curi("ex", "http://example.org/rels/{rel}")
                                .single(link("ex:foo", "http://example.org/items/1"))
                                .single(link("http://example.org/rels/bar", "http://example.org/items/2"))
                                .build()
                )))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{" +
                "\"_embedded\":{\"http://example.org/rels/nested\":[{\"_links\":{" +
                "\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"ex\"}]," +
                "\"ex:foo\":{\"href\":\"http://example.org/items/1\"}," +
                "\"ex:bar\":{\"href\":\"http://example.org/items/2\"}}}" +
                "]}}"));
    }

    @Test
    public void shouldRenderNestedCuriedRelAsArrayWithCuriAtTopLevel() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("ex", "http://example.org/rels/{rel}")
                        .build(),
                embedded("http://example.org/rels/nested", asList(new HalRepresentation(
                        linkingTo()
                                .array(
                                        link("ex:foo", "http://example.org/items/1"),
                                        link("http://example.org/rels/bar", "http://example.org/items/2"))
                                .build()
                )))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{" +
                "\"_links\":{" +
                "\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"ex\"}]}," +
                "\"_embedded\":{\"ex:nested\":[{\"_links\":{" +
                "\"ex:foo\":[{\"href\":\"http://example.org/items/1\"}]," +
                "\"ex:bar\":[{\"href\":\"http://example.org/items/2\"}]}}" +
                "]}}"));
    }

    @Test
    public void shouldRenderMultipleLinks() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .self("http://example.org/test/foo")
                        .single(
                                collection("http://example.org/test"))
                        .build()
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"},\"collection\":{\"href\":\"http://example.org/test\"}}}"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToAddMultipleSingleLinksForSameRel() {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .single(link("test", "http://example.org/test/foo"))
                        .single(link("test", "http://example.org/test/bar"))
                        .build()
        );
    }

    @Test
    public void shouldRenderTemplatedLink() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .single(link("search", "/test{?bar}"))
                        .build()
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
                linkingTo()
                        .single(linkBuilder("search", "/test{?bar}")
                                .withType("application/hal+json")
                                .withHrefLang("de-DE")
                                .withTitle("Some Title")
                                .withName("Foo")
                                .withProfile("http://example.org/profiles/test-profile")
                                .withDeprecation("http://example.org/deprecations/4711.html")
                                .build())
                        .single(linkBuilder("foo", "/test/bar")
                                .withType("application/hal+json")
                                .withHrefLang("de-DE")
                                .withTitle("Some Title")
                                .withName("Foo")
                                .withProfile("http://example.org/profiles/test-profile")
                                .withDeprecation("http://example.org/deprecations/4711.html")
                                .build())
                        .build()
        );
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
                linkingTo()
                        .self("/")
                        .item("/i/1")
                        .build()
        );
        // when
        representation.add(linkingTo().array(link("foo", "/foo/1")).build());
        representation.add(linkingTo().array(item("/i/2"), item("/i/3")).build());
        // then
        assertThat(representation.getLinks().getRels(), hasSize(3));
        assertThat(representation.getLinks().getLinkBy("foo").isPresent(), is(true));
        assertThat(representation.getLinks().getLinksBy("item"), contains(item("/i/1"),item("/i/2"),item("/i/3")));
    }

    @Test
    public void shouldBeAbleToAddLinksToEmptyLinksAfterConstruction() {
        // given
        final HalRepresentation representation = new HalRepresentation();
        // when
        representation.add(linkingTo().single(link("foo", "/foo/1")).build());
        // then
        assertThat(representation.getLinks().getRels(), hasSize(1));
        assertThat(representation.getLinks().getLinkBy("foo").isPresent(), is(true));
        assertThat(representation.getLinks().getLinksBy("foo"), contains(link("foo", "/foo/1")));
    }
}
