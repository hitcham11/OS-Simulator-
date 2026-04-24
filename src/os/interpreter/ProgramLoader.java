package os.interpreter;

import os.memory.Memory;
import os.process.Process;

import java.io.*;
import java.util.*;

public class ProgramLoader {

    public static int estimateProcessSize(String filename) {
        List<String> rawLines = readProgramLines(filename);
        if (rawLines == null) {
            return -1;
        }
        return rawLines.size() + 3 + 4;
    }

    public static Process load(String filename, int processID, int arrivalTime) {
        List<String> rawLines = readProgramLines(filename);
        if (rawLines == null) {
            return null;
        }

        // ================= MEMORY SIZE CALCULATION =================
        // Number of instructions in the program
        int codeSize = rawLines.size();

        // Total required memory:
        // [instructions] + [3 variables] + [4 PCB fields]
        int size = codeSize + 3 + 4;

        Memory memory = Memory.getInstance();

        // ================= MEMORY ALLOCATION =================
        // Allocate a contiguous block of memory
        int lower = memory.allocate(size);

        // Do NOT handle swapping here → responsibility of Main/Scheduler
        if (lower == -1) {
            System.out.println("Memory Full → cannot load P" + processID);
            return null;
        }

        // Calculate upper memory bound
        int upper = lower + size - 1;

        // Burst time equals number of instructions (used by scheduler)
        int burstTime = codeSize;

        // ================= PROCESS CREATION =================
        Process p = new Process(
                processID,
                arrivalTime,
                lower,
                upper,
                burstTime,
                codeSize
        );

        // ================= LOAD INSTRUCTIONS =================
        // Write each instruction into allocated memory space
        for (int i = 0; i < codeSize; i++) {
            memory.writeCodeLine(
                    processID,
                    i,
                    rawLines.get(i),
                    lower,
                    upper
            );
        }

        // ================= INITIALIZE PCB =================
        // Store process control block in memory
        memory.writePCB(
                processID,
                "READY",   // initial state
                0,         // program counter starts at 0
                lower,
                upper,
                codeSize
        );

        // Return the fully loaded process
        return p;
    }

    private static List<String> readProgramLines(String filename) {
        List<String> rawLines = new ArrayList<String>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    rawLines.add(line.trim());
                }
            }
            return rawLines;
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not find " + filename);
            return null;
        }
    }
}
