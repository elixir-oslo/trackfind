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

import java.util.Map;

@Slf4j
@Service
public class TrackFindUserDetailsService implements UserDetailsService {

    @Value("${trackfind.admin}")
    private String adminElixirId;

    private UserRepository userRepository;

    @SuppressWarnings("unchecked")
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Map<String, String> userDetails = new Gson().fromJson(username, Map.class);
        String elixirId = userDetails.get("oidc_claim_sub");
        if (StringUtils.isEmpty(elixirId)) {
            return new User("Anonymous", "Anonymous", false, false, false, false, AuthorityUtils.NO_AUTHORITIES);
        }
        TfUser user = userRepository.findByElixirId(elixirId);
        if (user == null) {
            user = userRepository.save(generateNewUser(userDetails, elixirId));
            log.info("New user saved: {}", user);
        }
        return user;
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
