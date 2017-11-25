package de.otto.edison.hal;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Optional;

import static de.otto.edison.hal.CuriTemplate.curiTemplateFor;
import static de.otto.edison.hal.CuriTemplate.matchingCuriTemplateFor;
import static de.otto.edison.hal.Link.curi;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.linkBuilder;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CuriTemplateTest {

    private final Link someCuri = curi("x", "http://example.org/rels/{rel}");
    private final Link someOtherCuri = curi("y", "http://example.com/link-relations/{rel}");

    private final List<Link> curies = asList(someCuri, someOtherCuri);;

    private final String someRel = "http://example.org/rels/product";
    private final String someCuriedRel = "x:product";

    private final String nonMatchingRel = "http://example.org/link-relations/product";
    private final String nonMatchingCuriedRel = "z:product";

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
        curiTemplateFor(linkBuilder("curies","/bar").withName("x").build());
    }

    @Test
    public void shouldFailToCreateCuriTemplateWithMissingName() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Parameter is not a CURI link.");
        curiTemplateFor(link("curies", "/bar/{rel}"));
    }

    @Test
    public void shouldFindMatchingUriTemplateForExpandedRel() {
        final Optional<CuriTemplate> curiTemplate = matchingCuriTemplateFor(curies, someRel);
        assertThat(curiTemplate.isPresent(), is(true));
        assertThat(curiTemplate.get().getCuri(), is(someCuri));
    }

    @Test
    public void shouldFindMatchingUriTemplateForCuriedRel() {
        final Optional<CuriTemplate> curiTemplate = matchingCuriTemplateFor(curies, someCuriedRel);
        assertThat(curiTemplate.isPresent(), is(true));
        assertThat(curiTemplate.get().getCuri(), is(someCuri));
    }

    @Test
    public void shouldNotFindMatchingUriTemplate() {
        final Optional<CuriTemplate> curiTemplate = matchingCuriTemplateFor(curies, nonMatchingRel);
        assertThat(curiTemplate.isPresent(), is(false));
    }

    @Test
    public void shouldMatch() {
        assertThat(curiTemplateFor(someCuri).isMatching(someRel), is(true));
        assertThat(curiTemplateFor(someCuri).isMatching(someCuriedRel), is(true));
        assertThat(curiTemplateFor(someCuri).isMatchingExpandedRel(someRel), is(true));
        assertThat(curiTemplateFor(someCuri).isMatchingCuriedRel(someCuriedRel), is(true));
    }

    @Test
    public void shouldNotMatch() {
        assertThat(curiTemplateFor(someCuri).isMatching(nonMatchingRel), is(false));
        assertThat(curiTemplateFor(someCuri).isMatching(nonMatchingCuriedRel), is(false));
        assertThat(curiTemplateFor(someCuri).isMatchingExpandedRel(someCuriedRel), is(false));
        assertThat(curiTemplateFor(someCuri).isMatchingCuriedRel(someRel), is(false));
    }

    @Test
    public void shouldExtractCuriedRel() {
        assertThat(curiTemplateFor(someCuri).curiedRelFrom(someRel), is("x:product"));
        assertThat(curiTemplateFor(someCuri).curiedRelFrom(someCuriedRel), is("x:product"));
    }

    @Test
    public void shouldFailToExtractCuriedRelForNonMatchingRel() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Rel does not match the CURI template.");
        curiTemplateFor(someCuri).curiedRelFrom(nonMatchingRel);
    }

    @Test
    public void shouldExpandRel() {
        assertThat(curiTemplateFor(someCuri).expandedRelFrom(someRel), is(someRel));
        assertThat(curiTemplateFor(someCuri).expandedRelFrom(someCuriedRel), is(someRel));
    }

    @Test
    public void shouldFailToExpandNonMatchingRel() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Rel does not match the CURI template.");
        curiTemplateFor(someCuri).expandedRelFrom(nonMatchingRel);
    }

    @Test
    public void shouldExtractPlaceholderValue() {
       assertThat(curiTemplateFor(someCuri).relPlaceHolderFrom(someRel), is("product"));
       assertThat(curiTemplateFor(someCuri).relPlaceHolderFrom(someCuriedRel), is("product"));
    }

    @Test
    public void shouldFailToExtractPlaceholderRelForNonMatchingRel() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Rel does not match the CURI template.");
        curiTemplateFor(someCuri).relPlaceHolderFrom(nonMatchingRel);
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