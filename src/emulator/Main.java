package emulator;

import chip.Chip;

public class Main {

    private Chip chip8;
    private ChipFrame frame;

    public Main() {
        this.chip8 = new Chip();
        chip8.init();
        this.frame = new ChipFrame(chip8);
    }

    public static void main(String[] args) {
    }
}