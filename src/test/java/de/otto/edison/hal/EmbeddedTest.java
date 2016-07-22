package de.otto.edison.hal;

import org.testng.annotations.Test;

import java.util.List;

import static de.otto.edison.hal.Embedded.Builder.copyOf;
import static de.otto.edison.hal.Embedded.embedded;
import static de.otto.edison.hal.Embedded.embeddedBuilder;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Created by guido on 19.07.16.
 */
public class EmbeddedTest {

    @Test
    public void shouldCreateEmptyEmbedded() {
        Embedded embedded = emptyEmbedded();
        assertThat(embedded.getItemsBy("foo"), is(emptyList()));
        assertThat(embedded.getItemsBy("foo", HalRepresentation.class), is(emptyList()));
    }

    @Test
    public void shouldCreateEmbedded() {
        Embedded embedded = embedded("foo", singletonList(new HalRepresentation()));
        assertThat(embedded.getItemsBy("foo"), hasSize(1));
    }

    @Test
    public void shouldCreateEmbeddedWithBuilder() {
        Embedded embedded = embeddedBuilder()
                .withEmbedded("foo", singletonList(new HalRepresentation()))
                .withEmbedded("bar", singletonList(new HalRepresentation()))
                .build();
        assertThat(embedded.getItemsBy("foo"), hasSize(1));
        assertThat(embedded.getItemsBy("bar"), hasSize(1));
        assertThat(embedded.getItemsBy("foobar"), hasSize(0));
    }

    @Test
    public void shouldAddRelToEmbeddedUsingBuilder() {
        Embedded embedded = copyOf(embedded("foo", singletonList(new HalRepresentation())))
                .withEmbedded("bar", singletonList(new HalRepresentation())).build();
        assertThat(embedded.getItemsBy("foo"), hasSize(1));
        assertThat(embedded.getItemsBy("bar"), hasSize(1));
        assertThat(embedded.getItemsBy("foobar"), hasSize(0));
    }

    @Test
    public void shouldGetItemsAsType() {
        Embedded embedded = embedded("foo", asList(
                new TestHalRepresentation(),
                new TestHalRepresentation())
        );
        final List<TestHalRepresentation> testHalRepresentations = embedded.getItemsBy("foo", TestHalRepresentation.class);
        assertThat(testHalRepresentations, hasSize(2));
        assertThat(testHalRepresentations.get(0), instanceOf(TestHalRepresentation.class));
    }

    static class TestHalRepresentation extends HalRepresentation {
    }
}