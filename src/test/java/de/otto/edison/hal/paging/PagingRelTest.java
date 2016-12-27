package de.otto.edison.hal.paging;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PagingRelTest {


    @Test
    public void shouldLowerCaseRel() {
        assertThat(PagingRel.LAST.toString(), is("last"));
    }
}