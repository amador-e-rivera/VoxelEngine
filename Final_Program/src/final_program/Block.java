/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

/**
 *
 * @author Amador
 */
public class Block {

    private BlockType blockType;
    private boolean active;
    private float x, y, z;

    public enum BlockType {

        Grass(0),
        Dirt(1),
        Stone(2),
        Water(3),
        BedRock(4),
        Sand(5),
        Wood(6);

        private int blockID;

        BlockType(int id) {
            blockID = id;
        }

        public int getID() {
            return blockID;
        }

        public void setID(int id) {
            blockID = id;
        }
    }

    public Block(BlockType blockType) {
        this.blockType = blockType;
    }
    
    public void setCoords(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public int getID() {
        return blockType.getID();
    }
}
