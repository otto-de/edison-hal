package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static de.otto.edison.hal.Link.*;
import static de.otto.edison.hal.Links.linkingTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class LinkTest {

    @Test
    public void shouldBuildSelfLink() {
        final Link self = self("http://example.org");
        assertThat(self.getRel(), is("self"));
        assertThat(self.getHref(), is("http://example.org"));
    }

    @Test
    public void shouldBuildProfileLink() {
        final Link profile = profile("http://example.org/profiles/test");
        assertThat(profile.getRel(), is("profile"));
        assertThat(profile.getHref(), is("http://example.org/profiles/test"));
    }

    @Test
    public void shouldBuildItemLink() {
        final Link self = item("http://example.org/items/42");
        assertThat(self.getRel(), is("item"));
        assertThat(self.getHref(), is("http://example.org/items/42"));
    }

    @Test
    public void shouldBuildCollectionLink() {
        final Link self = collection("http://example.org/items");
        assertThat(self.getRel(), is("collection"));
        assertThat(self.getHref(), is("http://example.org/items"));
    }

    @Test
    public void shouldBuildTemplatedLink() {
        final Link self = link("myRel", "/test/{foo}");
        assertThat(self.getRel(), is("myRel"));
        assertThat(self.getHref(), is("/test/{foo}"));
        assertThat(self.isTemplated(), is(true));
    }

    @Test
    public void shouldBuildTemplatedLinkUsingLinkBuilder() {
        final Link self = linkBuilder("myRel", "/test/{foo}")
                .withHrefLang("de_DE")
                .withTitle("title")
                .withName("name")
                .withProfile("my-profile")
                .withType("type")
                .build();
        assertThat(self.getRel(), is("myRel"));
        assertThat(self.getHref(), is("/test/{foo}"));
        assertThat(self.isTemplated(), is(true));
        assertThat(self.getHreflang(), is("de_DE"));
        assertThat(self.getTitle(), is("title"));
        assertThat(self.getName(), is("name"));
        assertThat(self.getType(), is("type"));
        assertThat(self.getProfile(), is("my-profile"));
    }

    @Test
    public void shouldBuildLinkUsingBuilder() {
        final Link link = linkBuilder("myRel", "/test/foo")
                .withHrefLang("de_DE")
                .withTitle("title")
                .withName("name")
                .withProfile("my-profile")
                .withType("type")
                .build();
        assertThat(link.getRel(), is("myRel"));
        assertThat(link.getHref(), is("/test/foo"));
        assertThat(link.isTemplated(), is(false));
        assertThat(link.getHreflang(), is("de_DE"));
        assertThat(link.getTitle(), is("title"));
        assertThat(link.getName(), is("name"));
        assertThat(link.getType(), is("type"));
        assertThat(link.getProfile(), is("my-profile"));
    }

    @Test
    public void shouldBuildCuri() throws JsonProcessingException {
        final Link link = Link.curi("t", "http://example.org/{rel}");
        assertThat(link.getName(), is("t"));
        assertThat(link.getRel(), is("curies"));
        assertThat(link.isTemplated(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToBuildCuriWithoutRelPlaceholder() throws JsonProcessingException {
        Link.curi("t", "http://example.org/rel");
    }

    @Test
    public void shouldBeEquivalent() {
        final Link first = linkBuilder("myrel", "/foo")
                .withType("some type")
                .withProfile("some profile")
                .build();
        final Link other = linkBuilder("myrel", "/foo")
                .withType("some type")
                .withProfile("some profile")
                .withTitle("ignored title")
                .withDeprecation("ignored deprecation")
                .withHrefLang("ignored language")
                .withName("ignored name")
                .build();
        assertThat(first.isEquivalentTo(other), is(true));

    }

    @Test
    public void shouldNotBeEquivalentIfHrefIsDifferent() {
        final Link first = link("myrel", "/foo");
        final Link other = link("myrel", "/bar");
        assertThat(first.isEquivalentTo(other), is(false));
    }

    @Test
    public void shouldNotBeEquivalentIfRelIsDifferent() {
        final Link first = link("myrel", "/foo");
        final Link other = link("myOtherRel", "/foo");
        assertThat(first.isEquivalentTo(other), is(false));
    }

    @Test
    public void shouldNotBeEquivalentIfTypeIsDifferent() {
        final Link first = linkBuilder("myrel", "/foo")
                .withType("some type")
                .build();
        final Link other = linkBuilder("myrel", "/foo")
                .withType("some other type")
                .build();
        assertThat(first.isEquivalentTo(other), is(false));
    }

    @Test
    public void shouldNotBeEquivalentIfProfileIsDifferent() {
        final Link first = linkBuilder("myrel", "/foo")
                .withType("some profile")
                .build();
        final Link other = linkBuilder("myrel", "/foo")
                .withType("some other profile")
                .build();
        assertThat(first.isEquivalentTo(other), is(false));
    }

}