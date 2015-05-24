/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import java.util.ArrayList;
import java.util.Random;
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

    private final int NUM_OF_CHUNKS = 4; //AMADOR: NUM_OF_CHUNKS x NUM_OF_CHUNKS = Total # of chunks generated

    //Each Block has rgb variables for its color and the x, y & z coordinates for that cube.
    private ArrayList<Chunk> chunks;
    private int noise_Seed;

    //3d vector to store the camera's position in
    private Vector3f position = null;
    private Vector3f lPosition = null;

    //The rotation around the Y axis of the camera
    private float yaw = 0.0f;

    //The rotation around the X axis of the camera
    private float pitch = 0.0f;

    public FPCameraController(float x, float y, float z) {
        position = new Vector3f(x, y, z);
        lPosition = new Vector3f(x, y, z);
        lPosition.x = 0f;
        lPosition.y = 0f;
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
        Crosshair crossHair = new Crosshair(-position.x, -position.y, position.z);
        Player player = new Player(-position.x, -position.y, position.z);

        float dx = 0f;
        float dy = 0f;
        float dz = 0f;
        float lastTime = 0.0f; //length of frame
        long time = 0;
        float mouseSensitivity = 0.09f;
        float movementSpeed = 0.75f;

        //AMADOR: This is where I get the seed to be used the the Chunk SimplexNoise object.
        Random r = new Random();
        noise_Seed = r.nextInt();//-1049537650, -1807078236;//r.nextInt();
        System.out.println("Seed: " + noise_Seed);

        //Generates an array of Chunks. The i and k values are sort of like the key for the chunk. It
        //tells the Chunk rebuildMesh method which chunk it is and its position in the world.
        chunks = new ArrayList<>();

        for (int i = 0; i < NUM_OF_CHUNKS; i++) {
            for (int k = 0; k < NUM_OF_CHUNKS; k++) {
                chunks.add(new Chunk(i, 15, k, noise_Seed));
            }
        }

        //hide the mouse
        Mouse.setGrabbed(true);

        time = Sys.getTime();

        //keep looping until the dipslay window is closed or ESC is pressed
        while (!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
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
            if (Mouse.hasWheel()) {
                int wheel = Mouse.getDWheel();
                if (wheel < 0) {
                    player.updateBlockType(-1);
                } else if (wheel > 0) {
                    player.updateBlockType(1);
                }

            }
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
            glRotatef(135f, 0f, 1f, 0f);
            glTranslatef(0f, -70f, 0f);
            //glTranslatef(-(Chunk.CHUNK_SIZE * NUM_OF_CHUNKS), -70f, -(Chunk.CHUNK_SIZE * NUM_OF_CHUNKS)); //Centers you in terrain

            player.render();
            crossHair.render();

            for (Chunk c : chunks) {
                c.render();
            }

            //draw the buffer to the screen
            Display.update();
            Display.sync(60);
        }
        Display.destroy();
    }
}
