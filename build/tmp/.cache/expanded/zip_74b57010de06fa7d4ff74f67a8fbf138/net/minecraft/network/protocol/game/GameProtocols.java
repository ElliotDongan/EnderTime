package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.SkipPacketDecoderException;
import net.minecraft.network.SkipPacketEncoderException;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.CodecModifier;
import net.minecraft.network.protocol.ProtocolInfoBuilder;
import net.minecraft.network.protocol.SimpleUnboundProtocol;
import net.minecraft.network.protocol.UnboundProtocol;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.CookiePacketTypes;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.PingPacketTypes;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;

public class GameProtocols {
    public static final CodecModifier<RegistryFriendlyByteBuf, ServerboundSetCreativeModeSlotPacket, GameProtocols.Context> HAS_INFINITE_MATERIALS = (p_389938_, p_389939_) -> new StreamCodec<RegistryFriendlyByteBuf, ServerboundSetCreativeModeSlotPacket>(
        
    ) {
        public ServerboundSetCreativeModeSlotPacket decode(RegistryFriendlyByteBuf p_392330_) {
            if (!p_389939_.hasInfiniteMaterials()) {
                throw new SkipPacketDecoderException("Not in creative mode");
            } else {
                return (ServerboundSetCreativeModeSlotPacket)p_389938_.decode(p_392330_);
            }
        }

        public void encode(RegistryFriendlyByteBuf p_392801_, ServerboundSetCreativeModeSlotPacket p_391657_) {
            if (!p_389939_.hasInfiniteMaterials()) {
                throw new SkipPacketEncoderException("Not in creative mode");
            } else {
                p_389938_.encode(p_392801_, p_391657_);
            }
        }
    };
    public static final UnboundProtocol<ServerGamePacketListener, RegistryFriendlyByteBuf, GameProtocols.Context> SERVERBOUND_TEMPLATE = ProtocolInfoBuilder.contextServerboundProtocol(
        ConnectionProtocol.PLAY,
        p_405110_ -> p_405110_.addPacket(GamePacketTypes.SERVERBOUND_ACCEPT_TELEPORTATION, ServerboundAcceptTeleportationPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_BLOCK_ENTITY_TAG_QUERY, ServerboundBlockEntityTagQueryPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_BUNDLE_ITEM_SELECTED, ServerboundSelectBundleItemPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CHANGE_DIFFICULTY, ServerboundChangeDifficultyPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CHANGE_GAME_MODE, ServerboundChangeGameModePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CHAT_ACK, ServerboundChatAckPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CHAT_COMMAND, ServerboundChatCommandPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CHAT_COMMAND_SIGNED, ServerboundChatCommandSignedPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CHAT, ServerboundChatPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CHAT_SESSION_UPDATE, ServerboundChatSessionUpdatePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CHUNK_BATCH_RECEIVED, ServerboundChunkBatchReceivedPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CLIENT_COMMAND, ServerboundClientCommandPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CLIENT_TICK_END, ServerboundClientTickEndPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.SERVERBOUND_CLIENT_INFORMATION, ServerboundClientInformationPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_COMMAND_SUGGESTION, ServerboundCommandSuggestionPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CONFIGURATION_ACKNOWLEDGED, ServerboundConfigurationAcknowledgedPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CONTAINER_BUTTON_CLICK, ServerboundContainerButtonClickPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CONTAINER_CLICK, ServerboundContainerClickPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CONTAINER_CLOSE, ServerboundContainerClosePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_CONTAINER_SLOT_STATE_CHANGED, ServerboundContainerSlotStateChangedPacket.STREAM_CODEC)
            .addPacket(CookiePacketTypes.SERVERBOUND_COOKIE_RESPONSE, ServerboundCookieResponsePacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.SERVERBOUND_CUSTOM_PAYLOAD, ServerboundCustomPayloadPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_DEBUG_SAMPLE_SUBSCRIPTION, ServerboundDebugSampleSubscriptionPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_EDIT_BOOK, ServerboundEditBookPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_ENTITY_TAG_QUERY, ServerboundEntityTagQueryPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_INTERACT, ServerboundInteractPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_JIGSAW_GENERATE, ServerboundJigsawGeneratePacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.SERVERBOUND_KEEP_ALIVE, ServerboundKeepAlivePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_LOCK_DIFFICULTY, ServerboundLockDifficultyPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS, ServerboundMovePlayerPacket.Pos.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS_ROT, ServerboundMovePlayerPacket.PosRot.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_MOVE_PLAYER_ROT, ServerboundMovePlayerPacket.Rot.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_MOVE_PLAYER_STATUS_ONLY, ServerboundMovePlayerPacket.StatusOnly.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_MOVE_VEHICLE, ServerboundMoveVehiclePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PADDLE_BOAT, ServerboundPaddleBoatPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PICK_ITEM_FROM_BLOCK, ServerboundPickItemFromBlockPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PICK_ITEM_FROM_ENTITY, ServerboundPickItemFromEntityPacket.STREAM_CODEC)
            .addPacket(PingPacketTypes.SERVERBOUND_PING_REQUEST, ServerboundPingRequestPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PLACE_RECIPE, ServerboundPlaceRecipePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PLAYER_ABILITIES, ServerboundPlayerAbilitiesPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PLAYER_ACTION, ServerboundPlayerActionPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PLAYER_COMMAND, ServerboundPlayerCommandPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PLAYER_INPUT, ServerboundPlayerInputPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_PLAYER_LOADED, ServerboundPlayerLoadedPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.SERVERBOUND_PONG, ServerboundPongPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_RECIPE_BOOK_CHANGE_SETTINGS, ServerboundRecipeBookChangeSettingsPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_RECIPE_BOOK_SEEN_RECIPE, ServerboundRecipeBookSeenRecipePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_RENAME_ITEM, ServerboundRenameItemPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.SERVERBOUND_RESOURCE_PACK, ServerboundResourcePackPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SEEN_ADVANCEMENTS, ServerboundSeenAdvancementsPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SELECT_TRADE, ServerboundSelectTradePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SET_BEACON, ServerboundSetBeaconPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SET_CARRIED_ITEM, ServerboundSetCarriedItemPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SET_COMMAND_BLOCK, ServerboundSetCommandBlockPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SET_COMMAND_MINECART, ServerboundSetCommandMinecartPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SET_CREATIVE_MODE_SLOT, ServerboundSetCreativeModeSlotPacket.STREAM_CODEC, HAS_INFINITE_MATERIALS)
            .addPacket(GamePacketTypes.SERVERBOUND_SET_JIGSAW_BLOCK, ServerboundSetJigsawBlockPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SET_STRUCTURE_BLOCK, ServerboundSetStructureBlockPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SET_TEST_BLOCK, ServerboundSetTestBlockPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SIGN_UPDATE, ServerboundSignUpdatePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_SWING, ServerboundSwingPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_TELEPORT_TO_ENTITY, ServerboundTeleportToEntityPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_TEST_INSTANCE_BLOCK_ACTION, ServerboundTestInstanceBlockActionPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_USE_ITEM_ON, ServerboundUseItemOnPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.SERVERBOUND_USE_ITEM, ServerboundUseItemPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.SERVERBOUND_CUSTOM_CLICK_ACTION, ServerboundCustomClickActionPacket.STREAM_CODEC)
    );
    public static final SimpleUnboundProtocol<ClientGamePacketListener, RegistryFriendlyByteBuf> CLIENTBOUND_TEMPLATE = ProtocolInfoBuilder.clientboundProtocol(
        ConnectionProtocol.PLAY,
        p_405111_ -> p_405111_.withBundlePacket(GamePacketTypes.CLIENTBOUND_BUNDLE, ClientboundBundlePacket::new, new ClientboundBundleDelimiterPacket())
            .addPacket(GamePacketTypes.CLIENTBOUND_ADD_ENTITY, ClientboundAddEntityPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_ANIMATE, ClientboundAnimatePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_AWARD_STATS, ClientboundAwardStatsPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_BLOCK_CHANGED_ACK, ClientboundBlockChangedAckPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_BLOCK_DESTRUCTION, ClientboundBlockDestructionPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_BLOCK_ENTITY_DATA, ClientboundBlockEntityDataPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_BLOCK_EVENT, ClientboundBlockEventPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_BLOCK_UPDATE, ClientboundBlockUpdatePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_BOSS_EVENT, ClientboundBossEventPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CHANGE_DIFFICULTY, ClientboundChangeDifficultyPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CHUNK_BATCH_FINISHED, ClientboundChunkBatchFinishedPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CHUNK_BATCH_START, ClientboundChunkBatchStartPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CHUNKS_BIOMES, ClientboundChunksBiomesPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CLEAR_TITLES, ClientboundClearTitlesPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_COMMAND_SUGGESTIONS, ClientboundCommandSuggestionsPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_COMMANDS, ClientboundCommandsPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CONTAINER_CLOSE, ClientboundContainerClosePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CONTAINER_SET_CONTENT, ClientboundContainerSetContentPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CONTAINER_SET_DATA, ClientboundContainerSetDataPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CONTAINER_SET_SLOT, ClientboundContainerSetSlotPacket.STREAM_CODEC)
            .addPacket(CookiePacketTypes.CLIENTBOUND_COOKIE_REQUEST, ClientboundCookieRequestPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_COOLDOWN, ClientboundCooldownPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_CUSTOM_CHAT_COMPLETIONS, ClientboundCustomChatCompletionsPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_CUSTOM_PAYLOAD, ClientboundCustomPayloadPacket.GAMEPLAY_STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_DAMAGE_EVENT, ClientboundDamageEventPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_DEBUG_SAMPLE, ClientboundDebugSamplePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_DELETE_CHAT, ClientboundDeleteChatPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_DISCONNECT, ClientboundDisconnectPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_DISGUISED_CHAT, ClientboundDisguisedChatPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_ENTITY_EVENT, ClientboundEntityEventPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_ENTITY_POSITION_SYNC, ClientboundEntityPositionSyncPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_EXPLODE, ClientboundExplodePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_FORGET_LEVEL_CHUNK, ClientboundForgetLevelChunkPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_GAME_EVENT, ClientboundGameEventPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_HORSE_SCREEN_OPEN, ClientboundHorseScreenOpenPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_HURT_ANIMATION, ClientboundHurtAnimationPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_INITIALIZE_BORDER, ClientboundInitializeBorderPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_KEEP_ALIVE, ClientboundKeepAlivePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_LEVEL_CHUNK_WITH_LIGHT, ClientboundLevelChunkWithLightPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_LEVEL_EVENT, ClientboundLevelEventPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_LEVEL_PARTICLES, ClientboundLevelParticlesPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_LIGHT_UPDATE, ClientboundLightUpdatePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_LOGIN, ClientboundLoginPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_MAP_ITEM_DATA, ClientboundMapItemDataPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_MERCHANT_OFFERS, ClientboundMerchantOffersPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS, ClientboundMoveEntityPacket.Pos.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS_ROT, ClientboundMoveEntityPacket.PosRot.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_MOVE_MINECART_ALONG_TRACK, ClientboundMoveMinecartPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_ROT, ClientboundMoveEntityPacket.Rot.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_MOVE_VEHICLE, ClientboundMoveVehiclePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_OPEN_BOOK, ClientboundOpenBookPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_OPEN_SCREEN, ClientboundOpenScreenPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_OPEN_SIGN_EDITOR, ClientboundOpenSignEditorPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_PING, ClientboundPingPacket.STREAM_CODEC)
            .addPacket(PingPacketTypes.CLIENTBOUND_PONG_RESPONSE, ClientboundPongResponsePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLACE_GHOST_RECIPE, ClientboundPlaceGhostRecipePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_ABILITIES, ClientboundPlayerAbilitiesPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_CHAT, ClientboundPlayerChatPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_COMBAT_END, ClientboundPlayerCombatEndPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_COMBAT_ENTER, ClientboundPlayerCombatEnterPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_COMBAT_KILL, ClientboundPlayerCombatKillPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_INFO_REMOVE, ClientboundPlayerInfoRemovePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_INFO_UPDATE, ClientboundPlayerInfoUpdatePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_LOOK_AT, ClientboundPlayerLookAtPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_POSITION, ClientboundPlayerPositionPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PLAYER_ROTATION, ClientboundPlayerRotationPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_RECIPE_BOOK_ADD, ClientboundRecipeBookAddPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_RECIPE_BOOK_REMOVE, ClientboundRecipeBookRemovePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_RECIPE_BOOK_SETTINGS, ClientboundRecipeBookSettingsPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_REMOVE_ENTITIES, ClientboundRemoveEntitiesPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_REMOVE_MOB_EFFECT, ClientboundRemoveMobEffectPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_RESET_SCORE, ClientboundResetScorePacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_RESOURCE_PACK_POP, ClientboundResourcePackPopPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_RESOURCE_PACK_PUSH, ClientboundResourcePackPushPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_RESPAWN, ClientboundRespawnPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_ROTATE_HEAD, ClientboundRotateHeadPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SECTION_BLOCKS_UPDATE, ClientboundSectionBlocksUpdatePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SELECT_ADVANCEMENTS_TAB, ClientboundSelectAdvancementsTabPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SERVER_DATA, ClientboundServerDataPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_ACTION_BAR_TEXT, ClientboundSetActionBarTextPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_BORDER_CENTER, ClientboundSetBorderCenterPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_BORDER_LERP_SIZE, ClientboundSetBorderLerpSizePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_BORDER_SIZE, ClientboundSetBorderSizePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_BORDER_WARNING_DELAY, ClientboundSetBorderWarningDelayPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_BORDER_WARNING_DISTANCE, ClientboundSetBorderWarningDistancePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_CAMERA, ClientboundSetCameraPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_CHUNK_CACHE_CENTER, ClientboundSetChunkCacheCenterPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_CHUNK_CACHE_RADIUS, ClientboundSetChunkCacheRadiusPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_CURSOR_ITEM, ClientboundSetCursorItemPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_DEFAULT_SPAWN_POSITION, ClientboundSetDefaultSpawnPositionPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_DISPLAY_OBJECTIVE, ClientboundSetDisplayObjectivePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_ENTITY_DATA, ClientboundSetEntityDataPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_ENTITY_LINK, ClientboundSetEntityLinkPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_ENTITY_MOTION, ClientboundSetEntityMotionPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_EQUIPMENT, ClientboundSetEquipmentPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_EXPERIENCE, ClientboundSetExperiencePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_HEALTH, ClientboundSetHealthPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_HELD_SLOT, ClientboundSetHeldSlotPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_OBJECTIVE, ClientboundSetObjectivePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_PASSENGERS, ClientboundSetPassengersPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_PLAYER_INVENTORY, ClientboundSetPlayerInventoryPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_PLAYER_TEAM, ClientboundSetPlayerTeamPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_SCORE, ClientboundSetScorePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_SIMULATION_DISTANCE, ClientboundSetSimulationDistancePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_SUBTITLE_TEXT, ClientboundSetSubtitleTextPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_TIME, ClientboundSetTimePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_TITLE_TEXT, ClientboundSetTitleTextPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SET_TITLES_ANIMATION, ClientboundSetTitlesAnimationPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SOUND_ENTITY, ClientboundSoundEntityPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SOUND, ClientboundSoundPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_START_CONFIGURATION, ClientboundStartConfigurationPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_STOP_SOUND, ClientboundStopSoundPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_STORE_COOKIE, ClientboundStoreCookiePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_SYSTEM_CHAT, ClientboundSystemChatPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_TAB_LIST, ClientboundTabListPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_TAG_QUERY, ClientboundTagQueryPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_TAKE_ITEM_ENTITY, ClientboundTakeItemEntityPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_TELEPORT_ENTITY, ClientboundTeleportEntityPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_TEST_INSTANCE_BLOCK_STATUS, ClientboundTestInstanceBlockStatus.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_TICKING_STATE, ClientboundTickingStatePacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_TICKING_STEP, ClientboundTickingStepPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_TRANSFER, ClientboundTransferPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_UPDATE_ADVANCEMENTS, ClientboundUpdateAdvancementsPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_UPDATE_ATTRIBUTES, ClientboundUpdateAttributesPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_UPDATE_MOB_EFFECT, ClientboundUpdateMobEffectPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_UPDATE_RECIPES, ClientboundUpdateRecipesPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_UPDATE_TAGS, ClientboundUpdateTagsPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_PROJECTILE_POWER, ClientboundProjectilePowerPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_CUSTOM_REPORT_DETAILS, ClientboundCustomReportDetailsPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_SERVER_LINKS, ClientboundServerLinksPacket.STREAM_CODEC)
            .addPacket(GamePacketTypes.CLIENTBOUND_WAYPOINT, ClientboundTrackedWaypointPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_CLEAR_DIALOG, ClientboundClearDialogPacket.STREAM_CODEC)
            .addPacket(CommonPacketTypes.CLIENTBOUND_SHOW_DIALOG, ClientboundShowDialogPacket.STREAM_CODEC)
    );

    public interface Context {
        boolean hasInfiniteMaterials();
    }
}