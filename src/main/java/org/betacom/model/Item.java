package org.betacom.model;

import java.util.UUID;

public class Item {
    private UUID id;
    private UUID owner; // user id
    private String name;

    // for new Item
    public Item(UUID owner, String name) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.name = name;
    }

    // Getters setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
