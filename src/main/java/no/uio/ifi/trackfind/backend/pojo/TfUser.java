package no.uio.ifi.trackfind.backend.pojo;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@Entity
@Table(name = "tf_users")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = {"versions"})
@AllArgsConstructor
@NoArgsConstructor
public class TfUser implements Serializable, UserDetails {

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

    @Column(name = "active", nullable = false)
    private boolean active;

    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private Set<TfVersion> versions;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return isAdmin() ? AuthorityUtils.createAuthorityList("ROLE_ADMIN") : AuthorityUtils.createAuthorityList("ROLE_USER");
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isActive();
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isActive();
    }

    @Override
    public boolean isEnabled() {
        return isActive();
    }

}
