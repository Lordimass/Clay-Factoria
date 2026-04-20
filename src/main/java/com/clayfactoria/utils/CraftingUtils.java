package com.clayfactoria.utils;

import com.clayfactoria.codecs.Job;
import com.clayfactoria.components.JobComponent;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.player.windows.MaterialExtraResourcesSection;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MaterialTransaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CraftingUtils {
    private CraftingUtils() {
    }

    /**
     * Remove the items required for a given crafting recipe from an item container.
     *
     * @param itemContainer  The container to take the resources from.
     * @param craftingRecipe The recipe to take the materials for.
     * @param quantity       The number of times the recipe is being made.
     * @return A <code>boolean</code> indicating whether the transaction was successful.
     */
    public static boolean removeInputFromInventory(@Nonnull ItemContainer itemContainer,
                                                   @Nonnull CraftingRecipe craftingRecipe,
                                                   int quantity) {
        List<MaterialQuantity> materialsToRemove = getInputMaterials(craftingRecipe, quantity);
        if (materialsToRemove.isEmpty()) {
            return true;
        } else {
            ListTransaction<MaterialTransaction> materialTransactions = itemContainer.removeMaterials(materialsToRemove, true, true, true);
            return materialTransactions.succeeded();
        }
    }

    /**
     * Retrieve the required materials for a given crafting recipe.
     *
     * @param recipe   The recipe to get the materials for.
     * @param quantity The number of times the recipe is being made.
     * @return A list of {@link MaterialQuantity}s representing the required materials for the crafting recipe.
     */
    public static List<MaterialQuantity> getInputMaterials(@Nonnull CraftingRecipe recipe,
                                                           int quantity) {
        Objects.requireNonNull(recipe);
        if (recipe.getInput() == null) {
            return Collections.emptyList();
        }

        ObjectList<MaterialQuantity> materials = new ObjectArrayList<>();

        for (MaterialQuantity craftingMaterial : recipe.getInput()) {
            String itemId = craftingMaterial.getItemId();
            String resourceTypeId = craftingMaterial.getResourceTypeId();
            int materialQuantity = craftingMaterial.getQuantity();
            BsonDocument metadata = craftingMaterial.getMetadata();
            materials.add(new MaterialQuantity(itemId, resourceTypeId, null, materialQuantity * quantity, metadata));
        }
        return materials;
    }

    private static List<ItemStack> convMaterialQuantitiesToItemStacks(List<MaterialQuantity> materialQuantities, int multiplier) {
        ObjectList<ItemStack> itemStacks = new ObjectArrayList<>();
        for (MaterialQuantity outputMaterial : materialQuantities) {
            String itemId = outputMaterial.getItemId();
            if (itemId == null) {
                return null;
            }
            int materialQuantity = outputMaterial.getQuantity() <= 0 ? 1 : outputMaterial.getQuantity();
            itemStacks.add(
                new ItemStack(itemId, materialQuantity * multiplier, outputMaterial.getMetadata())
            );
        }
        return itemStacks;
    }

    /**
     * Get the outputs of a given recipe.
     *
     * @param recipe   The recipe to get the outputs from.
     * @param quantity The number of times the recipe is being made.
     * @return A list of {@link ItemStack}s representing the outputs of the crafting recipe.
     */
    public static List<ItemStack> getOutputItemStacks(CraftingRecipe recipe, int quantity) {
        MaterialQuantity[] output = recipe.getOutputs();
        if (output == null) return List.of();
        else return convMaterialQuantitiesToItemStacks(Arrays.stream(output).toList(), quantity);
    }

    /**
     * Give the outputs of a given recipe to an entity.
     *
     * @param ref               The reference to the entity.
     * @param componentAccessor The component accessor for the entity.
     * @param craftingRecipe    The recipe to get the outputs from.
     * @param quantity          The number of multiples of the output resources to give.
     */
    public static void giveOutput(@Nonnull Ref<EntityStore> ref,
                                  @Nonnull ComponentAccessor<EntityStore> componentAccessor,
                                  @Nonnull CraftingRecipe craftingRecipe,
                                  int quantity) {
        assert NPCEntity.getComponentType() != null;
        NPCEntity npcEntity = componentAccessor.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity != null) {
            List<ItemStack> itemStacks = getOutputItemStacks(craftingRecipe, quantity);
            if (itemStacks == null) {
                return;
            }
            for (ItemStack itemStack : itemStacks) {
                if (!ItemStack.isEmpty(itemStack)) {
                    SimpleItemContainer.addOrDropItemStack(componentAccessor, ref,
                        InventoryComponent.getCombined(ref.getStore(), ref), itemStack);
                }
            }

        }
    }

    /**
     * Have an entity craft an item, consuming the input items and granting them the output items.
     *
     * @param ref               The reference to the entity.
     * @param componentAccessor The component accessor for the entity.
     * @param recipe            The recipe to craft.
     * @param quantity          The number of times to craft the recipe.
     * @param itemContainer     The item container to pull input materials from.
     * @return A <code>boolean</code> indicating whether the craft was successful.
     */
    public static boolean craftItem(@Nonnull Ref<EntityStore> ref,
                                    @Nonnull ComponentAccessor<EntityStore> componentAccessor,
                                    @Nonnull CraftingRecipe recipe,
                                    int quantity,
                                    @Nonnull ItemContainer itemContainer) {
        CraftRecipeEvent.Pre preEvent = new CraftRecipeEvent.Pre(recipe, quantity);
        componentAccessor.invoke(ref, preEvent);
        if (preEvent.isCancelled()) {
            return false;
        } else {
            if (!removeInputFromInventory(itemContainer, recipe, quantity)) {
                return false;
            }

            CraftRecipeEvent.Post postEvent = new CraftRecipeEvent.Post(recipe, quantity);
            componentAccessor.invoke(ref, postEvent);
            if (!postEvent.isCancelled()) {
                giveOutput(ref, componentAccessor, recipe, quantity);
            }
            return true;
        }
    }

    /**
     * Have an NPC craft an item, consuming the input items and granting them the output items.
     *
     * @param npcRef   The reference to the NPC.
     * @param blockRef The reference to the block entity of the crafting bench.
     * @param recipe   The recipe to craft.
     * @param quantity The number of times to craft the recipe.
     * @return A <code>boolean</code> indicating whether the craft was successful.
     */
    public static boolean craftItem(@Nonnull Ref<EntityStore> npcRef,
                                    @Nonnull Ref<ChunkStore> blockRef,
                                    @Nonnull CraftingRecipe recipe,
                                    int quantity) {
        CombinedItemContainer combinedSurroundingResources = getCombinedSurroundingResources(npcRef, blockRef);
        if (combinedSurroundingResources == null) return false;
        Store<EntityStore> npcStore = npcRef.getStore();
        return craftItem(npcRef, npcStore, recipe, quantity, combinedSurroundingResources);
    }

    @Nonnull
    public static CombinedItemContainer getCombinedSurroundingResources(Ref<EntityStore> npcRef,
                                                                        Ref<ChunkStore> blockRef) {
        Store<EntityStore> npcStore = npcRef.getStore();
        NPCEntity npc = TaskHelper.getNPCEntity(npcRef, npcStore);
        CombinedItemContainer npcInventory = InventoryComponent.getCombined(npcStore, npcRef);

        Vector3i loc;
        // Try to use the current job location, but if this fails, just search around the NPC for containers.
        JobComponent jobComponent = npcStore.getComponent(npcRef, JobComponent.getComponentType());
        if (jobComponent != null) {
            Job job = jobComponent.getCurrentJob();
            assert job != null;
            loc = job.getLocation();
        } else {
            loc = BlockUtils.getCorrectlyRoundedLocation(npc.getOldPosition());
        }
        if (loc == null) return npcInventory;

        World world = npc.getWorld();
        assert world != null;
        BlockType blockType = world.getBlockType(loc);
        if (blockType == null) return npcInventory;

        Store<ChunkStore> blockStore = blockRef.getStore();
        BenchBlock benchBlock = blockStore.getComponent(
            blockRef, BenchBlock.getComponentType());

        MaterialExtraResourcesSection extraResourcesSection = new MaterialExtraResourcesSection();
        CraftingManager.feedExtraResourcesSection(world,
            loc.x, loc.y, loc.z, blockType,
            world.getBlockRotationIndex(loc.x, loc.y, loc.z),
            blockType.getBench(), benchBlock != null ? benchBlock.getTierLevel() : 1, extraResourcesSection);

        return new CombinedItemContainer(npcInventory, extraResourcesSection.getItemContainer());
    }
}
