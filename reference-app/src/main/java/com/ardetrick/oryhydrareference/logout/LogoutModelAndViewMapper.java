package com.ardetrick.oryhydrareference.logout;

import com.ardetrick.oryhydrareference.logout.LogoutResult.DisplayLogoutUI;
import com.ardetrick.oryhydrareference.logout.LogoutResult.LogoutAcceptedFollowRedirect;
import com.ardetrick.oryhydrareference.logout.LogoutResult.LogoutCancelledReturnHome;
import com.ardetrick.oryhydrareference.logout.LogoutResult.LogoutRequestNotFound;
import com.ardetrick.oryhydrareference.modelandview.ModelAndViewUtils;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.springframework.web.servlet.ModelAndView;

@UtilityClass
public class LogoutModelAndViewMapper {

  public static ModelAndView map(@NonNull final LogoutResult result) {
    return switch (result) {
      case DisplayLogoutUI r ->
          new ModelAndView("logout")
              .addObject("logoutChallenge", r.logoutChallenge())
              .addObject("subject", r.subject());
      case LogoutAcceptedFollowRedirect r ->
          ModelAndViewUtils.redirectToDifferentContext(r.redirectUrl());
      case LogoutCancelledReturnHome ignored -> new ModelAndView("/home");
      case LogoutRequestNotFound ignored -> new ModelAndView("/home");
    };
  }
}
