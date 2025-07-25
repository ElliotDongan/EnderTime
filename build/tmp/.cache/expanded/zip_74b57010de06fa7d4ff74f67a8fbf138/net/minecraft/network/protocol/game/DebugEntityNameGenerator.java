package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class DebugEntityNameGenerator {
    private static final String[] NAMES_FIRST_PART = new String[]{
        "Slim",
        "Far",
        "River",
        "Silly",
        "Fat",
        "Thin",
        "Fish",
        "Bat",
        "Dark",
        "Oak",
        "Sly",
        "Bush",
        "Zen",
        "Bark",
        "Cry",
        "Slack",
        "Soup",
        "Grim",
        "Hook",
        "Dirt",
        "Mud",
        "Sad",
        "Hard",
        "Crook",
        "Sneak",
        "Stink",
        "Weird",
        "Fire",
        "Soot",
        "Soft",
        "Rough",
        "Cling",
        "Scar"
    };
    private static final String[] NAMES_SECOND_PART = new String[]{
        "Fox",
        "Tail",
        "Jaw",
        "Whisper",
        "Twig",
        "Root",
        "Finder",
        "Nose",
        "Brow",
        "Blade",
        "Fry",
        "Seek",
        "Wart",
        "Tooth",
        "Foot",
        "Leaf",
        "Stone",
        "Fall",
        "Face",
        "Tongue",
        "Voice",
        "Lip",
        "Mouth",
        "Snail",
        "Toe",
        "Ear",
        "Hair",
        "Beard",
        "Shirt",
        "Fist"
    };

    public static String getEntityName(Entity p_179487_) {
        if (p_179487_ instanceof Player) {
            return p_179487_.getName().getString();
        } else {
            Component component = p_179487_.getCustomName();
            return component != null ? component.getString() : getEntityName(p_179487_.getUUID());
        }
    }

    public static String getEntityName(UUID p_133669_) {
        RandomSource randomsource = getRandom(p_133669_);
        return getRandomString(randomsource, NAMES_FIRST_PART) + getRandomString(randomsource, NAMES_SECOND_PART);
    }

    private static String getRandomString(RandomSource p_237881_, String[] p_237882_) {
        return Util.getRandom(p_237882_, p_237881_);
    }

    private static RandomSource getRandom(UUID p_237884_) {
        return RandomSource.create(p_237884_.hashCode() >> 2);
    }
}