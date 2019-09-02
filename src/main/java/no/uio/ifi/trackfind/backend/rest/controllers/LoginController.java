package no.uio.ifi.trackfind.backend.rest.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Login controller.
 *
 * @author Dmytro Titov
 */
@RequestMapping("/")
@Controller
public class LoginController {

    /**
     * Redirects to the homepage.
     */
    @GetMapping(path = "/login")
    public RedirectView redirect() {
        boolean ssl = "on".equalsIgnoreCase(System.getenv("SSL_ENGINE"));
        String serverName = System.getenv("SERVER_NAME");
        String redirectURL = (ssl ? "https://" : "http://") + serverName;
        return new RedirectView(redirectURL);
    }

}
