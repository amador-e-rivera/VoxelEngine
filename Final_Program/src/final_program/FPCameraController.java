/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.Sys;

/**
 *
 * @author Amador
 */
public class FPCameraController {

    //3d vector to store the camera's position in
    private Vector3f position = null;
    private Vector3f lPosition = null;

    //The rotation around the Y axis of the camera
    private float yaw = 0.0f;

    //The rotation around the X axis of the camera
    private float pitch = 0.0f;
    private Vector3Float me;

    public FPCameraController(float x, float y, float z) {
        position = new Vector3f(x, y, z);
        lPosition = new Vector3f(x, y, z);
        lPosition.x = 0f;
        lPosition.y = 15f;
        lPosition.z = 0f;
    }

    //Increment the camera's current yaw rotation
    public void yaw(float amount) {
        yaw += amount;
    }

    //Increment the camera's current yaw rotation
    public void pitch(float amount) {
        pitch -= amount;
    }

    //Moves the camera forward relative to its current rotation (yaw)
    public void walkForward(float distance) {
        float xOffset = distance * (float) Math.sin(Math.toRadians(yaw));
        float zOffset = distance * (float) Math.cos(Math.toRadians(yaw));

        position.x -= xOffset;
        position.z += zOffset;
    }

    //Moves the camera backward relative to its current rotation (yaw)
    public void walkBackwards(float distance) {
        float xOffset = distance * (float) Math.sin(Math.toRadians(yaw));
        float zOffset = distance * (float) Math.cos(Math.toRadians(yaw));

        position.x += xOffset;
        position.z -= zOffset;
    }

    //Strafes the camera left relative to its current rotation (yaw)
    public void strafeLeft(float distance) {
        float xOffset = distance * (float) Math.sin(Math.toRadians(yaw - 90));
        float zOffset = distance * (float) Math.cos(Math.toRadians(yaw - 90));

        position.x -= xOffset;
        position.z += zOffset;
    }

    //Strafes the camera right relative to its current rotation (yaw)
    public void strafeRight(float distance) {
        float xOffset = distance * (float) Math.sin(Math.toRadians(yaw + 90));
        float zOffset = distance * (float) Math.cos(Math.toRadians(yaw + 90));

        position.x -= xOffset;
        position.z += zOffset;
    }

    //Moves camera up relative to its current rotation (yaw)
    public void moveUp(float distance) {
        position.y -= distance;
    }

    //Moves camera down
    public void moveDown(float distance) {
        position.y += distance;
    }

    //Translates and rotates the matrix so that it looks through the camera. This does what gluLookAt() does.
    public void lookThrough() {
        //Rotate the pitch around the x axis
        glRotatef(pitch, 1.0f, 0.0f, 0.0f);
        //Rotate the yaw around the y axis
        glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        //Rranslate to the position vectors location
        glTranslatef(position.x, position.y, position.z);
    }

    public void gameLoop() {
        FPCameraController camera = new FPCameraController(0, 0, 0);
        float dx = 0f;
        float dy = 0f;
        float dz = 0f;
        float lastTime = 0.0f; //length of frame
        long time = 0;
        float mouseSensitivity = 0.09f;
        float movementSpeed = 0.35f;

        //hide the mouse
        Mouse.setGrabbed(true);

        //keep looping until the dipslay window is closed or ESC is pressed
        while (!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            time = Sys.getTime();
            lastTime = time;

            //distance in mouse movement
            dx = Mouse.getDX();
            dy = Mouse.getDY();

            //Controll camera yaw from x movement from the mouse
            camera.yaw(dx * mouseSensitivity);
            //Controll camera pitch from y movement from the mouse
            camera.pitch(dy * mouseSensitivity);

            //When passing in the distance to move we times the movementSpeedwith dtthis is a time scale
            //so if its a slow frame u move more then a fast frame so on a slow computer you move just as 
            //fast as on a fast computer
            if (Keyboard.isKeyDown(Keyboard.KEY_W))//move forward
            {
                camera.walkForward(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_S))//move backwards
            {
                camera.walkBackwards(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_A)) { //strafe left
                camera.strafeLeft(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) { //strafe right
                camera.strafeRight(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) { //move up
                camera.moveUp(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) { //move down
                camera.moveDown(movementSpeed);
            }

            //set the modelviewmatrix back to the identity
            glLoadIdentity();
            //look through the camera before you draw anything
            camera.lookThrough();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            //you would draw your scene here.
            render();
            //draw the buffer to the screen
            Display.update();
            Display.sync(60);
        }
        Display.destroy();
    }

    private void render() {
        try {
                glBegin(GL_QUADS);

                    glColor3f(0.0f, 0.0f, 1.0f);
                    glVertex3f(2.0f, 2.0f, -2.0f);
                    glVertex3f(-2.0f, 2.0f, -2.0f);
                    glVertex3f(-2.0f, 2.0f, 2.0f);
                    glVertex3f(2.0f, 2.0f, 2.0f);

                    glColor3f(0.0f, 1.0f, 0.0f);
                    glVertex3f(2.0f, -2.0f, 2.0f);
                    glVertex3f(-2.0f, -2.0f, 2.0f);
                    glVertex3f(-2.0f, -2.0f, -2.0f);
                    glVertex3f(2.0f, -2.0f, -2.0f);

                    glColor3f(0.0f, 1.0f, 1.0f);
                    glVertex3f(2.0f, 2.0f, 2.0f);
                    glVertex3f(-2.0f, 2.0f, 2.0f);
                    glVertex3f(-2.0f, -2.0f, 2.0f);
                    glVertex3f(2.0f, -2.0f, 2.0f);

                    glColor3f(1.0f, 0.0f, 0.0f);
                    glVertex3f(2.0f, -2.0f, -2.0f);
                    glVertex3f(-2.0f, -2.0f, -2.0f);
                    glVertex3f(-2.0f, 2.0f, -2.0f);
                    glVertex3f(2.0f, 2.0f, -2.0f);

                    glColor3f(1.0f, 0.0f, 1.0f);
                    glVertex3f(-2.0f, 2.0f, 2.0f);
                    glVertex3f(-2.0f, 2.0f, -2.0f);
                    glVertex3f(-2.0f, -2.0f, -2.0f);
                    glVertex3f(-2.0f, -2.0f, 2.0f);

                    glColor3f(1.0f, 1.0f, 0.0f);
                    glVertex3f(2.0f, 2.0f, -2.0f);
                    glVertex3f(2.0f, 2.0f, 2.0f);
                    glVertex3f(2.0f, -2.0f, 2.0f);
                    glVertex3f(2.0f, -2.0f, -2.0f);
                
                glEnd();
        } catch (Exception e) {
        }
    }
}
