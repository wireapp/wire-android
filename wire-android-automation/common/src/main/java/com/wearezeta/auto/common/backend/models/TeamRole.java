package com.wearezeta.auto.common.backend.models;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum TeamRole {
    OWNER(8191), ADMIN(5951), MEMBER(1587), INVALID(1234), PARTNER(1025);

    private final int permissionBitMask;

    TeamRole(int permissionBitMask) {
        this.permissionBitMask = permissionBitMask;
    }

    public static TeamRole getByPermissionBitMask(int permissionBitMask) {
        return Arrays.stream(TeamRole.values())
                .filter(x -> x.getPermissionBitMask() == permissionBitMask)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("Permission bit mask '%s' is unknown",
                        permissionBitMask)));
    }

    public static TeamRole getByName(String roleName) {
        return Arrays.stream(TeamRole.values())
                .filter(x -> x.name().equalsIgnoreCase(roleName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("Team role '%s' is unknown", roleName)));
    }

    public int getPermissionBitMask() {
        return permissionBitMask;
    }

    @Override
    public String toString() {
        return name();
    }
}