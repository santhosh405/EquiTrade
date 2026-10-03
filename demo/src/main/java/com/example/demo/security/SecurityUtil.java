package com.example.demo.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Component
public class SecurityUtil {

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns the User for the currently authenticated principal, or null if
     * there is no authenticated user.  Never falls back to an arbitrary DB row.
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.orElse(null);
    }

    /**
     * Returns the User for the JWT principal.  The requestedUserId parameter is
     * intentionally IGNORED so that callers cannot impersonate another user by
     * supplying a foreign ID (IDOR fix).  The anonymous fallback has been
     * removed entirely — unauthenticated callers get null.
     *
     * @deprecated Pass null always; the parameter is kept only for binary
     *             compatibility with call-sites that will be cleaned up.  New
     *             code should call {@link #getCurrentUser()} directly.
     */
    @Deprecated
    public User resolveUser(Long ignoredUserId) {
        // SECURITY: requestedUserId is deliberately ignored to prevent IDOR.
        // Always resolve from the JWT in the SecurityContext.
        return getCurrentUser();
    }

    /**
     * Returns true when the currently authenticated principal is the given user
     * OR holds one of the admin roles defined in SecurityConfig.ADMIN_ROLES.
     */
    public boolean isCurrentUserOrAdmin(Long userId) {
        User current = getCurrentUser();
        if (current == null) return false;
        if (current.getId() != null && current.getId().equals(userId)) return true;
        String role = current.getRole();
        if (role == null) return false;
        String bare = role.startsWith("ROLE_") ? role.substring(5) : role;
        for (String adminRole : SecurityConfig.ADMIN_ROLES) {
            if (adminRole.equalsIgnoreCase(bare)) return true;
        }
        return false;
    }
}
