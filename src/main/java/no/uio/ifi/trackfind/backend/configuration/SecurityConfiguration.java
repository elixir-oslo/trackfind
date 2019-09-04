package no.uio.ifi.trackfind.backend.configuration;

import no.uio.ifi.trackfind.backend.filters.SecurityFilter;
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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static String[] PROTECTED_RESOURCES = new String[]{
            "/login/**",
            "/curation/**",
            "/hubs/**",
            "/monitor/**",
            "/references/**",
            "/versions/**",
            "/admin/**"
    };

    private TrackFindAuthenticationProvider authenticationProvider;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider);
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/VAADIN/**", "/favicon.ico");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .formLogin().disable().logout().disable()
                .addFilterBefore(new SecurityFilter(authenticationManagerBean(),
                                requestCache(),
                                authenticationSuccessHandler(),
                                new OrRequestMatcher(
                                        Arrays.stream(PROTECTED_RESOURCES).map(pr -> new AntPathRequestMatcher(pr, HttpMethod.GET.toString())).collect(Collectors.toList())
                                )),
                        BasicAuthenticationFilter.class)
                .authorizeRequests()
                .anyRequest().permitAll()
                .antMatchers(PROTECTED_RESOURCES).hasRole("ADMIN");
    }

    @Bean
    public RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler savedRequestAwareAuthenticationSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        savedRequestAwareAuthenticationSuccessHandler.setRequestCache(requestCache());
        return savedRequestAwareAuthenticationSuccessHandler;
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
