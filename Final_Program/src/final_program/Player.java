/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

import java.util.LinkedList;
import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author Amador
 */
public class Player {

    private Block block;
    private float[] vertices;
    private float[] tex;
    private LinkedList<Block.BlockType> blocks;

    public Player(float x, float y, float z) {
        block = new Block(Block.BlockType.Grass);
        vertices = Chunk.createCube(x - 2, y - 3, z - 1);
        tex = Chunk.createTexCube(0, 0, block);

        blocks = new LinkedList<>();

        blocks.add(Block.BlockType.Grass);
        blocks.add(Block.BlockType.Dirt);
        blocks.add(Block.BlockType.Stone);
        blocks.add(Block.BlockType.Water);
        blocks.add(Block.BlockType.Sand);
        blocks.add(Block.BlockType.BedRock);
        blocks.add(Block.BlockType.Wood);
    }

    public void updateBlockType(int direction) {
        if (direction < 0) {
            blocks.addLast(blocks.poll());
            block = new Block(blocks.peekFirst());
        } else {
            blocks.addFirst(blocks.pollLast());
            block = new Block(blocks.peekFirst());
        }
        tex = Chunk.createTexCube(0, 0, block);
    }

    public void render() {
        int count = 0;

        glPushMatrix();
        glLoadIdentity();
        glColor3f(1.0f, 1.0f, 1.0f);
        glBegin(GL_QUADS);

        for (int i = 0; i < vertices.length; i += 3) {
            glTexCoord2f(tex[count], tex[count + 1]);
            glVertex3f(vertices[i], vertices[i + 1], vertices[i + 2]);

            count += 2;
        }

        glEnd();
        glPopMatrix();
    }
}
