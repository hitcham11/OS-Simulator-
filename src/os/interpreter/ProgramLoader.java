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

        //MEMORY SIZE CALCULATION
        // no. of instructions fl the program
        int codeSize = rawLines.size();

        //total required memory
        //instructions+3 variables+4 PCB fields
        int size = codeSize + 3 + 4;

        Memory memory = Memory.getInstance();

        //MEMORY ALLOC
        // Allocate block of memory gambaha
        int lower = memory.allocate(size);

        if (lower == -1) {
            System.out.println("Memory Full -> cannot load P" + processID);
            return null;
        }

        // calc upper memory bound
        int upper = lower + size - 1;

        // Burst time = num of instructions (bta3 el scheduler)
        int burstTime = codeSize;

        //PROCESS CREATION
        Process p = new Process(
                processID,
                arrivalTime,
                lower,
                upper,
                burstTime,
                codeSize
        );

        //LOAD INSTRUCTIONS
        // Write each instruction gowa el allocated memory space
        for (int i = 0; i < codeSize; i++) {
            memory.writeCodeLine(
                    processID,
                    i,
                    rawLines.get(i),
                    lower,
                    upper
            );
        }

        //INITIALIZE PCB
        //Store process control block fl memory
        memory.writePCB(
                processID,
                "READY",   // initial state
                0,         // byebtedy 3and el 0
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
