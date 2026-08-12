package io.genai.robots.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * Bottom tool window hosting the latest sitemap-validation result.
 * <p>Written in Java on purpose: a Kotlin class implementing {@link ToolWindowFactory} emits
 * synthetic overrides of the interface's internal/experimental default methods (manage/getAnchor/
 * getIcon…), which the Plugin Verifier rejects as internal-API usage. Java just inherits them.
 */
public final class SitemapToolWindowFactory implements ToolWindowFactory, DumbAware {

    public static final String ID = "Sitemap Validation";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JBLabel placeholder = new JBLabel(
                "Right-click a robots.txt and choose “Validate Sitemap(s)” to see results here.");
        placeholder.setBorder(JBUI.Borders.empty(16));
        setContent(toolWindow, placeholder);
    }

    /** Open the tool window on {@code file} and start validating (with Stop/Rerun controls). */
    public static void show(@NotNull Project project, @NotNull VirtualFile file) {
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(ID);
        if (tw == null) return;
        setContent(tw, new SitemapValidationPanel(project, file));
        tw.setAvailable(true);
        tw.activate(null);
    }

    private static void setContent(@NotNull ToolWindow tw, @NotNull JComponent component) {
        Content content = ContentFactory.getInstance().createContent(component, "", false);
        tw.getContentManager().removeAllContents(true);
        tw.getContentManager().addContent(content);
    }
}
