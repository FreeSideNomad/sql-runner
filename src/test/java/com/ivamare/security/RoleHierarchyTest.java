package com.ivamare.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;

/** Tests for role hierarchy configuration. */
@SpringBootTest
@ActiveProfiles("test")
class RoleHierarchyTest {

  @Autowired private RoleHierarchy roleHierarchy;

  @Test
  void adminRole_shouldImplyUpdateRunnerRole() {
    Collection<? extends GrantedAuthority> authorities =
        AuthorityUtils.createAuthorityList("ROLE_ADMIN");

    Collection<? extends GrantedAuthority> reachableAuthorities =
        roleHierarchy.getReachableGrantedAuthorities(authorities);

    assertThat(reachableAuthorities)
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_ADMIN", "ROLE_UPDATE_RUNNER", "ROLE_SELECT_RUNNER");
  }

  @Test
  void updateRunnerRole_shouldImplySelectRunnerRole() {
    Collection<? extends GrantedAuthority> authorities =
        AuthorityUtils.createAuthorityList("ROLE_UPDATE_RUNNER");

    Collection<? extends GrantedAuthority> reachableAuthorities =
        roleHierarchy.getReachableGrantedAuthorities(authorities);

    assertThat(reachableAuthorities)
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_UPDATE_RUNNER", "ROLE_SELECT_RUNNER")
        .doesNotContain("ROLE_ADMIN");
  }

  @Test
  void selectRunnerRole_shouldNotImplyOtherRoles() {
    Collection<? extends GrantedAuthority> authorities =
        AuthorityUtils.createAuthorityList("ROLE_SELECT_RUNNER");

    Collection<? extends GrantedAuthority> reachableAuthorities =
        roleHierarchy.getReachableGrantedAuthorities(authorities);

    assertThat(reachableAuthorities)
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_SELECT_RUNNER");
  }
}
