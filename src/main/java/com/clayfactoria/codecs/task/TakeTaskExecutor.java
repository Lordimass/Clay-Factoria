package com.clayfactoria.codecs.task;

import com.clayfactoria.codecs.Task;
import com.clayfactoria.components.JobComponent;
import com.clayfactoria.utils.ContainerSlot;
import com.clayfactoria.utils.TaskHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;
import java.util.Objects;

import static com.clayfactoria.utils.TaskHelper.getItemContainerForCurrentJob;

public class TakeTaskExecutor extends PointTaskExecutor {

    @Override
    public boolean canPerformTask(Ref<EntityStore> entityRef) {
        ItemContainer itemContainer = TaskHelper.getItemContainerForCurrentJob(entityRef, ContainerSlot.Output);
        Store<EntityStore> store = entityRef.getStore();
        ItemStack heldItemStack = InventoryComponent.getItemInHand(store, entityRef);

        // There must be items available to be taken, and there must be space in hands
        //FIXME: should check if the filter item is present anywhere in the container
        return heldItemStack == null && !itemContainer.isEmpty();
    }

    @Override
    public boolean execute(Ref<EntityStore> entityRef) {
        Store<EntityStore> store = entityRef.getStore();
        NPCEntity npcEntity = store.getComponent(entityRef, Objects.requireNonNull(NPCEntity.getComponentType()));
        ItemContainer itemContainer = getItemContainerForCurrentJob(entityRef, ContainerSlot.Output);
        JobComponent jobComponent = store.getComponent(entityRef, JobComponent.getComponentType());
        assert npcEntity != null;
        assert jobComponent != null;
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
