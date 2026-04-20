package com.clayfactoria.codecs.task;

import com.clayfactoria.codecs.Job;
import com.clayfactoria.codecs.Task;
import com.clayfactoria.components.JobComponent;
import com.clayfactoria.utils.CraftingUtils;
import com.clayfactoria.utils.TaskHelper;
import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.clayfactoria.utils.TaskHelper.getNPCEntity;

public class WorkTaskExecutor extends PointTaskExecutor {

    private static @Nullable CraftingRecipe findCraftingRecipe(BlockType blockType, JobComponent jobComponent) {
        List<CraftingRecipe> benchRecipes = CraftingPlugin.getBenchRecipes(blockType.getBench());
        CraftingRecipe foundRecipe = null;
        for (CraftingRecipe recipe : benchRecipes) {
            String itemId = recipe.getPrimaryOutput().getItemId();
            assert itemId != null;
            if (itemId.equals(jobComponent.getFilterItem())) {
                foundRecipe = recipe;
                break;
            }
        }
        return foundRecipe;
    }

    private static boolean autocraft(Ref<EntityStore> entityRef, Ref<ChunkStore> blockRef, World world) {
        Store<EntityStore> entityStore = entityRef.getStore();
        JobComponent jobComponent = entityStore.getComponent(entityRef, JobComponent.getComponentType());
        assert jobComponent != null;
        Job currentJob = jobComponent.getCurrentJob();
        if (currentJob == null
            || jobComponent.getFilterItem() == null
            || currentJob.getLocation() == null
        ) return false;

        BlockType blockType = world.getBlockType(currentJob.getLocation());
        assert blockType != null;

        CraftingManager craftingManager = entityStore.ensureAndGetComponent(entityRef, CraftingManager.getComponentType());
        craftingManager.setBench(currentJob.getLocation().x, currentJob.getLocation().y, currentJob.getLocation().z, blockType);
        CraftingRecipe foundRecipe = findCraftingRecipe(blockType, jobComponent);
        if (foundRecipe == null) return false;

        boolean wasCrafted = CraftingUtils.craftItem(entityRef, blockRef, foundRecipe, 1);
        craftingManager.clearBench(entityRef, entityStore);
        return wasCrafted;
    }

    private static boolean enableProcessingBench(Ref<ChunkStore> blockRef) {
        Store<ChunkStore> blockStore = blockRef.getStore();
        BenchBlock benchBlock = blockStore.getComponent(blockRef, BenchBlock.getComponentType());
        ProcessingBenchBlock processingBenchBlock = blockStore.getComponent(blockRef, ProcessingBenchBlock.getComponentType());
        if (processingBenchBlock != null
            && benchBlock != null
            && !processingBenchBlock.getInputContainer().isEmpty()
        ) {
            return processingBenchBlock.setActive(true, benchBlock, null);
        } else {
            return false;
        }
    }

    private static boolean canDoProcessingBenchActivation(Ref<ChunkStore> ref, Store<ChunkStore> store) {
        ProcessingBenchBlock processingBenchBlock = store.getComponent(ref, ProcessingBenchBlock.getComponentType());
        if (processingBenchBlock != null) {
            return !processingBenchBlock.isActive();
        } else {
            return false;
        }
    }

    private static boolean canDoCraft(Ref<EntityStore> npcRef, Ref<ChunkStore> blockRef) {
        Store<EntityStore> store = npcRef.getStore();
        NPCEntity npcEntity = getNPCEntity(npcRef);
        JobComponent jobComponent = Objects.requireNonNull(store.getComponent(npcRef, JobComponent.getComponentType()));
        Job currentJob = Objects.requireNonNull(jobComponent.getCurrentJob());
        World world = Objects.requireNonNull(npcEntity.getWorld());
        Vector3i pos = Objects.requireNonNull(currentJob.getLocation());
        BlockType blockType = world.getBlockType(pos);
        if (blockType == null || blockType.getBench() == null) return false;

        CraftingRecipe recipe = findCraftingRecipe(blockType, jobComponent);
        if (recipe == null) return false;

        CombinedItemContainer combinedSurroundingResources = CraftingUtils
            .getCombinedSurroundingResources(npcRef, blockRef);
        List<MaterialQuantity> materials = CraftingUtils.getInputMaterials(recipe, 1);
        return combinedSurroundingResources.canRemoveMaterials(materials);
    }

    @Nullable
    private static Ref<ChunkStore> getTaskBlockRef(Ref<EntityStore> ref) {
        NPCEntity npcEntity = getNPCEntity(ref);
        Job currentJob = Objects.requireNonNull(JobComponent.getCurrentJob(ref));
        World world = Objects.requireNonNull(npcEntity.getWorld());
        Vector3i pos = Objects.requireNonNull(currentJob.getLocation());

        return TaskHelper.getBlockComponentHolderDirectReference(world, pos.x,
            pos.y, pos.z);
    }

    @Override
    public boolean canPerformTask(Ref<EntityStore> ref) {
        Ref<ChunkStore> blockRef = Objects.requireNonNull(getTaskBlockRef(ref));
        Store<ChunkStore> blockStore = blockRef.getStore();

        if (canDoProcessingBenchActivation(blockRef, blockStore)) {
            return true;
        } else {
            return canDoCraft(ref, blockRef);
        }
    }

    @Override
    public boolean execute(Ref<EntityStore> ref) {
        NPCEntity npcEntity = getNPCEntity(ref);
        Job currentJob = Objects.requireNonNull(JobComponent.getCurrentJob(ref));
        World world = Objects.requireNonNull(npcEntity.getWorld());
        Vector3i pos = Objects.requireNonNull(currentJob.getLocation());

        Ref<ChunkStore> blockRef = TaskHelper.getBlockComponentHolderDirectReference(world, pos.x,
            pos.y, pos.z);
        assert blockRef != null;

        if (enableProcessingBench(blockRef)) {
            return true;
        } else {
            return autocraft(ref, blockRef, world);
        }
    }

    @Override
    public Task relevantNextTask(List<Task> availableOptions) {
        return Task.WORK;
    }

}
