package memory;

public class MemoryWord {
    
    public enum WordType {
        Free,
        PCB,
        Variable,
        Instruction
    }

    public String name;
    public String value;
    public WordType type; // New type field

    public MemoryWord() {
        this.name = null;
        this.value = null;
        this.type = WordType.Free; // Default to free
    }

    public MemoryWord(String name, String value, WordType type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public void clear() {
        this.name = null;
        this.value = null;
        this.type = WordType.Free;
    }

    @Override
    public String toString() {
        return "[" + type + " | " + name + " : " + value + "]";
    }
}
