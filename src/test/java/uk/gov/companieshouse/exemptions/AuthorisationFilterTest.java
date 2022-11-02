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
class AuthorisationFilterTest {

    @Mock
    private Logger logger;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private AuthorisationFilter filter;

    @Test
    void authorisationFilterAllowsCallWithKeyPrivileges() throws Exception {
        when(request.getHeader("ERIC-Authorised-Key-Privileges")).thenReturn("*");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void authorisationFilterBlocksCallWithEmptyPrivileges() throws Exception {
        when(request.getHeader("ERIC-Authorised-Key-Privileges")).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
        verify(response, times(1)).sendError(403);
    }

    @Test
    void authorisationFilterBlocksCallWithWrongPrivileges() throws Exception {
        when(request.getHeader("ERIC-Authorised-Key-Privileges")).thenReturn("payment");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
        verify(response, times(1)).sendError(403);
    }
}
