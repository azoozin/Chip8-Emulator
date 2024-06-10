package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
        char opcode = (char)((memory[programCounter] << 8) | memory[programCounter + 1]);
        System.out.print(Integer.toHexString(opcode) + ": ");
        //decodes the operation code
        switch(opcode & 0xF000) {

            case 0x0000: // Multi case
                switch(opcode & 0x00FF) {
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
                if(V[x] == nn) {
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

                switch(opcode & 0x000F) {

                    case 0x0000: //8XY0: Sets VX to the value of VY.
                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                }

                break;

            case 0xA000: //ANNN: Set I to NNN
                I = (char)(opcode & 0x0FFF);
                programCounter += 2;
                break;

            case 0xD000: {//DXYN: Draw a sprite (X, Y) size (8, N). Sprite is located at I
                int x = V[(opcode & 0x0F00) >> 8];
                int y = V[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;
                V[0xF] = 0;

                for(int _y = 0; _y < height; _y++) {
                    int line = memory[I + _y];
                    for(int _x = 0; _x < 8; _x++) {
                        int pixel = line & (0x80 >> _x);
                        if(pixel != 0) {
                            int totalX = x + _x;
                            int totalY = y + _y;
                            int index = totalY * 64 + totalX;

                            if(display[index] == 1) {
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

            case 0xF000:
                switch(opcode & 0x00FF) {

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

                        int hundred = (value - (value % 100)) / 100; // value should be 1
                        value -= hundred * 100; // subtract hundreds to simplify calculation
                        int ten = (value - (value % 10)) / 10;
                        value -= ten * 10; // subtract tens to simplify calculation
                        int one = 6;
                        memory[I] = (char)hundred;
                        memory[I + 1] = (char)ten;
                        memory[I + 2] = (char)one;

                        programCounter += 2;
                        break;
                    }

                    case 0x065: {
                        int x = (opcode & 0x0F00) >> 8;
                        for(int i = 0; i < x; i++) {
                            V[i] = memory[I + 1];
                        }

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

    public void  loadProgram(String file) {
        DataInputStream input = null;
        try {
             input = new DataInputStream(new FileInputStream(new File(file)));

            int offset = 0;
            while (input.available() > 0) {
                memory[0x200 + offset] = (char)(input.readByte() & 0xFF);
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
        for(int i = 0; i < ChipData.fontset.length; i++) {
            memory[0x50 + i] = (char)(ChipData.fontset[i] & 0xFF);
        }
    }
}
