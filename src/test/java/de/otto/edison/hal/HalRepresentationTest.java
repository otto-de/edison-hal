package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.linkingTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HalRepresentationTest {

    @Test
    public void shouldRenderNullAttributes() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(self("http://example.org/test/bar")),
                emptyEmbedded())
        {
            public String someNullAttr=null;
        };
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"someNullAttr\":null," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}" +
                        "}"));
    }

    @Test
    public void shouldSkipAnnotatedNullAttributes() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(self("http://example.org/test/bar")),
                emptyEmbedded())
        {
            @JsonInclude(NON_NULL)
            public String someNullAttr=null;
        };
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}" +
                        "}"));
    }

}
