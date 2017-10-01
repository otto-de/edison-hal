package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static de.otto.edison.hal.Embedded.embedded;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.RelRegistry.defaultRelRegistry;
import static de.otto.edison.hal.RelRegistry.relRegistry;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class HalRepresentationCuriesTest {

    @Test
    public void shouldRenderSingleCuriAsArray() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        curi("x", "http://example.org/rels/{rel}"),
                        link("x:foo", "http://example.org/test"),
                        link("x:bar", "http://example.org/test"))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}],\"x:foo\":{\"href\":\"http://example.org/test\"},\"x:bar\":{\"href\":\"http://example.org/test\"}}}"));
    }

    @Test
    public void shouldRenderCuries() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        curi("x", "http://example.org/rels/{rel}"),
                        curi("y", "http://example.com/rels/{rel}"),
                        link("x:foo", "http://example.org/test"),
                        link("y:bar", "http://example.org/test"))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"},{\"href\":\"http://example.com/rels/{rel}\",\"templated\":true,\"name\":\"y\"}],\"x:foo\":{\"href\":\"http://example.org/test\"},\"y:bar\":{\"href\":\"http://example.org/test\"}}}"));
    }

    @Test
    public void shouldReplaceFullRelWithCuri() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(
                        curi("x", "http://example.org/rels/{rel}"),
                        link("http://example.org/rels/foo", "http://example.org/test"))
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}],\"x:foo\":{\"href\":\"http://example.org/test\"}}}"));
    }

    @Test
    public void shouldConstructWithRelRegistry() {
        final RelRegistry relRegistry = relRegistry(asList(curi("x", "http://example.com/rels/{rel}")));
        final HalRepresentation hal = new HalRepresentation(emptyLinks(), emptyEmbedded(), relRegistry);
        assertThat(hal.getRelRegistry().resolve("http://example.com/rels/foo"), is("x:foo"));
    }

    @Test
    public void shouldConstructWithLinksAndRelRegistry() {
        final RelRegistry relRegistry = relRegistry(asList(curi("x", "http://example.com/rels/{rel}")));
        final HalRepresentation hal = new HalRepresentation(
                linkingTo(link("http://example.com/rels/foo", "http://example.com")),
                emptyEmbedded(),
                relRegistry);
        assertThat(hal.getLinks().getRels(), contains("x:foo"));
        assertThat(hal.getLinks().getLinkBy("x:foo").isPresent(), is(true));
        assertThat(hal.getLinks().getLinkBy("http://example.com/rels/foo").isPresent(), is(true));
    }

    @Test
    public void shouldConstructWithEmbeddedAndRelRegistry() {
        final RelRegistry relRegistry = relRegistry(asList(curi("x", "http://example.com/rels/{rel}")));
        final HalRepresentation hal = new HalRepresentation(
                emptyLinks(),
                embedded(
                        "http://example.com/rels/nested",
                        singletonList(new HalRepresentation(
                                linkingTo(link("http://example.com/rels/foo", "http://example.com")),
                                emptyEmbedded()
                        ))),
                relRegistry);
        assertThat(hal.getEmbedded().getRels(), contains("x:nested"));
        final HalRepresentation embedded = hal.getEmbedded().getItemsBy("http://example.com/rels/nested").get(0);
        assertThat(embedded.getLinks().getLinkBy("x:foo").isPresent(), is(true));
        assertThat(embedded.getLinks().getLinkBy("http://example.com/rels/foo").isPresent(), is(true));
    }

    @Test
    public void shouldInheritRelRegistry() {
        final HalRepresentation embeddedHal = new HalRepresentation();
        final HalRepresentation representation = new HalRepresentation(emptyLinks(), embedded("http://example.com/rels/foo", singletonList(embeddedHal)));
        representation.mergeWithEmbedding(relRegistry(singletonList(curi("x", "http://example.com/rels/{rel}"))));
        assertThat(embeddedHal.getRelRegistry().resolve("http://example.com/rels/foo"), is("x:foo"));
    }

}
