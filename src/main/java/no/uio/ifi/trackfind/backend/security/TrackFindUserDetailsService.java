package no.uio.ifi.trackfind.backend.security;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.pojo.TfUser;
import no.uio.ifi.trackfind.backend.repositories.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Slf4j
@Service
public class TrackFindUserDetailsService implements UserDetailsService {

    @Value("${trackfind.admin}")
    private String adminElixirId;

    private UserRepository userRepository;

    // Note that username is not really a username here, it's a JSON serialized map of OIDC parameters
    @SuppressWarnings("unchecked")
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Map<String, String> userDetails = new Gson().fromJson(username, Map.class);
        String elixirId = userDetails.get("oidc_claim_sub");
        User anonymousUser = new User("Anonymous", "Anonymous", false, false, false, false, AuthorityUtils.NO_AUTHORITIES);
        if (StringUtils.isEmpty(elixirId)) {
            return anonymousUser;
        }
        TfUser user = userRepository.findByElixirId(elixirId);
        if (user == null && StringUtils.isNotEmpty(username)) {
            TfUser generatedUser = generateNewUser(userDetails, elixirId);
            if (generatedUser.getUsername() != null) {
                user = userRepository.save(generatedUser);
                log.info("New user saved: {}", user);
            } else {
                return anonymousUser;
            }
        }
        return user;
    }

    /**
     * Gets all users.
     *
     * @return All users.
     */
    public Collection<TfUser> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Activates user.
     *
     * @param user User to be activated.
     */
    public void activateUser(TfUser user) {
        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Deactivates user.
     *
     * @param user User to be deactivated.
     */
    public void deactivateUser(TfUser user) {
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Grants a user with ADMIN authority.
     *
     * @param user Target user.
     */
    public void grantAdminAuthority(TfUser user) {
        user.setAdmin(true);
        userRepository.save(user);
    }

    /**
     * Revokes ADMIN authority from the user.
     *
     * @param user Target user.
     */
    public void revokeAdminAuthority(TfUser user) {
        user.setAdmin(false);
        userRepository.save(user);
    }

    private TfUser generateNewUser(Map<String, String> userDetails, String elixirId) {
        return new TfUser(
                null,
                elixirId,
                userDetails.get("oidc_claim_preferred_username"),
                userDetails.get("oidc_claim_name"),
                userDetails.get("oidc_claim_email"),
                adminElixirId.equalsIgnoreCase(elixirId),
                true,
                null
        );
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
