package uk.gov.companieshouse.exemptions;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.logging.Logger;

public class AuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger;

    public AuthenticationFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ericId = request.getHeader("ERIC-Identity");

        if (StringUtils.isBlank(ericId)) {
            logger.error("Unauthenticated request received without eric identity");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String ericIdType = request.getHeader("ERIC-Identity-Type");

        if (StringUtils.isBlank(ericIdType) ||
                ! (ericIdType.equalsIgnoreCase("key") || ericIdType.equalsIgnoreCase("oauth2"))) {
            logger.error("Unauthenticated request received without eric identity type");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
