package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class Chip {

    private char[] memory;
    // registers
    private char[] V;
    // address pointer, 16bit wide, 12bit used?
    private char I;
    // memory pointer
    // initial point every program will start
    private char programCounter;

    private char stack[];
    private int stackPointer;

    private int delayTimer;
    private int soundTimer;

    private byte[] keys;

    private byte[] display;

    private boolean needRedraw;

    // initiate
    public void init() {
        // kB = 1024, kB * 4 = 4096 == 4kB
        memory = new char[4096];
        V = new char[16];
        I = 0x0;
        // program loaded in at position hexadecimal 200 slot decimal 512
        programCounter = 0x200;

        stack = new char[16];
        stackPointer = 0;

        delayTimer = 0;
        soundTimer = 0;

        keys = new byte[16];

        // display resolution 64x32
        // scale up 10x to 640x320
        display = new byte[64 * 32];

        needRedraw = false;

        loadFontset();
    }

    public void run() {
        //fetches opcode
        char opcode = (char) ((memory[programCounter] << 8) | memory[programCounter + 1]);
        System.out.print(Integer.toHexString(opcode) + ": ");
        //decodes the operation code
        switch (opcode & 0xF000) {

            case 0x0000: // Multi case
                switch (opcode & 0x00FF) {
                    case 0x00E0:
                        System.err.println("Unsupported operation code.");
                        System.exit(0);
                        break;

                    case 0x00EE:
//                        System.err.println("Unsupported operation code.");
//                        System.exit(0);
                        stackPointer--;
                        programCounter = (char)(stack[stackPointer] + 2);
                        break;

                    default:
                        System.err.println("Unsupported operation code.");
                        System.exit(0);
                        break;
                }
                break;

            case 0x1000: {//1NNN: Jumps to address NNN
                int nnn = opcode & 0x0FFF;
                programCounter = (char)nnn;
                break;
            }
            case 0x2000: //2NNN: Calls subroutine at NNN
                stack[stackPointer] = programCounter;
                stackPointer++;
                programCounter = (char)(opcode & 0x0FFF);
                break;

            case 0x3000: {//3XNN: Skips the next instruction if VX equals NN
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                if (V[x] == nn) {
                    programCounter += 4;
                } else {
                    programCounter += 2;
                }
                break;
            }

            case 0x4000: {
                int x = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;
                if (V[x] != nn) {
                    programCounter += 4;
                } else {
                    programCounter += 2;
                }
                break;
            }

            case 0x6000: {//6XNN: Set VX to NN
                int x = (opcode & 0x0F00) >> 8;
                V[x] = (char) (opcode & 0x00FF);
                programCounter += 2;
                break;
            }

            case 0x7000: {//7XNN: Adds NN to VX
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                V[x] = (char) ((V[x] + nn) & 0xFF);
                programCounter += 2;
                break;
            }

            case 0x8000: //Contains more data in last nibble

                switch (opcode & 0x000F) {

                    case 0x0000: {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = V[y];
                        programCounter += 2;
                        break;
                    }

                    case 0x0002: {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = (char)(V[x] & V[y]);
                        programCounter += 2;
                        break;
                    }

                    case 0x0004: {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        if (V[y] > 0xFF - V[x]) {
                            V[0xF] = 1;
                        } else {
                            V[0xF] = 0;
                        }
                        V[x] = (char) ((V[x] + V[y]) & 0xFF);
                        programCounter += 2;
                        break;
                    }

                    case 0x0005: {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        if(V[x] > V[y]) {
                            V[0xF] = 1;
                        } else {
                            V[0xF] = 0;
                        }
                        V[x] = (char)((V[x] - V[y]) & 0xFF);
                        programCounter += 2;
                        break;
                    }
//                    case 0x0000:
                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                }
                break;

            case 0xA000:
                I = (char) (opcode & 0x0FFF);
                programCounter += 2;
                break;

            case 0xC000: {
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                int randomNum = new Random().nextInt(255) & nn;
                V[x] = (char) randomNum;
                programCounter += 2;
                break;
            }

            case 0xD000: {
                int x = V[(opcode & 0x0F00) >> 8];
                int y = V[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;
                V[0xF] = 0;

                for (int _y = 0; _y < height; _y++) {
                    int line = memory[I + _y];
                    for (int _x = 0; _x < 8; _x++) {
                        int pixel = line & (0x80 >> _x);
                        if (pixel != 0) {
                            int totalX = x + _x;
                            int totalY = y + _y;
                            int index = totalY * 64 + totalX;

                            if (display[index] == 1) {
                                V[0xF] = 1;
                            }

                            display[index] ^= 1; // ^= is XOR
                        }
                    }
                }
                programCounter += 2;

                needRedraw = true;

                break;
            }

            case 0xE000: {
                switch (opcode & 0x00FF) {
                    case 0x009E: {
                        int x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if (keys[key] == 1) {
                            programCounter += 4;
                        } else {
                            programCounter += 2;
                        }
                        break;
                    }

                    case 0x00A1: {
                        int x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if (keys[key] == 0) {
                            programCounter += 4;
                        } else {
                            programCounter += 2;
                        }
                        break;
                    }
                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                }
                break;
            }

            case 0xF000:
                switch (opcode & 0x00FF) {

                    case 0x0007: {
                        int x = (opcode & 0x0F00) >> 8;
                        V[x] = (char) delayTimer;
                        programCounter += 2;
//                        break;
                    }

                    case 0x0015: {
                        int x = (opcode & 0x0F00) >> 8;
                        delayTimer = V[x];
                        programCounter += 2;
//                        break;
                    }

                    case 0x0018: {
                        int x = (opcode & 0x0F00) >> 8;
                        soundTimer = V[x];
                        programCounter += 2;
                        break;
                    }

                    case 0x0029: {
                        int x = (opcode & 0x0F00) >> 8;
                        int character = V[x];
                        I = (char)(0x050 + (character * 5));

                        programCounter += 2;
                        break;
                    }

                    case 0x0033: {
                        int x = (opcode & 0x0F00) >> 8;
                        int value = V[x];
                        int hundreds = (value - (value % 100)) / 100;
                        value -= hundreds * 100;
                        int tens = (value - (value % 10))/ 10;
                        value -= tens * 10;
                        memory[I] = (char)hundreds;
                        memory[I + 1] = (char)tens;
                        memory[I + 2] = (char)value;
                        System.out.println("Storing Binary-Coded Decimal V[" + x + "] = " + (int)(V[(opcode & 0x0F00) >> 8]) + " as { " + hundreds+ ", " + tens + ", " + value + "}");
                        programCounter += 2;
                        break;
//                        int x = (opcode & 0x0F00) >> 8;
//                        int value = V[x];
//
//                        int hundred = (value - (value % 100)) / 100; // value should be 1
//                        value -= hundred * 100; // subtract hundreds to simplify calculation
//                        int ten = (value - (value % 10)) / 10;
//                        value -= ten * 10; // subtract tens to simplify calculation
//                        int one = 6;
//                        memory[I] = (char) hundred;
//                        memory[I + 1] = (char) ten;
//                        memory[I + 2] = (char) one;
//
//                        programCounter += 2;
//                        break;
                    }

                    case 0x065: {
                        int x = (opcode & 0x0F00) >> 8;
                        for (int i = 0; i < x; i++) {
                            V[i] = memory[I + 1];
                        }
                        I = (char) (I + x + 1);
                        programCounter += 2;
                        break;
                    }

                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                }
                break;

            default:
                System.err.println("Unsupported Opcode!");
                System.exit(0);
        }

        if(soundTimer > 0) {
            soundTimer--;
        }
        if(delayTimer > 0) {
            delayTimer--;
        }
    }

    public byte[] getDisplay() {
        return display;
    }

    public boolean needsRedraw() {
        return needRedraw;
    }

    public void removeDrawFlag() {
        needRedraw = false;
    }

    public void loadProgram(String file) {
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(new File(file)));

            int offset = 0;
            while (input.available() > 0) {
                memory[0x200 + offset] = (char) (input.readByte() & 0xFF);
                offset++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {

                }
            }
        }
    }

    public void loadFontset() {
        for (int i = 0; i < ChipData.fontset.length; i++) {
            memory[0x50 + i] = (char) (ChipData.fontset[i] & 0xFF);
        }
    }

    public void setKeyBuffer(int[] keyBuffer) {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = (byte) keyBuffer[i];
        }
    }
}
