/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author Amador
 */
public class Crosshair {

    private final float offset = 0.0005f / 2.0f;
    private float[] coords;

    public Crosshair(float x, float y, float z) {
        coords = new float[]{
            x + offset, y - offset, z - 0.1f,
            x - offset, y - offset, z - 0.1f,
            x - offset, y + offset, z - 0.1f,
            x + offset, y + offset, z - 0.1f
        };
    }
    
    public void render() {
        glPushMatrix();
        glLoadIdentity();
        
        glColor3f(0.75f, 0.75f, 0.75f);
        glBindTexture(GL_TEXTURE_2D, 0);
        
        glBegin(GL_QUADS);
            glVertex3f(coords[0] + 0.0025f, coords[1], coords[2]);
            glVertex3f(coords[3] - 0.0025f, coords[4], coords[5]);
            glVertex3f(coords[6] - 0.0025f, coords[7], coords[8]);
            glVertex3f(coords[9] + 0.0025f, coords[10], coords[11]);
        glEnd();

        glBegin(GL_QUADS);
            glVertex3f(coords[0], coords[1] - 0.0025f, coords[2]);
            glVertex3f(coords[3], coords[4] - 0.0025f, coords[5]);
            glVertex3f(coords[6], coords[7] + 0.0025f, coords[8]);
            glVertex3f(coords[9], coords[10] + 0.0025f, coords[11]);
        glEnd();

        glPopMatrix();
    }
}
