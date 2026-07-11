package com.ardetrick.oryhydrareference.demo;

import java.util.List;

public record MvcClient(String name, String id, List<StartLink> startLinks) {

  /**
   * One clickable flow entry point per registered redirect URI. {@code servedByThisApp} is true
   * when the redirect URI is this app's own demo callback page, meaning the flow completes in the
   * browser end to end.
   */
  public record StartLink(String redirectUri, String url, boolean servedByThisApp) {}
}
