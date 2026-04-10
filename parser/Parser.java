package parser;

import java.util.HashMap;
import java.util.Map;

public class Parser {

    interface Command {
        void execute(String[] args);
    }

    private final Map<String, Command> commandMap = new HashMap<>();

    public Parser() {
        commandMap.put("semWait", this::semWait);
        commandMap.put("semSignal", this::semSignal);
        commandMap.put("print", this::print);
        commandMap.put("assign", this::assign);
        commandMap.put("writeFile", this::writeFile);
    }

    private void semWait(String[] args) throws IllegalArgumentException {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: semWait <mutexName>");
        }
    }

    private void semSignal(String[] args) throws IllegalArgumentException {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: semSignal <mutexName>");
        }
    }

    private void print(String[] args) throws IllegalArgumentException {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: print <message> || print <variable>");
        }
    }

    private void assign(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: assign <variable> <value>");
        }
    }

    private void writeFile(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: writeFile <filePath> <content>");
        }
    }

    public int getNumberOfLines(String filePath) {
        return -1;
    }

    public void parse(String input) {
        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

        Command command = commandMap.get(commandName);
        if (command != null) {
            command.execute(args);
        } else {
            System.out.println("Unknown command: " + input);
        }
    }
}