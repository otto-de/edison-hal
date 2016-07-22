package de.otto.edison.hal;

import org.testng.annotations.Test;

import static de.otto.edison.hal.Link.collection;
import static de.otto.edison.hal.Link.item;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Created by guido on 21.07.16.
 */
public class LinksTest {

    @Test
    public void shouldCreateEmptyLinks() {
        Links links = emptyLinks();
        assertThat(links.isEmpty(), is(true));
    }

    @Test
    public void shouldCreateLinks() {
        final Links links = linkingTo(self("http://example.org"));
        assertThat(links.getLinkBy("self").isPresent(), is(true));
        assertThat(links.getLinkBy("self").get().getRel(), is("self"));
        assertThat(links.getLinkBy("self").get().getHref(), is("http://example.org"));
    }

    @Test
    public void shouldCreateMultipleLinks() {
        final Links links = linkingTo(
                self("http://example.org/items/42"),
                collection("http://example.org/items")
        );
        assertThat(links.getLinkBy("self").isPresent(), is(true));
        assertThat(links.getLinkBy("collection").isPresent(), is(true));
    }

    @Test
    public void shouldGetFirstLink() {
        final Links links = linkingTo(
                item("http://example.org/items/42"),
                item("http://example.org/items/44")
        );
        assertThat(links.getLinkBy("item").isPresent(), is(true));
        assertThat(links.getLinkBy("item").get().getHref(), is("http://example.org/items/42"));
    }

    @Test
    public void shouldGetEmptyLinkForUnknownRel() {
        final Links links = emptyLinks();
        assertThat(links.getLinkBy("item").isPresent(), is(false));
    }

    @Test
    public void shouldGetAllLinks() {
        final Links links = linkingTo(
                item("http://example.org/items/42"),
                item("http://example.org/items/44")
        );
        assertThat(links.getLinksBy("item"), hasSize(2));
        assertThat(links.getLinksBy("item").get(0).getHref(), is("http://example.org/items/42"));
        assertThat(links.getLinksBy("item").get(1).getHref(), is("http://example.org/items/44"));
    }

    @Test
    public void shouldGetEmptyListForUnknownRel() {
        final Links links = emptyLinks();
        assertThat(links.getLinksBy("item"), hasSize(0));
    }

}