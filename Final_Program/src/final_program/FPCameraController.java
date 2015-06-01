/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.Sys;
import static org.lwjgl.util.glu.GLU.gluUnProject;

/**
 *
 * @author Amador
 */
public class FPCameraController {

    private final int NUM_OF_CHUNKS = 3; //AMADOR: NUM_OF_CHUNKS x NUM_OF_CHUNKS = Total # of chunks generated

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

        updateLight(xOffset, zOffset);
    }

    //Moves the camera backward relative to its current rotation (yaw)
    public void walkBackwards(float distance) {
        float xOffset = distance * (float) Math.sin(Math.toRadians(yaw));
        float zOffset = distance * (float) Math.cos(Math.toRadians(yaw));

        position.x += xOffset;
        position.z -= zOffset;

        updateLight(xOffset, zOffset);
    }

    //Strafes the camera left relative to its current rotation (yaw)
    public void strafeLeft(float distance) {
        float xOffset = distance * (float) Math.sin(Math.toRadians(yaw - 90));
        float zOffset = distance * (float) Math.cos(Math.toRadians(yaw - 90));

        position.x -= xOffset;
        position.z += zOffset;

        updateLight(xOffset, zOffset);
    }

    //Strafes the camera right relative to its current rotation (yaw)
    public void strafeRight(float distance) {
        float xOffset = distance * (float) Math.sin(Math.toRadians(yaw + 90));
        float zOffset = distance * (float) Math.cos(Math.toRadians(yaw + 90));

        position.x -= xOffset;
        position.z += zOffset;

        updateLight(xOffset, zOffset);
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

        FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
        lightPosition.put(0.2f).put(-0.3f).put(0.0f).put(0.0f).flip();
        glLight(GL_LIGHT0, GL_POSITION, lightPosition);
    }

    private void updateLight(float xOffset, float zOffset) {
        //FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
        //lightPosition.put(lPosition.x -= xOffset).put(lPosition.y).put(lPosition.z += zOffset).
        //        put(0.0f).flip();
        //glLight(GL_LIGHT0, GL_POSITION, lightPosition);
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
        noise_Seed = r.nextInt();//-194301400;
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
            /*
             if (Sys.getTime() - time > 90) {
             time = Sys.getTime();
             }
             */
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
            //glRotatef(90f, 0f, 1f, 0f);
            glTranslatef(-(Chunk.CHUNK_SIZE * NUM_OF_CHUNKS), -85f, -(Chunk.CHUNK_SIZE * NUM_OF_CHUNKS)); //Centers you in terrain

            player.render();
            crossHair.render();

            Vector3Float[] ray = getRay();
            for (Chunk c : chunks) {
                c.update(ray);
                c.render();
            }

            //draw the buffer to the screen
            Display.update();
            Display.sync(60);
        }
        Display.destroy();
    }

    //AMADOR: This method gets the ray for the center of the viewport in world space coordinates. It returns
    //the close and far points of the ray.
    public Vector3Float[] getRay() {
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        FloatBuffer close = BufferUtils.createFloatBuffer(3);
        FloatBuffer far = BufferUtils.createFloatBuffer(3);
        float winX, winY;

        glGetFloat(GL_MODELVIEW_MATRIX, modelview); //AMADOR: Get modelview matrix
        glGetFloat(GL_PROJECTION_MATRIX, projection); //AMADOR: Get projection matrix
        glGetInteger(GL_VIEWPORT, viewport); //AMADOR: Get viewport

        winX = (viewport.get(2) - viewport.get(0)) / 2; //AMADOR: Center of screen in x axis
        winY = (viewport.get(3) - viewport.get(1)) / 2; //AMADOR: Center of screen in y axis

        //AMADOR: Get world coordinates of center of screen (near plane).
        gluUnProject(winX, winY, 0, modelview, projection, viewport, close);

        //AMADOR: Get world coordinates of center of screen (far plane).
        gluUnProject(winX, winY, 1, modelview, projection, viewport, far);

        return new Vector3Float[]{new Vector3Float(close.get(0), close.get(1), close.get(2)),
            new Vector3Float(far.get(0), far.get(1), far.get(2))};
    }
}
