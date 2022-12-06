package com.ardetrick.oryhydrareference.modelandview;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@UtilityClass
public class ModelAndViewUtils {

    public ModelAndView redirectToDifferentContext(@NonNull String redirectUrl) {
        return new ModelAndView(new RedirectView(redirectUrl, false));
    }

}
