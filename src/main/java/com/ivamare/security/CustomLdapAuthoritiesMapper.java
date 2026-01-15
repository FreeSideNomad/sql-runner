package com.ivamare.security;

import com.ivamare.config.SecurityProperties;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.stereotype.Component;

/**
 * Maps LDAP group membership to application roles based on configuration.
 *
 * <p>AD groups are mapped to roles: - SQLRunner-Admins -> ROLE_ADMIN - SQLRunner-UpdateRunners ->
 * ROLE_UPDATE_RUNNER - SQLRunner-SelectRunners -> ROLE_SELECT_RUNNER
 */
@Component
@RequiredArgsConstructor
public class CustomLdapAuthoritiesMapper implements GrantedAuthoritiesMapper {

  private final SecurityProperties securityProperties;

  @Override
  public Collection<? extends GrantedAuthority> mapAuthorities(
      Collection<? extends GrantedAuthority> authorities) {
    Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
    Map<String, String> roleMapping = securityProperties.getRoleMapping();

    for (GrantedAuthority authority : authorities) {
      String authorityName = authority.getAuthority();

      for (Map.Entry<String, String> entry : roleMapping.entrySet()) {
        String roleName = entry.getKey();
        String adGroupDn = entry.getValue();

        if (matchesAdGroup(authorityName, adGroupDn)) {
          mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        }
      }
    }

    if (mappedAuthorities.isEmpty()) {
      mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_SELECT_RUNNER"));
    }

    return mappedAuthorities;
  }

  private boolean matchesAdGroup(String authority, String adGroupDn) {
    String normalizedAuthority = authority.toUpperCase();
    String normalizedGroupDn = adGroupDn.toUpperCase();

    if (normalizedAuthority.equals(normalizedGroupDn)) {
      return true;
    }

    String cnPrefix = "CN=";
    int cnStart = normalizedGroupDn.indexOf(cnPrefix);
    if (cnStart >= 0) {
      int cnEnd = normalizedGroupDn.indexOf(",", cnStart);
      String groupName =
          cnEnd > 0
              ? normalizedGroupDn.substring(cnStart + cnPrefix.length(), cnEnd)
              : normalizedGroupDn.substring(cnStart + cnPrefix.length());

      return normalizedAuthority.contains(groupName);
    }

    return false;
  }
}
