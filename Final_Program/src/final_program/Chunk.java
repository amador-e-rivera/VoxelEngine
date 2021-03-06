/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import java.nio.FloatBuffer;
import java.util.Random;
import java.util.TreeMap;
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
    private Texture texture;
    private int StartX, StartY, StartZ, noise_Seed;
    private Random r;
    private int bufferSize = (CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE) * 6 * 12;
    private int VBOVertexHandle, VBOColorHandle, VBOTextureHandle, VBONormalsHandle;
    private FloatBuffer VertexTextureData, VertexPositionData, VertexColorData, VertexNormalsData;
    private float[] vertices;
    private TreeMap<Float, float[]> selectedBlocks;

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
        
        //AMADOR: Used the following 2 commands for the vertex normals to render the lighting on the chunk.
        glBindBuffer(GL_ARRAY_BUFFER, VBONormalsHandle);
        glNormalPointer(GL_FLOAT,0,0L);

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
        double persistance = 0.1; //AMADOR: Not sure how to describe this.
        int i, j, k, x1, z1;

        //AMADOR: The seed is now generated outside this class so that all chunks use the same seed.
        //In order for the terrain to have smooth transitions between chunks, they all need ot use
        //the same seed.
        SimplexNoise noise = new SimplexNoise(mountain_Width, persistance, noise_Seed/*5*/);

        VBOColorHandle = glGenBuffers();
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();
        VBONormalsHandle = glGenBuffers();

        VertexPositionData = BufferUtils.createFloatBuffer(bufferSize);
        VertexColorData = BufferUtils.createFloatBuffer(bufferSize);
        VertexTextureData = BufferUtils.createFloatBuffer(bufferSize);
        VertexNormalsData = BufferUtils.createFloatBuffer(bufferSize);

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
                    k += StartZ != 0 ? -(2 * StartZ) : 0;

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

                    VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));
                    VertexTextureData.put(createTexCube((float) 0, (float) 0, blocks[x1][y][z1].getBlockType()));
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

        normals_Buffer(VertexPositionData);
    }

    private void setBlockType(int max_Height, int x, int y, int z) {

        if (y == 0) {
            blocks[x][y][z].setBlockType(Block.BlockType.BedRock);
        } else if (max_Height == 4 && y < 4) {

            if (r.nextInt(100) == 1) {
                blocks[x][y][z].setBlockType(Block.BlockType.Lily);
            } else {
                blocks[x][y][z].setBlockType(Block.BlockType.Water);
            }
        } else if (max_Height == 5 && y < 5) {
            blocks[x][y][z].setBlockType(Block.BlockType.Sand);
            SpawnCacti(x, y, z, 1);
        } else if (y == max_Height || y == max_Height - 1) {
            if (y > CHUNK_SIZE - 7) {
                blocks[x][y][z].setBlockType(Block.BlockType.Snow);
                SpawnJackO(x, y, z, 10);
            } else {
                blocks[x][y][z].setBlockType(Block.BlockType.Grass);
                SpawnPumkin(x, y, z, 1);
            }

            SpawnTree(x, y, z, 1);

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

    //Preston: Returns true if spawns a tree, must give percentage 0 to 100 on odds of spawning a tree
    private boolean SpawnTree(int x, int y, int z, int percentToSpawn) {

        if (r.nextInt(100) < percentToSpawn && y < CHUNK_SIZE - 8) {
            blocks[x][y][z].setBlockType(Block.BlockType.Wood);

            int hieght = r.nextInt(4) + 3;

            for (int i = 1; i < hieght; i++) {
                VertexTextureData.put(createTexCube((float) 0, (float) 0, Block.BlockType.Wood));
                VertexPositionData.put(
                        createCube(
                                (float) ((StartX * CHUNK_SIZE * 2) + x * CUBE_LENGTH),
                                (float) ((y + i) * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                                (float) ((StartZ * CHUNK_SIZE * 2) + z * CUBE_LENGTH))
                );
                VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));
            }

            for (int i = - 1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    VertexTextureData.put(createTexCube((float) 0, (float) 0, Block.BlockType.Leaf));
                    VertexPositionData.put(
                            createCube(
                                    (float) ((StartX * CHUNK_SIZE * 2) + (x + i) * CUBE_LENGTH),
                                    (float) ((y + hieght) * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                                    (float) ((StartZ * CHUNK_SIZE * 2) + (z + j) * CUBE_LENGTH))
                    );
                    VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));
                }
            }

            VertexTextureData.put(createTexCube((float) 0, (float) 0, Block.BlockType.Leaf));
            VertexPositionData.put(
                    createCube(
                            (float) ((StartX * CHUNK_SIZE * 2) + x * CUBE_LENGTH),
                            (float) ((y + hieght + 1) * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                            (float) ((StartZ * CHUNK_SIZE * 2) + z * CUBE_LENGTH))
            );
            VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));

            return true;
        }

        return false; //No tree spawned
    }

    private boolean SpawnCacti(int x, int y, int z, int percentToSpawn) {

        if (r.nextInt(100) < percentToSpawn && y < CHUNK_SIZE - 8) {

            for (int i = 1; i < 4; i++) {
                VertexTextureData.put(createTexCube((float) 0, (float) 0, Block.BlockType.Cacti));
                VertexPositionData.put(
                        createCube(
                                (float) ((StartX * CHUNK_SIZE * 2) + x * CUBE_LENGTH),
                                (float) ((y + i) * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                                (float) ((StartZ * CHUNK_SIZE * 2) + z * CUBE_LENGTH))
                );
                VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));
            }

            return true;
        }

        return false; //No tree spawned
    }

    private boolean SpawnPumkin(int x, int y, int z, int percentToSpawn) {

        if (r.nextInt(1000) < percentToSpawn && y < CHUNK_SIZE - 8) {

            VertexTextureData.put(createTexCube((float) 0, (float) 0, Block.BlockType.Pumkin));
            VertexPositionData.put(
                    createCube(
                            (float) ((StartX * CHUNK_SIZE * 2) + x * CUBE_LENGTH),
                            (float) ((y + 1) * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                            (float) ((StartZ * CHUNK_SIZE * 2) + z * CUBE_LENGTH))
            );
            VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));

            return true;
        }

        return false;
    }

    private boolean SpawnJackO(int x, int y, int z, int percentToSpawn) {

        if (r.nextInt(1000) < percentToSpawn && y < CHUNK_SIZE - 5) {

            VertexTextureData.put(createTexCube((float) 0, (float) 0, Block.BlockType.JackO));
            VertexPositionData.put(
                    createCube(
                            (float) ((StartX * CHUNK_SIZE * 2) + x * CUBE_LENGTH),
                            (float) ((y + 3) * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                            (float) ((StartZ * CHUNK_SIZE * 2) + z * CUBE_LENGTH))
            );
            VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));

            VertexTextureData.put(createTexCube((float) 0, (float) 0, Block.BlockType.BedRock));
            VertexPositionData.put(
                    createCube(
                            (float) ((StartX * CHUNK_SIZE * 2) + x * CUBE_LENGTH),
                            (float) ((y + 2) * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                            (float) ((StartZ * CHUNK_SIZE * 2) + z * CUBE_LENGTH))
            );
            VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));

            VertexTextureData.put(createTexCube((float) 0, (float) 0, Block.BlockType.BedRock));
            VertexPositionData.put(
                    createCube(
                            (float) ((StartX * CHUNK_SIZE * 2) + x * CUBE_LENGTH),
                            (float) ((y + 1) * CUBE_LENGTH + (int) (CHUNK_SIZE * .8)),
                            (float) ((StartZ * CHUNK_SIZE * 2) + z * CUBE_LENGTH))
            );
            VertexColorData.put(createCubeVertexCol(new float[]{1, 1, 1}));

            return true;
        }

        return false;
    }

    //AMADOR: In this method, I pass the ray for the center of the viewport. This method will use the ray
    //to determine if the ray passes through a block. If it does, then it will highlight it, otherwise it
    //does nothing.
    public void update(Vector3Float[] ray) {
        //AMADOR: Get vertex data and put it in a float array to save into memory.
        if (VertexPositionData.hasRemaining()) {
            vertices = new float[VertexPositionData.limit()];
            VertexPositionData.get(vertices);
        }

        if (vertices != null) {
            selectedBlocks = new TreeMap<>();
            Vector3Float intersect_Point;
            float distance = 0, tempDist = 0;
            boolean hit = false;

            //AMADOR: Loops through each block in the chunk. I loop through each block instead of each
            //vertex to reduce the number of loops. I then check each face of the block to see if the
            //ray intersects it.
            for (int i = 0; i < vertices.length; i += 72) {

                //TOP FACE
                intersect_Point = rayIntersect(ray[0], ray[1],
                        new Vector3Float(vertices[i], vertices[i + 1], vertices[i + 2]),
                        new Vector3Float(vertices[i + 3], vertices[i + 4], vertices[i + 5]),
                        new Vector3Float(vertices[i + 6], vertices[i + 7], vertices[i + 8]));

                //AMADOR: These if statments will determine if the the ray intersects the block. If it does, then
                //it sets hit to true. There is one of these if statements for each face of the block.
                if (intersect_Point != null && !hit) {
                    if (intersect_Point.x < vertices[i] && intersect_Point.x > vertices[i + 3]
                            && intersect_Point.z < vertices[i + 2] && intersect_Point.z > vertices[i + 8]) {
                        distance = (float) Math.sqrt(Math.pow((intersect_Point.x - ray[0].x), 2)
                                + Math.pow((intersect_Point.y - ray[0].y), 2)
                                + Math.pow((intersect_Point.z - ray[0].z), 2));
                        hit = true;
                    }
                }

                //BOTTOM FACE
                intersect_Point = rayIntersect(ray[0], ray[1],
                        new Vector3Float(vertices[i + 12], vertices[i + 13], vertices[i + 14]),
                        new Vector3Float(vertices[i + 15], vertices[i + 16], vertices[i + 17]),
                        new Vector3Float(vertices[i + 18], vertices[i + 19], vertices[i + 20]));

                if (intersect_Point != null && !hit) {
                    if (intersect_Point.x < vertices[i + 12] && intersect_Point.x > vertices[i + 15]
                            && intersect_Point.z > vertices[i + 14] && intersect_Point.z < vertices[i + 20]) {
                        tempDist = (float) Math.sqrt(Math.pow((intersect_Point.x - ray[0].x), 2)
                                + Math.pow((intersect_Point.y - ray[0].y), 2)
                                + Math.pow((intersect_Point.z - ray[0].z), 2));
                        distance = (tempDist < distance && hit) || !hit ? tempDist : distance;
                        hit = true;
                    }
                }

                //BACK FACE
                intersect_Point = rayIntersect(ray[0], ray[1],
                        new Vector3Float(vertices[i + 24], vertices[i + 25], vertices[i + 26]),
                        new Vector3Float(vertices[i + 27], vertices[i + 28], vertices[i + 29]),
                        new Vector3Float(vertices[i + 30], vertices[i + 31], vertices[i + 32]));

                if (intersect_Point != null && !hit) {
                    if (intersect_Point.x < vertices[i + 24] && intersect_Point.x > vertices[i + 27]
                            && intersect_Point.y < vertices[i + 25] && intersect_Point.y > vertices[i + 31]) {
                        tempDist = (float) Math.sqrt(Math.pow((intersect_Point.x - ray[0].x), 2)
                                + Math.pow((intersect_Point.y - ray[0].y), 2)
                                + Math.pow((intersect_Point.z - ray[0].z), 2));
                        distance = (tempDist < distance && hit) || !hit ? tempDist : distance;
                        hit = true;
                    }
                }

                //FRONT FACE
                intersect_Point = rayIntersect(ray[0], ray[1],
                        new Vector3Float(vertices[i + 36], vertices[i + 37], vertices[i + 38]),
                        new Vector3Float(vertices[i + 39], vertices[i + 40], vertices[i + 41]),
                        new Vector3Float(vertices[i + 42], vertices[i + 43], vertices[i + 44]));

                if (intersect_Point != null && !hit) {
                    if (intersect_Point.x < vertices[i + 36] && intersect_Point.x > vertices[i + 39]
                            && intersect_Point.y > vertices[i + 37] && intersect_Point.y < vertices[i + 43]) {
                        tempDist = (float) Math.sqrt(Math.pow((intersect_Point.x - ray[0].x), 2)
                                + Math.pow((intersect_Point.y - ray[0].y), 2)
                                + Math.pow((intersect_Point.z - ray[0].z), 2));
                        distance = (tempDist < distance && hit) || !hit ? tempDist : distance;
                        hit = true;
                    }
                }

                //LEFT FACE
                intersect_Point = rayIntersect(ray[0], ray[1],
                        new Vector3Float(vertices[i + 48], vertices[i + 49], vertices[i + 50]),
                        new Vector3Float(vertices[i + 51], vertices[i + 52], vertices[i + 53]),
                        new Vector3Float(vertices[i + 54], vertices[i + 55], vertices[i + 56]));

                if (intersect_Point != null && !hit) {
                    if (intersect_Point.z > vertices[i + 50] && intersect_Point.z < vertices[i + 53]
                            && intersect_Point.y < vertices[i + 49] && intersect_Point.y > vertices[i + 55]) {
                        tempDist = (float) Math.sqrt(Math.pow((intersect_Point.x - ray[0].x), 2)
                                + Math.pow((intersect_Point.y - ray[0].y), 2)
                                + Math.pow((intersect_Point.z - ray[0].z), 2));
                        distance = (tempDist < distance && hit) || !hit ? tempDist : distance;
                        hit = true;
                    }
                }

                //RIGHT FACE
                intersect_Point = rayIntersect(ray[0], ray[1],
                        new Vector3Float(vertices[i + 60], vertices[i + 61], vertices[i + 62]),
                        new Vector3Float(vertices[i + 63], vertices[i + 64], vertices[i + 65]),
                        new Vector3Float(vertices[i + 69], vertices[i + 70], vertices[i + 71]));

                if (intersect_Point != null && !hit) {
                    if (intersect_Point.z < vertices[i + 62] && intersect_Point.z > vertices[i + 65]
                            && intersect_Point.y < vertices[i + 61] && intersect_Point.y > vertices[i + 67]) {
                        tempDist = (float) Math.sqrt(Math.pow((intersect_Point.x - ray[0].x), 2)
                                + Math.pow((intersect_Point.y - ray[0].y), 2)
                                + Math.pow((intersect_Point.z - ray[0].z), 2));
                        distance = (tempDist < distance && hit) || !hit ? tempDist : distance;
                        hit = true;
                    }
                }

                //AMADOR: If the ray does hit the block, then outline the block
                if (hit) {
                    float[] block = new float[72];
                    System.arraycopy(vertices, i, block, 0, 72);
                    selectedBlocks.put(distance, block);
                }
                hit = false;
            }
            outlineBlock();
        }
    }

    //AMADOR: Determines if a ray intersects a plane. The ray0 and ray1 parameters represent two points of
    //ray line. The P, Q, R parameters represent three points in the plane. In this implementation, the plane
    //is the plane for the current face being tested on a block.
    public static Vector3Float rayIntersect(Vector3Float ray0, Vector3Float ray1,
            Vector3Float P, Vector3Float Q, Vector3Float R) {
        float t;

        //AMADOR:
        //Parametric representation of a 3D line:
        //F(t) = <x0, y0, z0> + t * <x1-x0, y1-y0, z1-z0>
        //     = <x0 + t(x1-x0), y0 + t(y1-y0), z0 + t(z1-z0)>
        //               x            y             z
        //
        //Plane Equation (Note that <a,b,c> is the normal of the plane):
        //f(x,y,z) = ax + by + cz + d
        Vector3Float normal = normal(P, Q, R); //AMADOR: normal is <a,b,c>
        float a = normal.x;
        float b = normal.y;
        float c = normal.z;
        float d = -((-P.x * a) + (-P.y * b) + (-P.z * c));

        t = (-((a * ray0.x + b * ray0.y + c * ray0.z)) + d)
                / ((a * (ray1.x - ray0.x)) + (b * (ray1.y - ray0.y)) + (c * (ray1.z - ray0.z)));

        if (t != 0 && !Float.isInfinite(t)) {
            return new Vector3Float(
                    (ray0.x + (t * (ray1.x - ray0.x))),
                    (ray0.y + (t * (ray1.y - ray0.y))),
                    (ray0.z + (t * (ray1.z - ray0.z)))
            );
        } else {
            return null;
        }
    }

    //AMADOR: Generates a normals buffer for each vertex. This is used to create more realistic lighting.
    private void normals_Buffer(FloatBuffer vertices) {
        VertexNormalsData = BufferUtils.createFloatBuffer(bufferSize);

        for (int i = 0; i < vertices.limit(); i += 12) {
            Vector3Float n = normal(
                    new Vector3Float(vertices.get(i), vertices.get(i + 1), vertices.get(i + 2)),
                    new Vector3Float(vertices.get(i + 3), vertices.get(i + 4), vertices.get(i + 5)),
                    new Vector3Float(vertices.get(i + 6), vertices.get(i + 7), vertices.get(i + 8))
            );

            Vector3Float normalized = normalize(n);
            
            //AMADOR: Since OpenGL needs a normal for each vertex in the vertex buffer, then I am simply
            //adding the same normalized vector for each vertex in the quad.
            VertexNormalsData.put(normalized.x).put(normalized.y).put(normalized.z);
            VertexNormalsData.put(normalized.x).put(normalized.y).put(normalized.z);
            VertexNormalsData.put(normalized.x).put(normalized.y).put(normalized.z);
            VertexNormalsData.put(normalized.x).put(normalized.y).put(normalized.z);
        }

        VertexNormalsData.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, VBONormalsHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexNormalsData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    //AMADOR: This method simply returns the normal (Cross Product) of two vectors.
    public static Vector3Float normal(Vector3Float P, Vector3Float Q, Vector3Float R) {
        Vector3Float PQ = new Vector3Float(Q.x - P.x, Q.y - P.y, Q.z - P.z);
        Vector3Float PR = new Vector3Float(R.x - P.x, R.y - P.y, R.z - P.z);

        return new Vector3Float((PQ.y * PR.z) - (PQ.z * PR.y), (PQ.z * PR.x) - (PQ.x * PR.z),
                (PQ.x * PR.y) - (PQ.y * PR.x));
    }

    //AMADOR: Normalizes a normal.
    public static Vector3Float normalize(Vector3Float v) {
        float length;

        length = (float) Math.sqrt((v.x * v.x) + (v.y * v.y) + (v.z * v.z));

        if (length == 0) {
            length = 1;
        }

        v.x /= length;
        v.y /= length;
        v.z /= length;

        return v;
    }

    //AMADOR: This method outlines a block.
    private void outlineBlock() {
        if (selectedBlocks.isEmpty()) {
            return;
        }

        float[] vertices = selectedBlocks.pollFirstEntry().getValue();

        glLineWidth(3f);
        glColor3f(0, 0, 0);
        glBegin(GL_LINE_LOOP);
        glVertex3f(vertices[0], vertices[1], vertices[2]);
        glVertex3f(vertices[3], vertices[4], vertices[5]);
        glVertex3f(vertices[6], vertices[7], vertices[8]);
        glVertex3f(vertices[9], vertices[10], vertices[11]);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3f(vertices[12], vertices[13], vertices[14]);
        glVertex3f(vertices[15], vertices[16], vertices[17]);
        glVertex3f(vertices[18], vertices[19], vertices[20]);
        glVertex3f(vertices[21], vertices[22], vertices[23]);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3f(vertices[24], vertices[25], vertices[26]);
        glVertex3f(vertices[27], vertices[28], vertices[29]);
        glVertex3f(vertices[30], vertices[31], vertices[32]);
        glVertex3f(vertices[33], vertices[34], vertices[35]);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3f(vertices[36], vertices[37], vertices[38]);
        glVertex3f(vertices[39], vertices[40], vertices[41]);
        glVertex3f(vertices[42], vertices[43], vertices[44]);
        glVertex3f(vertices[45], vertices[46], vertices[47]);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3f(vertices[48], vertices[49], vertices[50]);
        glVertex3f(vertices[51], vertices[52], vertices[53]);
        glVertex3f(vertices[54], vertices[55], vertices[56]);
        glVertex3f(vertices[57], vertices[58], vertices[59]);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3f(vertices[60], vertices[61], vertices[62]);
        glVertex3f(vertices[63], vertices[64], vertices[65]);
        glVertex3f(vertices[66], vertices[67], vertices[68]);
        glVertex3f(vertices[69], vertices[70], vertices[71]);
        glEnd();
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
        return new float[]{1, 0.5f, 0.5f};
    }

    public static float[] createTexCube(float x, float y, Block.BlockType blockType) {
        float offset = (1024f / 16) / 1024f;
        int x1 = 0, x2, y1 = 0, y2;

        switch (blockType) {
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
                x1 = 2;
                y1 = 0;
                break;
            case Stone:
                x1 = 1;
                y1 = 0;
                break;
            case Water:
                x1 = 13;
                y1 = 12;
                break;
            case Lily:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 3, y + offset * 3,
                    x + offset * 2, y + offset * 3,
                    x + offset * 2, y + offset * 2,
                    x + offset * 3, y + offset * 2,
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
                x1 = 1;
                y1 = 1;
                break;
            case Sand:
                x1 = 2;
                y1 = 1;
                break;
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
            case Leaf:
                x1 = 5;
                y1 = 3;
                break;
            case Cacti:
                x1 = 6;
                y1 = 4;
                break;
            case Snow:
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
                    x + offset * 15, y + offset * 3,
                    x + offset * 16, y + offset * 3,
                    x + offset * 16, y + offset * 4,
                    x + offset * 15, y + offset * 4,
                    // BACK QUAD
                    x + offset * 16, y + offset * 4,
                    x + offset * 15, y + offset * 4,
                    x + offset * 15, y + offset * 3,
                    x + offset * 16, y + offset * 3,
                    // LEFT QUAD
                    x + offset * 15, y + offset * 3,
                    x + offset * 16, y + offset * 3,
                    x + offset * 16, y + offset * 4,
                    x + offset * 15, y + offset * 4,
                    // RIGHT QUAD
                    x + offset * 15, y + offset * 3,
                    x + offset * 16, y + offset * 3,
                    x + offset * 16, y + offset * 4,
                    x + offset * 15, y + offset * 4
                };
            case Pumkin:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 13, y + offset * 6,
                    x + offset * 14, y + offset * 6,
                    x + offset * 14, y + offset * 7,
                    x + offset * 13, y + offset * 7,
                    // TOP!
                    x + offset * 13, y + offset * 6,
                    x + offset * 14, y + offset * 6,
                    x + offset * 14, y + offset * 7,
                    x + offset * 13, y + offset * 7,
                    // FRONT QUAD
                    x + offset * 11, y + offset * 6,
                    x + offset * 12, y + offset * 6,
                    x + offset * 12, y + offset * 7,
                    x + offset * 11, y + offset * 7,
                    // BACK QUAD
                    x + offset * 13, y + offset * 7,
                    x + offset * 12, y + offset * 7,
                    x + offset * 12, y + offset * 6,
                    x + offset * 13, y + offset * 6,
                    // LEFT QUAD
                    x + offset * 12, y + offset * 6,
                    x + offset * 13, y + offset * 6,
                    x + offset * 13, y + offset * 7,
                    x + offset * 12, y + offset * 7,
                    // RIGHT QUAD
                    x + offset * 12, y + offset * 6,
                    x + offset * 13, y + offset * 6,
                    x + offset * 13, y + offset * 7,
                    x + offset * 12, y + offset * 7
                };
            case JackO:
                return new float[]{
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset * 13, y + offset * 6,
                    x + offset * 14, y + offset * 6,
                    x + offset * 14, y + offset * 7,
                    x + offset * 13, y + offset * 7,
                    // TOP!
                    x + offset * 13, y + offset * 6,
                    x + offset * 14, y + offset * 6,
                    x + offset * 14, y + offset * 7,
                    x + offset * 13, y + offset * 7,
                    // FRONT QUAD
                    x + offset * 10, y + offset * 6,
                    x + offset * 11, y + offset * 6,
                    x + offset * 11, y + offset * 7,
                    x + offset * 10, y + offset * 7,
                    // BACK QUAD
                    x + offset * 13, y + offset * 7,
                    x + offset * 12, y + offset * 7,
                    x + offset * 12, y + offset * 6,
                    x + offset * 13, y + offset * 6,
                    // LEFT QUAD
                    x + offset * 12, y + offset * 6,
                    x + offset * 13, y + offset * 6,
                    x + offset * 13, y + offset * 7,
                    x + offset * 12, y + offset * 7,
                    // RIGHT QUAD
                    x + offset * 12, y + offset * 6,
                    x + offset * 13, y + offset * 6,
                    x + offset * 13, y + offset * 7,
                    x + offset * 12, y + offset * 7
                };
            default: // Currently default is dirt
                x1 = 2;
                y1 = 0;
                break;
        }

        x2 = x1 + 1;
        y2 = y1 + 1;

        return new float[]{
            // BOTTOM QUAD(DOWN=+Y)
            x + offset * x2, y + offset * y2,
            x + offset * x1, y + offset * y2,
            x + offset * x1, y + offset * y1,
            x + offset * x2, y + offset * y1,
            // TOP!
            x + offset * x2, y + offset * y2,
            x + offset * x1, y + offset * y2,
            x + offset * x1, y + offset * y1,
            x + offset * x2, y + offset * y1,
            // FRONT QUAD
            x + offset * x1, y + offset * y1,
            x + offset * x2, y + offset * y1,
            x + offset * x2, y + offset * y2,
            x + offset * x1, y + offset * y2,
            // BACK QUAD
            x + offset * x2, y + offset * y2,
            x + offset * x1, y + offset * y2,
            x + offset * x1, y + offset * y1,
            x + offset * x2, y + offset * y1,
            // LEFT QUAD
            x + offset * x1, y + offset * y1,
            x + offset * x2, y + offset * y1,
            x + offset * x2, y + offset * y2,
            x + offset * x1, y + offset * y2,
            // RIGHT QUAD
            x + offset * x1, y + offset * y1,
            x + offset * x2, y + offset * y1,
            x + offset * x2, y + offset * y2,
            x + offset * x1, y + offset * y2
        };
    }
}
