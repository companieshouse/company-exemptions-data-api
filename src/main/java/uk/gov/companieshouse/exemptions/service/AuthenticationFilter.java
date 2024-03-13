package uk.gov.companieshouse.exemptions.service;

import java.io.IOException;
import java.util.Optional;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.logging.Logger;

public class AuthenticationFilter extends OncePerRequestFilter {

    private final Logger authLogger;

    public AuthenticationFilter(Logger logger) {
        this.authLogger = logger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String ericIdentity = request.getHeader("ERIC-Identity");

        if (StringUtils.isBlank(ericIdentity)) {
            authLogger.error("Request received without eric identity");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String ericIdentityType = request.getHeader("ERIC-Identity-Type");

        if (!("key".equalsIgnoreCase(ericIdentityType)
                || ("oauth2".equalsIgnoreCase(ericIdentityType)))) {
            authLogger.error("Request received without correct eric identity type");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!isKeyAuthorised(request, ericIdentityType)) {
            authLogger.info("Supplied key does not have sufficient privilege for the action");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request,response);
    }

    private boolean isKeyAuthorised(HttpServletRequest request, String ericIdentityType) {
        String[] privileges = getApiKeyPrivileges(request);

        return request.getMethod().equals("GET")
                || (ericIdentityType.equalsIgnoreCase("key")
                && ArrayUtils.contains(privileges, "internal-app"));
    }

    private String[] getApiKeyPrivileges(HttpServletRequest request) {
        String commaSeparatedPrivilegeString = request.getHeader("ERIC-Authorised-Key-Privileges");

        return Optional.ofNullable(commaSeparatedPrivilegeString)
                .map(s -> s.split(","))
                .orElse(new String[]{});
    }
}
