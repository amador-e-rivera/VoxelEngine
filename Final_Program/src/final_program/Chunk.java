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

    static final int CHUNK_SIZE = 32; //AMADOR: Increased Chunk size to see the terrain a little better.
    static final int CUBE_LENGTH = 2;
    private Block[][][] blocks;
    private int VBOVertexHandle;
    private int VBOColorHandle;
    private int VBOTextureHandle;
    private Texture texture;
    private FloatBuffer NormalsBuffer;
    private int StartX, StartY, StartZ, noise_Seed;
    private Random r;

    public Chunk(int startX, int startY, int startZ, int noise_Seed) {
        try {
            texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("/textures/terrain.png"));
        } catch (Exception e) {
            System.out.print("ER-ROAR!");
        }

        this.noise_Seed = noise_Seed;
        r = new Random();

        blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    blocks[x][y][z] = new Block(Block.BlockType.Grass);
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
        //AMADOR: Used the following 2 commands to render the lighting on the chunk.
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glNormalPointer(0, NormalsBuffer);

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

    //method: rebuildMesh
    //purpose: This method builds the mesh for a chunk. It determines which chunk is being worked by
    //the startX, startY and startZ parameters. All chunks will most likely have the same startY,
    //but the startX and startZ will change depending on the chunk.
    public void rebuildMesh(float startX, float startY, float startZ) {
        int max_Height = (int) startY; //AMADOR: Max height (y) for the current xz position. No need to change this.
        int mountain_Height = 175; //AMADOR: Larger number makes the mountains steeper.
        int mountain_Width = 100; //AMADOR: A smaller value gives more peaks and less wide mountains.
        double persistance = 0.09; //AMADOR: Not sure how to describe this.
        int i, j, k, x1, z1;

        //AMADOR: The seed is now generated outside this class so that all chunks use the same seed.
        //In order for the terrain to have smooth transitions between chunks, they all need ot use
        //the same seed.
        SimplexNoise noise = new SimplexNoise(mountain_Width, persistance, noise_Seed);

        int bufferSize = (CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE) * 6 * 12;

        VBOColorHandle = glGenBuffers();
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();

        FloatBuffer VertexPositionData = BufferUtils.createFloatBuffer(bufferSize);
        FloatBuffer VertexColorData = BufferUtils.createFloatBuffer(bufferSize);
        FloatBuffer VertexTextureData = BufferUtils.createFloatBuffer(bufferSize);
        NormalsBuffer = BufferUtils.createFloatBuffer(bufferSize);

        //AMADOR: In order for the terrain to appear smooth between chunks, when one chunk is done being
        //generated, then the next chunk needs to pick up (in terms of x & z) where the last chunk left off. 
        //For example, if we are adding chunks along the x-axis, then the first chunk will
        //go from x == 0 to x == CHUNK_SIZE - 1. The next chunk will start at x == CHUNK_SIZE to
        //x == (CHUNK_SIZE * 2) - 1 and the third chunk will start at x == CHUNK_SIZE * 2 to 
        //x == (CHUNK_SIZE * 3) - 1...etc. The same applies when adding chunks along the z-axis. This assures
        //that when the max_Height is being calculated, the x and z values being passed to the noise algorithm
        //create a smooth transition between chunks. At least thats the idea.
        for (int x = CHUNK_SIZE * StartX; x < CHUNK_SIZE + (CHUNK_SIZE * StartX); x++) {
            for (int z = CHUNK_SIZE * StartZ; z < CHUNK_SIZE + (CHUNK_SIZE * StartZ); z++) {
                for (int y = 0; y < max_Height; y++) {
                    i = (int) (StartX + x * ((StartX - CHUNK_SIZE) / (double) CHUNK_SIZE));
                    j = (int) (StartY + y * ((StartY - CHUNK_SIZE) / (double) CHUNK_SIZE));
                    k = (int) (StartZ + z * ((StartZ - CHUNK_SIZE) / (double) CHUNK_SIZE));

                    //AMADOR: I am testing this code since it still doesnt line up perfectly.
                    i += StartX != 0 ? -(2 * StartX) : 0;
                    k += StartZ != 0 ? -(3 * StartZ) : 0;

                    //AMADOR: Added this if statement to prevent the max_Height from changing after it is
                    //initialized for each xz coordinate.
                    if (y == 0) {
                        max_Height = (StartY + (int) (mountain_Height * noise.getNoise(i, j, k))
                                * (CUBE_LENGTH / 2));
                    }

                    //AMADOR: Prevents height from being larger than the chunk size otherwise it throws an 
                    //array out of bounds error. It also sets the minimum height to 4 blocks. I think that 
                    //might be useful to set the block as water when max_Height == 4
                    if (max_Height >= CHUNK_SIZE) {
                        max_Height = CHUNK_SIZE;
                    } else if (max_Height < 4) {
                        max_Height = 4;
                    }

                    //AMADOR: I was receiving ArrayIndexOutofBoundsException when I realized that the values
                    //of x and z are only supposed to be used for the noise algorithm for smooth chunk 
                    //transitions. The actual blocks array still only has a size of CHUNK_SIZE, therefore, in
                    //order to reference a block in the blocks array, I had to use the modulus operator to
                    //get the approriate block xz coordinates.
                    x1 = x % CHUNK_SIZE;
                    z1 = z % CHUNK_SIZE;

                    setBlockType(max_Height, x1, y, z1);
                    VertexPositionData.put(
                            createCube(
                                    (float) ((StartX * CHUNK_SIZE * 2) + x1 * CUBE_LENGTH),
                                    (float) (y * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                                    (float) ((StartZ * CHUNK_SIZE * 2) + z1 * CUBE_LENGTH))
                    );

                    //Highlights chunk perimeter
                    
                    if (x1 == 0 || x1 == CHUNK_SIZE - 1 || z1 == 0 || z1 == CHUNK_SIZE - 1) {
                        VertexColorData.put(createCubeVertexCol(getCubeColor(blocks[x1][y][z1])));
                    } else {
                        VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));
                    }

                    //VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));
                    VertexTextureData.put(createTexCube((float) 0, (float) 0, blocks[x1][y][z1]));
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

        normalize_Vertices(VertexPositionData, NormalsBuffer);
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
            if (r.nextDouble() < 0.75) {
                blocks[x][y][z].setBlockType(Block.BlockType.Dirt);
            } else {
                blocks[x][y][z].setBlockType(Block.BlockType.Stone);
            }
        }
    }

    private void normalize_Vertices(FloatBuffer vertices, FloatBuffer normals) {
        float v1, v2, v3, v4, length;

        for (int i = 0; i < vertices.limit(); i += 4) {
            v1 = vertices.get(i);
            v2 = vertices.get(i + 1);
            v3 = vertices.get(i + 2);
            v4 = vertices.get(i + 3);

            length = (float) Math.sqrt((v1 * v1) + (v2 * v2) + (v3 * v3) + (v4 * v4));

            v1 = v1 / length;
            v2 = v2 / length;
            v3 = v3 / length;
            v4 = v4 / length;

            normals.put(i, v1);
            normals.put(i + 1, v2);
            normals.put(i + 2, v3);
            normals.put(i + 3, v4);
        }

        normals.flip();
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
        return new float[]{1f, 0.5f, 0.5f};
    }

    public static float[] createTexCube(float x, float y, Block block) {
        float offset = (1024f / 16) / 1024f;
        
        switch (block.getBlockType()) {
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
