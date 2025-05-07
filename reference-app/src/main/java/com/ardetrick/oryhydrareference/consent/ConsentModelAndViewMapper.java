package com.ardetrick.oryhydrareference.consent;

import com.ardetrick.oryhydrareference.consent.ConsentResponse.Accepted;
import com.ardetrick.oryhydrareference.consent.ConsentResponse.DisplayUI;
import com.ardetrick.oryhydrareference.consent.ConsentResponse.Skip;
import com.ardetrick.oryhydrareference.modelandview.ModelAndViewUtils;
import lombok.NonNull;
import org.springframework.web.servlet.ModelAndView;

public class ConsentModelAndViewMapper {

    public static ModelAndView map(@NonNull final ConsentResponse response) {
        return switch (response) {
            case Skip r -> handleSkip(r);
            case DisplayUI r -> handleDisplayUI(r);
            case Accepted r -> handleAccepted(r);
        };
    }

    private static ModelAndView handleSkip(Skip consentResponseSkip) {
        return ModelAndViewUtils.redirectToDifferentContext(consentResponseSkip.redirectTo());
    }

    private static ModelAndView handleDisplayUI(DisplayUI displayUI) {
        return new ModelAndView("consent")
                .addObject("consentChallenge", displayUI.consentChallenge())
                .addObject("scopes", displayUI.requestedScopes());
    }

    private static ModelAndView handleAccepted(Accepted accepted) {
        return ModelAndViewUtils.redirectToDifferentContext(accepted.redirectTo());
    }

}
