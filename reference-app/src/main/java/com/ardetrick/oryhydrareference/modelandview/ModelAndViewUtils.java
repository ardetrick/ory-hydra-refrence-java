package com.ardetrick.oryhydrareference.modelandview;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.springframework.web.servlet.ModelAndView;

@UtilityClass
public class ModelAndViewUtils {

    private static final String SPRING_REDIRECT_PREFIX = "redirect:";

    public ModelAndView redirect(@NonNull String redirectUrl) {
        return new ModelAndView(SPRING_REDIRECT_PREFIX + redirectUrl);
    }

}
