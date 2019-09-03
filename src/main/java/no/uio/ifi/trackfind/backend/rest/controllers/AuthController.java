package no.uio.ifi.trackfind.backend.rest.controllers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

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
    public RedirectView login(@RequestHeader(value = "referer", required = false) String referer,
                              @RequestHeader(value = "oidc_claim_name", required = false) String userFullName,
                              @RequestHeader(value = "oidc_claim_sub", required = false) String userId) {
        log.info("User {} ({}) logged in.", userFullName, userId);
        return new RedirectView(StringUtils.isEmpty(referer) ? "/" : referer);
    }

    /**
     * Signs out and redirects to the referrer page.
     */
    @GetMapping(path = "/logout")
    public RedirectView logout(@RequestHeader(value = "referer") String referer,
                               @RequestHeader(value = "oidc_claim_name", required = false) String userFullName,
                               @RequestHeader(value = "oidc_claim_sub", required = false) String userId) {
        log.info("User {} ({}) attempted to log out.", userFullName, userId);
        return new RedirectView("/oidc-protected?logout=" + referer);
    }

}
