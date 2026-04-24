package os.syscall;

import os.memory.Memory;
import os.process.Process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.Scanner;

public class SystemCalls {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Queue<String> scriptedInputs = new ArrayDeque<>();

    public static String readFile(String filename) {
        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException exception) {
            System.out.println("[ERROR] File not found: " + filename);
            return "";
        }

        return builder.toString().trim();
    }

    public static void writeFile(String filename, String data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(data);
            System.out.println("[FILE] Wrote data to " + filename);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to write file " + filename, exception);
        }
    }

    public static void print(String data) {
        System.out.println("[OUTPUT] " + data);
    }

    public static String takeInput() {
        System.out.print("Please enter a value: ");

        if (!scriptedInputs.isEmpty()) {
            String value = scriptedInputs.poll();
            System.out.println(value);
            return value;
        }

        if (!scanner.hasNextLine()) {
            System.out.println();
            System.out.println("[INPUT] No interactive input detected. Using empty string.");
            return "";
        }

        return scanner.nextLine().trim();
    }

    public static String readFromMemory(Process process, String variableName) {
        if (!process.pcb.isInMemory()) {
            return "";
        }

        return Memory.getInstance().readVariable(
                process.pcb.processID,
                variableName,
                process.pcb.memoryLowerBound,
                process.codeSize
        );
    }

    public static void writeToMemory(Process process, String variableName, String value) {
        if (!process.pcb.isInMemory()) {
            return;
        }

        Memory.getInstance().writeVariable(
                process.pcb.processID,
                variableName,
                value,
                process.pcb.memoryLowerBound,
                process.codeSize
        );
    }

    public static void setScriptedInputs(Collection<String> inputs) {
        scriptedInputs.clear();
        if (inputs != null) {
            scriptedInputs.addAll(inputs);
        }
    }

    public static void clearScriptedInputs() {
        scriptedInputs.clear();
    }
}
