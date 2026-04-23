package net.creeperhost.testmod.datagen;

import net.creeperhost.polylib.datagen.PolyLibLangProvider;
import net.minecraft.data.PackOutput;

/**
 * Datagen lang provider for testmod.
 * All keys registered through PolyLib APIs (accessibility toggles, player settings display names,
 * config panel labels, PolyRegistry item/block names) are contributed automatically.
 * Only add manual entries here for keys not covered by PolyLib's automatic contribution system.
 */
public class TestLangProvider extends PolyLibLangProvider
{
    public TestLangProvider(PackOutput output)
    {
        super(output, "testmod");
    }

    @Override
    protected void addModTranslations()
    {
        // Keys auto-contributed by PolyLib:
        //   testmod.setting.reduce_screenshake        (PlayerClientSettingsRegistry)
        //   testmod.accessibility.demo_category       (AccessibilityOptionsRegistry builder)
        //   testmod.accessibility.reduce_motion       (AccessibilityOptionsRegistry builder)
        //   testmod.accessibility.high_contrast       (AccessibilityOptionsRegistry builder)
        //   item.testmod.*  /  block.testmod.*        (PolyRegistry registrations in TestItems/TestBlocks)
        // No manual entries needed unless you add keys outside of PolyLib registration APIs.
    }
}
