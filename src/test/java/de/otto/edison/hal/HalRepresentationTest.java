package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.junit.Test;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Links.linkingTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HalRepresentationTest {

    @Test
    public void shouldRenderNullAttributes() throws JacksonException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo().self("http://example.org/test/bar").build(),
                emptyEmbedded())
        {
            public String someNullAttr=null;
        };
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}," +
                        "\"someNullAttr\":null" +
                        "}"));
    }

    @Test
    public void shouldSkipAnnotatedNullAttributes() throws JacksonException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                linkingTo().self("http://example.org/test/bar").build(),
                emptyEmbedded())
        {
            @JsonInclude(NON_NULL)
            public String someNullAttr=null;
        };
        // when
        final String json = JsonMapper.builder().build().writeValueAsString(representation);
        // then
        assertThat(json, is(
                "{" +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar\"}}" +
                        "}"));
    }

}
