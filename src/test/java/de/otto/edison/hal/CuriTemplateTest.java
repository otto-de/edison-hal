package de.otto.edison.hal;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static de.otto.edison.hal.CuriTemplate.curiTemplateFor;
import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CuriTemplateTest {

    private final Link curi = curi("x", "http://example.org/rels/{rel}");
    private final String rel = "http://example.org/rels/product";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void shouldFailToCreateCuriTemplateForWrongRel() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(matchesRegex(".*not a CURI link.*"));
        curiTemplateFor(link("foo", "/bar"));
    }

    @Test
    public void shouldFailToCreateCuriTemplateForWrongHref() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(matchesRegex(".*required.*placeholder.*"));
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

    private Matcher<String> matchesRegex(final String regex) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("not matching " + regex);
            }

            @Override
            protected boolean matchesSafely(final String item) {
                return item.matches(regex);
            }
        };
    }
}