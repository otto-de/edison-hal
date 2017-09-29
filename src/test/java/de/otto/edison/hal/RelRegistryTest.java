package de.otto.edison.hal;

import org.junit.Test;

import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.RelRegistry.defaultRelRegistry;
import static de.otto.edison.hal.RelRegistry.relRegistry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RelRegistryTest {

    @Test
    public void shouldResolveFullUri() {
        // given
        final RelRegistry registry = defaultRelRegistry();
        registry.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final String resolved = registry.resolve("http://spec.otto.de/rels/foo");
        // then
        assertThat(resolved, is("o:foo"));
    }

    @Test
    public void shouldResolveCuriedUri() {
        // given
        final RelRegistry registry = defaultRelRegistry();
        registry.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final String resolved = registry.resolve("o:foo");
        // then
        assertThat(resolved, is("o:foo"));
    }

    @Test
    public void shouldResolveUnknownFullUri() {
        // given
        final RelRegistry registry = defaultRelRegistry();
        registry.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final String resolved = registry.resolve("http://www.otto.de/some/other");
        // then
        assertThat(resolved, is("http://www.otto.de/some/other"));
    }

    @Test
    public void shouldResolveUnknownCuriedUri() {
        // given
        final RelRegistry registry = defaultRelRegistry();
        registry.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final String resolved = registry.resolve("x:other");
        // then
        assertThat(resolved, is("x:other"));
    }

    @Test
    public void shouldMergeRegistries() {
        // given
        final RelRegistry registry = defaultRelRegistry();
        registry.register(curi("x", "http://x.otto.de/rels/{rel}"));
        final RelRegistry other = defaultRelRegistry();
        other.register(curi("u", "http://u.otto.de/rels/{rel}"));
        // when
        final RelRegistry merged = registry.mergeWith(other);
        // then
        assertThat(merged.resolve("http://x.otto.de/rels/foo"), is("x:foo"));
        assertThat(merged.resolve("http://u.otto.de/rels/foo"), is("u:foo"));
    }

    @Test
    public void shouldMergeByReplacingExistingWithOther() {
        // given
        final RelRegistry registry = defaultRelRegistry();
        registry.register(curi("x", "http://x.otto.de/rels/{rel}"));
        final RelRegistry other = defaultRelRegistry();
        other.register(curi("x", "http://spec.otto.de/rels/{rel}"));
        // when
        final RelRegistry merged = registry.mergeWith(other);
        // then
        assertThat(merged.resolve("http://spec.otto.de/rels/foo"), is("x:foo"));
    }

    @Test
    public void shouldMergeEmptyRegistryWithNonEmpty() {
        // given
        final RelRegistry empty = defaultRelRegistry();
        final RelRegistry other = defaultRelRegistry();
        other.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final RelRegistry merged = empty.mergeWith(other);
        // then
        assertThat(empty, is(defaultRelRegistry()));
        assertThat(merged.resolve("http://spec.otto.de/rels/foo"), is("o:foo"));
    }

    @Test
    public void shouldExpandCuri() {
        // given
        final RelRegistry relRegistry = relRegistry(linkingTo(curi("x", "http://example.com/rels/{rel}")));
        // when
        final String expanded = relRegistry.expand("x:foo");
        // then
        assertThat(expanded, is("http://example.com/rels/foo"));
    }

    @Test
    public void shouldReturnCuriIfNotResolvable() {
        // given
        final RelRegistry relRegistry = defaultRelRegistry();
        // when
        final String expanded = relRegistry.expand("x:foo");
        // then
        assertThat(expanded, is("x:foo"));
    }

    @Test
    public void shouldReturnCuriIfAlreadyResolved() {
        // given
        final RelRegistry relRegistry = relRegistry(linkingTo(curi("x", "http://example.com/rels/{rel}")));
        // when
        final String expanded = relRegistry.expand("http://example.com/rels/foo");
        // then
        assertThat(expanded, is("http://example.com/rels/foo"));
    }
}