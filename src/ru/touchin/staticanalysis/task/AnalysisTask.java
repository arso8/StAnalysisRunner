package ru.touchin.staticanalysis.task;

import com.intellij.ide.browsers.OpenUrlHyperlinkInfo;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalysisTask extends Task.Backgroundable {

    private static final String NOTIFICATION_TITLE = "Static Analysis";

    @NotNull
    private final Project project;
    @Nullable
    private Process gradlewProcess;
    private boolean isRunning;

    public AnalysisTask(@NotNull Project project) {
        super(project, "StaticAnalysis", true);
        this.project = project;
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        isRunning = true;
        progressIndicator.setIndeterminate(true);

        try {
            runAnalysis(progressIndicator);
        } catch (ProcessCanceledException canceledException) {
            progressIndicator.cancel();
        } catch (Exception exception) {
            showErrorNotification("Exception: " + exception.getMessage());
        }
    }

    @Override
    public void onCancel() {
        if (gradlewProcess != null) {
            gradlewProcess.destroy();
        }
    }

    @Override
    public void onFinished() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void runAnalysis(@NotNull ProgressIndicator progressIndicator) throws Exception {
        String analysisOutput = getAnalysisOutput(progressIndicator);
        if (!analysisOutput.startsWith("Error") && !analysisOutput.startsWith("FAILURE")) {
            if (Pattern.compile("Overall: PASSED").matcher(analysisOutput).find()) {
                showInfoNotification("Overall: PASSED!");
            } else if (!Pattern.compile("Overall: FAILED").matcher(analysisOutput).find()) {
                showErrorNotification("Can't detect analysis result. Try to run it manually.");
            } else {
                Pattern errorsCountPattern = Pattern.compile("Overall: FAILED \\((.+)\\)\u001b");
                Pattern urlPattern = Pattern.compile("file:.+full_report.html");
                Matcher errorsCountMatcher = errorsCountPattern.matcher(analysisOutput);
                Matcher urlMatcher = urlPattern.matcher(analysisOutput);
                if (errorsCountMatcher.find() && urlMatcher.find()) {
                    showErrorNotification(String.format("Analysis failed: %s", errorsCountMatcher.group(1)));
                    (new OpenUrlHyperlinkInfo(urlMatcher.group())).navigate(project);
                } else {
                    showErrorNotification("Can't detect analysis result. Try to run it manually.");
                }
            }
        } else {
            showErrorNotification(analysisOutput);
        }
    }

    @NotNull
    private String getAnalysisOutput(@NotNull ProgressIndicator progressIndicator) throws Exception {
        final List<String> gradlewCommand = System.getProperty("os.name").startsWith("Windows")
                ? Arrays.asList("cmd", "/c", "gradlew.bat", "staticAnalys")
                : Arrays.asList("./gradlew", "staticAnalys");
        gradlewProcess = new ProcessBuilder(gradlewCommand)
                .directory(new File(project.getBasePath()))
                .redirectErrorStream(true)
                .start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gradlewProcess.getInputStream()));
        StringBuilder analysisOutputBuilder = new StringBuilder();

        String outputLine;
        while ((outputLine = bufferedReader.readLine()) != null) {
            progressIndicator.setText2(outputLine);
            progressIndicator.checkCanceled();
            analysisOutputBuilder.append(outputLine);
            analysisOutputBuilder.append('\n');
        }

        return analysisOutputBuilder.toString();
    }

    private void showErrorNotification(@NotNull String message) {
        new NotificationGroup(NOTIFICATION_TITLE, NotificationDisplayType.STICKY_BALLOON, true)
                .createNotification(NOTIFICATION_TITLE, message, NotificationType.ERROR, null)
                .notify(project);
    }

    private void showInfoNotification(@NotNull String message) {
        new NotificationGroup(NOTIFICATION_TITLE, NotificationDisplayType.STICKY_BALLOON, true,
                null, PluginIcons.PLUGIN_ICON);
        new Notification(NOTIFICATION_TITLE, PluginIcons.PLUGIN_ICON, NOTIFICATION_TITLE,
                null, message, NotificationType.INFORMATION, null)
                .notify(project);
    }

}
