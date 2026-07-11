package com.ardetrick.oryhydrareference.logout;

import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import com.ardetrick.oryhydrareference.logout.LogoutResult.DisplayLogoutUI;
import com.ardetrick.oryhydrareference.logout.LogoutResult.LogoutAcceptedFollowRedirect;
import com.ardetrick.oryhydrareference.logout.LogoutResult.LogoutCancelledReturnHome;
import com.ardetrick.oryhydrareference.logout.LogoutResult.LogoutRequestNotFound;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutService {

  @NonNull HydraAdminClient hydraAdminClient;

  public LogoutResult processInitialLogoutRequest(@NonNull final String logoutChallenge) {
    val maybeLogoutRequest = hydraAdminClient.getLogoutRequest(logoutChallenge);
    if (maybeLogoutRequest.isEmpty()) {
      return new LogoutRequestNotFound();
    }

    return new DisplayLogoutUI(logoutChallenge, maybeLogoutRequest.get().getSubject());
  }

  public LogoutResult processLogoutForm(@NonNull final LogoutForm logoutForm) {
    if (logoutForm.isCancelled()) {
      hydraAdminClient.rejectLogoutRequest(logoutForm.logoutChallenge());
      return new LogoutCancelledReturnHome();
    }

    val redirectTo = hydraAdminClient.acceptLogoutRequest(logoutForm.logoutChallenge());
    return new LogoutAcceptedFollowRedirect(redirectTo.getRedirectTo());
  }
}
