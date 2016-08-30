package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.testng.annotations.Test;

import java.util.List;

import static de.otto.edison.hal.Link.*;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.Links.linksBuilder;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
    public void shouldCreateMultipleLinksFromList() {
        final Links links = linkingTo(asList(
                self("http://example.org/items"),
                item("http://example.org/items/1"),
                item("http://example.org/items/2")
        ));
        assertThat(links.getLinkBy("self").isPresent(), is(true));
        assertThat(links.getLinkBy("item").isPresent(), is(true));
        assertThat(links.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldCreateMultipleLinksUsingBuilder() {
        final Links links = linksBuilder()
                .with(self("http://example.org/items"))
                .with(asList(
                        item("http://example.org/items/1"),
                        item("http://example.org/items/2")))
                .build();
        assertThat(links.getLinkBy("self").isPresent(), is(true));
        assertThat(links.getLinkBy("item").isPresent(), is(true));
        assertThat(links.getLinksBy("item"), hasSize(2));
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
    public void shouldStreamAllLinks() {
        final Links links = linkingTo(
                link("foo", "http://example.org/foo"),
                link("bar", "http://example.org/bar")
        );
        assertThat(links.stream().count(), is(2L));
    }

    @Test
    public void shouldGetAllLinkrelations() {
        final Links links = linkingTo(
                link("foo", "http://example.org/foo"),
                link("bar", "http://example.org/bar")
        );
        assertThat(links.getRels(), contains("foo", "bar"));
    }

    @Test
    public void shouldGetEmptyListForUnknownRel() {
        final Links links = emptyLinks();
        assertThat(links.getLinksBy("item"), hasSize(0));
    }

    @Test
    public void shouldGetCuriedLinksFromFullRel() throws JsonProcessingException {
        final Links links = linkingTo(
                curi("o", "http://spec.otto.de/rels/{rel}"),
                link("o:product", "http://example.org/products/42"),
                link("o:product", "http://example.org/products/44")
        );
        final List<String> productHrefs = links.getLinksBy("http://spec.otto.de/rels/product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

    @Test
    public void shouldGetCuriedLinksFromCuriedRel() throws JsonProcessingException {
        final Links links = linkingTo(
                curi("o", "http://spec.otto.de/rels/{rel}"),
                link("o:product", "http://example.org/products/42"),
                link("o:product", "http://example.org/products/44")
        );
        final List<String> productHrefs = links.getLinksBy("o:product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

    @Test
    public void shouldReplaceFullRelsWithCuriedRels() throws JsonProcessingException {
        final Links links = linkingTo(
                curi("o", "http://spec.otto.de/rels/{rel}"),
                link("http://spec.otto.de/rels/product", "http://example.org/products/42"),
                link("http://spec.otto.de/rels/product", "http://example.org/products/44")
        );
        final List<String> productHrefs = links.getLinksBy("o:product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

    @Test
    public void shouldIgnoreMissingCuries() throws JsonProcessingException {
        final Links links = linkingTo(
                link("o:product", "http://example.org/products/42"),
                link("o:product", "http://example.org/products/44")
        );
        final List<String> productHrefs = links.getLinksBy("o:product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

}