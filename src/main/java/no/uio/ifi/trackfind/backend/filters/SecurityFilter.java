package no.uio.ifi.trackfind.backend.filters;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.pojo.TfUser;
import no.uio.ifi.trackfind.backend.repositories.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@Order(1)
public class SecurityFilter implements Filter {

    private UserRepository userRepository;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null && context.getAuthentication().isAuthenticated()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String elixirId = httpServletRequest.getHeader("oidc_claim_sub");
        if (StringUtils.isEmpty(elixirId)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        TfUser user = userRepository.findByElixirId(elixirId);
        if (user == null) {
            user = new TfUser(
                    null,
                    elixirId,
                    httpServletRequest.getHeader("oidc_claim_preferred_username"),
                    httpServletRequest.getHeader("oidc_claim_name"),
                    httpServletRequest.getHeader("oidc_claim_email"),
                    false,
                    null
            );
            user = userRepository.save(user);
            log.info("New user saved: {}", user);
        }
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(
                user,
                null,
                Collections.singleton((GrantedAuthority) () -> "user")
        ));
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
