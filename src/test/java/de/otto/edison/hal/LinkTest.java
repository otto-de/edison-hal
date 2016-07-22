package de.otto.edison.hal;

import org.testng.annotations.Test;

import static de.otto.edison.hal.Link.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by guido on 21.07.16.
 */
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
        final Link self = templated("myRel", "/test/{foo}");
        assertThat(self.getRel(), is("myRel"));
        assertThat(self.getHref(), is("/test/{foo}"));
        assertThat(self.isTemplated(), is(true));
    }

    @Test
    public void shouldBuildLinkUsingTemplatedBuilder() {
        final Link self = templatedBuilder("myRel", "/test/{foo}")
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
        final Link self = linkBuilder("myRel", "/test/{foo}")
                .withHrefLang("de_DE")
                .withTitle("title")
                .withName("name")
                .withProfile("my-profile")
                .withType("type")
                .build();
        assertThat(self.getRel(), is("myRel"));
        assertThat(self.getHref(), is("/test/{foo}"));
        assertThat(self.isTemplated(), is(false));
        assertThat(self.getHreflang(), is("de_DE"));
        assertThat(self.getTitle(), is("title"));
        assertThat(self.getName(), is("name"));
        assertThat(self.getType(), is("type"));
        assertThat(self.getProfile(), is("my-profile"));
    }
}