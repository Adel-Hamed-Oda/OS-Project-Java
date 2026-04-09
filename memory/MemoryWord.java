package memory;

public class MemoryWord {
    private String name;
    private String value;

    public MemoryWord(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public void clear() {
        this.name = "Empty";
        this.value = "Empty";
    }

    @Override
    public String toString() {
        return "[" + name + " : " + value + "]";
    }
}
