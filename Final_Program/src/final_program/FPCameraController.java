/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import java.util.ArrayList;
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

    private final float WORLD_SIZE = 60f;
    private final float CUBE_SIZE = 5f;

    //Each Cube has rgb variables for its color and the x, y & z coordinates for that cube.
    private ArrayList<Cube> cubes;

    //3d vector to store the camera's position in
    private Vector3f position = null;
    private Vector3f lPosition = null;

    //The rotation around the Y axis of the camera
    private float yaw = 0.0f;

    //The rotation around the X axis of the camera
    private float pitch = 0.0f;
    private Vector3Float me;

    public FPCameraController(float x, float y, float z) {
        cubes = new ArrayList<>();
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
            glRotatef(120f, 0f, 1f, 0f);
            render();
            //draw the buffer to the screen
            Display.update();
            Display.sync(60);
        }
        Display.destroy();
    }

    private void render() {
        try {
            
            //This if statement and for loops creates the cubes to be placed in the world. Not efficient and
            //perhaps not useful for what we are going to do, but its fun to play with for now.
            if (cubes.isEmpty()) {
                for (int y = 0; y < (WORLD_SIZE * 2) / 20; y++) {
                    for (int z = 0; z < (WORLD_SIZE * 2) / 20; z++) {
                        for (int x = 0; x < (WORLD_SIZE * 2) / 20; x++) {
                            cubes.add(new Cube(CUBE_SIZE * x, CUBE_SIZE * y, CUBE_SIZE * z));
                        }
                    }
                }
            }

            glBegin(GL_QUADS);
            drawWorld();

            for (Cube c : cubes) {
                drawCube(c);
            }

            glEnd();
        } catch (Exception e) {
        }
    }

    //method: drawWorld()
    //purpose: Simply draws a large cube to act as the world.
    //Some of the colors are kinda weird. I took some rgb colors from online and divided it by 255 to get
    //the float values.
    private void drawWorld() {

        //Bottom Face
        glColor3f((float)87/255, (float)59/255, (float)12/255);
        glVertex3f(WORLD_SIZE, -WORLD_SIZE, WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, -WORLD_SIZE, WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, -WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(WORLD_SIZE, -WORLD_SIZE, -WORLD_SIZE);

        //Top Face
        glColor3f(0.0f, 0.0f, 0.0f);
        glVertex3f(WORLD_SIZE, WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, WORLD_SIZE, WORLD_SIZE);
        glVertex3f(WORLD_SIZE, WORLD_SIZE, WORLD_SIZE);

        //Front Face
        glColor3f((float)135/255, (float)206/255, (float)235/255);
        glVertex3f(WORLD_SIZE, WORLD_SIZE, WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, WORLD_SIZE, WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, -WORLD_SIZE, WORLD_SIZE);
        glVertex3f(WORLD_SIZE, -WORLD_SIZE, WORLD_SIZE);

        //Back Face
        glColor3f((float)135/255, (float)206/255, (float)235/255);
        glVertex3f(WORLD_SIZE, -WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, -WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(WORLD_SIZE, WORLD_SIZE, -WORLD_SIZE);

        //Left face
        glColor3f((float)135/255, (float)206/255, (float)235/255);
        glVertex3f(-WORLD_SIZE, WORLD_SIZE, WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, -WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(-WORLD_SIZE, -WORLD_SIZE, WORLD_SIZE);

        //Right Face
        glColor3f((float)135/255, (float)206/255, (float)235/255);
        glVertex3f(WORLD_SIZE, WORLD_SIZE, -WORLD_SIZE);
        glVertex3f(WORLD_SIZE, WORLD_SIZE, WORLD_SIZE);
        glVertex3f(WORLD_SIZE, -WORLD_SIZE, WORLD_SIZE);
        glVertex3f(WORLD_SIZE, -WORLD_SIZE, -WORLD_SIZE);

    }

    //method: drawCube(Cube c)
    //purpose: Draws a cube with each side of the cube having a size of CUBE_SIZE. The position of each
    //cube in the world is determined by the c.x, c.y and c.z values.
    private void drawCube(Cube c) {
        //Top Face
        glColor3d(c.r, c.g, c.b);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - c.z);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - c.z);

        //Bottom Face
        glColor3d(c.r, c.b, c.g);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - c.z); 
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - c.z);

        //Front Face
        glColor3d(c.g, c.r, c.b);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - c.z);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - c.z);

        //Back Face
        glColor3d(c.g, c.b, c.r);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);

        //Left face
        glColor3d(c.b, c.r, c.g);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - c.z);
        glVertex3f(WORLD_SIZE - CUBE_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);

        //Right Face
        glColor3d(c.b, c.g, c.r);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + CUBE_SIZE + c.y, WORLD_SIZE - c.z);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - c.z);
        glVertex3f(WORLD_SIZE - c.x, -WORLD_SIZE + c.y, WORLD_SIZE - CUBE_SIZE - c.z);

    }
}
