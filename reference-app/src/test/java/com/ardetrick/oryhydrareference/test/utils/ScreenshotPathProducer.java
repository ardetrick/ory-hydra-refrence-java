package com.ardetrick.oryhydrareference.test.utils;

import com.microsoft.playwright.Page;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.val;

import java.nio.file.Paths;

@Value
@Builder
public class ScreenshotPathProducer {

    private static final String PREFIX = "build/test-results/screenshots/";

    @NonNull String testName;
    @NonFinal @Builder.Default int currentIndexPrefix = 1;

    public Page.ScreenshotOptions screenshotOptionsForStepName(String stepName) {
        return new Page.ScreenshotOptions()
                .setPath(Paths.get(getPathAndIncrementIndex(stepName)));
    }

    private String getPathAndIncrementIndex(String stepName) {
        val path = PREFIX + testName + "/" + currentIndexPrefix + "-" + stepName  + ".png";
        currentIndexPrefix++;
        return path;
    }

}
