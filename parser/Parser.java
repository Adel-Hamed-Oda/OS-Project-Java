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

    private static final Map<String, Command> commandMap = new HashMap<>();

    public static void initParser() {
        commandMap.put("semWait", Parser::semWait);
        commandMap.put("semSignal", Parser::semSignal);
        commandMap.put("print", Parser::print);
        commandMap.put("assign", Parser::assign);
        commandMap.put("writeFile", Parser::writeFile);
        commandMap.put("printFromTo", Parser::printFromTo);
    }

    private static void semWait(String[] args) throws IllegalArgumentException {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: semWait <mutexName>");
        }

        switch (args[0]) {
            case "userInput" -> {
                if (!MutexManager.waitinput(Scheduler.getCurrentProcessID())) {
                    Scheduler.blockProcessOnInput();
                }
            }
            case "userOutput" -> {
                if (!MutexManager.waitoutput(Scheduler.getCurrentProcessID())) {
                    Scheduler.blockProcessOnOutput();
                }
            }
            case "file" -> {
                if (!MutexManager.waitmemory(Scheduler.getCurrentProcessID())) {
                    Scheduler.blockProcessOnMemory();
                }
            }
            default -> throw new IllegalArgumentException("Unknown mutex: " + args[0]);
        }
    }

    private static void semSignal(String[] args) throws IllegalArgumentException {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: semSignal <mutexName>");
        }

        // NOTE: I just realized this logic is very risky as this is technically just a semaphore and not a mutex
        // this is because the process is blocked, but we don't know what blocked it, so it can by change by unblocked
        // by an outside process, think semWait Input followed by semSignal Output. This doesn't happen in the given examples
        // but I think we should see what we can do about it

        switch (args[0]) {
            case "userInput" -> {
                MutexManager.signalinput();
                Scheduler.unblockProcessOnInput();
            }
            case "userOutput" -> {
                MutexManager.signaloutput();
                Scheduler.unblockProcessOnOutput();
            }
            case "file" -> {
                MutexManager.signalmemory();
                Scheduler.unblockProcessOnMemory();
            }
            default -> throw new IllegalArgumentException("Unknown mutex: " + args[0]);
        }
    }

    private static void print(String[] args) throws IllegalArgumentException {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: print <message> || print <variable> || print input || print readFile <filePath>");
        }

        if (args[0].startsWith("\"") && args[0].endsWith("\"")) {

            String message = args[0].substring(1, args[0].length() - 1);
            SystemCalls.print(message);

        } else if (args[0].equals("input")) {
            
            String input = SystemCalls.input();

            SystemCalls.print(input);

        } else if (args[0].equals("readFile")) {
            
            if (args.length < 2) {
                throw new IllegalArgumentException("usage: print readFile <filePath>");
            }

            String fileContent = SystemCalls.readFile(args[1]);

            SystemCalls.print(fileContent);

        } else {

            String value = SystemCalls.readFromMemory(args[0]);
            SystemCalls.print(value);

        }
    }

    private static void assign(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: assign <variable> <value> || assign <variable> input || assign <variable> readFile <filePath> || assign <variable1> <variable2>");
        }

        if (args[1].startsWith("\"") && args[1].endsWith("\"")) {
            
            args[1] = args[1].substring(1, args[1].length() - 1);

            SystemCalls.writeToMemory(args[0], args[1]);

        } else if (args[1].equals("input")) {
            
            String input = SystemCalls.input();

            SystemCalls.writeToMemory(args[0], input);

        } else if (args[1].equals("readFile")) {
            
            if (args.length < 3) {
                throw new IllegalArgumentException("usage: assign <variable> readFile <variable>");
            }

            String fileContent = SystemCalls.readFile(SystemCalls.readFromMemory(args[2]));

            SystemCalls.writeToMemory(args[0], fileContent);

        } else {

            String value = SystemCalls.readFromMemory(args[1]);

            SystemCalls.writeToMemory(args[0], value);

        }
    }

    private static void writeFile(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: writeFile <filePath> <content>");
        }

        if (args[1].startsWith("\"") && args[1].endsWith("\"")) {

            args[1] = args[1].substring(1, args[1].length() - 1);
        
            SystemCalls.writeFile(SystemCalls.readFromMemory(args[0]), args[1]);

        } else if (args[1].equals("input")) {

            String input = SystemCalls.input();

            SystemCalls.writeFile(SystemCalls.readFromMemory(args[0]), input);

        } else {
            
            String value = SystemCalls.readFromMemory(args[1]);
            
            SystemCalls.writeFile(SystemCalls.readFromMemory(args[0]), value);

        }
    }

    private static void printFromTo(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: printFromTo <variable1> <variable2>");
        }

        SystemCalls.printFromTo(args[0], args[1]);
    }

    public static void parse(String input) throws IllegalArgumentException {
        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0];
        String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

        Command command = commandMap.get(commandName);
        if (command != null) {
            command.execute(args);
        } else {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }
    }
}