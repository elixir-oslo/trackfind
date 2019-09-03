package no.uio.ifi.trackfind.backend.pojo;

import lombok.*;
import org.springframework.security.core.AuthenticatedPrincipal;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tf_users")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TfUser implements Serializable, AuthenticatedPrincipal {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "elixir_id", nullable = false)
    private String elixirId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "admin", nullable = false)
    private boolean admin;

    @Override
    public String getName() {
        return fullName;
    }

}
