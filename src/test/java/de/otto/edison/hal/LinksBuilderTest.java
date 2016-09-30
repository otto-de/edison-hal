package de.otto.edison.hal;

import org.junit.Test;

import static de.otto.edison.hal.Link.item;
import static de.otto.edison.hal.Link.linkBuilder;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.copyOf;
import static de.otto.edison.hal.Links.linkingTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class LinksBuilderTest {

    @Test
    public void shouldAddLinksWithNewRel() {
        final Links links = linkingTo(self("/foo"));
        final Links extendedLinks = copyOf(links)
                .with(linkingTo(item("/bar")))
                .build();
        assertThat(extendedLinks.getLinkBy("self").isPresent(), is(true));
        assertThat(extendedLinks.getLinkBy("item").isPresent(), is(true));
    }

    @Test
    public void shouldAddLinkWithNewRel() {
        final Links links = linkingTo(self("/foo"));
        final Links extendedLinks = copyOf(links)
                .with(item("/bar"))
                .build();
        assertThat(extendedLinks.getLinkBy("self").isPresent(), is(true));
        assertThat(extendedLinks.getLinkBy("item").isPresent(), is(true));
    }

    @Test
    public void shouldAddLinkToExistingRel() {
        final Links links = linkingTo(item("/foo"));
        final Links extendedLinks = copyOf(links)
                .with(item("/bar"))
                .build();
        assertThat(extendedLinks.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldAddLinksToExistingRel() {
        final Links links = linkingTo(item("/foo"));
        final Links extendedLinks = copyOf(links)
                .with(linkingTo(item("/bar")))
                .build();
        assertThat(extendedLinks.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldNotDuplicateLink() {
        final Links links = linkingTo(item("/foo"), item("/bar"));
        final Links extendedLinks = copyOf(links)
                .with(item("/bar"))
                .build();
        assertThat(extendedLinks.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldNotDuplicateLinks() {
        final Links links = linkingTo(item("/foo"), item("/bar"));
        final Links extendedLinks = copyOf(links)
                .with(linkingTo(item("/bar")))
                .build();
        assertThat(extendedLinks.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldAddDifferentButNotEquivalentLinks() {
        final Links links = linkingTo(
                linkBuilder("myrel", "/foo")
                        .withType("some type")
                        .withProfile("some profile")
                        .build());
        final Links extendedLinks = copyOf(links)
                .with(linkingTo(linkBuilder("myrel", "/foo")
                        .withType("some DIFFERENT type")
                        .withProfile("some profile")
                        .withTitle("ignored title")
                        .withDeprecation("ignored deprecation")
                        .withHrefLang("ignored language")
                        .withName("ignored name")
                        .build()))
                .build();
        assertThat(extendedLinks.getLinksBy("myrel"), hasSize(2));
    }

    @Test
    public void shouldNotAddEquivalentLinks() {
        final Links links = linkingTo(
                linkBuilder("myrel", "/foo")
                        .withType("some type")
                        .withProfile("some profile")
                        .build());
        final Links extendedLinks = copyOf(links)
                .with(linkingTo(linkBuilder("myrel", "/foo")
                        .withType("some type")
                        .withProfile("some profile")
                        .withName("foo")
                        .build()))
                .build();
        assertThat(extendedLinks.getLinksBy("myrel"), hasSize(1));
    }
}