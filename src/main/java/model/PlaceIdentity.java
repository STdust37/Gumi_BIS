package model;

public class PlaceIdentity {
    private final String id;
    private final String name;

    public PlaceIdentity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
