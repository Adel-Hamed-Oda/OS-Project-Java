package memory;

public class MemoryWord {
    public String name;
    public String value;

    public MemoryWord() {
        this.name = "";
        this.value = "";
    }

    public MemoryWord(String name, String value) {
        this.name = name;
        this.value = value;
    }

    // removed the getters and setters since this is spaghetti code and we access fields directly

    // clear should make it "" not have "Empty" inside, what if we want to use "Empty" as a value?
    public void clear() {
        this.name = "";
        this.value = "";
    }

    // 7elw 3ashan el testing
    @Override
    public String toString() {
        return "[" + name + " : " + value + "]";
    }
}
