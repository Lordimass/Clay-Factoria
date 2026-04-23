package com.clayfactoria.codecs.task;

import com.clayfactoria.codecs.Task;
import com.clayfactoria.components.JobComponent;
import com.clayfactoria.utils.BlockUtils;
import com.clayfactoria.utils.ContainerSlot;
import com.clayfactoria.utils.TaskHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;
import java.util.Objects;

import static com.clayfactoria.utils.TaskHelper.*;

public class TakeTaskExecutor extends PointTaskExecutor {

    @Override
    public boolean canPerformTask(Ref<EntityStore> entityRef) {
        ItemContainer itemContainer = TaskHelper.getItemContainerForCurrentJob(entityRef, ContainerSlot.Output);
        Store<EntityStore> store = entityRef.getStore();
        ItemStack heldItemStack = InventoryComponent.getItemInHand(store, entityRef);

        // Open container state
        World world = getNPCEntity(entityRef).getWorld();
        assert world != null;
        Vector3i blockPos = Objects.requireNonNull(JobComponent.getCurrentJob(entityRef)).getLocation();
        assert blockPos != null;
        BlockUtils.setBlockInteractionState("OpenWindow", world, blockPos);

        // There must be items available to be taken, there must be space in hands, and it must contain the filter item
        JobComponent jobComponent = store.getComponent(entityRef, JobComponent.getComponentType());
        String filterItem = jobComponent != null ? jobComponent.getFilterItem() : null;
        return heldItemStack == null && !itemContainer.isEmpty() && checkForAnyFilterItem(itemContainer, filterItem);
    }

    @Override
    public boolean execute(Ref<EntityStore> entityRef) {
        Store<EntityStore> store = entityRef.getStore();
        NPCEntity npcEntity = store.getComponent(entityRef, Objects.requireNonNull(NPCEntity.getComponentType()));
        ItemContainer itemContainer = getItemContainerForCurrentJob(entityRef, ContainerSlot.Output);
        JobComponent jobComponent = store.getComponent(entityRef, JobComponent.getComponentType());
        assert npcEntity != null;
        assert jobComponent != null;

        // Close container state
        assert jobComponent.getCurrentJob() != null;
        Vector3i blockPos = jobComponent.getCurrentJob().getLocation();
        World world = npcEntity.getWorld();
        assert world != null;
        assert blockPos != null;
        BlockUtils.setBlockInteractionState("CloseWindow", world, blockPos);

        ItemContainer npcInventory = TaskHelper.getNPCInventory(npcEntity, store);
        return TaskHelper.transferItem(itemContainer, npcInventory, 1, jobComponent.getFilterItem());
    }

    @Override
    public Task relevantNextTask(List<Task> availableOptions) {
        if (availableOptions.contains(Task.DEPOSIT)) {
            return Task.DEPOSIT;
        } else {
            return Task.WORK;
        }
    }

}
