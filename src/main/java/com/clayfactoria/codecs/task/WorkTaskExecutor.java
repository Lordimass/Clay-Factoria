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
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
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

    private static void cleanUp(Ref<EntityStore> ref, World world, Vector3i blockPos, JobComponent jobComponent) {
        AnimationUtils.stopAnimation(ref, AnimationSlot.Status, ref.getStore());
        setBlockInteractionState("default", world, blockPos);
        jobComponent.setJobStartTime(0);
    }

    private static boolean canDoCraft(Ref<EntityStore> npcRef, Ref<ChunkStore> blockRef) {
        Store<EntityStore> store = npcRef.getStore();
        NPCEntity npcEntity = getNPCEntity(npcRef);
        JobComponent jobComponent = Objects.requireNonNull(store.getComponent(npcRef, JobComponent.getComponentType()));
        Job currentJob = Objects.requireNonNull(jobComponent.getCurrentJob());
        World world = Objects.requireNonNull(npcEntity.getWorld());
        Vector3i pos = Objects.requireNonNull(currentJob.getLocation());
        BlockType blockType = world.getBlockType(pos);
        if (blockType == null || blockType.getBench() == null) {
            cleanUp(npcRef, world, pos, jobComponent);
            return false;
        }

        CraftingRecipe recipe = findCraftingRecipe(blockType, jobComponent);
        if (recipe == null) {
            cleanUp(npcRef, world, pos, jobComponent);
            return false;
        }

        CombinedItemContainer combinedSurroundingResources = CraftingUtils
            .getCombinedSurroundingResources(npcRef, blockRef);
        List<MaterialQuantity> materials = CraftingUtils.getInputMaterials(recipe, 1);
        if (!combinedSurroundingResources.canRemoveMaterials(materials)
            || TaskHelper.areExtraItemsInInventory(npcRef)) {
            cleanUp(npcRef, world, pos, jobComponent);
            return false;
        }

        if (jobComponent.getJobStartTime() == 0) {
            jobComponent.setJobStartTime(System.currentTimeMillis());
            if (recipe.getTimeSeconds() == 0) {
                setBlockInteractionState("CraftCompletedInstant", world, pos);
            } else {
                AnimationUtils.playAnimation(npcRef, AnimationSlot.Status, "Craft", false, store);
                setBlockInteractionState("CraftCompleted", world, pos);
            }
            return false;
        }

        return (System.currentTimeMillis() - jobComponent.getJobStartTime()) >= recipe.getTimeSeconds() * 1000L;
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

    private static void setBlockInteractionState(@Nonnull String state, @Nonnull World world, @Nonnull Vector3i pos) {
        WorldChunk worldChunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (worldChunk != null) {
            BlockType blockType = worldChunk.getBlockType(pos.x, pos.y, pos.z);
            if (blockType != null) {
                worldChunk.setBlockInteractionState(pos.x, pos.y, pos.z, blockType, state, true);
            }
        }
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
        Store<EntityStore> entityStore = ref.getStore();
        NPCEntity npcEntity = getNPCEntity(ref);
        Job currentJob = Objects.requireNonNull(JobComponent.getCurrentJob(ref));
        World world = Objects.requireNonNull(npcEntity.getWorld());
        Vector3i pos = Objects.requireNonNull(currentJob.getLocation());
        JobComponent jobComponent = Objects.requireNonNull(entityStore.getComponent(ref, JobComponent.getComponentType()));
        cleanUp(ref, world, pos, jobComponent);

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
        if (availableOptions.contains(Task.DEPOSIT)) {
            return Task.DEPOSIT;
        }
        return Task.WORK;
    }

}
