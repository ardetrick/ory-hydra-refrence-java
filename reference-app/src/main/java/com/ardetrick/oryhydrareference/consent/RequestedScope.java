package com.ardetrick.oryhydrareference.consent;

import java.util.List;
import java.util.Map;

/**
 * A requested OAuth scope paired with a human-readable description. A real consent screen shows the
 * user what they are granting ("Stay signed in") rather than the raw scope token
 * ("offline_access"), so a consent provider keeps a catalog like this one. Scopes with no entry
 * fall back to displaying just their raw name.
 */
public record RequestedScope(String name, String description) {

  private static final Map<String, String> DESCRIPTIONS =
      Map.of(
          "openid", "Confirm your identity",
          "profile", "Access your basic profile",
          "email", "See your email address",
          "offline_access", "Stay signed in by issuing a refresh token");

  public static List<RequestedScope> fromNames(List<String> names) {
    return names.stream().map(name -> new RequestedScope(name, DESCRIPTIONS.get(name))).toList();
  }
}
