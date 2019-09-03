package no.uio.ifi.trackfind.frontend.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {

    private String id;
    private String username;
    private String fullName;
    private String email;

}
