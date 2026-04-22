package com.clayfactoria.systems;

import com.clayfactoria.codecs.Automaton;
import com.clayfactoria.components.BrushComponent;
import com.clayfactoria.components.JobComponent;
import com.clayfactoria.ui.BrushLegend;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entityui.UIComponentList;
import com.hypixel.hytale.server.core.modules.entityui.UIComponentSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

public class BrushLegendSystem extends UIComponentSystems.Update {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();


    public BrushLegendSystem() {
        super(EntityTrackerSystems.Visible.getComponentType(), UIComponentList.getComponentType());
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) {
            return;
        }
        InventoryComponent.Hotbar hotbarComponent = archetypeChunk.getComponent(index,
            InventoryComponent.Hotbar.getComponentType());
        Objects.requireNonNull(hotbarComponent);
        ItemStack itemStack = hotbarComponent.getActiveItem();

        if (itemStack != null && itemStack.getItemId().equals("Tool_Brush")) {
            update(index, archetypeChunk, store, commandBuffer);
        } else {
            player.getHudManager()
                .resetHud(player.getPlayerRef());
        }
    }

    @Override
    public @NotNull Query<EntityStore> getQuery() {
        return Query.any();
    }

    private void update(int index,
                        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                        @Nonnull Store<EntityStore> store,
                        @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        BrushComponent brushComponent = archetypeChunk.getComponent(index,
            BrushComponent.getComponentType());
        if (brushComponent == null) {
            return;
        }
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        Objects.requireNonNull(player);
        Ref<EntityStore> npcRef = getNPCRef(player, brushComponent);
        Automaton automaton = getAutomaton(commandBuffer, npcRef);
        String filterItemId = getFilterItemId(commandBuffer, npcRef);
        player.getHudManager().setCustomHud(
            player.getPlayerRef(),
            new BrushLegend(player.getPlayerRef(), brushComponent, automaton, filterItemId));
    }

    private @Nullable Automaton getAutomaton(CommandBuffer<EntityStore> commandBuffer,
                                             Ref<EntityStore> npcRef
    ) {
        if (npcRef == null) {
            return null;
        }

        NPCEntity npc = commandBuffer.getComponent(npcRef,
            Objects.requireNonNull(NPCEntity.getComponentType()));
        return npc == null ? null : Automaton.getFromRole(npc.getRole());
    }

    private @Nullable String getFilterItemId(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> npcRef) {
        if (npcRef == null) {
            return null;
        }
        JobComponent jobComponent = commandBuffer.getComponent(npcRef, JobComponent.getComponentType());
        return jobComponent == null ? null : jobComponent.getFilterItem();
    }

    private @Nullable Ref<EntityStore> getNPCRef(Player player,
                                                 BrushComponent brushComponent) {
        UUID entityID = brushComponent.getEntityId();
        if (entityID == null) {
            return null;
        }
        Objects.requireNonNull(player.getWorld());
        return player.getWorld().getEntityRef(entityID);
    }
}
