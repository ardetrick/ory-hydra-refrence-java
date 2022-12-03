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


/**
 * extends layout
 *
 * block content
 *     h1 An application requests access to your data!
 *     form(action=action, method="POST")
 *         input(type="hidden", name="challenge", value=challenge)
 *         input(type="hidden", name="_csrf", value=csrfToken)
 *
 *         if client.logo_uri
 *             img(src=client.logo_uri)
 *
 *         p.
 *             Hi #{user}, application <strong>#{client.client_name || client.client_id}</strong> wants access resources on your behalf and to:
 *
 *         each scope in requested_scope
 *             input(type="checkbox", class="grant_scope", id=scope, value=scope, name="grant_scope")
 *             label(for=scope) #{scope}
 *             br
 *
 *         p.
 *             Do you want to be asked next time when this application wants to access your data? The application will
 *             not be able to ask for more permissions without your consent.
 *         ul
 *             if client.policy_uri
 *                 li
 *                     a(href=client.policy_uri) Policy
 *             if client.tos_uri
 *                 li
 *                     a(href=client.tos_uri) Terms of Service
 *         p
 *             input(type="checkbox", id="remember", name="remember", value="1")
 *             label(for="remember") Do not ask me again
 *         p
 *             input(type="submit", id="accept", name="submit", value="Allow access")
 *             input(type="submit", id="reject", name="submit", value="Deny access")
 */

// Accepting:

/**
 * // ...
 *   let grantScope = req.body.grant_scope
 *   if (!Array.isArray(grantScope)) {
 *     grantScope = [grantScope]
 *   }
 *
 *   // The session allows us to set session data for id and access tokens
 *   let session: AcceptOAuth2ConsentRequestSession = {
 *     // This data will be available when introspecting the token. Try to avoid sensitive information here,
 *     // unless you limit who can introspect tokens.
 *     access_token: {
 *       // foo: 'bar'
 *     },
 *
 *     // This data will be available in the ID token.
 *     id_token: {
 *       // baz: 'bar'
 *     },
 *   }
 *
 *   // Here is also the place to add data to the ID or access token. For example,
 *   // if the scope 'profile' is added, add the family and given name to the ID Token claims:
 *   // if (grantScope.indexOf('profile')) {
 *   //   session.id_token.family_name = 'Doe'
 *   //   session.id_token.given_name = 'John'
 *   // }
 *
 *   // Let's fetch the consent request again to be able to set `grantAccessTokenAudience` properly.
 *   hydraAdmin
 *     .adminGetOAuth2ConsentRequest(challenge)
 *     // This will be called if the HTTP request was successful
 *     .then(({ data: body }) => {
 *       return hydraAdmin
 *         .adminAcceptOAuth2ConsentRequest(challenge, {
 *           // We can grant all scopes that have been requested - hydra already checked for us that no additional scopes
 *           // are requested accidentally.
 *           grant_scope: grantScope,
 *
 *           // If the environment variable CONFORMITY_FAKE_CLAIMS is set we are assuming that
 *           // the app is built for the automated OpenID Connect Conformity Test Suite. You
 *           // can peak inside the code for some ideas, but be aware that all data is fake
 *           // and this only exists to fake a login system which works in accordance to OpenID Connect.
 *           //
 *           // If that variable is not set, the session will be used as-is.
 *           session: oidcConformityMaybeFakeSession(grantScope, body, session),
 *
 *           // ORY Hydra checks if requested audiences are allowed by the client, so we can simply echo this.
 *           grant_access_token_audience: body.requested_access_token_audience,
 *
 *           // This tells hydra to remember this consent request and allow the same client to request the same
 *           // scopes from the same user, without showing the UI, in the future.
 *           remember: Boolean(req.body.remember),
 *
 *           // When this "remember" sesion expires, in seconds. Set this to 0 so it will never expire.
 *           remember_for: 3600,
 *         })
 *         .then(({ data: body }) => {
 *           // All we need to do now is to redirect the user back to hydra!
 *           res.redirect(String(body.redirect_to))
 *         })
 *     })
 *     // This will handle any error that happens when making HTTP calls to hydra
 *     .catch(next)
 *   // label:docs-accept-consent
 * // ...
 */

// Rejecting:

/**
 * // Copyright © 2022 Ory Corp
 * // SPDX-License-Identifier: Apache-2.0
 *
 * import express from "express"
 * import url from "url"
 * import urljoin from "url-join"
 * import csrf from "csurf"
 * import { hydraAdmin } from "../config"
 * import { oidcConformityMaybeFakeSession } from "./stub/oidc-cert"
 * import { AcceptOAuth2ConsentRequestSession } from "@ory/client"
 *
 * // Sets up csrf protection
 * const csrfProtection = csrf({
 *   cookie: {
 *     sameSite: "lax",
 *   },
 * })
 * const router = express.Router()
 *
 * router.get("/", csrfProtection, (req, res, next) => {
 *   // Parses the URL query
 *   const query = url.parse(req.url, true).query
 *
 *   // The challenge is used to fetch information about the consent request from ORY hydraAdmin.
 *   const challenge = String(query.consent_challenge)
 *   if (!challenge) {
 *     next(new Error("Expected a consent challenge to be set but received none."))
 *     return
 *   }
 *
 *   // This section processes consent requests and either shows the consent UI or
 *   // accepts the consent request right away if the user has given consent to this
 *   // app before
 *   hydraAdmin
 *     .adminGetOAuth2ConsentRequest(challenge)
 *     // This will be called if the HTTP request was successful
 *     .then(({ data: body }) => {
 *       // If a user has granted this application the requested scope, hydra will tell us to not show the UI.
 *       if (body.skip) {
 *         // You can apply logic here, for example grant another scope, or do whatever...
 *         // ...
 *
 *         // Now it's time to grant the consent request. You could also deny the request if something went terribly wrong
 *         return hydraAdmin
 *           .adminAcceptOAuth2ConsentRequest(challenge, {
 *             // We can grant all scopes that have been requested - hydra already checked for us that no additional scopes
 *             // are requested accidentally.
 *             grant_scope: body.requested_scope,
 *
 *             // ORY Hydra checks if requested audiences are allowed by the client, so we can simply echo this.
 *             grant_access_token_audience: body.requested_access_token_audience,
 *
 *             // The session allows us to set session data for id and access tokens
 *             session: {
 *               // This data will be available when introspecting the token. Try to avoid sensitive information here,
 *               // unless you limit who can introspect tokens.
 *               // accessToken: { foo: 'bar' },
 *               // This data will be available in the ID token.
 *               // idToken: { baz: 'bar' },
 *             },
 *           })
 *           .then(({ data: body }) => {
 *             // All we need to do now is to redirect the user back to hydra!
 *             res.redirect(String(body.redirect_to))
 *           })
 *       }
 *
 *       // If consent can't be skipped we MUST show the consent UI.
 *       res.render("consent", {
 *         csrfToken: req.csrfToken(),
 *         challenge: challenge,
 *         // We have a bunch of data available from the response, check out the API docs to find what these values mean
 *         // and what additional data you have available.
 *         requested_scope: body.requested_scope,
 *         user: body.subject,
 *         client: body.client,
 *         action: urljoin(process.env.BASE_URL || "", "/consent"),
 *       })
 *     })
 *     // This will handle any error that happens when making HTTP calls to hydra
 *     .catch(next)
 *   // The consent request has now either been accepted automatically or rendered.
 * })
 *
 * router.post("/", csrfProtection, (req, res, next) => {
 *   // The challenge is now a hidden input field, so let's take it from the request body instead
 *   const challenge = req.body.challenge
 *
 *   // Let's see if the user decided to accept or reject the consent request..
 *   if (req.body.submit === "Deny access") {
 *     // Looks like the consent request was denied by the user
 *     return (
 *       hydraAdmin
 *         .adminRejectOAuth2ConsentRequest(challenge, {
 *           error: "access_denied",
 *           error_description: "The resource owner denied the request",
 *         })
 *         .then(({ data: body }) => {
 *           // All we need to do now is to redirect the browser back to hydra!
 *           res.redirect(String(body.redirect_to))
 *         })
 *         // This will handle any error that happens when making HTTP calls to hydra
 *         .catch(next)
 *     )
 *   }
 *   // label:consent-deny-end
 * // ...
 */

// SKipping

/**
 * // ...
 *   // This section processes consent requests and either shows the consent UI or
 *   // accepts the consent request right away if the user has given consent to this
 *   // app before
 *   hydraAdmin
 *     .adminGetOAuth2ConsentRequest(challenge)
 *     // This will be called if the HTTP request was successful
 *     .then(({ data: body }) => {
 *       // If a user has granted this application the requested scope, hydra will tell us to not show the UI.
 *       if (body.skip) {
 *         // You can apply logic here, for example grant another scope, or do whatever...
 *         // ...
 *
 *         // Now it's time to grant the consent request. You could also deny the request if something went terribly wrong
 *         return hydraAdmin
 *           .adminAcceptOAuth2ConsentRequest(challenge, {
 *             // We can grant all scopes that have been requested - hydra already checked for us that no additional scopes
 *             // are requested accidentally.
 *             grant_scope: body.requested_scope,
 *
 *             // ORY Hydra checks if requested audiences are allowed by the client, so we can simply echo this.
 *             grant_access_token_audience: body.requested_access_token_audience,
 *
 *             // The session allows us to set session data for id and access tokens
 *             session: {
 *               // This data will be available when introspecting the token. Try to avoid sensitive information here,
 *               // unless you limit who can introspect tokens.
 *               // accessToken: { foo: 'bar' },
 *               // This data will be available in the ID token.
 *               // idToken: { baz: 'bar' },
 *             },
 *           })
 *           .then(({ data: body }) => {
 *             // All we need to do now is to redirect the user back to hydra!
 *             res.redirect(String(body.redirect_to))
 *           })
 *       }
 *
 *       // If consent can't be skipped we MUST show the consent UI.
 *       res.render("consent", {
 *         csrfToken: req.csrfToken(),
 *         challenge: challenge,
 *         // We have a bunch of data available from the response, check out the API docs to find what these values mean
 *         // and what additional data you have available.
 *         requested_scope: body.requested_scope,
 *         user: body.subject,
 *         client: body.client,
 *         action: urljoin(process.env.BASE_URL || "", "/consent"),
 *       })
 *     })
 *     // This will handle any error that happens when making HTTP calls to hydra
 *     .catch(next)
 *   // The consent request has now either been accepted automatically or rendered.
 * // ...
 */

// Full

/**
 * // Copyright © 2022 Ory Corp
 * // SPDX-License-Identifier: Apache-2.0
 *
 * import express from "express"
 * import url from "url"
 * import urljoin from "url-join"
 * import csrf from "csurf"
 * import { hydraAdmin } from "../config"
 * import { oidcConformityMaybeFakeSession } from "./stub/oidc-cert"
 * import { AcceptOAuth2ConsentRequestSession } from "@ory/client"
 *
 * // Sets up csrf protection
 * const csrfProtection = csrf({
 *   cookie: {
 *     sameSite: "lax",
 *   },
 * })
 * const router = express.Router()
 *
 * router.get("/", csrfProtection, (req, res, next) => {
 *   // Parses the URL query
 *   const query = url.parse(req.url, true).query
 *
 *   // The challenge is used to fetch information about the consent request from ORY hydraAdmin.
 *   const challenge = String(query.consent_challenge)
 *   if (!challenge) {
 *     next(new Error("Expected a consent challenge to be set but received none."))
 *     return
 *   }
 *
 *   // This section processes consent requests and either shows the consent UI or
 *   // accepts the consent request right away if the user has given consent to this
 *   // app before
 *   hydraAdmin
 *     .adminGetOAuth2ConsentRequest(challenge)
 *     // This will be called if the HTTP request was successful
 *     .then(({ data: body }) => {
 *       // If a user has granted this application the requested scope, hydra will tell us to not show the UI.
 *       if (body.skip) {
 *         // You can apply logic here, for example grant another scope, or do whatever...
 *         // ...
 *
 *         // Now it's time to grant the consent request. You could also deny the request if something went terribly wrong
 *         return hydraAdmin
 *           .adminAcceptOAuth2ConsentRequest(challenge, {
 *             // We can grant all scopes that have been requested - hydra already checked for us that no additional scopes
 *             // are requested accidentally.
 *             grant_scope: body.requested_scope,
 *
 *             // ORY Hydra checks if requested audiences are allowed by the client, so we can simply echo this.
 *             grant_access_token_audience: body.requested_access_token_audience,
 *
 *             // The session allows us to set session data for id and access tokens
 *             session: {
 *               // This data will be available when introspecting the token. Try to avoid sensitive information here,
 *               // unless you limit who can introspect tokens.
 *               // accessToken: { foo: 'bar' },
 *               // This data will be available in the ID token.
 *               // idToken: { baz: 'bar' },
 *             },
 *           })
 *           .then(({ data: body }) => {
 *             // All we need to do now is to redirect the user back to hydra!
 *             res.redirect(String(body.redirect_to))
 *           })
 *       }
 *
 *       // If consent can't be skipped we MUST show the consent UI.
 *       res.render("consent", {
 *         csrfToken: req.csrfToken(),
 *         challenge: challenge,
 *         // We have a bunch of data available from the response, check out the API docs to find what these values mean
 *         // and what additional data you have available.
 *         requested_scope: body.requested_scope,
 *         user: body.subject,
 *         client: body.client,
 *         action: urljoin(process.env.BASE_URL || "", "/consent"),
 *       })
 *     })
 *     // This will handle any error that happens when making HTTP calls to hydra
 *     .catch(next)
 *   // The consent request has now either been accepted automatically or rendered.
 * })
 *
 * router.post("/", csrfProtection, (req, res, next) => {
 *   // The challenge is now a hidden input field, so let's take it from the request body instead
 *   const challenge = req.body.challenge
 *
 *   // Let's see if the user decided to accept or reject the consent request..
 *   if (req.body.submit === "Deny access") {
 *     // Looks like the consent request was denied by the user
 *     return (
 *       hydraAdmin
 *         .adminRejectOAuth2ConsentRequest(challenge, {
 *           error: "access_denied",
 *           error_description: "The resource owner denied the request",
 *         })
 *         .then(({ data: body }) => {
 *           // All we need to do now is to redirect the browser back to hydra!
 *           res.redirect(String(body.redirect_to))
 *         })
 *         // This will handle any error that happens when making HTTP calls to hydra
 *         .catch(next)
 *     )
 *   }
 *   // label:consent-deny-end
 *
 *   let grantScope = req.body.grant_scope
 *   if (!Array.isArray(grantScope)) {
 *     grantScope = [grantScope]
 *   }
 *
 *   // The session allows us to set session data for id and access tokens
 *   let session: AcceptOAuth2ConsentRequestSession = {
 *     // This data will be available when introspecting the token. Try to avoid sensitive information here,
 *     // unless you limit who can introspect tokens.
 *     access_token: {
 *       // foo: 'bar'
 *     },
 *
 *     // This data will be available in the ID token.
 *     id_token: {
 *       // baz: 'bar'
 *     },
 *   }
 *
 *   // Here is also the place to add data to the ID or access token. For example,
 *   // if the scope 'profile' is added, add the family and given name to the ID Token claims:
 *   // if (grantScope.indexOf('profile')) {
 *   //   session.id_token.family_name = 'Doe'
 *   //   session.id_token.given_name = 'John'
 *   // }
 *
 *   // Let's fetch the consent request again to be able to set `grantAccessTokenAudience` properly.
 *   hydraAdmin
 *     .adminGetOAuth2ConsentRequest(challenge)
 *     // This will be called if the HTTP request was successful
 *     .then(({ data: body }) => {
 *       return hydraAdmin
 *         .adminAcceptOAuth2ConsentRequest(challenge, {
 *           // We can grant all scopes that have been requested - hydra already checked for us that no additional scopes
 *           // are requested accidentally.
 *           grant_scope: grantScope,
 *
 *           // If the environment variable CONFORMITY_FAKE_CLAIMS is set we are assuming that
 *           // the app is built for the automated OpenID Connect Conformity Test Suite. You
 *           // can peak inside the code for some ideas, but be aware that all data is fake
 *           // and this only exists to fake a login system which works in accordance to OpenID Connect.
 *           //
 *           // If that variable is not set, the session will be used as-is.
 *           session: oidcConformityMaybeFakeSession(grantScope, body, session),
 *
 *           // ORY Hydra checks if requested audiences are allowed by the client, so we can simply echo this.
 *           grant_access_token_audience: body.requested_access_token_audience,
 *
 *           // This tells hydra to remember this consent request and allow the same client to request the same
 *           // scopes from the same user, without showing the UI, in the future.
 *           remember: Boolean(req.body.remember),
 *
 *           // When this "remember" sesion expires, in seconds. Set this to 0 so it will never expire.
 *           remember_for: 3600,
 *         })
 *         .then(({ data: body }) => {
 *           // All we need to do now is to redirect the user back to hydra!
 *           res.redirect(String(body.redirect_to))
 *         })
 *     })
 *     // This will handle any error that happens when making HTTP calls to hydra
 *     .catch(next)
 *   // label:docs-accept-consent
 * })
 *
 * export default router
 */