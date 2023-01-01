package com.ardetrick.oryhydrareference.demo;

import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * This controller is for demonstration purposes only to make it easier to run through the OAuth2 flow and interact with
 * Hydra clients. It is not required nor expected to be used for any production use cases.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/demo")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DemoController {

    @NonNull HydraAdminClient hydraAdminClient;

    @GetMapping(produces = {MediaType.TEXT_HTML_VALUE})
    @ResponseBody
    public ModelAndView test() {
        val clients = hydraAdminClient.listOAuth2Clients()
                .stream()
                .map(client -> new MvcClient(client.getClientName(), client.getClientId(), client.getRedirectUris()))
                .toList();

        return new ModelAndView("demo")
                .addObject("clients", clients);
    }

}
