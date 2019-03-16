package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static de.otto.edison.hal.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.HalParser.parse;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.emptyLinks;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
    public void shouldParseEmbeddedItemsAsPlainHalRepresentationWithSpecificObjectMapper() throws IOException {
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
        final SimpleHalRepresentation result = parse(json, new ObjectMapper()).as(SimpleHalRepresentation.class);
        final SimpleHalRepresentation expectedResult = parse(json).as(SimpleHalRepresentation.class);
        // then
        assertThat(result, is(expectedResult));
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
        final SimpleHalRepresentation result = parse(json)
                .as(SimpleHalRepresentation.class, withEmbedded("bar", EmbeddedHalRepresentation.class));
        // then
        final List<EmbeddedHalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar", EmbeddedHalRepresentation.class);
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getClass(), equalTo(EmbeddedHalRepresentation.class));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }

    @Test
    public void shouldParseNestedEmbeddedItemsWithSpecificType() throws IOException {
        // given
        final String json =
                "{" +
                        "\"_embedded\":{\"foo\":[" +
                        "   {" +
                        "       \"_links\":{\"self\":[{\"href\":\"http://example.org/test/foo/01\"}]}," +
                        "       \"_embedded\":{\"bar\":[" +
                        "          {" +
                        "              \"value\":\"3\"," +
                        "              \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                        "          }," +
                        "          {" +
                        "              \"value\":\"4\"," +
                        "              \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/02\"}]}" +
                        "          }" +
                        "       ]}" +
                        "   }" +
                        "]}" +
                        "}";
        // when
        final HalRepresentation result = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("foo", HalRepresentation.class,
                                withEmbedded("bar", EmbeddedHalRepresentation.class)));
        // then
        final HalRepresentation foo = result.getEmbedded().getItemsBy("foo", HalRepresentation.class).get(0);
        final List<EmbeddedHalRepresentation> embeddedItems = foo.getEmbedded().getItemsBy("bar", EmbeddedHalRepresentation.class);
        assertThat(embeddedItems, hasSize(2));
        assertThat(embeddedItems.get(0).getClass(), equalTo(EmbeddedHalRepresentation.class));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
        assertThat(embeddedItems.get(0).value, is("3"));
        assertThat(embeddedItems.get(1).getClass(), equalTo(EmbeddedHalRepresentation.class));
        assertThat(embeddedItems.get(1).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/02")));
        assertThat(embeddedItems.get(1).value, is("4"));
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

    @Test
    public void shouldParseNestedEmbeddedItemsWithDifferentSpecificTypes() throws IOException {
        // given
        final String json =
                "{" +
                "   \"_embedded\":{\"test\":[{" +
                "      \"_embedded\":{" +
                "          \"foo\":[{" +
                "              \"fooValue\":\"1\"," +
                "              \"_links\":{\"self\":[{\"href\":\"http://example.org/test/foo\"}]}" +
                "          }]," +
                "          \"bar\":[{" +
                "              \"value\":\"2\"," +
                "              \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar\"}]}" +
                "          }]" +
                "      }" +
                "      }" +
                "   ]}" +
                "}";
        // when
        final SimpleHalRepresentation result = parse(json)
                .as(SimpleHalRepresentation.class,
                        withEmbedded("test", HalRepresentation.class,
                                withEmbedded("bar", EmbeddedHalRepresentation.class))
                );
        final HalRepresentation test = result.getEmbedded().getItemsBy("test").get(0);
        // then
        final List<HalRepresentation> embeddedFoo = test.getEmbedded().getItemsBy("foo");
        assertThat(embeddedFoo, hasSize(1));
        assertThat(embeddedFoo.get(0).getClass(), equalTo(HalRepresentation.class));
        assertThat(embeddedFoo.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/foo")));
        // and
        final List<EmbeddedHalRepresentation> embeddedBar = test.getEmbedded().getItemsBy("bar", EmbeddedHalRepresentation.class);
        assertThat(embeddedBar, hasSize(1));
        assertThat(embeddedBar.get(0).getClass(), equalTo(EmbeddedHalRepresentation.class));
        assertThat(embeddedBar.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar")));
    }

    @Test
    public void shouldParseNestedEmbeddedItemsWithDifferentSpecificTypesUsingCustomObjectMapper() throws IOException {
        // given
        final String json =
                "{" +
                "   \"_embedded\":{\"test\":[{" +
                "      \"_embedded\":{" +
                "          \"foo\":[{" +
                "              \"fooValue\":\"1\"," +
                "              \"_links\":{\"self\":[{\"href\":\"http://example.org/test/foo\"}]}" +
                "          }]," +
                "          \"bar\":[{" +
                "              \"value\":\"2\"," +
                "              \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar\"}]}" +
                "          }]" +
                "      }" +
                "      }" +
                "   ]}" +
                "}";
        final ObjectMapper objectMapper = new ObjectMapper();

        // when
        final SimpleHalRepresentation result = parse(json, objectMapper)
                .as(SimpleHalRepresentation.class,
                        withEmbedded("test", HalRepresentation.class,
                                withEmbedded("bar", EmbeddedHalRepresentation.class))
                );
        // then
        assertThat(result, is(parse(json)
                .as(SimpleHalRepresentation.class,
                        withEmbedded("test", HalRepresentation.class,
                                withEmbedded("bar", EmbeddedHalRepresentation.class)))));
    }

    @Test
    public void shouldParseEmbeddedObjectWithNestedHalRepresentation() throws IOException {
        // given
        final String json = "{" +
                "   \"_embedded\":{\"bar\":" +
                "       {\"foo\":{\"_links\":{\"self\":{\"href\":\"http://example.com/example/foo\"}}}}" +
                "   }" +
                "}";

        // when
        final HalRepresentation result = parse(json)
                .as(HalRepresentation.class, withEmbedded("bar", NestedHalRepresentation.class));

        // then
        final HalRepresentation nested = result
                .getEmbedded()
                .getItemsBy("bar", NestedHalRepresentation.class)
                .get(0)
                .foo;
        assertThat(nested
                .getLinks()
                .getLinkBy("self")
                .get()
                .getHref(), is("http://example.com/example/foo"));
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

    static class NestedHalRepresentation extends HalRepresentation {
        @JsonProperty("foo")
        private HalRepresentation foo;
    }

}