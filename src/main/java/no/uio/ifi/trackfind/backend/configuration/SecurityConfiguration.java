package no.uio.ifi.trackfind.backend.configuration;

import no.uio.ifi.trackfind.backend.security.filters.SecurityFilter;
import no.uio.ifi.trackfind.backend.security.TrackFindAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private TrackFindAuthenticationProvider authenticationProvider;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider);
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(HttpMethod.OPTIONS, "/actuator/**").antMatchers(HttpMethod.GET, "/actuator/health/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        List<RequestMatcher> protectedMatchers = new ArrayList<>();
        for (String protectedResource : new String[]{
                "/actuator/**",
                "/login/**",
                "/curation/**",
                "/hubs/**",
                "/monitor/**",
                "/references/**",
                "/versions/**",
                "/users/**"
        }) {
            for (HttpMethod httpMethod : HttpMethod.values()) {
                protectedMatchers.add(new AntPathRequestMatcher(protectedResource, httpMethod.toString(), false));
            }
        }
        http
                .csrf().disable()
                .formLogin().disable().logout().disable()
                .addFilterBefore(
                        new SecurityFilter(authenticationManagerBean(), new OrRequestMatcher(protectedMatchers)),
                        BasicAuthenticationFilter.class
                )
                .authorizeRequests()
                .antMatchers("/login/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(new String[]{
                        "/actuator/**",
                        "/curation/**",
                        "/hubs/**",
                        "/monitor/**",
                        "/references/**",
                        "/versions/**",
                        "/users/**"
                }).hasRole("ADMIN");
    }

    @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Autowired
    public void setAuthenticationProvider(TrackFindAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

}
