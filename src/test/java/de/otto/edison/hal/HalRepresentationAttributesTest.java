package de.otto.edison.hal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static de.otto.edison.hal.HalParser.parse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class HalRepresentationAttributesTest {

    static class NestedHalRepresentation extends HalRepresentation {
        @JsonProperty
        public List<HalRepresentation> nested;
    }

    @Test
    public void shouldParseExtraAttributes() throws IOException {
        // given
        final String json = "{" +
                "\"foo\":\"Hello World\"," +
                "\"bar\":[\"Hello\", \"World\"]" +
                "}";
        // when
        HalRepresentation resource = parse(json).as(HalRepresentation.class);
        // then
        assertThat(resource.getAttributes().keySet(), containsInAnyOrder("foo", "bar"));
        assertThat(resource.getAttribute("foo").asText(), is("Hello World"));
        assertThat(resource.getAttribute("bar").at("/0").asText(), is("Hello"));
        assertThat(resource.getAttribute("bar").at("/1").asText(), is("World"));
    }

    @Test
    public void shouldGenerateExtraAttributes() throws IOException {
        // given
        final String json = "{" +
                "\"foo\":\"Hello World\"," +
                "\"foobar\":{\"foo\":\"bar\"}," +
                "\"bar\":[\"Hello\",\"World\"]" +
                "}";
        // when
        HalRepresentation resource = parse(json).as(HalRepresentation.class);
        final String generatedJson = new ObjectMapper().writeValueAsString(resource);
        // then
        assertThat(generatedJson, is(json));
    }

    @Test
    public void shouldParseAttributesInNestedResourceObjects() throws IOException {
        // given
        final String json = "{\n" +
                "    \"nested\" : [\n" +
                "        {\n" +
                "           \"name\" : \"Some issue\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        // when
        NestedHalRepresentation resource = parse(json).as(NestedHalRepresentation.class);
        // then
        assertThat(resource.nested.get(0).getAttribute("name").textValue(), is("Some issue"));
    }

    @Test
    public void shouldGenerateAttributesInNestedResourceObjects() throws IOException {
        // given
        final String json = "{\n" +
                "    \"nested\" : [\n" +
                "        {\n" +
                "           \"name\" : \"issue\",\n" +
                "           \"description\" : \"description\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        // when
        NestedHalRepresentation resource = parse(json).as(NestedHalRepresentation.class);
        final String generatedJson = new ObjectMapper().writeValueAsString(resource);
        // then
        assertThat(generatedJson, is(json.replaceAll("\\s", "")));
    }
}
