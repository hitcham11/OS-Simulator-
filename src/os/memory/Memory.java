package os.memory;

import os.process.Process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Memory {

    public static final int TOTAL_WORDS = 40;

    private final MemoryWord[] words = new MemoryWord[TOTAL_WORDS];

    private static Memory instance;

    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }
        return instance;
    }

    private Memory() {
        for (int i = 0; i < TOTAL_WORDS; i++) {
            words[i] = new MemoryWord();
        }
    }

    private boolean isValid(int index) {
        return index >= 0 && index < TOTAL_WORDS;
    }
//memory allocation
    public int allocate(int size) {
        for (int i = 0; i <= TOTAL_WORDS - size; i++) {
            boolean fits = true;

            for (int j = i; j < i + size; j++) {
                if (!words[j].isFree()) {
                    fits = false;
                    break;
                }
            }

            if (fits) {
                return i;
            }
        }

        return -1;
    }

    public boolean canAllocateIfFreed(int size, int lower, int upper) {
        for (int i = 0; i <= TOTAL_WORDS - size; i++) {
            boolean fits = true;

            for (int j = i; j < i + size; j++) {
                boolean freedByCandidate = j >= lower && j <= upper;
                if (!freedByCandidate && !words[j].isFree()) {
                    fits = false;
                    break;
                }
            }

            if (fits) {
                return true;
            }
        }

        return false;
    }

    public void free(int processID, int lower, int upper) {
        for (int i = lower; i <= upper; i++) {
            if (isValid(i)) {
                words[i].clear();
            }
        }

        System.out.println("[MEMORY] Freed memory for P" + processID);
    }

    public String fetchInstruction(int pc, int lower, int upper, int codeSize) {
        int index = lower + pc;

        if (index >= lower && index < lower + codeSize && index <= upper && isValid(index)) {
            if (!words[index].isFree() && words[index].getKey() != null && words[index].getKey().startsWith("code_")) {
                return words[index].getValue();
            }
        }

        return null;
    }

    public void writeCodeLine(int processID, int lineIndex, String line, int lower, int upper) {
        int index = lower + lineIndex;

        if (index <= upper && isValid(index)) {
            words[index].setKey("code_" + lineIndex);
            words[index].setValue(line);
        }
    }

    public void writePCB(int processID, String state, int pc, int lower, int upper, int codeSize) {
        int pcbStart = lower + codeSize + 3;

        if (isValid(pcbStart + 3)) {
            words[pcbStart].setKey("pcb_id");
            words[pcbStart].setValue(String.valueOf(processID));

            words[pcbStart + 1].setKey("pcb_state");
            words[pcbStart + 1].setValue(state);

            words[pcbStart + 2].setKey("pcb_pc");
            words[pcbStart + 2].setValue(String.valueOf(pc));

            words[pcbStart + 3].setKey("pcb_bounds");
            words[pcbStart + 3].setValue(lower + "-" + upper);
        }
    }

    public void writeVariable(int processID, String varName, String value, int lower, int codeSize) {
        int start = lower + codeSize;
        int end = lower + codeSize + 2;
        String key = "var_" + varName;

        for (int i = start; i <= end; i++) {
            if (isValid(i) && (words[i].isFree() || key.equals(words[i].getKey()))) {
                words[i].setKey(key);
                words[i].setValue(value);
                return;
            }
        }

        System.out.println("[MEMORY] No free variable slot for " + varName + " in P" + processID);
    }

    public String readVariable(int processID, String varName, int lower, int codeSize) {
        String key = "var_" + varName;

        for (int i = lower + codeSize; i <= lower + codeSize + 2; i++) {
            if (isValid(i) && key.equals(words[i].getKey())) {
                return words[i].getValue();
            }
        }

        return null;
    }

    public void syncPCB(Process process) {
        if (!process.pcb.isInMemory()) {
            return;
        }

        int pcbStart = process.pcb.memoryLowerBound + process.codeSize + 3;

        if (isValid(pcbStart + 3)) {
            words[pcbStart].setKey("pcb_id");
            words[pcbStart].setValue(String.valueOf(process.pcb.processID));

            words[pcbStart + 1].setKey("pcb_state");
            words[pcbStart + 1].setValue(process.pcb.state);

            words[pcbStart + 2].setKey("pcb_pc");
            words[pcbStart + 2].setValue(String.valueOf(process.pcb.programCounter));

            words[pcbStart + 3].setKey("pcb_bounds");
            words[pcbStart + 3].setValue(process.pcb.memoryLowerBound + "-" + process.pcb.memoryUpperBound);
        }
    }

    public void swapToDisk(int processID, int lower, int upper) {
        File file = getSwapFile(processID);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("# Process " + processID + " disk image");

            for (int i = lower; i <= upper; i++) {
                if (isValid(i) && !words[i].isFree()) {
                    writer.println((i - lower) + ":" + words[i].getKey() + "=" + words[i].getValue());
                }

                if (isValid(i)) {
                    words[i].clear();
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to swap P" + processID + " to disk.", exception);
        }
    }

    public int swapFromDisk(int processID, int size) {
        File file = getSwapFile(processID);

        if (!file.exists()) {
            return -1;
        }

        int lower = allocate(size);
        if (lower == -1) {
            return -1;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] outer = line.split(":", 2);
                int offset = Integer.parseInt(outer[0]);
                String[] inner = outer[1].split("=", 2);

                int index = lower + offset;
                if (isValid(index)) {
                    words[index].setKey(inner[0]);
                    words[index].setValue(inner.length > 1 ? inner[1] : "");
                }
            }

        } catch (IOException exception) {
            throw new RuntimeException("Failed to restore P" + processID + " from disk.", exception);
        }

        if (!file.delete()) {
            System.out.println("[DISK] Warning: could not delete old swap file for P" + processID);
        }

        return lower;
    }

    public void printDiskImage(int processID) {
        File file = getSwapFile(processID);

        if (!file.exists()) {
            return;
        }

        System.out.println("[DISK] process_" + processID + "_disk.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("       " + line);
            }
        } catch (IOException exception) {
            System.out.println("[DISK] Failed to read disk image for P" + processID);
        }
    }

    public void printMemory() {
        System.out.println("\n------ MEMORY ------");
        for (int i = 0; i < TOTAL_WORDS; i++) {
            System.out.println(i + " -> " + words[i]);
        }
    }

    public void reset() {
        for (MemoryWord word : words) {
            word.clear();
        }

        File currentDirectory = new File(".");
        File[] swapFiles = currentDirectory.listFiles((dir, name) ->
                name.startsWith("process_") && name.endsWith("_disk.txt"));

        if (swapFiles != null) {
            for (File swapFile : swapFiles) {
                if (!swapFile.delete()) {
                    System.out.println("[DISK] Warning: could not delete " + swapFile.getName());
                }
            }
        }
    }

    private File getSwapFile(int processID) {
        return new File("process_" + processID + "_disk.txt");
    }
}
