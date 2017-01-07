package ru.touchin.staticanalysis.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import ru.touchin.staticanalysis.task.AnalysisTask;

public class RunStaticAnalysis extends AnAction {

    @Nullable
    private AnalysisTask analysisTask;

    public RunStaticAnalysis() {
        super("Touchin static analysis");
    }

    @Override
    public void actionPerformed(AnActionEvent actionEvent) {
        final Project project = actionEvent.getData(PlatformDataKeys.PROJECT);
        assert project != null;
        analysisTask = new AnalysisTask(project);
        analysisTask.queue();
    }

    @Override
    public void update(AnActionEvent actionEvent) {
        actionEvent.getPresentation().setEnabled(!(analysisTask != null && analysisTask.isRunning()));
    }

}
