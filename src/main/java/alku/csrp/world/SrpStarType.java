package alku.csrp.world;

public enum SrpStarType {
    NORMAL(0, "normal"),
    COLD(1, "cold"),
    WARM(2, "warm");

    private final int value;
    private final String id;

    SrpStarType(int value, String id) {
        this.value = value;
        this.id = id;
    }

    public int value() {
        return value;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "options.csrp.star_type." + id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    public static SrpStarType byId(String id) {
        for (SrpStarType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return NORMAL;
    }

    public static SrpStarType byValue(int value) {
        for (SrpStarType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return NORMAL;
    }
}
