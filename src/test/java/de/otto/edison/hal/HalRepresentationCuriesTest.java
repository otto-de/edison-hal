package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static de.otto.edison.hal.Curies.curies;
import static de.otto.edison.hal.Embedded.embedded;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class HalRepresentationCuriesTest {

    @Test
    public void shouldRenderSingleCuriAsArray() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}")
                        .single(
                                link("x:foo", "http://example.org/test"),
                                link("x:bar", "http://example.org/test"))
                        .build()
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
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}")
                        .curi("y", "http://example.com/rels/{rel}")
                        .single(link("x:foo", "http://example.org/test"))
                        .single(link("y:bar", "http://example.org/test"))
                        .build()
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
                linkingTo()
                        .curi("x", "http://example.org/rels/{rel}")
                        .single(link("http://example.org/rels/foo", "http://example.org/test"))
                        .build()
        );
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"curies\":[{\"href\":\"http://example.org/rels/{rel}\",\"templated\":true,\"name\":\"x\"}],\"x:foo\":{\"href\":\"http://example.org/test\"}}}"));
    }

    @Test
    public void shouldConstructWithCuries() {
        final Curies curies = curies(asList(curi("x", "http://example.com/rels/{rel}")));
        final HalRepresentation hal = new HalRepresentation(emptyLinks(), emptyEmbedded(), curies);
        assertThat(hal.getCuries().resolve("http://example.com/rels/foo"), is("x:foo"));
    }

    @Test
    public void shouldConstructWithLinksAndCuries() {
        final Curies curies = curies(asList(curi("x", "http://example.com/rels/{rel}")));
        final HalRepresentation hal = new HalRepresentation(
                linkingTo().single(link("http://example.com/rels/foo", "http://example.com")).build(),
                emptyEmbedded(),
                curies);
        assertThat(hal.getLinks().getRels(), contains("x:foo"));
        assertThat(hal.getLinks().getLinkBy("x:foo").isPresent(), is(true));
        assertThat(hal.getLinks().getLinkBy("http://example.com/rels/foo").isPresent(), is(true));
    }

    @Test
    public void shouldConstructWithEmbeddedAndCuries() {
        final Curies curies = curies(asList(curi("x", "http://example.com/rels/{rel}")));
        final HalRepresentation hal = new HalRepresentation(
                emptyLinks(),
                embedded(
                        "http://example.com/rels/nested",
                        singletonList(new HalRepresentation(
                                linkingTo().single(link("http://example.com/rels/foo", "http://example.com")).build(),
                                emptyEmbedded()
                        ))),
                curies);
        assertThat(hal.getEmbedded().getRels(), contains("x:nested"));
        final HalRepresentation embedded = hal.getEmbedded().getItemsBy("http://example.com/rels/nested").get(0);
        assertThat(embedded.getLinks().getLinkBy("x:foo").isPresent(), is(true));
        assertThat(embedded.getLinks().getLinkBy("http://example.com/rels/foo").isPresent(), is(true));
    }

    @Test
    public void shouldInheritCuries() {
        final HalRepresentation embeddedHal = new HalRepresentation();
        final HalRepresentation representation = new HalRepresentation(emptyLinks(), embedded("http://example.com/rels/foo", singletonList(embeddedHal)));
        representation.mergeWithEmbedding(curies(singletonList(curi("x", "http://example.com/rels/{rel}"))));
        assertThat(embeddedHal.getCuries().resolve("http://example.com/rels/foo"), is("x:foo"));
    }

    @Test
    public void shouldRemoveDuplicateCuries() {
        // given
        final HalRepresentation embeddedHal = new HalRepresentation(linkingTo().curi("x", "http://example.com/rels/{rel}").build());
        final HalRepresentation representation = new HalRepresentation(linkingTo().curi("x", "http://example.com/rels/{rel}").build(), embedded("http://example.com/rels/foo", singletonList(embeddedHal)));
        // when
        final HalRepresentation embeddedAfterCreation = representation.getEmbedded().getItemsBy("x:foo").get(0);
        // then
        assertThat(embeddedAfterCreation.getCuries().resolve("http://example.com/rels/foo"), is("x:foo"));
        assertThat(embeddedAfterCreation.getLinks().getLinksBy("curies"), is(empty()));
        assertThat(representation.getLinks().getLinksBy("curies"), contains(curi("x", "http://example.com/rels/{rel}")));
        // but
        assertThat(embeddedHal.getLinks().getLinksBy("curies"), is(empty()));
    }

}
