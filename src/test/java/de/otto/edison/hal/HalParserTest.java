package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static de.otto.edison.hal.HalParser.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.HalParser.parse;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.emptyLinks;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class HalParserTest {

    @Test
    public void shouldParseEmptyHalDocument() throws IOException {
        // given
        final String json =
                 "{}";
        // when
        final HalRepresentation result = parse(json).as(HalRepresentation.class);
        // then
        final Links links = result.getLinks();
        assertThat(links, is(emptyLinks()));
    }

    @Test
    public void shouldParseSimpleHalDocumentsWithoutEmbeddedItems() throws IOException {
        // given
        final String json =
                 "{" +
                        "\"ignored\":\"some value\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}}" +
                 "}";
        // when
        final HalRepresentation result = parse(json).as(HalRepresentation.class);
        // then
        final Links links = result.getLinks();
        assertThat(links.getLinkBy("self").get(), is(self("http://example.org/test/foo")));
    }

    @Test
    public void shouldParseItemsWithSomePropertiesAndLinks() throws IOException {
        // given
        final String json =
                 "{" +
                        "\"first\":\"1\"," +
                        "\"second\":\"2\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}}," +
                        "\"_embedded\":{\"bar\":[" +
                        "   {" +
                        "       \"value\":\"3\"," +
                        "       \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                        "   }" +
                        "]}" +
                        "}";
        // when
        final SimpleHalRepresentation result = parse(json).as(SimpleHalRepresentation.class);
        // then
        assertThat(result.first, is("1"));
        assertThat(result.second, is("2"));
        final Links links = result.getLinks();
        assertThat(links.getLinkBy("self").get(), is(self("http://example.org/test/foo")));
    }

    @Test
    public void shouldParseEmbeddedItemsAsPlainHalRepresentation() throws IOException {
        // given
        final String json =
                "{" +
                "   \"_embedded\":{\"bar\":[" +
                "       {" +
                "           \"value\":\"3\"," +
                "           \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                "       }" +
                "   ]}" +
                "}";
        // when
        final SimpleHalRepresentation result = parse(json).as(SimpleHalRepresentation.class);
        // then
        final List<HalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar");
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getClass(), equalTo(HalRepresentation.class));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }

    @Test
    public void shouldParseEmbeddedItemsWithSpecificType() throws IOException {
        // given
        final String json =
                "{" +
                        "\"_embedded\":{\"bar\":[" +
                        "   {" +
                        "       \"value\":\"3\"," +
                        "       \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                        "   }" +
                        "]}" +
                        "}";
        // when
        final SimpleHalRepresentation result = parse(json).as(SimpleHalRepresentation.class, withEmbedded("bar", EmbeddedHalRepresentation.class));
        // then
        final List<EmbeddedHalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar", EmbeddedHalRepresentation.class);
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getClass(), equalTo(EmbeddedHalRepresentation.class));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }

    @Test
    public void shouldParseEmbeddedItemsWithDifferentSpecificTypes() throws IOException {
        // given
        final String json =
                "{" +
                "   \"_embedded\":{" +
                "       \"foo\":[{" +
                "           \"fooValue\":\"1\"," +
                "           \"_links\":{\"self\":[{\"href\":\"http://example.org/test/foo\"}]}" +
                "       }]," +
                "       \"bar\":[{" +
                "           \"value\":\"2\"," +
                "           \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar\"}]}" +
                "       }]" +
                "   }" +
                "}";
        // when
        final SimpleHalRepresentation result = parse(json).as(SimpleHalRepresentation.class, withEmbedded("bar", EmbeddedHalRepresentation.class));
        // then
        final List<HalRepresentation> embeddedFoo = result.getEmbedded().getItemsBy("foo");
        assertThat(embeddedFoo, hasSize(1));
        assertThat(embeddedFoo.get(0).getClass(), equalTo(HalRepresentation.class));
        assertThat(embeddedFoo.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/foo")));
        // and
        final List<EmbeddedHalRepresentation> embeddedBar = result.getEmbedded().getItemsBy("bar", EmbeddedHalRepresentation.class);
        assertThat(embeddedBar, hasSize(1));
        assertThat(embeddedBar.get(0).getClass(), equalTo(EmbeddedHalRepresentation.class));
        assertThat(embeddedBar.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar")));
    }

    static class SimpleHalRepresentation extends HalRepresentation {
        @JsonProperty("first")
        private String first = "foo";
        @JsonProperty("second")
        private String second = "bar";
    }

    static class EmbeddedHalRepresentation extends HalRepresentation {
        @JsonProperty("value")
        private String value = "foobar";
    }

}