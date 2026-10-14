package org.codecooker.roulette;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.util.Mth;

@Config(name = "TNT Config")
public class TNTRouletteConfig implements ConfigData {
    @ConfigEntry.Category("general") @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Category("general") @ConfigEntry.Gui.Tooltip
    public boolean affectCreative = false;

    //Revolver
    @ConfigEntry.Category("revolver") @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 2, max = 12)
    public int chambers = 6;

    @ConfigEntry.Category("revolver") @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 11)
    public int bullets = 1;

    @ConfigEntry.Category("revolver") @ConfigEntry.Gui.Tooltip
    public boolean respinAfterClick = false;

    @ConfigEntry.Category("revolver") @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int misfireChance = 10;

    @ConfigEntry.Category("revolver") @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
    public int fuseTicks = 40;

    @ConfigEntry.Category("revolver") @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 45)
    public int graceSeconds = 2;

    //Breaking
    @ConfigEntry.Category("breaking")
    public boolean breakEnabled = true;

    @ConfigEntry.Category("breaking") @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int breakBaseChance = 4;

    @ConfigEntry.Category("breaking") @ConfigEntry.Gui.Tooltip
    public boolean ignoreInstantBreakBlocks = true;

    //Placing
    @ConfigEntry.Category("placing")
    public boolean placeEnabled = true;

    @ConfigEntry.Category("placing") @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int placeBaseChance = 6;

    //Streamer
    @ConfigEntry.Category("others") @ConfigEntry.Gui.Tooltip
    public boolean showCylinder = true;

    @ConfigEntry.Category("others") @ConfigEntry.Gui.Tooltip
    public boolean clickSound = true;

    @ConfigEntry.Category("others") @ConfigEntry.Gui.Tooltip
    public boolean announceInChat = false;

    @Override
    public void validatePostLoad() {
        chambers           = Mth.clamp(chambers, 2, 12);
        bullets            = Mth.clamp(bullets, 1, chambers - 1);
        misfireChance      = Mth.clamp(misfireChance, 0, 100);
        fuseTicks          = Mth.clamp(fuseTicks, 10, 200);
        graceSeconds       = Mth.clamp(graceSeconds, 0, 45);
        breakBaseChance = Mth.clamp(breakBaseChance, 0, 100);
        placeBaseChance = Mth.clamp(placeBaseChance, 0, 100);
    }
}
