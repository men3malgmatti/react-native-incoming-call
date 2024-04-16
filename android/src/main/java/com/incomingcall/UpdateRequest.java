package com.incomingcall;

public class UpdateRequest {
    private String name;
    private String uuid;

    public UpdateRequest(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }
}