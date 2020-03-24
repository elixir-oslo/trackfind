package no.uio.ifi.trackfind.backend.security.filters;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SecurityFilter extends AbstractAuthenticationProcessingFilter {

    public SecurityFilter(AuthenticationManager authenticationManager,
                          RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
        setAuthenticationManager(authenticationManager);
        setContinueChainBeforeSuccessfulAuthentication(true);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("oidc_claim_sub", request.getHeader("oidc_claim_sub"));
        userDetails.put("oidc_claim_preferred_username", request.getHeader("oidc_claim_preferred_username"));
        userDetails.put("oidc_claim_name", request.getHeader("oidc_claim_name"));
        userDetails.put("oidc_claim_email", request.getHeader("oidc_claim_email"));

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(new Gson().toJson(userDetails), null);
        Authentication authResult = getAuthenticationManager().authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authResult);
        return authResult;
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ((authentication instanceof UsernamePasswordAuthenticationToken) && authentication.isAuthenticated()) {
            return false;
        }
        return super.requiresAuthentication(request, response);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) {
        getRememberMeServices().loginSuccess(request, response, authResult);
        if (this.eventPublisher != null) {
            eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
        }
    }

}
