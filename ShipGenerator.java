//ShipGenerator.java
//C. Bikle
//COS327, Project 5: Battleship
//Spring '13, 04/19/13
package battleshipserver;

import java.util.Random;

public class ShipGenerator {

   private static final char SEPARATOR_TOKEN = '&';
   private int shipsPerPlayer;
   private int numberOfPlayers;
   private int[] lengths;
   /*Lengths is an array of ship lengths, e.g., 1 sub, 2 pt boats, &
    * 3 destroyers would be {1, 2, 2, 3, 3, 3}.
    • The length of lengths must match shipsPerPlayer */
   private int[][] map;
   private int[][][] ships;
   private int shipNumber;

   public ShipGenerator(int numberOfPlayers, int shipsPerPlayer, int[] lengths, int mapSizeRows, int mapSizeColumns) {
      this.numberOfPlayers = numberOfPlayers;
      this.shipsPerPlayer = shipsPerPlayer;
      this.lengths = lengths;
      map = new int[mapSizeRows][mapSizeColumns];
      mapInit();
      int longestShip = findLongestShip();
      ships = new int[shipsPerPlayer * numberOfPlayers][longestShip][2];
      shipNumber = 0;
      shipsInit();
      generateShips();
   }

   private void mapInit() {
      for (int i = 0; i < map.length; i++) {
         for (int j = 0; j < map[0].length; j++) {
            map[i][j] = -1;
         }
      }
   }

   private void shipsInit() {
      for (int i = 0; i < ships.length; i++) {
         for (int j = 0; j < ships[0].length; j++) {
            for (int m = 0; m < ships[0][0].length; m++) {
               ships[i][j][m] = -1;
            }
         }
      }
   }

   private int findLongestShip() {
      int longest = 0;

      for (int x : lengths) {
         if (x > longest) {
            longest = x;
         }
      }

      return longest;
   }

   private void generateShips() {
      long seed = System.currentTimeMillis();
      Random rand = new Random(seed);

      for (int i = 0; i < numberOfPlayers; i++) {//each player
         for (int j = 0; j < shipsPerPlayer; j++) {//each ship
            startShip(rand, shipNumber, lengths[lengths.length - (j + 1)]);
            shipNumber++;
         }
      }
   }

   //pick row and col
   //if pick direction
   //extend ship in that direction
   //if blocked, remove extension, pick new direction
   //if all directions blocked, remove start point and restart
   private void /*boolean*/ startShip(Random r, int shipNum, int lengthLeft) {//need to add code to terminate if taking too long?
      int startPoint[];
      int direction;
      int tryNumber = 1;
      do {
         startPoint = pickPoint(r);
         direction = pickDirection(r);
         //System.out.println("ship #"+shipNum+", try #"+(tryNumber++) + ", row:"+startPoint[0]+", col:"+startPoint[1]);
      } while (!placeShip(shipNum, startPoint[0], startPoint[1], direction, lengthLeft));

      //return false;
   }

   private boolean placeShip(int shipNum, int row, int col, int direction, int lengthLeft) {
      //System.out.println("trying:"+row+", "+col+"; lengthLeft:"+lengthLeft);
      if (lengthLeft == 0) {
         return true;
      } else {
         if (!isValid(row, col)) {
            //System.out.println("invalid:"+row+", "+col);
            return false;
         } else {
            //System.out.println("valid:"+row+", "+col);
            map[row][col] = shipNum;

            int[] directionA = convertDirection(direction);
            if (placeShip(shipNum, (row + directionA[0]), (col + directionA[1]), direction, lengthLeft - 1)) {
               ships[shipNum][lengthLeft-1][0] = row;
               ships[shipNum][lengthLeft-1][1] = col;
                       //ship#, compartment#, row | col = row | col
               return true;
            } else {
               //System.out.println("lower level was invalid");
               map[row][col] = -1;
               return false;
            }
         }
      }
   }

   private int[] pickPoint(Random r) {
      int[] point = new int[2];
      point[0] = r.nextInt(map.length);
      point[1] = r.nextInt(map[0].length);

      return point;
   }

   private int pickDirection(Random r) {

      return r.nextInt(4);//0 == north (up), 1 == south (down), 2 == east (right), 3 == west (left)
   }

   private int[] convertDirection(int d) {
      int[] direction = new int[2];
      switch (d) {
         case 0:
            direction[0] = -1;
            direction[1] = 0;
            break;
         case 1:
            direction[0] = 1;
            direction[1] = 0;
            break;
         case 2:
            direction[0] = 0;
            direction[1] = 1;
            break;
         case 3:
            direction[0] = 0;
            direction[1] = -1;
            break;
      }

      return direction;
   }

   private boolean isValid(int row, int col) {
      boolean valid = false;
      if (isInbounds(row, col) && !isOverlap(row, col)) {
         valid = true;
      }

      return valid;
   }

   private boolean isInbounds(int row, int col) {
      if ((row > -1 && row < map.length) && (col > -1 && col < map[0].length)) {
         return true;
      } else {
         return false;
      }
   }

   private boolean isOverlap(int row, int col) {
      if (map[row][col] != -1) {
         return true;
      } else {
         return false;
      }
   }

   public String getMapAsString() {
      String s = "";

      for (int i = 0; i < map.length; i++) {
         for (int j = 0; j < map[0].length; j++) {
            String c = ".";
            int x = map[i][j];
            if (x > -1) {
               c = x + "";
            }
            int padNeeded = 3 - c.length();
            for (int m = 0; m < padNeeded; m++) {
               c = " " + c;
            }
            s += c;
         }
         s += '\n';
      }

      return s;
   }
   
   public String[] getShipsAsStrings() {
      String shipStrings[] = new String[ships.length];

      for(int i = 0; i < ships.length; i++) {
         shipStrings[i] = "";
         for(int j = 0; j < ships[0].length; j++) {
            if(ships[i][j][0] > -1) {
               shipStrings[i] += ships[i][j][0];
               shipStrings[i] += SEPARATOR_TOKEN;
               shipStrings[i] += ships[i][j][1];
               
               if(j+1 < ships[0].length) {
                  if(ships[i][j][0] > -1) {
                     shipStrings[i] += SEPARATOR_TOKEN;
                  }
               }
            }
         }
      }

      return shipStrings;
   }

   public static void main(String args[]) {
      long startMillis = System.currentTimeMillis();
      int players = 12;
      int[] lens = {3, 3, 3, 3, 3};
      int shipsPerPlayer = lens.length;
      int rows = 26;
      int cols = 39;
      ShipGenerator sg = new ShipGenerator(players, shipsPerPlayer, lens, rows, cols);
      //System.out.println(sg.getMapAsString());
      
      long endMillis = System.currentTimeMillis();
		double durationSecs = ((double)(endMillis - startMillis))/1000d;
		System.out.println("elapsed time in seconds: "+durationSecs);
      
 /*     String[] fleets = sg.getShipsAsStrings();
      for(int i = 0; i < fleets.length; i++) {
         System.out.println("ship#"+i+": "+fleets[i]);
      }*/
   }
}
