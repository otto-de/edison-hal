package de.otto.edison.hal.traverson;

import org.junit.Test;

import static de.otto.edison.hal.traverson.TraversionError.Type.INVALID_JSON;
import static de.otto.edison.hal.traverson.TraversionError.traversionError;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TraversionErrorTest {

    @Test(expected = NullPointerException.class)
    public void shouldFailToBuildWithoutType() {
        traversionError(null, "Foo");
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailToBuildWithoutMessage() {
        traversionError(INVALID_JSON, null);
    }

    @Test
    public void shouldGetCorretValues() {
        final TraversionError traversionError = traversionError(INVALID_JSON, "some message");

        assertThat(traversionError.getCause(), is(empty()));
        assertThat(traversionError.getType(), is(INVALID_JSON));
        assertThat(traversionError.getMessage(), is("some message"));
    }

    @Test
    public void shouldGetCorretValuesWithCause() {
        final TraversionError traversionError = traversionError(INVALID_JSON, "some message", new IllegalArgumentException("some error"));

        assertThat(traversionError.getCause().get().getMessage(), is("some error"));
        assertThat(traversionError.getType(), is(INVALID_JSON));
        assertThat(traversionError.getMessage(), is("some message"));
    }

    @Test
    public void shouldImplementEqualsAndHashCode() {
        final IllegalArgumentException expectedException = new IllegalArgumentException("some error");
        final TraversionError traversionError1 = traversionError(INVALID_JSON, "some message", expectedException);
        final TraversionError traversionError2 = traversionError(INVALID_JSON, "some message", expectedException);
        final TraversionError traversionError3 = traversionError(INVALID_JSON, "some different message", expectedException);

        assertThat(traversionError1, equalTo(traversionError2));
        assertThat(traversionError2, equalTo(traversionError1));
        assertThat(traversionError2, not(equalTo(traversionError3)));

        assertThat(traversionError1.hashCode(), equalTo(traversionError2.hashCode()));
        assertThat(traversionError1.hashCode(), not(equalTo(traversionError3.hashCode())));
    }
}