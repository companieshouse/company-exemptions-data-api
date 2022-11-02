package uk.gov.companieshouse.exemptions;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.logging.Logger;

public class AuthorisationFilter extends OncePerRequestFilter {

    private final Logger logger;

    public AuthorisationFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String privileges = request.getHeader("ERIC-Authorised-Key-Privileges");

        if (!"*".equals(privileges)) {
            logger.error(String.format("Unauthorised request received with incorrect privileges: %s", privileges));
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
