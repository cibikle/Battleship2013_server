updated 04/19/13 2243

manifest:
•ShipGenerator.java--a no-hassle ship-gen module: instantiate, getShipsAsStrings(), and you're done.

--------------------
API(s):
•ShipGenerator.java

 constructor summary:
  ShipGenerator(int numberOfPlayers, int shipsPerPlayer, int[] shipLengths, int mapSizeRows, int mapSizeColumns)
   •notes: shipsPerPlayers must match the length of shipLengths

 method summary:
  String[] | getShipsAsStrings()
   •notes: returns an array of Strings; each String contains the coordinates of each section of ship as int row&col pairs
           each row and column is separated by an ampersand, as is each pair; there is no trailing ampersand
  String | getMapAsString()
   •notes: returns a String representation of the map created as part of the ship generation
           indended for debugging use, but could be used as basis for a no-gui client
