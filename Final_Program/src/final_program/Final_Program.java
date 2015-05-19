/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.glu.GLU;

/**
 * test
 *
 */
public class Final_Program {

    private FPCameraController fp;
    private DisplayMode displayMode;

    public void start() {
        try {
            createWindow();
            initGL();
            fp = new FPCameraController(0f, 0f, 0f);
            fp.gameLoop(); //render();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createWindow() throws Exception {
        Display.setFullscreen(false);
        DisplayMode d[] = Display.getAvailableDisplayModes();
        for (int i = 0; i < d.length; i++) {
            if (d[i].getWidth() == 640
                    && d[i].getHeight() == 480
                    && d[i].getBitsPerPixel() == 32) {
                displayMode = d[i];
                break;
            }
        }
        Display.setDisplayMode(displayMode);
        Display.setTitle("Final Program - Checkpoint 1");
        Display.create();
    }

    private void initGL() {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);
        
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);
        glEnable(GL_DEPTH_TEST);
        
        glEnable(GL_TEXTURE_2D);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GLU.gluPerspective(100.0f, (float) displayMode.getWidth()
                / (float) displayMode.getHeight(), 0.1f, 300.0f);
        glMatrixMode(GL_MODELVIEW);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Final_Program final_Program = new Final_Program();
        final_Program.start();
    }
}
