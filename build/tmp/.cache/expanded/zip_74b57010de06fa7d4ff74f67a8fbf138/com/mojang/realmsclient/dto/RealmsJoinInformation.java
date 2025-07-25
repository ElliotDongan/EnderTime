package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record RealmsJoinInformation(
    @Nullable @SerializedName("address") String address,
    @Nullable @SerializedName("resourcePackUrl") String resourcePackUrl,
    @Nullable @SerializedName("resourcePackHash") String resourcePackHash,
    @Nullable @SerializedName("sessionRegionData") RealmsJoinInformation.RegionData regionData
) implements ReflectionBasedSerialization {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RealmsJoinInformation EMPTY = new RealmsJoinInformation(null, null, null, null);

    public static RealmsJoinInformation parse(GuardedSerializer p_408204_, String p_406962_) {
        try {
            RealmsJoinInformation realmsjoininformation = p_408204_.fromJson(p_406962_, RealmsJoinInformation.class);
            if (realmsjoininformation == null) {
                LOGGER.error("Could not parse RealmsServerAddress: {}", p_406962_);
                return EMPTY;
            } else {
                return realmsjoininformation;
            }
        } catch (Exception exception) {
            LOGGER.error("Could not parse RealmsServerAddress: {}", exception.getMessage());
            return EMPTY;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record RegionData(
        @Nullable @SerializedName("regionName") RealmsRegion region, @Nullable @SerializedName("serviceQuality") ServiceQuality serviceQuality
    ) implements ReflectionBasedSerialization {
    }
}