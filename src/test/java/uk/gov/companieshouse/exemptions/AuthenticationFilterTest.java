package uk.gov.companieshouse.exemptions;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.logging.Logger;

@SpringBootTest
class AuthenticationFilterTest {

    @Mock
    private Logger logger;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private AuthenticationFilter filter;

    @Test
    void authenticationFilterAllowsCallWithKeyCredentials() throws Exception {
        when(request.getHeader("ERIC-Identity")).thenReturn("TEST");
        when(request.getHeader("ERIC-Identity-Type")).thenReturn("KEY");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void authenticationFilterAllowsCallWithOauth2Credentials() throws Exception {
        when(request.getHeader("ERIC-Identity")).thenReturn("TEST");
        when(request.getHeader("ERIC-Identity-Type")).thenReturn("OAUTH2");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void authenticationFilterBlocksCallWithEmptyIdentity() throws Exception {
        when(request.getHeader("ERIC-Identity")).thenReturn("");
        when(request.getHeader("ERIC-Identity-Type")).thenReturn("OAUTH2");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
        verify(response, times(1)).sendError(401);
    }

    @Test
    void authenticationFilterBlocksCallWithIncorrectIdentityType() throws Exception {
        when(request.getHeader("ERIC-Identity")).thenReturn("TEST");
        when(request.getHeader("ERIC-Identity-Type")).thenReturn("INVALID");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
        verify(response, times(1)).sendError(401);
    }
}
