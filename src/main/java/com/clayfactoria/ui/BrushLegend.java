package com.clayfactoria.ui;

import com.clayfactoria.codecs.Automaton;
import com.clayfactoria.codecs.Job;
import com.clayfactoria.components.BrushComponent;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

public class BrushLegend extends CustomUIHud {

    private final BrushComponent brushComponent;
    private final @Nullable Automaton selectedAutomaton;
    private final @Nullable String filterItem;
    private int maxTaskDisplay = 8;

    public BrushLegend(@NotNull PlayerRef playerRef, BrushComponent brushComponent,
                       @Nullable Automaton selectedAutomaton, @Nullable String filterItem) {
        super(playerRef);
        this.brushComponent = brushComponent;
        this.selectedAutomaton = selectedAutomaton;
        this.filterItem = filterItem;
    }

    @Override
    protected void build(@NotNull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Hud/ToolsLegends/BrushLegend.ui");
        uiCommandBuilder.append("Hud/ToolsLegends/CustomToolsLegendsCommon.ui");

        if (selectedAutomaton == null) {
            uiCommandBuilder.set("#LeftClick.Visible", false);
            uiCommandBuilder.set("#RightClick.Visible", false);
            uiCommandBuilder.set("#Description.Visible", false);
            uiCommandBuilder.set("#SelectedIcon.Background", "Common/UnknownItemIcon.png");
            return;
        }

        uiCommandBuilder.set("#Description.Text", selectedAutomaton.description);
        uiCommandBuilder.set("#SelectedIcon.Background",
            "Hud/ToolsLegends/" + selectedAutomaton.roleName + ".png");
        uiCommandBuilder.set("#SelectToBegin.Text",
            "You have selected a " + selectedAutomaton.name + "!");
        uiCommandBuilder.set("#LeftClickLabel.Text",
            "Add a '" + this.brushComponent.getTask().name + "' task here");
        uiCommandBuilder.appendInline("#Page1",
            "Group {Anchor: (Height: 2, Vertical: 10); Background: #ffffff(0.15);}");
        buildFilterItemInfo(uiCommandBuilder);
        buildJobList(uiCommandBuilder);
    }

    private void buildFilterItemInfo(@Nonnull UICommandBuilder uiCommandBuilder) {
        if (selectedAutomaton == null || selectedAutomaton.filterItemDescription == null) {
            return;
        }
        maxTaskDisplay -= 2;
        uiCommandBuilder.appendInline("#Page1", buildLabel(selectedAutomaton.filterItemDescription + "\n"));
        if (filterItem != null) {
            uiCommandBuilder.appendInline("#Page1", "ItemIcon {ItemId: \"" + filterItem + "\"; Anchor: (Width: 64, Height: 64); }");
        }
        uiCommandBuilder.appendInline("#Page1",
            "Group {Anchor: (Height: 2, Vertical: 10); Background: #ffffff(0.15);}");
    }

    private void buildJobList(@NotNull UICommandBuilder uiCommandBuilder) {
        List<Job> jobs = brushComponent.getJobs();
        if (jobs.isEmpty()) {
            uiCommandBuilder.appendInline("#Page1", buildLabel("No tasks set yet"));
            return;
        }
        for (Job job : jobs.subList(0, Math.min(maxTaskDisplay, jobs.size()))) {
            uiCommandBuilder.appendInline("#Page1", buildJobString(job));
        }
        if (jobs.size() > maxTaskDisplay) {
            uiCommandBuilder.appendInline("#Page1", buildLabel("..."));
        }
    }

    private String buildLabel(String label) {
        return "Group {"
            + "          FlexWeight: 1;"
            + "          Anchor: (Top: 0);"
            + "          LayoutMode: Center;"
            + "          Label {"
            + "            Text: \"" + label + "\";"
            + "            Style: ("
            + "              FontSize: 14,"
            + "              TextColor: #96a9be,"
            + "              Wrap: true"
            + "            );"
            + "          }"
            + "        }";
    }

    private String buildImageLabel(String imagePath, String label) {
        return "Group {LayoutMode: CenterMiddle; Anchor: (Vertical: 4);"
            + "Group {Padding: (Horizontal: 6, Vertical: 8); Anchor: (MinWidth: 24, Right: 10); Background: (TexturePath: \"Common/InputBinding.png\", Border: 6); LayoutMode: CenterMiddle;"
            + "Group {Anchor: (Width: 24, Height: 24); "
            + "Background: \"" + imagePath + "\";}}"
            + "Group {FlexWeight: 1; Anchor: (Top: 0); LayoutMode: Center;"
            + "Label {Text: \"" + label + "\"; "
            + "Style: (FontSize: 14, TextColor: #96a9be, Wrap: true);}}}";
    }

    private String buildJobString(Job job) {
        return buildImageLabel(job.getTask().iconAssetPath, "'" + job.getTask().name + "' Task");
    }
}
