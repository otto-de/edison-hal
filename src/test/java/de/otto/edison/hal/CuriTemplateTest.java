package de.otto.edison.hal;

import org.testng.annotations.Test;

import static de.otto.edison.hal.CuriTemplate.curiTemplateFor;
import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CuriTemplateTest {

    private final Link curi = curi("x", "http://example.org/rels/{rel}");
    private final String rel = "http://example.org/rels/product";

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*not a CURI link.*")
    public void shouldFailToCreateCuriTemplateForWrongRel() {
        curiTemplateFor(link("foo", "/bar"));
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*required.*placeholder.*")
    public void shouldFailToCreateCuriTemplateForWrongHref() {
        curiTemplateFor(link("curies", "/bar"));
    }

    @Test
    public void shouldMatch() {
        assertThat(curiTemplateFor(curi).matches(rel), is(true));
    }

    @Test
    public void shouldExtractCuriedRel() {
        assertThat(curiTemplateFor(curi).curiedRelFrom(rel), is("x:product"));
    }

    @Test
    public void shouldExtractPlaceholderValue() {
       assertThat(curiTemplateFor(curi).relPlaceHolderFrom(rel), is("product"));
    }
}