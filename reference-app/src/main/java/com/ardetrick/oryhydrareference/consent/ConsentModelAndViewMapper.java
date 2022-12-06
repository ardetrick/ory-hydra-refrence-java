package com.ardetrick.oryhydrareference.consent;

import com.ardetrick.oryhydrareference.modelandview.ModelAndViewUtils;
import lombok.NonNull;
import org.springframework.web.servlet.ModelAndView;

public class ConsentModelAndViewMapper {

    public static ModelAndView map(@NonNull final ConsentResponse response) {
        if (response instanceof ConsentResponseSkip accepted) {
            return ModelAndViewUtils.redirectToDifferentContext(accepted.redirectTo());
        }
        if (response instanceof ConsentResponseRequiresUIDisplay uiRedirect) {
            return new ModelAndView("consent")
                    .addObject("consentChallenge", uiRedirect.consentChallenge())
                    .addObject("scopes", uiRedirect.requestedScopes());
        }
        if (response instanceof ConsentResponseAccepted accepted) {
            return ModelAndViewUtils.redirectToDifferentContext(accepted.redirectTo());
        }

        throw new IllegalStateException("Unknown response type: " + response.getClass());
    }

}
