package uk.gov.companieshouse.exemptions.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    private static final String ERIC_IDENTITY_HEADER_KEY = "ERIC-Identity";
    private static final String ERIC_IDENTITY_TYPE_HEADER_KEY = "ERIC-Identity-Type";
    private static final String OAUTH_2 = "OAUTH2";
    private static final String KEY = "KEY";
    private static final String GET_METHOD = "GET";
    private static final String ERIC_IDENTITY_HEADER = "SOME-IDENTITY";
    private static final String PUT_METHOD = "PUT";
    private static final String ERIC_AUTHORISED_KEY_PRIVILEGES_HEADER_KEY = "ERIC-Authorised-Key-Privileges";
    private static final String INTERNAL_APP_PRIVILEGES = "internal-app";
    private static final String INVALID_IDENTITY_TYPE = "identityType";
    private static final String INVALID_PRIVILEGE = "privilege";
    private static final String INVALID_ERIC_IDENTITY_TYPE = "notKeyOrOauth2";
    private static final String INVALID_KEY = "key";

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

    static Stream<Arguments> validFilterCombos() {
        return Stream.of(
            Arguments.of(OAUTH_2, GET_METHOD),
            Arguments.of(KEY, GET_METHOD)
        );
    }

    @ParameterizedTest(name = "ERIC-Identity-Type {0}, Method {1} passes the filter")
    @MethodSource("validFilterCombos")
    void whenValidParameters_thenFilterPasses(final String ericIdentityType, final String method) throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(ericIdentityType);
        when(request.getMethod()).thenReturn(method);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    static Stream<Arguments> invalidFilterCombos() {
        return Stream.of(
                Arguments.of(OAUTH_2, PUT_METHOD),
                Arguments.of(KEY, PUT_METHOD)
        );
    }

    @ParameterizedTest(name = "ERIC-Identity-Type {0}, Method {1} fails the filter")
    @MethodSource("invalidFilterCombos")
    void whenInvalidParameters_thenFilterFails(final String ericIdentityType, final String method) throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(ericIdentityType);
        when(request.getMethod()).thenReturn(method);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }

    @Test
    @DisplayName("KEY type GET request with internal app privileges passes filter")
    void doFilterInternalKeyAndInternalApp() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(KEY);
        when(request.getHeader(ERIC_AUTHORISED_KEY_PRIVILEGES_HEADER_KEY)).thenReturn(INTERNAL_APP_PRIVILEGES);
        when(request.getMethod()).thenReturn(GET_METHOD);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("KEY type PUT request with internal app privileges passes filter")
    void doFilterInternalMethodPutTypeKeyAndInternalApp() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(KEY);
        when(request.getHeader(ERIC_AUTHORISED_KEY_PRIVILEGES_HEADER_KEY)).thenReturn(INTERNAL_APP_PRIVILEGES);
        when(request.getMethod()).thenReturn(PUT_METHOD);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Request with no identity fails")
    void doFilterInternalNoIdentity() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }

    @Test
    @DisplayName("Request with no identity type fails")
    void doFilterInternalNoIdentityType() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }

    @Test
    @DisplayName("Request with wrong identity type fails")
    void doFilterInternalWrongIdentityType() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(INVALID_IDENTITY_TYPE);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }
    @Test
    @DisplayName("PUT Request with OAUTH2 type and internal app privilege fails")
    void doFilterInternalOauth2WrongMethodWithPrivilege() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(OAUTH_2);
        when(request.getHeader(ERIC_AUTHORISED_KEY_PRIVILEGES_HEADER_KEY)).thenReturn(INTERNAL_APP_PRIVILEGES);
        when(request.getMethod()).thenReturn(PUT_METHOD);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }

    @Test
    @DisplayName("PUT Request with KEY type with wrong privileges fails")
    void doFilterInternalKeyWrongPrivileges() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(KEY);
        when(request.getHeader(ERIC_AUTHORISED_KEY_PRIVILEGES_HEADER_KEY)).thenReturn(INVALID_PRIVILEGE);
        when(request.getMethod()).thenReturn(PUT_METHOD);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }

    @Test
    @DisplayName("Set ERIC Id type as an invalid value and fail")
    void doFilterInternalWithInvalidEricIdentityType() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(INVALID_ERIC_IDENTITY_TYPE);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }

    @Test
    @DisplayName("Send invalid key due to wrong id and throw error")
    void doFilterInternalWithUnauthorizedKeyDueToWrongIdType() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(OAUTH_2);
        when(request.getHeader(ERIC_AUTHORISED_KEY_PRIVILEGES_HEADER_KEY)).thenReturn(INVALID_PRIVILEGE);
        when(request.getMethod()).thenReturn(PUT_METHOD);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }

    @Test
    @DisplayName("Send invalid key due to wrong privileges and throw error")
    void doFilterInternalWithUnauthorizedKeyDueToWrongPrivileges() throws ServletException, IOException {
        when(request.getHeader(ERIC_IDENTITY_HEADER_KEY)).thenReturn(ERIC_IDENTITY_HEADER);
        when(request.getHeader(ERIC_IDENTITY_TYPE_HEADER_KEY)).thenReturn(INVALID_KEY);
        when(request.getHeader(ERIC_AUTHORISED_KEY_PRIVILEGES_HEADER_KEY)).thenReturn(null);
        when(request.getMethod()).thenReturn(PUT_METHOD);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(0)).doFilter(request, response);
    }
}
