package com.ardetrick.oryhydrareference.consent;

import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;
import sh.ory.hydra.model.OAuth2RedirectTo;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsentService {

    @NonNull HydraAdminClient hydraAdminClient;

    public InitialConsentResponse processInitialConsentRequest(@NonNull String consentChallenge) {
        val consentRequest = hydraAdminClient.getConsentRequest(consentChallenge);

        val skip = consentRequest.getSkip();
        if(Boolean.TRUE.equals(skip)) {
            val acceptConsentResponse = hydraAdminClient.acceptConsentRequest(consentChallenge, true, consentRequest);
            return new InitialConsentResponseAcceptedRedirect(acceptConsentResponse.getRedirectTo());
        }
        return new InitialConsentResponseUIRedirect();
    }

    public OAuth2RedirectTo processConsentForm(ConsentForm consentForm) {
        val consentRequest = hydraAdminClient.getConsentRequest(consentForm.consentChallenge());

        return hydraAdminClient.acceptConsentRequest(
                consentForm.consentChallenge(),
                consentForm.isRemember(),
                consentRequest
        );
    }

}

sealed interface InitialConsentResponse
        permits InitialConsentResponseAcceptedRedirect, InitialConsentResponseUIRedirect {
}

record InitialConsentResponseAcceptedRedirect(String redirectTo) implements InitialConsentResponse {}
record InitialConsentResponseUIRedirect() implements InitialConsentResponse {}
