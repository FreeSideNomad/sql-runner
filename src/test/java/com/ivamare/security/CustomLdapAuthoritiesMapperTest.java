package com.ivamare.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.config.SecurityProperties;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

/** Tests for CustomLdapAuthoritiesMapper. */
class CustomLdapAuthoritiesMapperTest {

  private CustomLdapAuthoritiesMapper mapper;
  private SecurityProperties properties;

  @BeforeEach
  void setUp() {
    properties = new SecurityProperties();
    Map<String, String> roleMapping = new HashMap<>();
    roleMapping.put("ADMIN", "CN=SQLRunner-Admins,OU=Groups,DC=company,DC=com");
    roleMapping.put("UPDATE_RUNNER", "CN=SQLRunner-UpdateRunners,OU=Groups,DC=company,DC=com");
    roleMapping.put("SELECT_RUNNER", "CN=SQLRunner-SelectRunners,OU=Groups,DC=company,DC=com");
    properties.setRoleMapping(roleMapping);

    mapper = new CustomLdapAuthoritiesMapper(properties);
  }

  @Test
  void mapAuthorities_withAdminGroup_shouldReturnAdminRole() {
    Collection<? extends GrantedAuthority> ldapAuthorities =
        AuthorityUtils.createAuthorityList("CN=SQLRunner-Admins,OU=Groups,DC=company,DC=com");

    Collection<? extends GrantedAuthority> mappedAuthorities =
        mapper.mapAuthorities(ldapAuthorities);

    assertThat(mappedAuthorities).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN");
  }

  @Test
  void mapAuthorities_withUpdateRunnerGroup_shouldReturnUpdateRunnerRole() {
    Collection<? extends GrantedAuthority> ldapAuthorities =
        AuthorityUtils.createAuthorityList(
            "CN=SQLRunner-UpdateRunners,OU=Groups,DC=company,DC=com");

    Collection<? extends GrantedAuthority> mappedAuthorities =
        mapper.mapAuthorities(ldapAuthorities);

    assertThat(mappedAuthorities)
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_UPDATE_RUNNER");
  }

  @Test
  void mapAuthorities_withSelectRunnerGroup_shouldReturnSelectRunnerRole() {
    Collection<? extends GrantedAuthority> ldapAuthorities =
        AuthorityUtils.createAuthorityList(
            "CN=SQLRunner-SelectRunners,OU=Groups,DC=company,DC=com");

    Collection<? extends GrantedAuthority> mappedAuthorities =
        mapper.mapAuthorities(ldapAuthorities);

    assertThat(mappedAuthorities)
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_SELECT_RUNNER");
  }

  @Test
  void mapAuthorities_withNoMatchingGroups_shouldReturnDefaultSelectRunnerRole() {
    Collection<? extends GrantedAuthority> ldapAuthorities =
        AuthorityUtils.createAuthorityList("CN=OtherGroup,OU=Groups,DC=company,DC=com");

    Collection<? extends GrantedAuthority> mappedAuthorities =
        mapper.mapAuthorities(ldapAuthorities);

    assertThat(mappedAuthorities)
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_SELECT_RUNNER");
  }

  @Test
  void mapAuthorities_withMultipleGroups_shouldReturnAllMatchedRoles() {
    Collection<? extends GrantedAuthority> ldapAuthorities =
        AuthorityUtils.createAuthorityList(
            "CN=SQLRunner-Admins,OU=Groups,DC=company,DC=com",
            "CN=SQLRunner-UpdateRunners,OU=Groups,DC=company,DC=com");

    Collection<? extends GrantedAuthority> mappedAuthorities =
        mapper.mapAuthorities(ldapAuthorities);

    assertThat(mappedAuthorities)
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_ADMIN", "ROLE_UPDATE_RUNNER");
  }

  @Test
  void mapAuthorities_withPartialGroupNameMatch_shouldMapCorrectly() {
    Collection<? extends GrantedAuthority> ldapAuthorities =
        AuthorityUtils.createAuthorityList("SQLRUNNER-ADMINS");

    Collection<? extends GrantedAuthority> mappedAuthorities =
        mapper.mapAuthorities(ldapAuthorities);

    assertThat(mappedAuthorities).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN");
  }
}
