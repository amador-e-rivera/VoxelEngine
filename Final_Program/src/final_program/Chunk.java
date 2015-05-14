/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import java.nio.FloatBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;

public class Chunk {

    static final int CHUNK_SIZE = 60; //AMADOR: Increased Chunk size to see the terrain a little better.
    static final int CUBE_LENGTH = 2;
    private Block[][][] blocks;
    private int VBOVertexHandle;
    private int VBOColorHandle;
    private int VBOTextureHandle;
    private Texture texture;
    private int StartX, StartY, StartZ;
    private Random r;

    public Chunk(int startX, int startY, int startZ) {
        try {
            texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("/textures/terrain.png"));
        } catch (Exception e) {
            System.out.print("ER-ROAR!");
        }

        r = new Random();
        blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    blocks[x][y][z] = new Block(Block.BlockType.BedRock);
                }
            }
        }

        VBOColorHandle = glGenBuffers();
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();
        StartX = startX;
        StartY = startY;
        StartZ = startZ;

        rebuildMesh(startX, startY, startZ);
    }

    public void render() {
        glPushMatrix();
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, VBOColorHandle);
        glColorPointer(3, GL_FLOAT, 0, 0L);

        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBindTexture(GL_TEXTURE_2D, 1);
        glTexCoordPointer(2, GL_FLOAT, 0, 0L);

        glDrawArrays(GL_QUADS, 0, CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * 24);
        glPopMatrix();
    }

    //AMADOR: Play with the mountain_Height, mountain_Width and persistance.
    public void rebuildMesh(float startX, float startY, float startZ) {
        int max_Height = CHUNK_SIZE; //AMADOR: Max height (y) for the current xz position. No need to change this.
        int mountain_Height = 175; //AMADOR: Larger number makes the mountains steeper.
        int mountain_Width = 90; //AMADOR: A smaller value gives more peaks and less wide mountains.
        double persistance = 0.08; //AMADOR: Not sure how to describe this.
        int i, j, k;

        //AMADOR: I set the seed to use the random object for random map generation.
        SimplexNoise noise = new SimplexNoise(mountain_Width, persistance, r.nextInt());

        int bufferSize = (CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE) * 6 * 12;

        VBOColorHandle = glGenBuffers();
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();

        FloatBuffer VertexPositionData = BufferUtils.createFloatBuffer(bufferSize);
        FloatBuffer VertexColorData = BufferUtils.createFloatBuffer(bufferSize);
        FloatBuffer VertexTextureData = BufferUtils.createFloatBuffer(bufferSize);

        for (int x = 0; x < CHUNK_SIZE; x += 1) {
            for (int z = 0; z < CHUNK_SIZE; z += 1) {
                for (int y = 0; y < max_Height; y++) {
                    //AMADOR: Casted CHUNK_SIZE to double, otherwsie the result of the division would be 0 and
                    //the max_Height will not change.
                    i = (int) (StartX + x * ((CHUNK_SIZE - StartX) / (double) CHUNK_SIZE));
                    j = (int) (StartY + max_Height * ((CHUNK_SIZE - StartY) / (double) CHUNK_SIZE));
                    k = (int) (StartZ + z * ((CHUNK_SIZE - StartZ) / (double) CHUNK_SIZE));

                    //AMADOR: Dividing CUBE_LENGTH by 2 made the map look a lot better. It looks like this 
                    //is where the mountain height is set. I set the variable mountain_Height to reflect that.
                    max_Height = (StartY + (int) (mountain_Height * noise.getNoise(i, j, k)) * (CUBE_LENGTH / 2));

                    //AMADOR: Prevents height from being larger than the chunk size otherwise it throws an 
                    //array out of bounds error. It also sets the minimum height to 4 blocks. I think that 
                    //might be useful to set the block as water when max_Height == 4
                    if (max_Height >= CHUNK_SIZE) {
                        max_Height = CHUNK_SIZE;
                    } else if (max_Height < 4) {
                        //AMADOR: If you comment this out, you will see holes in the map.
                        max_Height = 4;
                    }

                    setBlockType(max_Height, x, y, z);
                    VertexPositionData.put(createCube((float) (startX + x * CUBE_LENGTH),
                            (float) (y * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                            (float) (startZ + z * CUBE_LENGTH)));
                    VertexColorData.put(createCubeVertexCol(getCubeColor(blocks[x][y][z])));
                    VertexTextureData.put(createTexCube((float) 0, (float) 0,
                            blocks[x][y][z]));
                }
            }
        }

        VertexColorData.flip();
        VertexPositionData.flip();
        VertexTextureData.flip();

        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexPositionData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, VBOColorHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexColorData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexTextureData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void setBlockType(int max_Height, int x, int y, int z) {
        if (y == 0) {
            blocks[x][y][z].setBlockType(Block.BlockType.BedRock);
        } else if (max_Height == 4 && y < 4) {
            blocks[x][y][z].setBlockType(Block.BlockType.Water);
        } else if (max_Height == 5 && y < 5) {
            blocks[x][y][z].setBlockType(Block.BlockType.Sand);
        } else if (y == max_Height || y == max_Height - 1) {
            blocks[x][y][z].setBlockType(Block.BlockType.Grass);
        } else if (max_Height >= 6 && y <= 3) {
            blocks[x][y][z].setBlockType(Block.BlockType.BedRock);
        } else if (max_Height >= 6 && y < max_Height - 1) {
            if (r.nextDouble() < 0.8) {
                blocks[x][y][z].setBlockType(Block.BlockType.Dirt);
            } else {
                blocks[x][y][z].setBlockType(Block.BlockType.Stone);
            }
        }
    }

    private float[] createCubeVertexCol(float[] CubeColorArray) {
        float[] cubeColors = new float[CubeColorArray.length * 4 * 6];
        for (int i = 0; i < cubeColors.length; i++) {
            cubeColors[i] = CubeColorArray[i % CubeColorArray.length];
        }
        return cubeColors;
    }

    public static float[] createCube(float x, float y, float z) {
        int offset = CUBE_LENGTH / 2;
        return new float[]{
            // TOP QUAD
            x + offset, y + offset, z,
            x - offset, y + offset, z,
            x - offset, y + offset, z - CUBE_LENGTH,
            x + offset, y + offset, z - CUBE_LENGTH,
            // BOTTOM QUAD
            x + offset, y - offset, z - CUBE_LENGTH,
            x - offset, y - offset, z - CUBE_LENGTH,
            x - offset, y - offset, z,
            x + offset, y - offset, z,
            // FRONT QUAD
            x + offset, y + offset, z - CUBE_LENGTH,
            x - offset, y + offset, z - CUBE_LENGTH,
            x - offset, y - offset, z - CUBE_LENGTH,
            x + offset, y - offset, z - CUBE_LENGTH,
            // BACK QUAD
            x + offset, y - offset, z,
            x - offset, y - offset, z,
            x - offset, y + offset, z,
            x + offset, y + offset, z,
            // LEFT QUAD
            x - offset, y + offset, z - CUBE_LENGTH,
            x - offset, y + offset, z,
            x - offset, y - offset, z,
            x - offset, y - offset, z - CUBE_LENGTH,
            // RIGHT QUAD
            x + offset, y + offset, z,
            x + offset, y + offset, z - CUBE_LENGTH,
            x + offset, y - offset, z - CUBE_LENGTH,
            x + offset, y - offset, z
        };
    }

    private float[] getCubeColor(Block block) {
        return new float[]{1, 1, 1};
    }

    public static float[] createTexCube(float x, float y, Block block) {
        float offset = (1024f / 16) / 1024f;
        Block.BlockType temp = block.getBlockType();
        switch (temp) {
            case Grass:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 3, y + offset * 10,
                    x + offset * 2, y + offset * 10,
                    x + offset * 2, y + offset * 9,
                    x + offset * 3, y + offset * 9,
                    // TOP!
                    x + offset * 3, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    x + offset * 2, y + offset * 0,
                    x + offset * 3, y + offset * 0,
                    // FRONT QUAD
                    x + offset * 3, y + offset * 0,
                    x + offset * 4, y + offset * 0,
                    x + offset * 4, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    // BACK QUAD
                    x + offset * 4, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    x + offset * 3, y + offset * 0,
                    x + offset * 4, y + offset * 0,
                    // LEFT QUAD
                    x + offset * 3, y + offset * 0,
                    x + offset * 4, y + offset * 0,
                    x + offset * 4, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    // RIGHT QUAD
                    x + offset * 3, y + offset * 0,
                    x + offset * 4, y + offset * 0,
                    x + offset * 4, y + offset * 1,
                    x + offset * 3, y + offset * 1
                };
            case Dirt:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 3, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    x + offset * 2, y + offset * 0,
                    x + offset * 3, y + offset * 0,
                    // TOP!
                    x + offset * 3, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    x + offset * 2, y + offset * 0,
                    x + offset * 3, y + offset * 0,
                    // FRONT QUAD
                    x + offset * 2, y + offset * 0,
                    x + offset * 3, y + offset * 0,
                    x + offset * 3, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    // BACK QUAD
                    x + offset * 3, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    x + offset * 2, y + offset * 0,
                    x + offset * 3, y + offset * 0,
                    // LEFT QUAD
                    x + offset * 2, y + offset * 0,
                    x + offset * 3, y + offset * 0,
                    x + offset * 3, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    // RIGHT QUAD
                    x + offset * 2, y + offset * 0,
                    x + offset * 3, y + offset * 0,
                    x + offset * 3, y + offset * 1,
                    x + offset * 2, y + offset * 1
                };
            case Stone:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 2, y + offset * 1,
                    x + offset * 1, y + offset * 1,
                    x + offset * 1, y + offset * 0,
                    x + offset * 2, y + offset * 0,
                    // TOP!
                    x + offset * 2, y + offset * 1,
                    x + offset * 1, y + offset * 1,
                    x + offset * 1, y + offset * 0,
                    x + offset * 2, y + offset * 0,
                    // FRONT QUAD
                    x + offset * 1, y + offset * 0,
                    x + offset * 2, y + offset * 0,
                    x + offset * 2, y + offset * 1,
                    x + offset * 1, y + offset * 1,
                    // BACK QUAD
                    x + offset * 2, y + offset * 1,
                    x + offset * 1, y + offset * 1,
                    x + offset * 1, y + offset * 0,
                    x + offset * 2, y + offset * 0,
                    // LEFT QUAD
                    x + offset * 1, y + offset * 0,
                    x + offset * 2, y + offset * 0,
                    x + offset * 2, y + offset * 1,
                    x + offset * 1, y + offset * 1,
                    // RIGHT QUAD
                    x + offset * 1, y + offset * 0,
                    x + offset * 2, y + offset * 0,
                    x + offset * 2, y + offset * 1,
                    x + offset * 1, y + offset * 1
                };
            case Water:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 14, y + offset * 13,
                    x + offset * 13, y + offset * 13,
                    x + offset * 13, y + offset * 12,
                    x + offset * 14, y + offset * 12,
                    // TOP!
                    x + offset * 14, y + offset * 13,
                    x + offset * 13, y + offset * 13,
                    x + offset * 13, y + offset * 12,
                    x + offset * 14, y + offset * 12,
                    // FRONT QUAD
                    x + offset * 13, y + offset * 12,
                    x + offset * 14, y + offset * 12,
                    x + offset * 14, y + offset * 13,
                    x + offset * 13, y + offset * 13,
                    // BACK QUAD
                    x + offset * 14, y + offset * 13,
                    x + offset * 13, y + offset * 13,
                    x + offset * 13, y + offset * 12,
                    x + offset * 14, y + offset * 12,
                    // LEFT QUAD
                    x + offset * 13, y + offset * 12,
                    x + offset * 14, y + offset * 12,
                    x + offset * 14, y + offset * 13,
                    x + offset * 13, y + offset * 13,
                    // RIGHT QUAD
                    x + offset * 13, y + offset * 12,
                    x + offset * 14, y + offset * 12,
                    x + offset * 14, y + offset * 13,
                    x + offset * 13, y + offset * 13
                };
            case BedRock:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 2, y + offset * 2,
                    x + offset * 1, y + offset * 2,
                    x + offset * 1, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    // TOP!
                    x + offset * 2, y + offset * 2,
                    x + offset * 1, y + offset * 2,
                    x + offset * 1, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    // FRONT QUAD
                    x + offset * 1, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    x + offset * 2, y + offset * 2,
                    x + offset * 1, y + offset * 2,
                    // BACK QUAD
                    x + offset * 2, y + offset * 2,
                    x + offset * 1, y + offset * 2,
                    x + offset * 1, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    // LEFT QUAD
                    x + offset * 1, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    x + offset * 2, y + offset * 2,
                    x + offset * 1, y + offset * 2,
                    // RIGHT QUAD
                    x + offset * 1, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    x + offset * 2, y + offset * 2,
                    x + offset * 1, y + offset * 2
                };
            case Sand:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 3, y + offset * 2,
                    x + offset * 2, y + offset * 2,
                    x + offset * 2, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    // TOP!
                    x + offset * 3, y + offset * 2,
                    x + offset * 2, y + offset * 2,
                    x + offset * 2, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    // FRONT QUAD
                    x + offset * 2, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    x + offset * 3, y + offset * 2,
                    x + offset * 2, y + offset * 2,
                    // BACK QUAD
                    x + offset * 3, y + offset * 2,
                    x + offset * 2, y + offset * 2,
                    x + offset * 2, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    // LEFT QUAD
                    x + offset * 2, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    x + offset * 3, y + offset * 2,
                    x + offset * 2, y + offset * 2,
                    // RIGHT QUAD
                    x + offset * 2, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    x + offset * 3, y + offset * 2,
                    x + offset * 2, y + offset * 2
                };
            case Wood:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 6, y + offset * 2,
                    x + offset * 5, y + offset * 2,
                    x + offset * 5, y + offset * 1,
                    x + offset * 6, y + offset * 1,
                    // TOP!
                    x + offset * 6, y + offset * 2,
                    x + offset * 5, y + offset * 2,
                    x + offset * 5, y + offset * 1,
                    x + offset * 6, y + offset * 1,
                    // FRONT QUAD
                    x + offset * 4, y + offset * 1,
                    x + offset * 5, y + offset * 1,
                    x + offset * 5, y + offset * 2,
                    x + offset * 4, y + offset * 2,
                    // BACK QUAD
                    x + offset * 5, y + offset * 2,
                    x + offset * 4, y + offset * 2,
                    x + offset * 4, y + offset * 1,
                    x + offset * 5, y + offset * 1,
                    // LEFT QUAD
                    x + offset * 4, y + offset * 1,
                    x + offset * 5, y + offset * 1,
                    x + offset * 5, y + offset * 2,
                    x + offset * 4, y + offset * 2,
                    // RIGHT QUAD
                    x + offset * 4, y + offset * 1,
                    x + offset * 5, y + offset * 1,
                    x + offset * 5, y + offset * 2,
                    x + offset * 4, y + offset * 2
                };
            default: // Currently default is grass, recommend making default to dirt in the future
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 3, y + offset * 10,
                    x + offset * 2, y + offset * 10,
                    x + offset * 2, y + offset * 9,
                    x + offset * 3, y + offset * 9,
                    // TOP!
                    x + offset * 3, y + offset * 1,
                    x + offset * 2, y + offset * 1,
                    x + offset * 2, y + offset * 0,
                    x + offset * 3, y + offset * 0,
                    // FRONT QUAD
                    x + offset * 3, y + offset * 0,
                    x + offset * 4, y + offset * 0,
                    x + offset * 4, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    // BACK QUAD
                    x + offset * 4, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    x + offset * 3, y + offset * 0,
                    x + offset * 4, y + offset * 0,
                    // LEFT QUAD
                    x + offset * 3, y + offset * 0,
                    x + offset * 4, y + offset * 0,
                    x + offset * 4, y + offset * 1,
                    x + offset * 3, y + offset * 1,
                    // RIGHT QUAD
                    x + offset * 3, y + offset * 0,
                    x + offset * 4, y + offset * 0,
                    x + offset * 4, y + offset * 1,
                    x + offset * 3, y + offset * 1
                };
        }
    }
}
