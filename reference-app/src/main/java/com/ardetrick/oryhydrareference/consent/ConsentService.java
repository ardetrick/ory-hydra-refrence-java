package com.ardetrick.oryhydrareference.consent;

import com.ardetrick.oryhydrareference.consent.ConsentResponse.Accepted;
import com.ardetrick.oryhydrareference.consent.ConsentResponse.DisplayUI;
import com.ardetrick.oryhydrareference.consent.ConsentResponse.Skip;
import com.ardetrick.oryhydrareference.hydra.AcceptConsentRequest;
import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsentService {

    @NonNull HydraAdminClient hydraAdminClient;

    public ConsentResponse processInitialConsentRequest(@NonNull final String consentChallenge) {
        val consentRequest = hydraAdminClient.getConsentRequest(consentChallenge);

        if (Boolean.TRUE.equals(consentRequest.getSkip())) {
            val acceptConsentRequest = AcceptConsentRequest.builder()
                    .consentChallenge(consentChallenge)
                    .remember(true)
                    .grantAccessTokenAudience(consentRequest.getRequestedAccessTokenAudience())
                    .scopes(consentRequest.getRequestedScope())
                    .build();
            val acceptConsentResponse = hydraAdminClient.acceptConsentRequest(acceptConsentRequest);
            return new Skip(acceptConsentResponse.getRedirectTo());
        }

        return new DisplayUI(consentRequest.getRequestedScope(), consentChallenge);
    }

    public ConsentResponse processConsentForm(@NonNull final ConsentForm consentForm) {
        val consentChallenge = consentForm.consentChallenge();

        val consentRequest = hydraAdminClient.getConsentRequest(consentChallenge);

        val acceptConsentRequest = AcceptConsentRequest.builder()
                .consentChallenge(consentChallenge)
                .remember(consentForm.isRemember())
                .grantAccessTokenAudience(consentRequest.getRequestedAccessTokenAudience())
                .scopes(consentForm.scopes())
                .build();

        val oauth2RedirectTo = hydraAdminClient.acceptConsentRequest(acceptConsentRequest);

        return new Accepted(oauth2RedirectTo.getRedirectTo());
    }

}
