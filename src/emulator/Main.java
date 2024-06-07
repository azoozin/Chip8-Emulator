package emulator;

import chip.Chip;

public class Main extends Thread{

    private Chip chip8;
    private ChipFrame frame;

    public Main() {
        this.chip8 = new Chip();
        chip8.init();
        chip8.loadProgram("pong2.c8");
        this.frame = new ChipFrame(chip8);
    }

    // basic loop of thread
    // chip-8 runs at 60 fps/60 hZ
    public void run() {
        while (true) {
            chip8.run();
            if (chip8.needsRedraw()) {
                frame.repaint();
                chip8.removeDrawFlag();
            }
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {

            }
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }
}