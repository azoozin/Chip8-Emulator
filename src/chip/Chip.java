package chip;

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
    }

    public void run() {
        // fetching operation code
        // << 8 shifts an entire byte to the left
        char opcode = (char) ((char)(memory[programCounter] << 8) | memory[programCounter + 1]);
        System.out.println(Integer.toHexString(opcode));

        // decoding opcodes
        // get value o first bitwise nibble of opcode
        switch(opcode & 0xF000) {

            case 0x8000:
                // ge value last nibble
                switch(opcode & 0x000F) {
                    case 0x000:
                    default:
                        System.err.println("Operation Code not supported.");
                        System.exit(0);
                        break;
                }
                break;
            default:
                System.err.println("Operation Code not supported.");
                System.exit(0);
        }
    //        execute opcode (during/inside decoding func?)
    }

    public byte[] getDisplay() {
        return this.display;
    }
}
