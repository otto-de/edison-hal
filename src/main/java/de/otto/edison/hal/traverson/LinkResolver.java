package de.otto.edison.hal.traverson;

import de.otto.edison.hal.Link;

import java.io.IOException;

/**
 * Functional interface used to resolve a {@link de.otto.edison.hal.Link} and return the {@code application/hal+json}
 * resource as a String.
 * <p>
 *     The function will be called by the Traverson, whenever a Link must be followed. The Traverson will
 *     take care of URI templates (templated links), so the implementation of the function can rely on the
 *     Link parameter to be not {@link Link#isTemplated() templated}.
 * </p>
 * <p>
 *     Typical implementations of the Function will rely on some HTTP client. Especially in this case,
 *     the function should take care of the link's {@link Link#getType() type} and {@link Link#getProfile()},
 *     so the proper HTTP Accept header is used.
 * </p>
 * @since 2.0.0
 */
@FunctionalInterface
public interface LinkResolver {

    /**
     * Resolves an absolute link and returns the linked resource representation as a String.
     *
     * @param link the link of the resource
     * @return String containing the {@code application/hal+json} representation of the resource
     *
     * @throws IOException if a low-level I/O problem (unexpected end-of-input, network error) occurs.
     */
    String apply(Link link) throws IOException;

}
