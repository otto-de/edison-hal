package de.otto.edison.hal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static de.otto.edison.hal.HalParser.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.HalParser.parse;
import static de.otto.edison.hal.Link.*;
import static de.otto.edison.hal.Links.emptyLinks;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Created by guido on 05.07.16.
 */
public class HalRepresentationParsingTest {

    static class SimpleHalRepresentation extends HalRepresentation {
        public String first = "foo";
        public String second = "bar";
    }

    static class EmbeddedHalRepresentation extends HalRepresentation {
        public final String value = "foobar";
    }

    @Test
    public void shouldParseSimpleHalRepresentationWithoutLinks() throws IOException {
        // given
        final String json = "{\"first\":\"foo\",\"second\":\"bar\"}";
        // when
        final SimpleHalRepresentation result = new ObjectMapper().readValue(json.getBytes(), SimpleHalRepresentation.class);
        // then
        assertThat(result.getLinks(), is(emptyLinks()));
        assertThat(result.first, is("foo"));
        assertThat(result.second, is("bar"));
    }

    @Test
    public void shouldIgnoreExtraAttributes() throws IOException {
        // given
        final String json = "{\"first\":\"foo\",\"second\":\"bar\",\"third\":\"foobar\"}";
        // when
        final SimpleHalRepresentation result = new ObjectMapper().readValue(json.getBytes(), SimpleHalRepresentation.class);
        // then
        assertThat(result.getLinks(), is(emptyLinks()));
        assertThat(result.first, is("foo"));
        assertThat(result.second, is("bar"));
    }

    @Test
    public void shouldParseLinks() throws IOException {
        // given
        final String json = "{\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"},\"test\":{\"href\":\"http://example.org/test/bar\"}},\"first\":\"foo\"}";
        // when
        final SimpleHalRepresentation result = new ObjectMapper().readValue(json.getBytes(), SimpleHalRepresentation.class);
        // then
        Links links = result.getLinks();
        assertThat(links.getLinkBy("self").get(), is(self("http://example.org/test/foo")));
        assertThat(links.getLinkBy("test").get(), is(link("test", "http://example.org/test/bar")));
        assertThat(result.first, is("foo"));
    }

    @Test
    public void shouldParseDifferentEmbeddedItems() throws IOException {
        // given
        final String json =
                "{" +
                    "\"first\":\"1\"," +
                    "\"second\":\"2\"," +
                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}}," +
                    "\"_embedded\":{\"bar\":[" +
                        "{" +
                            "\"value\":\"3\"," +
                            "\"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                        "}" +
                    "]}" +
                "}";
        // when
        final SimpleHalRepresentation result = new ObjectMapper().readValue(json.getBytes(), SimpleHalRepresentation.class);
        // then
        final Links links = result.getLinks();
        assertThat(links.getLinkBy("self").get(), is(self("http://example.org/test/foo")));
        assertThat(result.first, is("1"));
        assertThat(result.second, is("2"));
        // and
        final List<HalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar");
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }

    @Test
    public void shouldDeserializeEmbeddedItemsAsDomainObject() throws IOException {
        // given
        final String json =
                "{" +
                        "\"_embedded\":{\"bar\":[" +
                        "{" +
                        "\"value\":\"3\"," +
                        "\"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                        "}" +
                        "]}" +
                        "}";
        // when
        SimpleHalRepresentation result = parse(json).as(SimpleHalRepresentation.class, withEmbedded("bar", EmbeddedHalRepresentation.class));
        // then
        final List<HalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar");
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0), is(instanceOf(EmbeddedHalRepresentation.class)));
    }

    @Test
    public void shouldParseMultipleLinksForSingleRel() throws IOException {
        // given
        final String json = "{\"_links\":{\"test\":[{\"href\":\"http://example.org/test/foo\"},{\"href\":\"http://example.org/test/bar\"}]}}";
        // when
        Links links = parse(json).as(HalRepresentation.class).getLinks();
        // then
        assertThat(links.getLinksBy("test"), contains(
                link("test", "http://example.org/test/foo"),
                link("test", "http://example.org/test/bar")
        ));
    }

    @Test
    public void shouldParseTemplatedLink() throws IOException {
        // given
        final String json = "{\"_links\":{\"search\":{\"href\":\"/test{?bar}\",\"templated\":true}}}";
        // when
        Links links = parse(json).as(HalRepresentation.class).getLinks();
        // then
        assertThat(links.getLinksBy("search"), contains(
                templated("search", "/test{?bar}")
        ));
    }

    @Test
    public void shouldParseEvenMoreComplexLinks() throws IOException {
        // given
        final String json = "{\"_links\":{" + "" +
                "\"search\":{\"href\":\"/test{?bar}\",\"templated\":true,\"type\":\"application/hal+json\",\"hreflang\":\"de-DE\",\"title\":\"Some Title\",\"name\":\"Foo\",\"profile\":\"http://example.org/profiles/test-profile\",\"deprecation\":true}," +
                "\"foo\":{\"href\":\"/test/bar\",\"type\":\"application/hal+json\",\"hreflang\":\"de-DE\",\"title\":\"Some Title\",\"name\":\"Foo\",\"profile\":\"http://example.org/profiles/test-profile\",\"deprecation\":false}" +
                "}}";
        // when
        Links links = parse(json).as(HalRepresentation.class).getLinks();
        // then
        assertThat(links.getLinksBy("search"), contains(
                templatedBuilder("search", "/test{?bar}")
                        .withType("application/hal+json")
                        .withProfile("http://example.org/profiles/test-profile")
                        .withHrefLang("de-DE")
                        .withTitle("Some Title")
                        .withName("Foo")
                        .beeingDeprecated()
                        .build()
        ));
        assertThat(links.getLinksBy("foo"), contains(
                linkBuilder("foo", "/test/bar")
                        .withType("application/hal+json")
                        .withProfile("http://example.org/profiles/test-profile")
                        .withHrefLang("de-DE")
                        .withTitle("Some Title")
                        .withName("Foo")
                        .build()
        ));
    }

}
