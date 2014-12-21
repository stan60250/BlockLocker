package nl.rutgerkok.chestsignprotect.impl;

import java.util.Collection;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.google.common.collect.ImmutableList;

/**
 * Represents a single or double door. Used to find all blocks of a door, as
 * well to open or close a door.
 *
 */
public final class Door {

    private static boolean isTopHalf(Block doorBlock) {
        @SuppressWarnings("deprecation")
        byte data = doorBlock.getData();
        return (data & 0x8) != 0;
    }

    private static boolean isHingeOnLeftSide(Block topHalfDoorBlock) {
        @SuppressWarnings("deprecation")
        byte data = topHalfDoorBlock.getData();
        return (data & 0x1) == 0;
    }

    private static BlockFace getFaceToRightDoor(Block bottomHalfDoorBlock) {
        @SuppressWarnings("deprecation")
        byte data = bottomHalfDoorBlock.getData();
        switch (data & 0x3) {
            case 0:
                return BlockFace.SOUTH;
            case 1:
                return BlockFace.WEST;
            case 2:
                return BlockFace.NORTH;
            case 3:
                return BlockFace.EAST;
            default:
                throw new AssertionError("Error: " + data);
        }
    }

    private static BlockFace getFaceToLeftDoor(Block bottomHalfDoorBlock) {
        return getFaceToRightDoor(bottomHalfDoorBlock).getOppositeFace();
    }

    @Nullable
    private static Block ofMaterialOrNull(Material requiredMaterial, Block block) {
        if (block.getType() == requiredMaterial) {
            return block;
        }
        return null;
    }

    @Nullable
    private final Block topLeftBlock;
    @Nullable
    private final Block topRightBlock;
    @Nullable
    private final Block bottomLeftBlock;
    @Nullable
    private final Block bottomRightBlock;

    /**
     * Creates a new door. The given block must be part of the door.
     * 
     * @param doorBlock
     *            A block that is part of the door.
     */
    public Door(Block doorBlock) {
        Block topBlock;
        Block bottomBlock;
        Material doorMaterial = doorBlock.getType();
        if (isTopHalf(doorBlock)) {
            // Top half
            topBlock = doorBlock;
            bottomBlock = doorBlock.getRelative(BlockFace.DOWN);
        } else {
            // Bottom half
            bottomBlock = doorBlock;
            topBlock = doorBlock.getRelative(BlockFace.UP);
        }

        if (isHingeOnLeftSide(topBlock)) {
            // Hinge on the left, there may be another door on the right
            topLeftBlock = topBlock;
            bottomLeftBlock = bottomBlock;

            BlockFace faceToRightDoor = getFaceToRightDoor(bottomBlock);
            topRightBlock = ofMaterialOrNull(doorMaterial, topLeftBlock.getRelative(faceToRightDoor));
            bottomRightBlock = ofMaterialOrNull(doorMaterial, bottomLeftBlock.getRelative(faceToRightDoor));
        } else {
            // Hinge on the right, there may be another door on the left
            topRightBlock = topBlock;
            bottomRightBlock = bottomBlock;

            BlockFace faceToLeftDoor = getFaceToLeftDoor(bottomBlock);
            topLeftBlock = ofMaterialOrNull(doorMaterial, topRightBlock.getRelative(faceToLeftDoor));
            bottomLeftBlock = ofMaterialOrNull(doorMaterial, bottomRightBlock.getRelative(faceToLeftDoor));
        }
    }

    /**
     * Opens or closes the door.
     * 
     * @param open
     *            Whether the door must be opened (true) or closed (false).
     */
    @SuppressWarnings("deprecation")
    private void setOpen(boolean open) {
        if (open) {
            // Open door
            if (bottomLeftBlock != null) {
                bottomLeftBlock.setData((byte) (bottomLeftBlock.getData() | 0x4));
            }
            if (bottomRightBlock != null) {
                bottomRightBlock.setData((byte) (bottomRightBlock.getData() | 0x4));
            }
        } else {
            // Close door
            if (bottomLeftBlock != null) {
                bottomLeftBlock.setData((byte) (bottomLeftBlock.getData() & ~0x4));
            }
            if (bottomRightBlock != null) {
                bottomRightBlock.setData((byte) (bottomRightBlock.getData() & ~0x4));
            }
        }
    }

    /**
     * Gets a collection of all blocks where attached protection signs are used
     * for this door.
     *
     * @return All blocks that can have protection signs attached.
     */
    public Collection<Block> getBlocksForSigns() {
        ImmutableList.Builder<Block> blocks = ImmutableList.builder();
        if (bottomLeftBlock != null) {
            blocks.add(bottomLeftBlock);
            blocks.add(bottomLeftBlock.getRelative(BlockFace.DOWN));
        }
        if (bottomRightBlock != null) {
            blocks.add(bottomRightBlock);
            blocks.add(bottomRightBlock.getRelative(BlockFace.DOWN));
        }
        if (topLeftBlock != null) {
            blocks.add(topLeftBlock);
            blocks.add(topLeftBlock.getRelative(BlockFace.UP));
        }
        if (topRightBlock != null) {
            blocks.add(topRightBlock);
            blocks.add(topRightBlock.getRelative(BlockFace.UP));
        }
        return blocks.build();
    }

    /**
     * Closes the door if the door is open, opens the door if the door is
     * closed. If parts of the door is opened, parts is closed then the whole
     * door will be in one state after calling this method, but it is unknown in
     * which state.
     */
    public void toggleOpen() {
        setOpen(!isOpen());
    }

    @SuppressWarnings("deprecation")
    private boolean isOpen() {
        if (bottomRightBlock != null) {
            return (bottomRightBlock.getData() & 0x4) != 0;
        }
        if (bottomLeftBlock != null) {
            return (bottomLeftBlock.getData() & 0x4) != 0;
        }
        return false;
    }

}