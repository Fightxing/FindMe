package com.buuz135.findme;

import java.awt.*;

public class FindMeConfig {

    public Common COMMON = new Common();
    public Client CLIENT = new Client();


    public static class Client {

        public int CONTAINER_TRACK_TIME = 30 * 20;
        public boolean CONTAINER_TRACKING = true;
        public boolean SNAP_TO_CONTAINER = true;
        public String CONTAINER_HIGHLIGHT_COLOR = "#cf9d15";
        private transient Color currentColor = null;

        // Laser highlight config
        public int LASER_DURATION = 100; // ticks (5 seconds)
        public boolean LASER_THROUGH_WALLS = true;
        public String BLOCK_LASER_COLOR = "#FF2200";
        private transient Color blockLaserColor = null;
        public String ITEM_LASER_COLOR = "#AA44FF";
        private transient Color itemLaserColor = null;
        public String ENTITY_LASER_COLOR = "#FFD700";
        private transient Color entityLaserColor = null;

        public Color getColor() {
            if (currentColor == null) {
                try {
                    currentColor = Color.decode(CONTAINER_HIGHLIGHT_COLOR.toLowerCase());
                } catch (NumberFormatException e) {
                    currentColor = Color.decode("#cf9d15");
                }
            }
            return currentColor;
        }

        public Color getBlockLaserColor() {
            if (blockLaserColor == null) {
                try {
                    blockLaserColor = Color.decode(BLOCK_LASER_COLOR.toLowerCase());
                } catch (NumberFormatException e) {
                    blockLaserColor = Color.decode("#FF2200");
                }
            }
            return blockLaserColor;
        }

        public Color getItemLaserColor() {
            if (itemLaserColor == null) {
                try {
                    itemLaserColor = Color.decode(ITEM_LASER_COLOR.toLowerCase());
                } catch (NumberFormatException e) {
                    itemLaserColor = Color.decode("#AA44FF");
                }
            }
            return itemLaserColor;
        }

        public Color getEntityLaserColor() {
            if (entityLaserColor == null) {
                try {
                    entityLaserColor = Color.decode(ENTITY_LASER_COLOR.toLowerCase());
                } catch (NumberFormatException e) {
                    entityLaserColor = Color.decode("#FFD700");
                }
            }
            return entityLaserColor;
        }


    }

    public static class Common {
        public int RADIUS_RANGE = 8;
        public boolean IGNORE_ITEM_DAMAGE = false;
        public boolean SEARCH_ITEM_ENTITIES = true;
        public boolean SEARCH_ENTITY_INVENTORIES = true;

    }
}
