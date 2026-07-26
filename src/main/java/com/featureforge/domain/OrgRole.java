package com.featureforge.domain;

public enum OrgRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean atLeast(OrgRole required) {
        return this.ordinal() <= required.ordinal();
    }
}
