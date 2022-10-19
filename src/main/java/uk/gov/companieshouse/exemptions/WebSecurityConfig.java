package uk.gov.companieshouse.exemptions;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * Configure Web Security.
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        // Excluding healthcheck endpoint from security filter
        web.ignoring().antMatchers("/healthcheck");
    }
}
