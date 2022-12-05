package com.ardetrick.oryhydrareference.consent;

import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;
import sh.ory.hydra.model.OAuth2RedirectTo;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsentService {

    @NonNull HydraAdminClient hydraAdminClient;

    public InitialConsentResponse processInitialConsentRequest(@NonNull String consentChallenge) {
        val consentRequest = hydraAdminClient.getConsentRequest(consentChallenge);

        val skip = consentRequest.getSkip();
        if(Boolean.TRUE.equals(skip)) {
            val acceptConsentResponse = hydraAdminClient.acceptConsentRequest(
                    consentChallenge,
                    true,
                    Objects.requireNonNull(consentRequest.getRequestedScope()),
                    consentRequest
            );
            return new InitialConsentResponseAcceptedRedirect(acceptConsentResponse.getRedirectTo());
        }
        return new InitialConsentResponseUIRedirect(consentRequest.getRequestedScope());
    }

    public OAuth2RedirectTo processConsentForm(ConsentForm consentForm) {
        val consentRequest = hydraAdminClient.getConsentRequest(consentForm.consentChallenge());

        return hydraAdminClient.acceptConsentRequest(
                consentForm.consentChallenge(),
                consentForm.isRemember(),
                consentForm.scopes(),
                consentRequest
        );
    }

}

sealed interface InitialConsentResponse
        permits InitialConsentResponseAcceptedRedirect, InitialConsentResponseUIRedirect {
}

record InitialConsentResponseAcceptedRedirect(String redirectTo) implements InitialConsentResponse {}
record InitialConsentResponseUIRedirect(List<String> requestedScopes) implements InitialConsentResponse {}
