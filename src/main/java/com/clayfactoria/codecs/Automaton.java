package com.clayfactoria.codecs;

import com.hypixel.hytale.server.npc.role.Role;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public enum Automaton {
    CLAY_TRORK(
        "Clay Trork",
        "Trork_Clay",
        "Clay Trorks can be programmed to carry items from place to place.",
        List.of(Task.TAKE, Task.DEPOSIT, Task.POSITION),
        "If a filter item is set, this automaton will only pick up items which match."
    ),
    CLAY_KWEEBEC(
        "Clay Kweebec",
        "Kweebec_Clay",
        "Clay Kweebecs can be programmed to work at various stations to perform tasks like crafting and enabling furnaces!",
        List.of(Task.WORK, Task.POSITION, Task.DEPOSIT),
        "When crafting, this automaton will only craft the item set on it."),
    CLAY_FERAN(
        "Clay Feran",
        "Feran_Clay",
        "Clay Ferans can be programmed to harvest crops in an area.",
        List.of(Task.HARVEST, Task.DEPOSIT, Task.POSITION),
        null
    );

    public final String name;
    public final String roleName;
    public final String description;
    public final List<Task> tasks;
    public final String filterItemDescription;

    Automaton(String name, String roleName, String description, List<Task> tasks, String filterItemDescription) {
        this.name = name;
        this.roleName = roleName;
        this.description = description;
        this.tasks = tasks;
        this.filterItemDescription = filterItemDescription;
    }

    @Nullable
    public static Automaton getFromRole(@Nullable Role role) {
        if (role == null) {
            return null;
        }
        String roleName = role.getRoleName();
        for (Automaton automaton : Automaton.values()) {
            if (Objects.equals(automaton.roleName, roleName)) {
                return automaton;
            }
        }
        return null;
    }
}
