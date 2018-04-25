package de.otto.edison.hal;

import org.junit.Test;

import static de.otto.edison.hal.Link.*;
import static de.otto.edison.hal.Links.copyOf;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LinksBuilderTest {

    @Test
    public void shouldAddLinksWithNewRel() {
        final Links links = linkingTo().self("/foo").build();
        final Links extendedLinks = copyOf(links)
                .with(linkingTo().item("/bar").build())
                .build();
        assertThat(extendedLinks.getLinkBy("self").isPresent(), is(true));
        assertThat(extendedLinks.getLinkBy("item").isPresent(), is(true));
    }

    @Test
    public void shouldAddLinkWithNewRel() {
        final Links links = linkingTo().self("/foo").build();
        final Links extendedLinks = copyOf(links)
                .array(item("/bar"))
                .build();
        assertThat(extendedLinks.getLinkBy("self").isPresent(), is(true));
        assertThat(extendedLinks.getLinkBy("item").isPresent(), is(true));
    }

    @Test
    public void shouldAddLinkToExistingItemRel() {
        final Links links = linkingTo().item("/foo").build();
        final Links extendedLinks = copyOf(links)
                .array(item("/bar"))
                .build();
        assertThat(extendedLinks.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldAddLinkToExistingArrayRel() {
        final Links links = linkingTo().array(link("some-rel", "/foo")).build();
        final Links extendedLinks = copyOf(links)
                .array(link("some-rel", "/bar"))
                .build();
        assertThat(extendedLinks.getLinksBy("some-rel"), hasSize(2));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAddLinkToExistingArrayRelUsingSingle() {
        final Links links = linkingTo().array(link("some-rel", "/foo")).build();
        copyOf(links)
                .single(link("some-rel", "/bar"))
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToAddLinkToExistingSingleRel() {
        final Links links = linkingTo().single(link("some-rel", "/foo")).build();
        copyOf(links)
                .array(link("some-rel", "/bar"))
                .build();
    }

    @Test
    public void shouldAddLinksToExistingArrayRel() {
        final Links links = linkingTo().array(link("some-rel", "/foo")).build();
        final Links extendedLinks = copyOf(links)
                .array(asList(item("/bar"), item("/foobar")))
                .build();
        assertThat(extendedLinks.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldNotDuplicateLink() {
        final Links links = linkingTo().item("/foo").item("/bar").build();
        final Links extendedLinks = copyOf(links)
                .array(item("/bar"))
                .build();
        assertThat(extendedLinks.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldAddDifferentButNotEquivalentLinks() {
        final Links links = linkingTo()
                .array(linkBuilder("myrel", "/foo")
                        .withType("some type")
                        .withProfile("some profile")
                        .build())
                .build();
        final Links extendedLinks = copyOf(links)
                .array(linkBuilder("myrel", "/foo")
                        .withType("some DIFFERENT type")
                        .withProfile("some profile")
                        .withTitle("ignored title")
                        .withDeprecation("ignored deprecation")
                        .withHrefLang("ignored language")
                        .withName("ignored name")
                        .build())
                .build();
        assertThat(extendedLinks.getLinksBy("myrel"), hasSize(2));
    }

    @Test
    public void shouldNotAddEquivalentLinks() {
        final Links links = linkingTo()
                .array(linkBuilder("myrel", "/foo")
                        .withType("some type")
                        .withProfile("some profile")
                        .build())
                .build();
        final Links extendedLinks = copyOf(links)
                .array(linkBuilder("myrel", "/foo")
                        .withType("some type")
                        .withProfile("some profile")
                        .withName("foo")
                        .build())
                .build();
        assertThat(extendedLinks.getLinksBy("myrel"), hasSize(1));
    }

    @Test
    public void shouldMergeCuries() {
        final Links.Builder someLinks = Links.linkingTo().curi("x", "http://example.com/rels/{rel}");
        final Links otherLinks = Links.linkingTo().curi("y", "http://example.org/rels/{rel}").build();

        final Links mergedLinks = someLinks.with(otherLinks).build();

        assertThat(mergedLinks.getLinksBy("curies"), contains(
                curi("x", "http://example.com/rels/{rel}"),
                curi("y", "http://example.org/rels/{rel}")));
    }
}