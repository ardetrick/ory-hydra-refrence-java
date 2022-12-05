package com.ardetrick.oryhydrareference.consent;

import com.ardetrick.oryhydrareference.modelandview.ModelAndViewUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/consent")
@RequiredArgsConstructor
public class ConsentController {

    @NonNull ConsentService consentService;

    /**
     * The Consent Endpoint (set by urls.consent) is an application written by you. You can find an exemplary Node.js reference implementation on our GitHub.
     * The Consent Endpoint uses the consent_challenge value in the URL to fetch information about the consent request by making a HTTP GET request to:
     * http(s)://<HYDRA_ADMIN_URL>/oauth2/auth/requests/consent?consent_challenge=<challenge>
     * The response (see below in "Consent Challenge Response" tab) contains information about the consent request. The body contains a skip value. If the value is false, the user interface must be shown. If skip is true, you shouldn't show the user interface but instead just accept or reject the consent request! For more details about the implementation check the "Implementing the Consent Endpoint" Guide.
     * <p>
     * <a href="https://www.ory.sh/docs/hydra/guides/login">...</a>
     * <a href="https://www.ory.sh/docs/hydra/concepts/consent">...</a>
     */
    @GetMapping
    public ModelAndView consentEndpoint(@RequestParam("consent_challenge") String consentChallenge) {
        val response = consentService.processInitialConsentRequest(consentChallenge);
        if (response instanceof InitialConsentResponseAcceptedRedirect) {
            return ModelAndViewUtils.redirect(
                    ((InitialConsentResponseAcceptedRedirect) response).redirectTo()
            );
        }

        if (response instanceof InitialConsentResponseUIRedirect) {
            return new ModelAndView("consent")
                    .addObject("consentChallenge", consentChallenge);
        }

        throw new IllegalStateException("Unknown response type: " + response.getClass());
    }

    @PostMapping
    public ModelAndView submitConsentForm(ConsentForm consentForm) {
        val x = consentService.processConsentForm(consentForm);

        return ModelAndViewUtils.redirect(x.getRedirectTo());
    }

}
