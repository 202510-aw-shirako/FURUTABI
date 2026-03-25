package com.furutabi.security;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserRow user = jdbcTemplate.query(
            """
                SELECT id, email, password_hash
                FROM users
                WHERE email = ?
                """,
            rs -> rs.next() ? new UserRow(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("password_hash")
            ) : null,
            username
        );

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        List<GrantedAuthority> authorities = jdbcTemplate.query(
            """
                SELECT role_name
                FROM user_roles
                WHERE user_id = ?
                ORDER BY id
                """,
            (rs, rowNum) -> new SimpleGrantedAuthority("ROLE_" + rs.getString("role_name")),
            user.id()
        );

        return User.withUsername(user.email())
            .password(user.passwordHash())
            .authorities(authorities)
            .build();
    }

    private record UserRow(long id, String email, String passwordHash) {
    }
}
