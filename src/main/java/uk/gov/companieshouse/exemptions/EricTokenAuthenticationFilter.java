package uk.gov.companieshouse.exemptions;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.logging.Logger;

public class EricTokenAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger;

    public EricTokenAuthenticationFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ericId = request.getHeader("ERIC-Identity");

        if (StringUtils.isBlank(ericId)) {
            logger.error("Unauthorised request received without eric identity");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String ericIdType = request.getHeader("ERIC-Identity-Type");

        if (StringUtils.isBlank(ericIdType) ||
                ! (ericIdType.equalsIgnoreCase("key") || ericIdType.equalsIgnoreCase("oauth2"))) {
            logger.error("Unauthorised request received without eric identity type");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
