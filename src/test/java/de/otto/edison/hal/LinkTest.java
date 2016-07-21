package de.otto.edison.hal;

import org.testng.annotations.Test;

import static de.otto.edison.hal.Link.*;
import static de.otto.edison.hal.Link.templated;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by guido on 21.07.16.
 */
public class LinkTest {

    @Test
    public void shouldBuildSelfLink() {
        final Link self = self("http://example.org");
        assertThat(self.rel, is("self"));
        assertThat(self.href, is("http://example.org"));
    }

    @Test
    public void shouldBuildProfileLink() {
        final Link profile = profile("http://example.org/profiles/test");
        assertThat(profile.rel, is("profile"));
        assertThat(profile.href, is("http://example.org/profiles/test"));
    }

    @Test
    public void shouldBuildItemLink() {
        final Link self = item("http://example.org/items/42");
        assertThat(self.rel, is("item"));
        assertThat(self.href, is("http://example.org/items/42"));
    }

    @Test
    public void shouldBuildCollectionLink() {
        final Link self = collection("http://example.org/items");
        assertThat(self.rel, is("collection"));
        assertThat(self.href, is("http://example.org/items"));
    }

    @Test
    public void shouldBuildTemplatedLink() {
        final Link self = templated("myRel", "/test/{foo}");
        assertThat(self.rel, is("myRel"));
        assertThat(self.href, is("/test/{foo}"));
        assertThat(self.templated, is(true));
    }

    @Test
    public void shouldBuildLinkUsingTemplatedBuilder() {
        final Link self = templatedBuilderBuilder("myRel", "/test/{foo}")
                .withHrefLang("de_DE")
                .withTitle("title")
                .withName("name")
                .withProfile("my-profile")
                .withType("type")
                .build();
        assertThat(self.rel, is("myRel"));
        assertThat(self.href, is("/test/{foo}"));
        assertThat(self.templated, is(true));
        assertThat(self.hreflang, is("de_DE"));
        assertThat(self.title, is("title"));
        assertThat(self.name, is("name"));
        assertThat(self.type, is("type"));
        assertThat(self.profile, is("my-profile"));
    }
}