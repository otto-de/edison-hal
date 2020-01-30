package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static de.otto.edison.hal.EmbeddedTypeInfo.withEmbedded;
import static de.otto.edison.hal.HalParser.parse;
import static de.otto.edison.hal.Link.*;
import static de.otto.edison.hal.Links.emptyLinks;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class HalRepresentationParsingTest {

    static class SimpleHalRepresentation extends HalRepresentation {
        @JsonProperty
        public String first;
        @JsonProperty
        public String second;
    }

    static class EmbeddedHalRepresentation extends HalRepresentation {
        @JsonProperty
        public String value;
    }

    static class NestedHalRepresentation extends HalRepresentation {
        @JsonProperty
        public List<HalRepresentation> nested;
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
    public void shouldParseSingleEmbeddedItemForMultipleRelsWithProperlyConfiguredObjectMapper() throws IOException {
        // given
        final String json =
                "{" +
                    "\"_embedded\":{" +
                        "\"foo\":{" +
                            "\"_links\":{\"self\":[{\"href\":\"http://example.org/test/foo\"}]}" +
                        "}," +
                        "\"bar\":[{" +
                            "\"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar\"}]}" +
                        "}]" +
                    "}" +
                "}";
        // and
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        // when
        final SimpleHalRepresentation result = objectMapper.readValue(json.getBytes(), SimpleHalRepresentation.class);
        // then
        final List<HalRepresentation> embeddedFoo = result.getEmbedded().getItemsBy("foo");
        assertThat(embeddedFoo, hasSize(1));
        assertThat(embeddedFoo.get(0).getLinks().getLinkBy("self").get().getHref(), is("http://example.org/test/foo"));
        final List<HalRepresentation> embeddedBar = result.getEmbedded().getItemsBy("bar");
        assertThat(embeddedBar, hasSize(1));
        assertThat(embeddedBar.get(0).getLinks().getLinkBy("self").get().getHref(), is("http://example.org/test/bar"));
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
    public void shouldDeserializeSingleEmbeddedItemAsDomainObject() throws IOException {
        // given
        final String json =
                "{" +
                        "\"_embedded\":{\"bar\":{" +
                        "   \"value\":\"3\"," +
                        "   \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                        "}}}";
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
                link("search", "/test{?bar}")
        ));
        assertThat(links.getLinksBy("search").get(0).isTemplated(), is(true));
    }

    @Test
    public void shouldParseEvenMoreComplexLinks() throws IOException {
        // given
        final String json = "{\"_links\":{" + "" +
                "\"search\":{\"href\":\"/test{?bar}\",\"templated\":true,\"type\":\"application/hal+json\",\"hreflang\":\"de-DE\",\"title\":\"Some Title\",\"name\":\"Foo\",\"profile\":\"http://example.org/profiles/test-profile\",\"deprecation\":\"http://example.org/deprecations/4711.html\"}," +
                "\"foo\":{\"href\":\"/test/bar\",\"type\":\"application/hal+json\",\"hreflang\":\"de-DE\",\"title\":\"Some Title\",\"name\":\"Foo\",\"profile\":\"http://example.org/profiles/test-profile\"}" +
                "}}";
        // when
        Links links = parse(json).as(HalRepresentation.class).getLinks();
        // then
        assertThat(links.getLinksBy("search"), contains(
                linkBuilder("search", "/test{?bar}")
                        .withType("application/hal+json")
                        .withProfile("http://example.org/profiles/test-profile")
                        .withHrefLang("de-DE")
                        .withTitle("Some Title")
                        .withName("Foo")
                        .withDeprecation("http://example.org/deprecations/4711.html")
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
        assertThat(links.getLinksBy("search").get(0).isTemplated(), is(true));
        assertThat(links.getLinksBy("foo").get(0).isTemplated(), is(false));
    }

    @Test
    public void shouldParseCuriedLinks() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}," +
                "\"x:foo\":{\"href\":\"http://example.org/test\"}," +
                "\"x:bar\":{\"href\":\"http://example.org/test\"}}" +
                "}" +
                "}";
        // when
        Links links = parse(json).as(HalRepresentation.class).getLinks();
        // then
        assertThat(links.getLinksBy("http://example.org/rels/foo"), hasSize(1));
    }

    @Test
    public void shouldParseCuriedEmbeddeds() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "\"_embedded\":{\"x:foo\":[" +
                    "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                    "}," +
                    "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                    "}" +
                "]}" +
                "}" +
                "}";
        // when
        Embedded embedded = parse(json).as(HalRepresentation.class).getEmbedded();
        // then
        final List<HalRepresentation> items = embedded.getItemsBy("http://example.org/rels/foo");
        assertThat(items, hasSize(2));
        assertThat(items.get(0).getLinks().getLinkBy("http://example.org/rels/bar").get().getHref(), is("http://example.org/test/bar/01"));
    }

    @Test
    public void shouldParseSingleCuriedEmbeddeds() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "\"_embedded\":{\"x:foo\":" +
                    "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar\"}}" +
                    "}" +
                "}" +
                "}" +
                "}";
        // when
        Embedded embedded = parse(json).as(HalRepresentation.class).getEmbedded();
        // then
        final List<HalRepresentation> items = embedded.getItemsBy("http://example.org/rels/foo");
        assertThat(items, hasSize(1));
        assertThat(embedded.isArray("x:foo"), is(true));
        assertThat(embedded.isArray("http://example.org/rels/foo"), is(true));
    }

    @Test
    public void shouldIgnoreMissingEmbeddeds() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "\"_embedded\":{\"x:test\":[{" +
                "}]}" +
                "}";
        // when
        final HalRepresentation halRepresentation = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("x:test", SimpleHalRepresentation.class,
                                withEmbedded("x:doesnotexist", EmbeddedHalRepresentation.class)));
        // then
        final List<HalRepresentation> items = halRepresentation.getEmbedded().getItemsBy("http://example.org/rels/test");
        assertThat(items, hasSize(1));
        assertThat(items.get(0), is(instanceOf(SimpleHalRepresentation.class)));
        assertThat(items.get(0).getEmbedded().isEmpty(), is(true));
    }

    @Test
    public void shouldParseNestedCuriedEmbeddeds() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "\"_embedded\":{\"x:test\":[{" +
                "   \"_embedded\":{" +
                "      \"http://example.org/rels/foo\":[" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                "         }," +
                "         {" +
                "            \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                "         }" +
                "      ]," +
                "      \"x:bar\":[" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/03\"}}" +
                "         }," +
                "         {" +
                "            \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/04\"}}" +
                "         }" +
                "      ]" +
                "   }" +
                "}]}" +
                "}";
        // when
        Embedded embedded = parse(json).as(HalRepresentation.class).getEmbedded();
        // then
        List<HalRepresentation> items = embedded.getItemsBy("http://example.org/rels/test").get(0).getEmbedded().getItemsBy("http://example.org/rels/foo");
        assertThat(items, hasSize(2));
        assertThat(items.get(0).getLinks().getLinkBy("x:bar").get().getHref(), is("http://example.org/test/bar/01"));
        assertThat(items.get(1).getLinks().getLinkBy("http://example.org/rels/bar").get().getHref(), is("http://example.org/test/bar/02"));

        items = embedded.getItemsBy("http://example.org/rels/test").get(0).getEmbedded().getItemsBy("x:bar");
        assertThat(items, hasSize(2));
        assertThat(items.get(0).getLinks().getLinkBy("http://example.org/rels/bar").get().getHref(), is("http://example.org/test/bar/03"));
        assertThat(items.get(1).getLinks().getLinkBy("x:bar").get().getHref(), is("http://example.org/test/bar/04"));
    }

    @Test
    public void shouldParseCuriedEmbeddedsWithDerivedType() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"ex\"}}," +
                "\"_embedded\":{\"ex:bar\":[" +
                "{" +
                "\"value\":\"Hello World\"," +
                "\"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                "}" +
                "]}" +
                "}" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class, withEmbedded("http://example.org/rels/bar", EmbeddedHalRepresentation.class))
                .getEmbedded();
        // then
        final List<EmbeddedHalRepresentation> items = embedded
                .getItemsBy("http://example.org/rels/bar", EmbeddedHalRepresentation.class);
        assertThat(items, hasSize(1));
        assertThat(items.get(0).value, is("Hello World"));
    }

    @Test
    public void shouldParseSingleCuriedEmbeddedWithDerivedType() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"ex\"}}," +
                "\"_embedded\":{\"ex:bar\":{" +
                "   \"value\":\"Hello World\"," +
                "   \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                "}}}}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class, withEmbedded("http://example.org/rels/bar", EmbeddedHalRepresentation.class))
                .getEmbedded();
        // then
        final List<EmbeddedHalRepresentation> items = embedded
                .getItemsBy("http://example.org/rels/bar", EmbeddedHalRepresentation.class);
        assertThat(items, hasSize(1));
        assertThat(items.get(0).value, is("Hello World"));
    }

    @Test
    public void shouldParseCuriedEmbeddedsWithDerivedTypes() throws IOException {
        // given
        final String json = "{" +
                "   \"_links\":{" +
                "       \"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "   \"_embedded\":{" +
                "      \"http://example.org/rels/foo\":[" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                "         }," +
                "         {" +
                "            \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                "         }" +
                "      ]," +
                "      \"x:bar\":[" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/03\"}}" +
                "         }," +
                "         {" +
                "            \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/04\"}}" +
                "         }" +
                "      ]" +
                "   }" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("x:foo", EmbeddedHalRepresentation.class),
                        withEmbedded("http://example.org/rels/bar", SimpleHalRepresentation.class))
                .getEmbedded();
        // then
        assertThat(embedded.getItemsBy("x:foo"), hasSize(2));
        assertThat(embedded.getItemsBy("x:foo").get(0), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("http://example.org/rels/foo").get(1), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:bar"), hasSize(2));
        assertThat(embedded.getItemsBy("x:bar").get(0), is(instanceOf(SimpleHalRepresentation.class)));
        assertThat(embedded.getItemsBy("http://example.org/rels/bar").get(1), is(instanceOf(SimpleHalRepresentation.class)));
    }

    @Test
    public void shouldParseSingleCuriedEmbeddedsWithDerivedTypes() throws IOException {
        // given
        final String json = "{" +
                "   \"_links\":{" +
                "       \"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "   \"_embedded\":{" +
                "      \"http://example.org/rels/foo\":" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                "         }," +
                "      \"x:bar\":" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/03\"}}" +
                "         }" +
                "   }" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("x:foo", EmbeddedHalRepresentation.class),
                        withEmbedded("http://example.org/rels/bar", SimpleHalRepresentation.class))
                .getEmbedded();
        // then
        assertThat(embedded.getItemsBy("x:foo"), hasSize(1));
        assertThat(embedded.getItemsBy("x:foo").get(0), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:bar"), hasSize(1));
        assertThat(embedded.getItemsBy("x:bar").get(0), is(instanceOf(SimpleHalRepresentation.class)));
    }

    @Test
    public void shouldParseNestedCuriedEmbeddedsWithDerivedTypes() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "\"_embedded\":{\"x:test\":[{" +
                "   \"_embedded\":{" +
                "      \"http://example.org/rels/foo\":[" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                "         }," +
                "         {" +
                "            \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                "         }" +
                "      ]," +
                "      \"x:bar\":[" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/03\"}}" +
                "         }," +
                "         {" +
                "            \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/04\"}}" +
                "         }" +
                "      ]" +
                "   }" +
                "}]}" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("x:test", HalRepresentation.class,
                                withEmbedded("x:foo", EmbeddedHalRepresentation.class),
                                withEmbedded("http://example.org/rels/bar", SimpleHalRepresentation.class)))
                .getEmbedded().getItemsBy("x:test").get(0).getEmbedded();
        // then
        assertThat(embedded.getItemsBy("x:foo"), hasSize(2));
        assertThat(embedded.getItemsBy("x:foo").get(0), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:foo").get(1), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:bar"), hasSize(2));
        assertThat(embedded.getItemsBy("x:bar").get(0), is(instanceOf(SimpleHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:bar").get(1), is(instanceOf(SimpleHalRepresentation.class)));
    }

    @Test
    public void shouldParseSingleNestedCuriedEmbeddedsWithDerivedTypes() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "\"_embedded\":{\"x:test\":[{" +
                "   \"_embedded\":{" +
                "      \"http://example.org/rels/foo\":" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                "         }," +
                "      \"x:bar\":" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/03\"}}" +
                "         }" +
                "   }" +
                "}]}" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("x:test", HalRepresentation.class,
                                withEmbedded("x:foo", EmbeddedHalRepresentation.class),
                                withEmbedded("http://example.org/rels/bar", SimpleHalRepresentation.class)))
                .getEmbedded().getItemsBy("x:test").get(0).getEmbedded();
        // then
        assertThat(embedded.getItemsBy("x:foo"), hasSize(1));
        assertThat(embedded.getItemsBy("x:foo").get(0), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:bar"), hasSize(1));
        assertThat(embedded.getItemsBy("x:bar").get(0), is(instanceOf(SimpleHalRepresentation.class)));
    }

    @Test
    public void shouldParseNestedEmbeddedsWithDifferentDerivedTypesForSameRel() throws IOException {
        // given
        final String json = "{" +
                "\"_embedded\":{" +
                "   \"item\":[{" +
                "       \"first\":\"first value\"," +
                "       \"second\":\"second value\"," +
                "       \"_embedded\":{" +
                "           \"item\":[" +
                "           {" +
                "               \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}," +
                "               \"value\":\"first value\"" +
                "           }," +
                "           {" +
                "               \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}," +
                "               \"value\":\"second value\"" +
                "           }" +
                "           ]" +
                "       }" +
                "   }]" +
                "}" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("item", SimpleHalRepresentation.class,
                                withEmbedded("item", EmbeddedHalRepresentation.class)))
                .getEmbedded();
        // then
        assertThat(embedded.getItemsBy("item"), hasSize(1));
        assertThat(embedded.getItemsBy("item", SimpleHalRepresentation.class).get(0).first, is("first value"));
        assertThat(embedded.getItemsBy("item", SimpleHalRepresentation.class).get(0).second, is("second value"));
        // and
        embedded = embedded.getItemsBy("item").get(0).getEmbedded();

        assertThat(embedded.getItemsBy("item"), hasSize(2));
        assertThat(embedded.getItemsBy("item", EmbeddedHalRepresentation.class).get(0).value, is("first value"));
        assertThat(embedded.getItemsBy("item", EmbeddedHalRepresentation.class).get(1).value, is("second value"));
    }

    @Test
    public void shouldParseSingleNestedEmbeddedsWithDifferentDerivedTypesForSameRel() throws IOException {
        // given
        final String json = "{" +
                "\"_embedded\":{" +
                "   \"item\":{" +
                "       \"first\":\"first value\"," +
                "       \"second\":\"second value\"," +
                "       \"_embedded\":{" +
                "           \"item\":" +
                "           {" +
                "               \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}," +
                "               \"value\":\"first value\"" +
                "           }" +
                "       }" +
                "   }" +
                "}" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("item", SimpleHalRepresentation.class,
                                withEmbedded("item", EmbeddedHalRepresentation.class)))
                .getEmbedded();
        // then
        assertThat(embedded.getItemsBy("item"), hasSize(1));
        assertThat(embedded.getItemsBy("item", SimpleHalRepresentation.class).get(0).first, is("first value"));
        assertThat(embedded.getItemsBy("item", SimpleHalRepresentation.class).get(0).second, is("second value"));
        // and
        embedded = embedded.getItemsBy("item").get(0).getEmbedded();

        assertThat(embedded.getItemsBy("item"), hasSize(1));
        assertThat(embedded.getItemsBy("item", EmbeddedHalRepresentation.class).get(0).value, is("first value"));
    }

    @Test
    public void shouldParseNestedCuriedEmbeddedsWithDifferentDerivedTypesForSameRel() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "\"_embedded\":{\"x:foo\":[{" +
                "   \"_embedded\":{" +
                "      \"http://example.org/rels/foo\":[" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                "         }," +
                "         {" +
                "            \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                "         }" +
                "      ]," +
                "      \"x:bar\":[" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/03\"}}" +
                "         }," +
                "         {" +
                "            \"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/04\"}}" +
                "         }" +
                "      ]" +
                "   }" +
                "}]}" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("http://example.org/rels/foo", SimpleHalRepresentation.class,
                                withEmbedded("x:foo", EmbeddedHalRepresentation.class),
                                withEmbedded("http://example.org/rels/bar", SimpleHalRepresentation.class))
                        )
                .getEmbedded();
        // then
        assertThat(embedded.getItemsBy("x:foo"), hasSize(1));
        assertThat(embedded.getItemsBy("http://example.org/rels/foo").get(0), is(instanceOf(SimpleHalRepresentation.class)));
        // and
        embedded = embedded.getItemsBy("x:foo").get(0).getEmbedded();
        assertThat(embedded.getItemsBy("x:foo"), hasSize(2));
        assertThat(embedded.getItemsBy("x:foo").get(0), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:foo").get(1), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:bar"), hasSize(2));
        assertThat(embedded.getItemsBy("x:bar").get(0), is(instanceOf(SimpleHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:bar").get(1), is(instanceOf(SimpleHalRepresentation.class)));
    }

    @Test
    public void shouldParseSingleNestedCuriedEmbeddedsWithDifferentDerivedTypesForSameRel() throws IOException {
        // given
        final String json = "{\"_links\":{" +
                "\"curies\":{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}}," +
                "\"_embedded\":{\"x:foo\":{" +
                "   \"_embedded\":{" +
                "      \"http://example.org/rels/foo\":" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                "         }," +
                "      \"x:bar\":" +
                "         {" +
                "            \"_links\":{\"http://example.org/rels/bar\":{\"href\":\"http://example.org/test/bar/03\"}}" +
                "         }" +
                "   }" +
                "}}" +
                "}";
        // when
        Embedded embedded = parse(json)
                .as(HalRepresentation.class,
                        withEmbedded("http://example.org/rels/foo", SimpleHalRepresentation.class,
                                withEmbedded("x:foo", EmbeddedHalRepresentation.class),
                                withEmbedded("http://example.org/rels/bar", SimpleHalRepresentation.class))
                        )
                .getEmbedded();
        // then
        assertThat(embedded.getItemsBy("x:foo"), hasSize(1));
        assertThat(embedded.getItemsBy("http://example.org/rels/foo").get(0), is(instanceOf(SimpleHalRepresentation.class)));
        // and
        embedded = embedded.getItemsBy("x:foo").get(0).getEmbedded();
        assertThat(embedded.getItemsBy("x:foo"), hasSize(1));
        assertThat(embedded.getItemsBy("x:foo").get(0), is(instanceOf(EmbeddedHalRepresentation.class)));
        assertThat(embedded.getItemsBy("x:bar"), hasSize(1));
        assertThat(embedded.getItemsBy("x:bar").get(0), is(instanceOf(SimpleHalRepresentation.class)));
    }

    @Test
    public void shouldParseMultipleNestedEmbeddedsWithSameRel() throws IOException {
        final String json = "{" +
                "   \"_links\":{\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]}," +
                "   \"_embedded\":{" +
                "       \"x:bar\":[" +
                "           {" +
                "               \"first\":\"foo\"," +
                "               \"_embedded\":{" +
                "                   \"x:foobar\":[" +
                "                       {\"value\":\"value1\"}" +
                "                   ]" +
                "               }" +
                "           }," +
                "           {" +
                "               \"first\":\"bar\"," +
                "               \"_embedded\":{" +
                "                   \"x:foobar\":" +
                "                       {\"value\":\"value2\"}" +
                "               }" +
                "           }" +
                "       ]" +
                "   }" +
                "}";
        // when
        final HalRepresentation hal = parse(json).as(HalRepresentation.class,
                withEmbedded("x:bar", SimpleHalRepresentation.class,
                        withEmbedded("x:foobar", EmbeddedHalRepresentation.class)));
        final List<String> barValues = hal.getEmbedded()
                .getItemsBy("x:bar", SimpleHalRepresentation.class)
                .stream()
                .map(r -> r.first + " " + r.getEmbedded().getItemsBy("x:foobar", EmbeddedHalRepresentation.class).get(0).value)
                .collect(toList());
        // then
        assertThat(barValues, contains("foo value1", "bar value2"));
    }

    @Test
    public void shouldParseMultipleNestedEmbeddedsWithSameLinkRel() throws IOException {
        final String json = "{" +
                "   \"_links\":{\"curies\":[{\"href\":\"http://example.com/rels/{rel}\",\"name\":\"x\",\"templated\":true}]}," +
                "   \"_embedded\":{" +
                "       \"x:bar\":[" +
                "           {" +
                "               \"first\":\"foo\"," +
                "               \"_links\":{" +
                "                   \"x:foobar\":" +
                "                       {\"href\":\"/test/a\"}" +
                "               }" +
                "           }," +
                "           {" +
                "               \"first\":\"bar\"," +
                "               \"_links\":{" +
                "                   \"x:foobar\":[" +
                "                       {\"href\":\"/test/b\"}" +
                "                   ]" +
                "               }" +
                "           }" +
                "       ]" +
                "   }" +
                "}";
        // when
        final HalRepresentation hal = parse(json).as(HalRepresentation.class,
                withEmbedded("x:bar", SimpleHalRepresentation.class,
                        withEmbedded("x:foobar", EmbeddedHalRepresentation.class)));
        final List<String> barValues = hal.getEmbedded()
                .getItemsBy("x:bar", SimpleHalRepresentation.class)
                .stream()
                .map(r -> r.first + " " + r.getLinks().getLinksBy("x:foobar").get(0).getHref())
                .collect(toList());
        // then
        assertThat(barValues, contains("foo /test/a", "bar /test/b"));
    }

    @Test
    public void shouldParseLinksInNestedResourceObjects() throws IOException {
        // given
        final String json = "{\n" +
                "    \"nested\" : [\n" +
                "        {\n" +
                "           \"_links\": { \n" +
                "               \"self\" : {\"href\": \"/audits/1/issues/1\"}\n" +
                "           }\n" +
                "        }\n" +
                "    ],\n" +
                "    \"_links\": { \n" +
                "        \"self\" : {\"href\": \"/audits/1\"}\n" +
                "    }\n" +
                "}";
        // when
        NestedHalRepresentation resource = parse(json).as(NestedHalRepresentation.class);
        // then
        assertThat(resource.nested.get(0).getLinks().getRels(), contains("self"));
        assertThat(resource.nested.get(0).getLinks().getLinkBy("self").orElse(null).getHref(), is("/audits/1/issues/1"));
    }
}
