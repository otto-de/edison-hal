package de.otto.edison.hal;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.util.List;

import static de.otto.edison.hal.Embedded.*;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class HalRepresentationEmbeddingTest {

    @Test
    public void shouldNotRenderEmptyEmbedded() throws JacksonException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo().self("http://example.org/test/bar").build(),
                emptyEmbedded())
        {
            public String total="4753€";
        };
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"total\":\"4753€\"" +
                        "}"));
    }

    @Test
    public void shouldRenderEmbeddedResourcesWithProperties() throws JacksonException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo().self("http://example.org/test/bar/01").build()) {public String amount="42€";},
                new HalRepresentation(linkingTo().self("http://example.org/test/bar/02").build()) {public String amount="4711€";}
        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo().self("http://example.org/test/bar").build(),
                embedded("orders", items)) {public String total="4753€";};
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_embedded\":{\"orders\":[" +
                                "{" +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}," +
                                    "\"amount\":\"42€\"" +
                                "}," +
                                "{" +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/02\"}}," +
                                    "\"amount\":\"4711€\"" +
                                "}" +
                                "]}," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"total\":\"4753€\"" +
                        "}"));
    }

    @Test
    public void shouldRenderNestedEmbeddedResourcesWithProperties() throws JacksonException {
        // given
        final List<HalRepresentation> items = List.of(
                new HalRepresentation(
                        linkingTo().self("http://example.org/test/bar/01").build(),
                        embedded("foo", singletonList(
                                new HalRepresentation(linkingTo().self("http://example.org/test/bar/02").build()) {
                                    public String amount = "4711€";
                                })
                        )) {
                    public String amount = "42€";
                }

        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo().self("http://example.org/test/bar").build(),
                embedded("orders", items)) {public String total="4753€";};
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_embedded\":{\"orders\":[" +
                            "{\"_embedded\":{\"foo\":[{" +
                                "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/02\"}}," +
                                "\"amount\":\"4711€\"}]}," +
                            "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}," +
                            "\"amount\":\"42€\"}" +
                        "]}," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"total\":\"4753€\"" +
                        "}"));
    }

    @Test
    public void shouldRenderEmbeddedResourcesWithMultipleLinks() throws JacksonException {
        // given
        final List<HalRepresentation> items = List.of(
                new HalRepresentation(linkingTo().array(
                        link("test", "http://example.org/test/bar/01"),
                        link("test", "http://example.org/test/bar/02")).build()) {
                    public String amount = "42€";
                }
        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo().self("http://example.org/test/bar").build(),
                embedded("orders", items)) {public String total="4753€";};
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_embedded\":{\"orders\":[" +
                                "{" +
                                    "\"_links\":{\"test\":[{\"href\":\"http://example.org/test/bar/01\"},{\"href\":\"http://example.org/test/bar/02\"}]}," +
                                    "\"amount\":\"42€\"" +
                                "}" +
                                "]}," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"total\":\"4753€\"" +
                        "}"));
    }

    @Test
    public void shouldRenderMultipleEmbeddedResources() throws JacksonException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo().self("http://example.org/test/bar").build(),
                embeddedBuilder()
                        .with("foo", asList(
                                new HalRepresentation(linkingTo().self("http://example.org/test/foo/01").build()),
                                new HalRepresentation(linkingTo().self("http://example.org/test/foo/02").build())
                        ))
                        .with("bar", asList(
                                new HalRepresentation(linkingTo().self("http://example.org/test/bar/01").build()),
                                new HalRepresentation(linkingTo().self("http://example.org/test/bar/02").build())
                        ))
                        .with("foobar", new HalRepresentation(Links.linkingTo().self("http://example.org/test/foobar").build()))
                        .build());
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"_embedded\":{" +
                            "\"foo\":[{" +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo/01\"}}" +
                                "},{" +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo/02\"}}" +
                                "}]," +
                            "\"bar\":[{" +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                                "},{" +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                                "}]," +
                            "\"foobar\":{" +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foobar\"}}" +
                                "}" +
                        "}" +
                "}"));
    }

    @Test
    public void shouldUseCuriesInEmbedded() throws JacksonException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo().self("http://example.org/test/bar/01").build()),
                new HalRepresentation(linkingTo().self("http://example.org/test/bar/02").build())
        );
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}").build(),
                embedded("x:orders", items)) {};
        // then
        assertThat(representation.getEmbedded().getItemsBy("x:orders"), is(items));
    }

    @Test
    public void shouldRemoveDuplicateCuriesFromEmbedded() {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo()
                        .self("http://example.org/test/bar/01")
                        .curi("x", "http://example.org/rels/{rel}")
                        .build()),
                new HalRepresentation(linkingTo()
                        .self("http://example.org/test/bar/02")
                        .curi("x", "http://example.org/rels/{rel}")
                        .build())
        );
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}").build(),
                embedded("x:orders", items)) {};
        // then
        List<HalRepresentation> embeddedItems = representation.getEmbedded().getItemsBy("x:orders");
        assertThat(embeddedItems, hasSize(2));
        embeddedItems.forEach(item -> {
            assertThat(item.getLinks().getLinkBy("curies").isPresent(), is(false));
        });
    }

    @Test
    public void shouldRemoveDuplicateCuriesFromEmbeddedUsingWithEmbedded() {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo()
                        .self("http://example.org/test/bar/01")
                        .curi("x", "http://example.org/rels/{rel}")
                        .build()),
                new HalRepresentation(linkingTo()
                        .self("http://example.org/test/bar/02")
                        .curi("x", "http://example.org/rels/{rel}")
                        .build())
        );
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}").build());

        final HalRepresentation embedding = representation.withEmbedded("x:orders", items);

        // then
        List<HalRepresentation> embeddedItems = embedding.getEmbedded().getItemsBy("x:orders");
        assertThat(embeddedItems, hasSize(2));
        embeddedItems.forEach(item -> {
            assertThat(item.getLinks().getLinkBy("curies").isPresent(), is(false));
        });
    }

    @Test
    public void shouldUseCuriesInSingleEmbedded() {
        // given
        final HalRepresentation item = new HalRepresentation(linkingTo().self("http://example.org/test/bar/01").build());
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}").build(),
                embedded("x:foo", item)) {};
        // then
        assertThat(representation.getEmbedded().getItemsBy("x:foo"), contains(item));
        assertThat(representation.getEmbedded().getItemsBy("http://example.org/rels/foo"), contains(item));
        assertThat(representation.getEmbedded().isArray("x:foo"), is(false));
        assertThat(representation.getEmbedded().isArray("http://example.org/rels/foo"), is(false));
    }

    @Test
    public void shouldUseCuriesByFullRelInEmbedded() {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo().self("http://example.org/test/bar/01").build()),
                new HalRepresentation(linkingTo().self("http://example.org/test/bar/02").build())
        );
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}").build(),
                embedded("x:orders", items)) {};
        // then
        assertThat(representation.getEmbedded().getItemsBy("http://example.org/rels/orders"), is(items));
    }

    @Test
    public void shouldUseCuriesInNestedEmbedded() {
        // given
        final List<HalRepresentation> nestedEmbedded = singletonList(new HalRepresentation(linkingTo().self("http://example.org/test/bar/02").build()));
        final List<HalRepresentation> items = singletonList(
                new HalRepresentation(
                        linkingTo()
                                .self("http://example.org/test/bar/01").build(),
                        embedded("x:foo", nestedEmbedded))
        );
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}").build(),
                embedded("x:orders", items)) {};
        // then
        final List<HalRepresentation> orders = representation.getEmbedded().getItemsBy("x:orders");
        assertThat(orders, is(items));
        assertThat(orders.get(0).getEmbedded().getItemsBy("x:foo"), is(nestedEmbedded));
        assertThat(orders.get(0).getEmbedded().getItemsBy("http://example.org/rels/foo"), is(nestedEmbedded));
    }

    @Test
    public void shouldUseCuriesInSingleNestedEmbedded() {
        // given
        final HalRepresentation nestedEmbedded = new HalRepresentation(linkingTo().self("http://example.org/test/bar/02").build());
        final List<HalRepresentation> items = singletonList(
                new HalRepresentation(
                        linkingTo()
                                .self("http://example.org/test/bar/01").build(),
                        embedded("x:foo", nestedEmbedded))
        );
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}").build(),
                embedded("x:orders", items)) {};
        // then
        assertThat(representation.getEmbedded().isArray("x:orders"), is(true));
        final List<HalRepresentation> orders = representation.getEmbedded().getItemsBy("x:orders");
        assertThat(orders, is(items));
        assertThat(orders.get(0).getEmbedded().isArray("x:foo"), is(false));
        assertThat(orders.get(0).getEmbedded().getItemsBy("x:foo"), is(singletonList(nestedEmbedded)));
        assertThat(orders.get(0).getEmbedded().getItemsBy("http://example.org/rels/foo"), is(singletonList(nestedEmbedded)));
    }

    @Test
    public void shouldReplaceEmbeddedFullRelWithCuri() throws JacksonException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo().self("http://example.org/test/bar/01").build()),
                new HalRepresentation(linkingTo().self("http://example.org/test/bar/02").build())
        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .self("http://example.org/test/bar")
                        .curi("x", "http://example.org/rels/{rel}")
                        .build(),
                embedded("http://example.org/rels/orders", items)) {};
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_embedded\":{\"x:orders\":[" +
                        "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                        "}," +
                        "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                        "}" +
                        "]}," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"},\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}]}" +
                        "}"));
    }

    @Test
    public void shouldReplaceSingleEmbeddedFullRelWithCuri() throws JacksonException {
        // given
        final HalRepresentation item = new HalRepresentation(linkingTo().self("http://example.org/test/bar/01").build());
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .self("http://example.org/test/bar")
                        .curi("x", "http://example.org/rels/{rel}")
                        .build(),
                embedded("http://example.org/rels/foo", item)) {};
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_embedded\":{\"x:foo\":" +
                        "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                        "}" +
                        "}," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"},\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}]}" +
                        "}"));
    }

    @Test
    public void shouldReplaceNestedEmbeddedFullRelWithCuri() throws JacksonException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .self("http://example.org/test/toplevel")
                        .curi("x", "http://example.org/rels/{rel}").build(),
                embedded("http://example.org/rels/orders", singletonList(
                        new HalRepresentation(
                                linkingTo().single(link("http://example.org/rels/bar", "http://example.org/test/bar/01")).build(),
                                embedded("http://example.org/rels/foo", singletonList(
                                        new HalRepresentation(linkingTo().single(link("http://example.org/rels/foobar", "http://example.org/test/bar/02")).build()
                                ))
                        )
                ))));
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_links\":{" +
                            "\"self\":{\"href\":\"http://example.org/test/toplevel\"}," +
                            "\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}]}," +
                        "\"_embedded\":{" +
                            "\"x:orders\":[{" +
                                "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/01\"}}," +
                                "\"_embedded\":{\"x:foo\":[" +
                                    "{\"_links\":{" +
                                        "\"x:foobar\":{\"href\":\"http://example.org/test/bar/02\"}}}" +
                            "]}}" +
                        "]}}"));
    }

    @Test
    public void shouldReplaceEmbeddedFullRelWithCuriInNestedLinks() throws JacksonException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo().single(link("http://example.org/rels/bar", "http://example.org/test/bar/01")).build()),
                new HalRepresentation(linkingTo().single(link("http://example.org/rels/bar", "http://example.org/test/bar/02")).build())
        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}").build(),
                embedded("http://example.org/rels/foo", items)) {};
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_embedded\":{\"x:foo\":[" +
                        "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                        "}," +
                        "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                        "}" +
                        "]}," +
                        "\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}]}" +
                        "}"));
    }

    @Test
    public void shouldReplaceEmbeddedFullRelWithCuriInNestedLinksAfterConstruction() throws JacksonException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo().single(link("http://example.org/rels/bar", "http://example.org/test/bar/01")).build()),
                new HalRepresentation(linkingTo().single(link("http://example.org/rels/bar", "http://example.org/test/bar/02")).build())
        );
        final HalRepresentation representation = new HalRepresentation(
                emptyLinks(),
                embedded("http://example.org/rels/foo", items)) {};
        // when
        representation.add(linkingTo().curi("x", "http://example.org/rels/{rel}").build());
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_embedded\":{\"x:foo\":[" +
                        "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                        "}," +
                        "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                        "}" +
                        "]}," +
                        "\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}]}" +
                        "}"));
    }


}
