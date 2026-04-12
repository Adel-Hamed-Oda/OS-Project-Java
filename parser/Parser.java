package parser;

import java.util.HashMap;
import java.util.Map;
import mutex.MutexManager;
import os_system.SystemCalls;
import scheduler.Scheduler;

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
        commandMap.put("printToFrom", this::printToFrom);
    }

    private void semWait(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: semWait <mutexName>");
        }

        switch (args[1]) {
            case "input" -> {
                if (!MutexManager.waitinput(Scheduler.getCurrentProcessID())) {
                    Scheduler.blockProcessOnInput();
                }
            }
            case "output" -> {
                if (!MutexManager.waitoutput(Scheduler.getCurrentProcessID())) {
                    Scheduler.blockProcessOnOutput();
                }
            }
            case "memory" -> {
                if (!MutexManager.waitmemory(Scheduler.getCurrentProcessID())) {
                    Scheduler.blockProcessOnMemory();
                }
            }
            default -> throw new IllegalArgumentException("Unknown mutex: " + args[1]);
        }
    }

    private void semSignal(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: semSignal <mutexName>");
        }

        // NOTE: I just realized this logic is very risky as this is technically just a semaphore and not a mutex
        // this is because the process is blocked, but we don't know what blocked it, so it can by change by unblocked
        // by an outside process, think semWait Input followed by semSignal Output. This doesn't happen in the given examples
        // but I think we should see what we can do about it

        switch (args[1]) {
            case "input" -> {
                MutexManager.signalinput();
                Scheduler.unblockProcessOnInput();
            }
            case "output" -> {
                MutexManager.signaloutput();
                Scheduler.unblockProcessOnOutput();
            }
            case "memory" -> {
                MutexManager.signalmemory();
                Scheduler.unblockProcessOnMemory();
            }
            default -> throw new IllegalArgumentException("Unknown mutex: " + args[1]);
        }
    }

    private void print(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: print <message> || print <variable> || print input || print readFile <filePath>");
        }

        if (args[1].startsWith("\"") && args[1].endsWith("\"")) {

            String message = args[1].substring(1, args[1].length() - 1);
            SystemCalls.print(message);

        } else if (args[1].equals("input")) {
            
            String input = SystemCalls.input();

            SystemCalls.print(input);

        } else if (args[1].equals("readFile")) {
            
            if (args.length < 3) {
                throw new IllegalArgumentException("usage: print readFile <filePath>");
            }

            String fileContent = SystemCalls.readFile(args[2]);

            SystemCalls.print(fileContent);

        } else {

            String value = SystemCalls.readFromMemory(args[1]);
            SystemCalls.print(value);

        }
    }

    private void assign(String[] args) throws IllegalArgumentException {
        if (args.length < 3) {
            throw new IllegalArgumentException("usage: assign <variable> <value> || assign <variable> input || assign <variable> readFile <filePath> || assign <variable1> <variable2>");
        }

        if (args[2].startsWith("\"") && args[2].endsWith("\"")) {
            
            args[2] = args[2].substring(1, args[2].length() - 1);

            SystemCalls.writeToMemory(args[1], args[2]);

        } else if (args[2].equals("input")) {
            
            String input = SystemCalls.input();

            SystemCalls.writeToMemory(args[1], input);

        } else if (args[2].equals("readFile")) {
            
            if (args.length < 4) {
                throw new IllegalArgumentException("usage: assign <variable> readFile <filePath>");
            }

            String fileContent = SystemCalls.readFile(args[3]);

            SystemCalls.writeToMemory(args[1], fileContent);

        } else {

            String value = SystemCalls.readFromMemory(args[2]);

            SystemCalls.writeToMemory(args[1], value);

        }
    }

    private void writeFile(String[] args) throws IllegalArgumentException {
        if (args.length < 3) {
            throw new IllegalArgumentException("usage: writeFile <filePath> <content>");
        }

        if (args[2].startsWith("\"") && args[2].endsWith("\"")) {

            args[2] = args[2].substring(1, args[2].length() - 1);
        
            SystemCalls.writeFile(args[1], args[2]);

        } else if (args[2].equals("input")) {

            String input = SystemCalls.input();

            SystemCalls.writeFile(args[1], input);

        } else {
            
            String value = SystemCalls.readFromMemory(args[2]);
            
            SystemCalls.writeFile(args[1], value);

        }
    }

    private void printToFrom(String[] args) throws IllegalArgumentException {
        if (args.length < 3) {
            throw new IllegalArgumentException("usage: printToFrom <variable1> <variable2>");
        }

        SystemCalls.printToFrom(args[1], args[2]);
    }

    public void parse(String input) throws IllegalArgumentException {
        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

        Command command = commandMap.get(commandName);
        if (command != null) {
            command.execute(args);
        } else {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }
    }
}