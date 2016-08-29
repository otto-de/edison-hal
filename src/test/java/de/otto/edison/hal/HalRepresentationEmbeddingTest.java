package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.util.List;

import static de.otto.edison.hal.Embedded.embedded;
import static de.otto.edison.hal.Embedded.embeddedBuilder;
import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by guido on 05.07.16.
 */
public class HalRepresentationEmbeddingTest {

    @Test
    public void shouldNotRenderEmptyEmbedded() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(self("http://example.org/test/bar")),
                Embedded.emptyEmbedded()) {public String total="4753€";};
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"total\":\"4753€\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}" +
                        "}"));
    }

    @Test
    public void shouldRenderEmbeddedResourcesWithProperties() throws JsonProcessingException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo(self("http://example.org/test/bar/01"))) {public String amount="42€";},
                new HalRepresentation(linkingTo(self("http://example.org/test/bar/02"))) {public String amount="4711€";}
        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(self("http://example.org/test/bar")),
                Embedded.embedded("orders", items)) {public String total="4753€";};
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"total\":\"4753€\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"_embedded\":{\"orders\":[" +
                                "{" +
                                    "\"amount\":\"42€\"," +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                                "}," +
                                "{" +
                                    "\"amount\":\"4711€\"," +
                                    "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                                "}" +
                                "]}" +
                "}"));
    }

    @Test
    public void shouldRenderEmbeddedResourcesWithMultipleLinks() throws JsonProcessingException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo(
                        link("test", "http://example.org/test/bar/01"),
                        link("test", "http://example.org/test/bar/02"))) {public String amount="42€";}
        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(self("http://example.org/test/bar")),
                Embedded.embedded("orders", items)) {public String total="4753€";};
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"total\":\"4753€\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"_embedded\":{\"orders\":[" +
                                "{" +
                                    "\"amount\":\"42€\"," +
                                    "\"_links\":{\"test\":[{\"href\":\"http://example.org/test/bar/01\"},{\"href\":\"http://example.org/test/bar/02\"}]}" +
                                "}" +
                                "]}" +
                "}"));
    }

    @Test
    public void shouldRenderMultipleEmbeddedResources() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(self("http://example.org/test/bar")),
                embeddedBuilder()
                        .with("foo", asList(
                                new HalRepresentation(linkingTo(self("http://example.org/test/foo/01"))),
                                new HalRepresentation(linkingTo(self("http://example.org/test/foo/02")))
                        ))
                        .with("bar", asList(
                                new HalRepresentation(linkingTo(self("http://example.org/test/bar/01"))),
                                new HalRepresentation(linkingTo(self("http://example.org/test/bar/02")))
                        )).build());
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
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
                                "}]" +
                        "}" +
                "}"));
    }

    @Test
    public void shouldUseCuriesInEmbedded() throws JsonProcessingException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo(self("http://example.org/test/bar/01"))),
                new HalRepresentation(linkingTo(self("http://example.org/test/bar/02")))
        );
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        curi("x", "http://example.org/rels/{rel}")),
                embedded("x:orders", items)) {};
        // then
        assertThat(representation.getEmbedded().getItemsBy("x:orders"), is(items));
    }

    @Test
    public void shouldUseCuriesByFullRelInEmbedded() throws JsonProcessingException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo(self("http://example.org/test/bar/01"))),
                new HalRepresentation(linkingTo(self("http://example.org/test/bar/02")))
        );
        // when
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        curi("x", "http://example.org/rels/{rel}")),
                embedded("x:orders", items)) {};
        // then
        assertThat(representation.getEmbedded().getItemsBy("http://example.org/rels/orders"), is(items));
    }

    @Test
    public void shouldReplaceEmbeddedFullRelWithCuri() throws JsonProcessingException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo(self("http://example.org/test/bar/01"))),
                new HalRepresentation(linkingTo(self("http://example.org/test/bar/02")))
        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        self("http://example.org/test/bar"),
                        curi("x", "http://example.org/rels/{rel}")),
                embedded("http://example.org/rels/orders", items)) {};
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}],\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"_embedded\":{\"x:orders\":[" +
                        "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                        "}," +
                        "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                        "}" +
                        "]}" +
                        "}"));
    }

    @Test
    public void shouldReplaceEmbeddedFullRelWithCuriInNestedLinks() throws JsonProcessingException {
        // given
        final List<HalRepresentation> items = asList(
                new HalRepresentation(linkingTo(link("http://example.org/rels/bar", "http://example.org/test/bar/01"))),
                new HalRepresentation(linkingTo(link("http://example.org/rels/bar", "http://example.org/test/bar/02")))
        );
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        curi("x", "http://example.org/rels/{rel}")),
                embedded("http://example.org/rels/foo", items)) {};
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}]}," +
                        "\"_embedded\":{\"x:foo\":[" +
                        "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                        "}," +
                        "{" +
                        "\"_links\":{\"x:bar\":{\"href\":\"http://example.org/test/bar/02\"}}" +
                        "}" +
                        "]}" +
                        "}"));
    }


}
