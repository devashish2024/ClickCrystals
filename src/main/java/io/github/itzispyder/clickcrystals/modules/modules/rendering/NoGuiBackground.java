package io.github.itzispyder.clickcrystals.modules.modules.rendering;

import io.github.itzispyder.clickcrystals.modules.Categories;
import io.github.itzispyder.clickcrystals.modules.ModuleSetting;
import io.github.itzispyder.clickcrystals.modules.modules.DummyModule;
import io.github.itzispyder.clickcrystals.modules.settings.SettingSection;

public class NoGuiBackground extends DummyModule {

    private final SettingSection scGeneral = getGeneralSection();
    public final ModuleSetting<Boolean> DisableBlackOverlayBackground = scGeneral.add(createBoolSetting()
            .name("disable-black-overlay-effect")
            .description("Disable the black overlay background in the GUI.")
            .def(true)
            .build()
    );

    public final ModuleSetting<Boolean> blurToggle = scGeneral.add(createBoolSetting()
            .name("disable-blur-effect.")
            .description("Disable the blur effect in the GUI.")
            .def(true)
            .build()
    );

    public NoGuiBackground() {
        super("no-gui-background", Categories.RENDER, "disable the black effect when opening the GUI");
    }
}
