package no.uio.ifi.trackfind.backend.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;

/**
 * Login controller.
 *
 * @author Dmytro Titov
 */
@Slf4j
@RequestMapping("/")
@Controller
public class AuthController {

    /**
     * Redirects to the referrer page.
     */
    @GetMapping(path = "/login")
    public RedirectView login(@RequestHeader(value = "oidc_claim_name", required = false) String userFullName,
                              @RequestHeader(value = "oidc_claim_sub", required = false) String userId) {
        log.info("User {} ({}) logged in.", userFullName, userId);
        return new RedirectView("/");
    }

    /**
     * Signs out and redirects to the referrer page.
     */
    @GetMapping(path = "/logout")
    public RedirectView logout(HttpServletRequest request,
                               @RequestHeader(value = "oidc_claim_name", required = false) String userFullName,
                               @RequestHeader(value = "oidc_claim_sub", required = false) String userId) {
        log.info("User {} ({}) attempted to log out.", userFullName, userId);
        SecurityContextHolder.clearContext();
        String redirectURL = String.format("%s://%s", request.getScheme(), request.getServerName());
        int serverPort = request.getServerPort();
        if (serverPort != 80 && serverPort != 443) {
            redirectURL += ":" + serverPort;
        }
        return new RedirectView("/oidc-protected?logout=" + redirectURL);
    }

}
