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

    public enum BlockType {

        Grass(0),
        Dirt(1),
        Stone(2),
        Water(3),
        BedRock(4),
        Sand(5),
        Wood(6),
        Leaf(7),
        Cacti(8),
        Snow(9),
        Lily(10),
        Pumkin(11),
        JackO(12);

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
    public BlockType getBlockType() {
        return blockType;
    }

    public Block(BlockType blockType) {
        this.blockType = blockType;
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
    
    //AMADOR: Added this method which will be useful to set the block on the map depending on its position.
    //For example, if the max y for the current xz position is 5, then we could use this to set the block
    //as Water. If the max y for the current xz poition is 6, then we can set that as a Sand block...etc
    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }
}
