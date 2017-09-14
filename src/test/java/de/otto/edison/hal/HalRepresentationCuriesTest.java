package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;
import static org.hamcrest.MatcherAssert.assertThat;
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

}
