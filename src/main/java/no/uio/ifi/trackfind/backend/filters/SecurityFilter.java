package no.uio.ifi.trackfind.backend.filters;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SecurityFilter extends AbstractAuthenticationProcessingFilter {

    private RequestCache requestCache;

    public SecurityFilter(AuthenticationManager authenticationManager,
                          RequestCache requestCache,
                          AuthenticationSuccessHandler authenticationSuccessHandler,
                          RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
        this.requestCache = requestCache;
        setAuthenticationSuccessHandler(authenticationSuccessHandler);
        setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("oidc_claim_sub", request.getHeader("oidc_claim_sub"));
        userDetails.put("oidc_claim_preferred_username", request.getHeader("oidc_claim_preferred_username"));
        userDetails.put("oidc_claim_name", request.getHeader("oidc_claim_name"));
        userDetails.put("oidc_claim_email", request.getHeader("oidc_claim_email"));

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(new Gson().toJson(userDetails), null);
        requestCache.saveRequest(request, response);
        return getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null && super.requiresAuthentication(request, response);
    }

}
