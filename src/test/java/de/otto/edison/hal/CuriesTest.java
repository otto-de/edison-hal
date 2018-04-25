package de.otto.edison.hal;

import org.junit.Test;

import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;
import static de.otto.edison.hal.Curies.emptyCuries;
import static de.otto.edison.hal.Curies.curies;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CuriesTest {

    @Test
    public void shouldBuildRegistryWithCuries() {
        // given
        final Curies curies = Curies.curies(
                linkingTo()
                        .curi("x", "http://example.com/rels/{rel}")
                        .curi("y", "http://example.org/rels/{rel}").build()
        );
        // then
        assertThat(curies.resolve("http://example.com/rels/foo"), is("x:foo"));
        assertThat(curies.resolve("http://example.org/rels/bar"), is("y:bar"));
    }

    @Test
    public void shouldExpandFullRel() {
        // given
        final Curies curies = Curies.curies(linkingTo().curi("x", "http://example.com/rels/{rel}").build());
        // when
        final String first = curies.expand("http://example.com/rels/foo");
        final String second = curies.expand("item");
        // then
        assertThat(first, is("http://example.com/rels/foo"));
        assertThat(second, is("item"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToRegisterNonCuriLink() {
        emptyCuries().register(link("foo", "http://example.com/foo"));
    }

    @Test
    public void shouldResolveFullUri() {
        // given
        final Curies registry = emptyCuries();
        registry.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final String resolved = registry.resolve("http://spec.otto.de/rels/foo");
        // then
        assertThat(resolved, is("o:foo"));
    }

    @Test
    public void shouldResolveCuriedUri() {
        // given
        final Curies registry = emptyCuries();
        registry.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final String resolved = registry.resolve("o:foo");
        // then
        assertThat(resolved, is("o:foo"));
    }

    @Test
    public void shouldResolveUnknownFullUri() {
        // given
        final Curies registry = emptyCuries();
        registry.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final String resolved = registry.resolve("http://www.otto.de/some/other");
        // then
        assertThat(resolved, is("http://www.otto.de/some/other"));
    }

    @Test
    public void shouldResolveUnknownCuriedUri() {
        // given
        final Curies registry = emptyCuries();
        registry.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final String resolved = registry.resolve("x:other");
        // then
        assertThat(resolved, is("x:other"));
    }

    @Test
    public void shouldMergeRegistries() {
        // given
        final Curies registry = emptyCuries();
        registry.register(curi("x", "http://x.otto.de/rels/{rel}"));
        final Curies other = emptyCuries();
        other.register(curi("u", "http://u.otto.de/rels/{rel}"));
        // when
        final Curies merged = registry.mergeWith(other);
        // then
        assertThat(merged.resolve("http://x.otto.de/rels/foo"), is("x:foo"));
        assertThat(merged.resolve("http://u.otto.de/rels/foo"), is("u:foo"));
    }

    @Test
    public void shouldMergeByReplacingExistingWithOther() {
        // given
        final Curies registry = emptyCuries();
        registry.register(curi("x", "http://x.otto.de/rels/{rel}"));
        final Curies other = emptyCuries();
        other.register(curi("x", "http://spec.otto.de/rels/{rel}"));
        // when
        final Curies merged = registry.mergeWith(other);
        // then
        assertThat(merged.resolve("http://spec.otto.de/rels/foo"), is("x:foo"));
    }

    @Test
    public void shouldMergeEmptyRegistryWithNonEmpty() {
        // given
        final Curies empty = emptyCuries();
        final Curies other = emptyCuries();
        other.register(curi("o", "http://spec.otto.de/rels/{rel}"));
        // when
        final Curies merged = empty.mergeWith(other);
        // then
        assertThat(empty, is(emptyCuries()));
        assertThat(merged.resolve("http://spec.otto.de/rels/foo"), is("o:foo"));
    }

    @Test
    public void shouldExpandCuri() {
        // given
        final Curies curies = Curies.curies(linkingTo().curi("x", "http://example.com/rels/{rel}").build());
        // when
        final String expanded = curies.expand("x:foo");
        // then
        assertThat(expanded, is("http://example.com/rels/foo"));
    }

    @Test
    public void shouldReturnCuriIfNotResolvable() {
        // given
        final Curies curies = emptyCuries();
        // when
        final String expanded = curies.expand("x:foo");
        // then
        assertThat(expanded, is("x:foo"));
    }

    @Test
    public void shouldReturnCuriIfAlreadyResolved() {
        // given
        final Curies curies = Curies.curies(linkingTo().curi("x", "http://example.com/rels/{rel}").build());
        // when
        final String expanded = curies.expand("http://example.com/rels/foo");
        // then
        assertThat(expanded, is("http://example.com/rels/foo"));
    }
}