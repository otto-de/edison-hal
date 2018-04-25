package de.otto.edison.hal;

import org.junit.Test;

import java.util.List;

import static de.otto.edison.hal.Embedded.Builder.copyOf;
import static de.otto.edison.hal.Embedded.*;
import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


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
    public void shouldCreateSingleEmbedded() {
        Embedded embedded = embedded("foo", new HalRepresentation());
        assertThat(embedded.getItemsBy("foo"), hasSize(1));
    }

    @Test
    public void shouldCreateEmbeddedAsArray() {
        Embedded embedded = embedded("foo", singletonList(new HalRepresentation()));
        assertThat(embedded.hasItem("foo"), is(true));
        assertThat(embedded.isArray("foo"), is(true));
    }

    @Test
    public void shouldCreateEmbeddedAsSingleObject() {
        Embedded embedded = embedded("foo", new HalRepresentation());
        assertThat(embedded.hasItem("foo"), is(true));
        assertThat(embedded.isArray("foo"), is(false));
    }

    @Test
    public void shouldCreateEmbeddedWithBuilder() {
        Embedded embedded = embeddedBuilder()
                .with("foo", singletonList(new HalRepresentation()))
                .with("bar", singletonList(new HalRepresentation()))
                .build();
        assertThat(embedded.getItemsBy("foo"), hasSize(1));
        assertThat(embedded.getItemsBy("bar"), hasSize(1));
        assertThat(embedded.getItemsBy("foobar"), hasSize(0));
    }

    @Test
    public void shouldAddRelToEmbeddedUsingBuilder() {
        Embedded embedded = copyOf(embedded("foo", singletonList(new HalRepresentation())))
                .with("bar", singletonList(new HalRepresentation())).build();
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

    @Test
    public void shouldGetAllLinkrelations() {
        Embedded embedded = embeddedBuilder()
                .with("foo", singletonList(new HalRepresentation()))
                .with("bar", singletonList(new HalRepresentation()))
                .build();
        assertThat(embedded.getRels(), contains("foo", "bar"));
    }

    @Test
    public void shouldReplaceRelsWithCuriedRels() {
        Curies curies = Curies.curies(asList(
                curi("test", "http://example.com/rels/{rel}"))
        );
        Embedded embedded = embeddedBuilder()
                .with("http://example.com/rels/foo", singletonList(new HalRepresentation()))
                .with("http://example.com/rels/bar", singletonList(new HalRepresentation()))
                .build();
        assertThat(embedded.using(curies).getRels(), contains("test:foo", "test:bar"));
    }

    @Test
    public void shouldReplaceNestedRelsWithCuriedRels() {
        Curies curies = Curies.curies(asList(
                curi("test", "http://example.com/rels/{rel}"))
        );
        Embedded embedded = embeddedBuilder()
                .with("http://example.com/rels/foo", singletonList(
                        new HalRepresentation(null,
                                embeddedBuilder()
                                        .with("http://example.com/rels/bar", singletonList(new HalRepresentation()))
                                        .build())))
                .using(curies)
                .build();
        assertThat(embedded.getRels(), contains("test:foo"));
        assertThat(embedded.getItemsBy("test:foo").get(0).getEmbedded().getRels(), contains("test:bar"));
    }

    @Test
    public void shouldReplaceNestedLinkRelsWithCuriedLinkRels() {
        Curies curies = Curies.curies(asList(
                curi("test", "http://example.com/rels/{rel}"))
        );
        Embedded embedded = embeddedBuilder()
                .with("http://example.com/rels/foo", singletonList(
                        new HalRepresentation(
                                linkingTo()
                                        .single(link("http://example.com/rels/bar", "http://example.com"))
                                        .build()
                        )))
                .using(curies)
                .build();
        assertThat(embedded.getItemsBy("test:foo").get(0).getLinks().getRels(), contains("test:bar"));
    }

    @Test
    public void shouldReplaceRelsWithCuriedRelsUsingBuilder() {
        Curies curies = Curies.curies(asList(
                curi("test", "http://example.com/rels/{rel}"))
        );
        Embedded embedded = embeddedBuilder()
                .with("http://example.com/rels/foo", singletonList(new HalRepresentation()))
                .with("http://example.com/rels/bar", singletonList(new HalRepresentation()))
                .using(curies)
                .build();
        assertThat(embedded.getRels(), contains("test:foo", "test:bar"));
    }

    static class TestHalRepresentation extends HalRepresentation {
    }
}