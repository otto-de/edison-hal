package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static de.otto.edison.hal.Embedded.embedded;
import static de.otto.edison.hal.Embedded.embeddedBuilder;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

/**
 * Simple Tests to prove that the examples used in the README.md are compiling and working
 */
public class ReadmeExamples {

    private static final ObjectMapper objectMapper;
    private static final ObjectMapper prettyObjectMapper;
    static {
        objectMapper = new ObjectMapper();
        prettyObjectMapper = new ObjectMapper();
        prettyObjectMapper.enable(INDENT_OUTPUT);
    }

    @Test
    public void Example_4_2_1() {
        // snippet
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .self("http://example.org/test/bar")
                        .item("http://example.org/test/foo/01")
                        .item("http://example.org/test/foo/02")
                        .build(),
                embeddedBuilder()
                        .with("item", asList(
                                new HalRepresentation(linkingTo().self("http://example.org/test/foo/01").build()),
                                new HalRepresentation(linkingTo().self("http://example.org/test/foo/02").build())))
                        .build());
        // /snippet
        assertThat(jsonOf("Example_4_2_1", representation), is("{\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"},\"item\":[{\"href\":\"http://example.org/test/foo/01\"},{\"href\":\"http://example.org/test/foo/02\"}]},\"_embedded\":{\"item\":[{\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo/01\"}}},{\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo/02\"}}}]}}"));
    }

    @Test
    public void Example_4_2_2() {
        // snippet
        class Example_4_2_2 extends HalRepresentation {
            @JsonProperty("someProperty")
            private String someProperty = "some value";
            @JsonProperty("someOtherProperty")
            private String someOtherProperty = "some other value";

            Example_4_2_2() {
                super(linkingTo()
                        .self("http://example.org/test/bar")
                        .build()
                );
            }
        }
        // /snippet
        Example_4_2_2 representation = new Example_4_2_2();
        assertThat(jsonOf("Example_4_2_2", representation), is("{\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}},\"someProperty\":\"some value\",\"someOtherProperty\":\"some other value\"}"));
    }

    @Test
    public void Example_4_3_1() {
        // snippet
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .self("http://example.com/foo/42")
                        .single(link("next", "http://example.com/foo/43"))
                        .single(link("prev", "http://example.com/foo/41"))
                        .array(
                                link("item", "http://example.com/bar/01"),
                                link("item", "http://example.com/bar/02"),
                                link("item", "http://example.com/bar/03")
                        )
                        .build()
        );
        // /snippet
        assertThat(jsonOf("Example_4_3_1", representation), is("{\"_links\":{\"self\":{\"href\":\"http://example.com/foo/42\"},\"next\":{\"href\":\"http://example.com/foo/43\"},\"prev\":{\"href\":\"http://example.com/foo/41\"},\"item\":[{\"href\":\"http://example.com/bar/01\"},{\"href\":\"http://example.com/bar/02\"},{\"href\":\"http://example.com/bar/03\"}]}}"));
    }

    @Test
    public void Example_4_4_1() {
        // snippet
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}")
                        .curi("y", "http://example.com/rels/{rel}")
                        .single(
                                link("http://example.org/rels/foo", "http://example.org/test"))
                        .array(
                                link("http://example.com/rels/bar", "http://example.org/test/1"),
                                link("http://example.com/rels/bar", "http://example.org/test/2"))
                        .build()
                );
        // /snippet
        assertThat(jsonOf("Example_4_4_1", representation), is("{\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"},{\"href\":\"http://example.com/rels/{rel}\",\"templated\":true,\"name\":\"y\"}],\"x:foo\":{\"href\":\"http://example.org/test\"},\"y:bar\":[{\"href\":\"http://example.org/test/1\"},{\"href\":\"http://example.org/test/2\"}]}}"));
    }

    @Test
    public void Example_4_6_1() throws IOException {

        // given
        final String json =
                "{" +
                        "\"someProperty\":\"1\"," +
                        "\"someOtherProperty\":\"2\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}}," +
                        "\"_embedded\":{\"bar\":[" +
                                "{" +
                                "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                                "}" +
                        "]}" +
                "}";

        // when
        final TestHalRepresentation result = new ObjectMapper().readValue(json.getBytes(), TestHalRepresentation.class);

        // then
        assertThat(result.someProperty, is("1"));
        assertThat(result.someOtherProperty, is("2"));

        // and
        final Links links = result.getLinks();
        assertThat(links.getLinkBy("self").get(), is(self("http://example.org/test/foo")));

        // and
        final List<HalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar");
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }

    static class TestHalRepresentation extends HalRepresentation {
        @JsonProperty("someProperty")
        private String someProperty;
        @JsonProperty("someOtherProperty")
        private String someOtherProperty;

        TestHalRepresentation() {
            super(
                    linkingTo()
                            .self("http://example.org/test/foo")
                            .build(),
                    embedded("bar", singletonList(new HalRepresentation(
                            linkingTo()
                                    .self("http://example.org/test/bar/01")
                                    .build()
                    )))
            );
        }
    }

    @Test
    public void shouldParseHal() throws IOException {

        // given
        final String json =
                "{" +
                        "\"someProperty\":\"1\"," +
                        "\"someOtherProperty\":\"2\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}}," +
                        "\"_embedded\":{\"bar\":[" +
                        "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                        "}" +
                        "]}" +
                        "}";

        // when
        final TestHalRepresentation result = new ObjectMapper().readValue(json.getBytes(), TestHalRepresentation.class);

        // then
        assertThat(result.someProperty, is("1"));
        assertThat(result.someOtherProperty, is("2"));

        // and
        final Links links = result.getLinks();
        assertThat(links.getLinkBy("self").get(), is(self("http://example.org/test/foo")));

        // and
        final List<HalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar");
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }

    private String jsonOf(String method, HalRepresentation representation) {
        try {
            System.out.println(method + ":");
            System.out.println(prettyObjectMapper.writeValueAsString(representation));
            return objectMapper.writeValueAsString(representation);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
