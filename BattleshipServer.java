//BattleshipServer.java
//C. Bikle
//COS327, Project 5: Battleship
//Spring '13
//04/10/13
package battleshipserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BattleshipServer {

   /* CODES */
   /* client to server */
   public static final String ELO = "ELO";
   public static final String MSG = "MSG";
   public static final String FIR = "FIR";
   public static final String BYE = "BYE";
   /* server to client */
   public static final String PLAYER_MESSAGE = "001";
   public static final String SYSTEM_MESSAGE = "002";
   public static final String ACK_FIR_MISS = "100";
   public static final String ACK_FIR_HIT = "150";
   public static final String ACK_FIR_SELFHIT = "151";
   public static final String ACK_FIR_SUNK = "190";
   public static final String ACK_CONNECTION = "220";
   public static final String ACK_ELO_ACCEPT = "310";
   public static final String ACK_ELO_REJECT_NAMETAKEN = "351";
   public static final String ACK_ELO_REJECT_NOROOM = "390";
   public static final String SHIP_HIT = "500";
   public static final String SHIP_SUNK = "505";
   public static final String SHIPS_SUNK_ALL = "555";
   public static final String START_GAME = "800";
   public static final String ACK_BYE = "900";
   public static final String GAMEOVER_PLAYERSLEFT = "990";
   public static final String GAMEOVER_WON = "999";
   public static final String CRLF = "\r\n";
   public static final String PROTOTCOL_VERSION = "bsP/2013";
   public static final int lengthOfCmds = 3;
   /* members */
   private static final String SEPARATOR_TOKEN = "&";
   private static final String TEMP_TOKEN = "#";
   private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   private static final int MAP_WIDTH = 39;//number of columns
   private static final int MAP_HEIGHT = 26;//number of rows
   private int firingDelay = 5000;//in millis, so 5 seconds
   private static int timeout = 180000;
   private static int port = 8060;
   private ServerSocket listenSocket = null;
   private boolean isFirstPlayer = true;
   private boolean run = true;
   private int numberOfPlayers;
   private int connectedPlayers = 0;
   private int[] lengths = {3, 3, 3, 3, 3};
   private int shipsPerPlayer = lengths.length;
   private Player[] players = null;
   private Ship[] ships = null;
   private Compartment[][] map;
   private final ConcurrentLinkedQueue<Message> msgQueue;
   private ConnectionAccepter receptionist;
   private Thread receptionistThread;
   private ShipGenerator shipGen;
   private Player server;
   private boolean gameStarted = false;
   private int playersOut = 0;
   private int highestScore = 0;
   private final int POINTS_HIT = 1;
   private final int POINTS_SUNK = 1;
   private final int POINTS_LAST_SUNK = 1;
//   private final int POINTS_SELFHIT = 0;
//   private final int POINTS_MISS = 0;
   private final int PENALTY_HIT = 0;
   private final int PENALTY_SUNK = 0;
   private final int PENALTY_LAST_SUNK = 0;
   private final int PENALTY_SELFHIT = 0;
   private final int PENALTY_MISS = 0;
   private static final boolean GAME_WON = true;
   private static final boolean GAME_ABANDONDED = false;
   private boolean gameEnded = false;
   private boolean gameStartAnnounced = false;

   public static void main(String[] args) {
      if (args.length == 1) {
         try {
            port = Integer.parseInt(args[0]);
            System.out.println("Using port " + port);
         } catch (NumberFormatException e) {
            //e.printStackTrace();
            System.out.println("Using default port " + port);
         }
      } else {
         System.out.println("Using default port " + port);
      }

      BattleshipServer bss = null;
      try {
         bss = new BattleshipServer();
      } catch (IOException ex) {
         Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
         System.exit(1);
      }
   }

   public BattleshipServer() throws IOException {
      server = new Player();
      server.setName("SERVER");

      msgQueue = new ConcurrentLinkedQueue<Message>();
      listenSocket = new ServerSocket(port);
      runAcceptor();
      handleQueue();
   }
   
   private void mapInit() {
      map = new Compartment[MAP_HEIGHT][MAP_WIDTH];//rows THEN columns
   }

   private void runAcceptor() {
      receptionist = new ConnectionAccepter(listenSocket);
      receptionistThread = new Thread(receptionist);
      receptionistThread.start();
   }

   private void handleQueue() {
      while (run) {
         Message m = msgQueue.poll();
         while (m != null) {
            if (BYE.equals(m.getCommand())) {
               System.out.println("BYE received");
               signOut(m);
            } else if (m.getCommand().equals(ELO)) {
               System.out.println("ELO received");
               signIn(m);

               if (gameStarted && !gameStartAnnounced) {
                  startGame();
               }
            } else if (m.getCommand().equals(MSG)) {
               System.out.println("MSG received");
               System.out.println(Arrays.deepToString(m.getArgs()));

               sendMsgToPlayers(m);
            } else if (m.getCommand().equals(FIR)) {
               System.out.println("FIR received");
               System.out.println(Arrays.deepToString(m.getArgs()));

               fire(m);
            }

            m = msgQueue.poll();
         }

         try {
            System.out.println(Thread.currentThread().getName() + ": About to wait");
            synchronized (msgQueue) {
               msgQueue.wait();
            }
         } catch (InterruptedException ex) {
            Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
   }

   private void fire(Message m) {
      //get coordinates from message
      //lookup
      //if hit
      //set attackermsg to 'hit'
      //set compartment to hit
      //set defendermsg to 'hit'
      //check if ship is sunk
      //set ship to sunk
      //set attackermsg to 'sunk'
      //set defendermsg to 'sunk'
      //check if all ships sunk
      //set defendermsg to 'all-sunk'
      //check if ship is shooter's
      //set attackermsg to 'self-hit'
      //inform shipOwner
      //else
      //set attackermsg to 'miss'
      //inform attacker

      if (!gameStarted) {
         System.out.println("game not started");
         return;
      }

      System.out.println(Arrays.deepToString(m.getArgs()));

      String attackerMsg;
      String defenderMsg;

      int attackerPoints;
      int defenderPoints;

      int row = alphabet.indexOf(m.getArgs()[0]);
      int col = Integer.parseInt(m.getArgs()[1]);
      System.out.println("map:[" + alphabet.charAt(row) + "][" + col + "]:" + map[row][col]);
      if (map[row][col] != null && !map[row][col].isHit()) {//if it's a hit
         System.out.println("HIT");
         map[row][col].setHit();

         String shipOwnerName = map[row][col].getShip().getOwner();
         Player shipOwner = lookupPlayer(shipOwnerName);

         if (map[row][col].getShip().isSunk()) {
            System.out.println("SUNK");
            attackerMsg = ACK_FIR_SUNK;
            attackerPoints = POINTS_SUNK;
            if (shipOwner.allShipsSunk()) {
               System.out.println("SUNK ALL");
               attackerPoints = POINTS_LAST_SUNK;
               defenderMsg = SHIPS_SUNK_ALL;
               defenderPoints = PENALTY_LAST_SUNK;

               playersOut++;
               if (playersOut == (connectedPlayers - 1)) {
                  endGame(GAME_WON);
               }
            } else {
               defenderMsg = SHIP_SUNK;
               defenderPoints = PENALTY_SUNK;
            }
         } else {
            attackerMsg = ACK_FIR_HIT;
            attackerPoints = POINTS_HIT;
            defenderMsg = SHIP_HIT;
            defenderPoints = PENALTY_HIT;
         }

         if (shipOwnerName.equals(m.getSender().getName())) {
            System.out.println("SELFHIT");
            attackerMsg = ACK_FIR_SELFHIT;
            attackerPoints = PENALTY_SELFHIT;
         }
         shipOwner.updateScore(defenderPoints);
         try {
            shipOwner.sendMessageToPlayer(defenderMsg + " " + m.getArgs()[0]
                    + SEPARATOR_TOKEN + m.getArgs()[1] + SEPARATOR_TOKEN
                    + shipOwner.getScore() + CRLF);
         } catch (IOException ex) {
            Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
         }

         if (shipOwner.getScore() > highestScore) {
            highestScore = shipOwner.getScore();
         }
      } else {//it's a miss
         attackerMsg = ACK_FIR_MISS;
         attackerPoints = PENALTY_MISS;
      }

      m.getSender().updateScore(attackerPoints);
      try {
         String c = attackerMsg + " " + m.getArgs()[0]
                 + SEPARATOR_TOKEN + m.getArgs()[1] + SEPARATOR_TOKEN
                 + m.getSender().getScore();
         m.getSender().sendMessageToPlayer(c + CRLF);
      } catch (IOException ex) {
         Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
      }

      if (m.getSender().getScore() > highestScore) {
         highestScore = m.getSender().getScore();
      }
   }

   private Player lookupPlayer(String name) {
      for (Player t : players) {
         if (t.getName().equals(name)) {
            return t;
         }
      }

      return null;
   }

   private void endGame(boolean endType) {
      gameStarted = false;
      gameEnded = true;
      String msg;

      if (endType == GAME_WON) {//true = someone won; false = everyone else left
         msg = GAMEOVER_WON + " " + tallyScores();
      } else {
         msg = GAMEOVER_PLAYERSLEFT;
      }

      sendMessageToPlayers(msg);

//      System.exit(0);
      
      resetServer();
   }
   
   private void resetServer() {
      mapInit();
      isFirstPlayer = true;
      gameStarted = false;
      gameEnded = false;
      gameStartAnnounced = false;
   }

   private String tallyScores() {//expand later for all players' scores
      String scorer = "";
      int highest = 0;
      for (Player p : players) {
         if (p != null && p.getScore() > highest) {
            highest = p.getScore();
            scorer = p.getName();
         }
      }
      scorer += " - " + highest;

      return scorer;
   }

   private void startGame() {
      sendMessageToPlayers(START_GAME);
      gameStartAnnounced = true;
   }

   private void signOut(Message m) {
      try {
         m.getSender().sendMessageToPlayer(ACK_BYE + CRLF);
      } catch (IOException ex) {
         Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
      }

      if (m.getSender().isRegistered()) {
         System.out.println(m.getSender().getName() + " is registered");
         connectedPlayers--;

         if (connectedPlayers == 1 && gameStarted) {
            //System.out.println("&&&&&&");
            endGame(GAME_ABANDONDED);
         } else if (connectedPlayers == 0) {
            resetServer();
         }

         Ship[] tmpShips = m.getSender().getShips();
         if (gameStarted) {
            for (int i = 0; i < tmpShips.length; i++) {
               tmpShips[i].setSunk(true);
            }
         } else {
            //reset ships to have no owner
            for (int i = 0; i < tmpShips.length; i++) {
               tmpShips[i].setOwner("");
            }
         }

         //null the player in Players
         for (int i = 0; i < players.length; i++) {
            if (players[i] != null && players[i].getName().equals(m.getSender().getName())) {
               System.out.println(m.getSender().getName() + " removed");
               players[i] = null;
               i = players.length;
            }
         }

         m.getSender().setRegistered(false);

         String[] disconMsg = {"Player " + m.getSender().getName() + " disconnected."};
         sendMsgToPlayers(new Message(server, MSG, disconMsg));
      }
   }

   private void signIn(Message m) {
      System.out.println(m);

      if (isFirstPlayer) {
         System.out.println("I am the first player to send an ELO");
         isFirstPlayer = false;
         numberOfPlayers = Integer.parseInt(m.getArgs()[1]);
         players = new Player[numberOfPlayers];
         ships = new Ship[numberOfPlayers * shipsPerPlayer];
         System.out.println("#players:" + numberOfPlayers + "; sPP:" + shipsPerPlayer + "; ships:" + ships.length);
         shipGenHandler();
      }

      if (connectedPlayers < numberOfPlayers && !gameStarted && !gameEnded) {
         if (!nameTaken(m.getArgs()[0])) {
            m.getSender().setName(m.getArgs()[0]);
            int playerSlot = findFirstOpenPlace();
            try {
               players[playerSlot] = m.getSender();
            } catch (ArrayIndexOutOfBoundsException e) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, "Somehow a player was admitted to the game when there was no room.", e);

               try {
                  m.getSender().sendMessageToPlayer(ACK_ELO_REJECT_NOROOM + " No Room or Game Has Started" + CRLF);
               } catch (IOException ex) {
                  Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
               }
               return;
            }
            connectedPlayers++;

            String[] args = {"Player " + m.getSender().getName() + " joined the game", connectedPlayers + ""};
            sendMsgToPlayers(new Message(server, MSG, args));

            Ship[] tmpShips = new Ship[shipsPerPlayer];
            for (int i = 0; i < tmpShips.length; i++) {
               tmpShips[i] = ships[i + ((playerSlot) * shipsPerPlayer)];
            }
            players[playerSlot].setShips(tmpShips);

            String message = ACK_ELO_ACCEPT + " " + numberOfPlayers
                    + SEPARATOR_TOKEN + firingDelay + SEPARATOR_TOKEN
                    + shipsPerPlayer;

            for (Ship t : tmpShips) {
               message += SEPARATOR_TOKEN + t.getLocation();
            }

            try {
               m.getSender().sendMessageToPlayer(message + CRLF);
            } catch (IOException ex) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (connectedPlayers == numberOfPlayers) {
               gameStarted = true;
               System.out.println("Game starting");
            }
         } else {
            try {
               m.getSender().sendMessageToPlayer(ACK_ELO_REJECT_NAMETAKEN + " Name Taken" + CRLF);
            } catch (IOException ex) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
      } else {
         try {
            m.getSender().sendMessageToPlayer(ACK_ELO_REJECT_NOROOM + " No Room or Game Has Started" + CRLF);
         } catch (IOException ex) {
            Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
   }

   private int findFirstOpenPlace() {
      for (int i = 0; i < players.length; i++) {
         if (players[i] == null) {
            return i;
         }
      }

      return -1;
   }

   private boolean nameTaken(String name) {
      for (Player p : players) {
         if (p != null) {
            System.out.println("p:" + p.getName() + "; n:" + name);
            if (p.getName().equals(name)) {
               return true;
            }
         }
      }

      return false;
   }

   private void sendMsgToPlayers(Message m) {
      String message = m.getSender().getName();
      message += ": " + m.getArgs()[0];
      message = escape(message);

      String code = PLAYER_MESSAGE;
      if (m.getSender().getName().equals(server.getName())) {
         code = SYSTEM_MESSAGE;
         message += SEPARATOR_TOKEN + connectedPlayers;
      }

      message = code + " " + message;

      //System.out.println("message:" + message);

      sendMessageToPlayers(message);
   }

   private void sendMessageToPlayers(String message) {
      for (Player p : players) {
         if (p != null) {
            try {
               p.sendMessageToPlayer(message);
            } catch (IOException ex) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, "Could not reach player", ex);
            }
         }
      }
   }

   private String escape(String s) {
      //System.out.println("s:"+s);
      if (s.contains(SEPARATOR_TOKEN)) {
         String[] parts = s.split(SEPARATOR_TOKEN);
         s = "";
         for (String p : parts) {
            s += p + "\\" + SEPARATOR_TOKEN;
         }
         System.out.println(s);
         s = s.substring(0, s.lastIndexOf("\\" + SEPARATOR_TOKEN));
         System.out.println(s);
      }

      return s;
   }

   private void shipGenHandler() {
      shipGen = new ShipGenerator(numberOfPlayers, shipsPerPlayer, lengths, MAP_HEIGHT, MAP_WIDTH);
//System.out.println(shipGen.getMapAsString());
      String[] shipArray = shipGen.getShipsAsStrings();
      int i = 0;
      for (String s : shipArray) {
         ships[i] = new Ship();
         String[] pairs = s.split(SEPARATOR_TOKEN);
         Compartment[] compartments = new Compartment[pairs.length / 2];

         for (int j = 0; j < compartments.length; j++) {
            compartments[j] = new Compartment(Integer.parseInt(pairs[j * 2]), Integer.parseInt(pairs[j * 2 + 1]), ships[i]);

            map[compartments[j].getRow()][compartments[j].getCol()] = compartments[j];
         }

         ships[i].setCompartments(compartments);

         i++;
      }
   }

//------CONNECTION ACCEPTER class------//
   private class ConnectionAccepter implements Runnable {

      private ServerSocket listen;

      public ConnectionAccepter(ServerSocket listen) {
         this.listen = listen;
      }

      @Override
      public void run() {
         while (run) {
            try {
               Socket s = listen.accept();
               //s.setSoTimeout(timeout);
               new Thread(new Player(s)).start();
            } catch (IOException ex) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
      }
   }

//------PLAYER class------//
   private final class Player implements Runnable {

      private Socket socket;
      private String name;
      private int score = 0;
      private Ship[] fleet;
      private BufferedReader fromPlayer;
      private DataOutputStream toPlayer;
      private boolean allShipsSunk = false;
      private boolean run = true;
      private boolean isRegistered = false;

      public Player(Socket socket) throws IOException {
         this.socket = socket;
         fromPlayer = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
         toPlayer = new DataOutputStream(this.socket.getOutputStream());

         sendMessageToPlayer(ACK_CONNECTION + " " + PROTOTCOL_VERSION + CRLF);
      }

      public Player() {
      }

      public Socket getSocket() {
         return socket;
      }

      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public int getScore() {
         return score;
      }

      public void setScore(int score) {
         this.score = score;
      }

      public int updateScore(int addlPoints) {
         return (score += addlPoints);
      }

      public boolean isRegistered() {
         return isRegistered;
      }

      public void setRegistered(boolean status) {
         this.isRegistered = status;
      }

      public String listenToPlayer() throws IOException {
         if (!socket.isClosed()) {
            return fromPlayer.readLine();
         }
         return null;
      }

      public void sendMessageToPlayer(String msg) throws IOException {
         //System.out.println("msg:"+msg);
         if (!msg.endsWith(CRLF)) {
            msg += CRLF;
         }
         if (!socket.isClosed()) {
            toPlayer.writeBytes(msg);
         }

         if (msg.startsWith(ACK_BYE)) {
            System.out.println("bye ack'd");
            close();
         } else if (msg.startsWith(ACK_ELO_ACCEPT)) {
            System.out.println("is now registered");
            setRegistered(true);
         }
      }

      public Ship[] getShips() {
         return fleet;
      }

      public void setShips(Ship[] ships) {
         fleet = ships;
         for (Ship s : fleet) {
            s.setOwner(name);
         }
      }

      public boolean allShipsSunk() {
         if (!allShipsSunk) {
            boolean evenASingleShipRemainsAfloat = false;
            for (Ship s : fleet) {
               if (!s.isSunk()) {
                  evenASingleShipRemainsAfloat = true;
               }
            }
            allShipsSunk = !evenASingleShipRemainsAfloat;
         }

         return allShipsSunk;
      }

      @Override
      public void run() {
         String input = "";

         while (this.run) {
            try {
               input = listenToPlayer();

               System.out.println(input);

               if (input == null) {
                  input = BYE;
               }
            } catch (SocketException e) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, e);
               input = BYE;
            } catch (IOException ex) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            Message m = parseInput(input);
            if (m != null) {
               if (isRegistered || m.getCommand().equals(ELO) || m.getCommand().equals(BYE)) {
                  addMessageToQueue(m);
               }
               if (BYE.equals(m.getCommand())) {
                  System.out.println("shutdown");
                  this.run = false;
               }
            }
         }

         System.out.println("shutdown 2");
      }

      private void close() {
         try {
            fromPlayer.close();
            toPlayer.close();
            socket.close();
         } catch (IOException ex) {
            Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, ex);
         }
      }

      private Message parseInput(String input) {
         System.out.println("input:" + input);

         Message m;
         String cmd;
         String arguments;
         int posOfSpace = input.indexOf(" ");
         System.out.println("pos: " + posOfSpace);
         if (posOfSpace == lengthOfCmds) {//it may be a command other than BYE
            cmd = input.substring(0, posOfSpace).toUpperCase();
            arguments = input.substring(posOfSpace).trim();
         } else if (posOfSpace == -1) {//it may be BYE
            cmd = input.toUpperCase();
            arguments = "";
         } else {//it doesn't fit any recognized command--period
            cmd = null;
            arguments = "";
         }

         System.out.println("cmd: " + cmd);
         String[] args = splitArguments(cmd, arguments);

         if (isValidCommand(cmd) && validateArguments(cmd, args)) {
            m = new Message(this, cmd, args);
         } else {
            Logger.getLogger(BattleshipServer.class.getName()).log(Level.WARNING, "Unrecognized command or arguments: {0}", input);
            m = null;
         }

         return m;
      }

      private boolean isValidCommand(String cmd) {
         return (ELO.equals(cmd) || MSG.equals(cmd) || FIR.equals(cmd) || BYE.equals(cmd));
      }

      private String[] splitArguments(String cmd, String args) {
         String[] argsA = null;
         if (ELO.equals(cmd)) {
            args = args.replace("\\" + SEPARATOR_TOKEN, "\\" + TEMP_TOKEN);

            argsA = args.split(SEPARATOR_TOKEN);

            for (int i = 0; i < argsA.length; i++) {
               argsA[i] = argsA[i].replace("\\" + TEMP_TOKEN, SEPARATOR_TOKEN);
            }
         } else if (FIR.equals(cmd)) {
            argsA = args.toUpperCase().split(SEPARATOR_TOKEN);
         } else if (MSG.equals(cmd)) {
            args = args.replace("\\", "");
            argsA = new String[1];
            argsA[0] = args;
         } else {//BYE
            argsA = new String[1];
            argsA[0] = "";
         }

         return argsA;
      }

      private boolean validateArguments(String cmd, String[] args) {
         if (ELO.equals(cmd)) {
            boolean rightLength = args.length == 2;
            boolean secondArgIsNumber = true;
            try {
               Integer.parseInt(args[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, "Wrong number of args", e);
               secondArgIsNumber = false;
            } catch (NumberFormatException e) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, "Second arg not a number", e);
               secondArgIsNumber = false;
            }

            //TODO: make sure username doesn't contain malicious code

            return (rightLength && secondArgIsNumber);
         } else if (MSG.equals(cmd)) {

            //TODO: make sure msg doesn't contain malicious code

            return (args.length == 1);
         } else if (FIR.equals(cmd)) {
            boolean rightLength = args.length == 2;
            boolean firstArgIsLetter = false;
            try {
               firstArgIsLetter = Character.isLetter(args[0].charAt(0));
            } catch (StringIndexOutOfBoundsException e) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, e);
            }
            boolean firstArgIsRightLength = (args[0].length() == 1);
            boolean secondArgIsNumber = false;
            try {
               Integer.parseInt(args[1]);
               secondArgIsNumber = true;
            } catch (ArrayIndexOutOfBoundsException e) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, e);
            } catch (NumberFormatException e) {
               Logger.getLogger(BattleshipServer.class.getName()).log(Level.SEVERE, null, e);
            }

            return (rightLength && firstArgIsLetter && firstArgIsRightLength && secondArgIsNumber && inBounds(args[0], args[1]));
         } else {//BYE
            return (args.length == 1);
         }
      }

      private boolean inBounds(String row, String col) {
         int rowInt;
         int colInt;

         try {
            rowInt = alphabet.indexOf(row);
            colInt = Integer.parseInt(col);
         } catch (Exception e) {
            return false;
         }

         return (rowInt >= 0 && rowInt <= MAP_HEIGHT && colInt >= 0 && colInt <= MAP_WIDTH);
      }

      private void addMessageToQueue(Message m) {
         msgQueue.add(m);
         synchronized (msgQueue) {
            msgQueue.notify();
         }
      }
   }

//------MESSAGE class------//
   private class Message {

      private Player sender;
      private String command;
      private String[] args;

      public Message(Player sender, String command, String[] args) {
         this.sender = sender;
         this.command = command;
         this.args = args;
      }

      public Player getSender() {
         return sender;
      }

      public String getCommand() {
         return command;
      }

      public String[] getArgs() {
         return args;
      }

      @Override
      public String toString() {
         return sender.getName() + ": " + command + " " + Arrays.deepToString(args);
      }
   }

//------SHIP class------//
   private class Ship {

      private Compartment[] sections;
      private int hits;
      private boolean isSunk;
      private String owner;

      public Ship() {
         hits = 0;
         isSunk = false;
      }

      public void setCompartments(Compartment[] compartments) {
         sections = compartments;
      }

      public Compartment[] getCompartments() {
         return sections;
      }

      public String getLocation() {
         String coordinates = "";

         for (int i = 0; i < sections.length; i++) {
            coordinates += alphabet.charAt(sections[i].getRow()) + "&" + sections[i].getCol() + "&";
         }

         coordinates = coordinates.substring(0, coordinates.length() - 1);

         return coordinates;
      }

      public void setOwner(String name) {
         owner = name;
      }

      public String getOwner() {
         return owner;
      }

      public void incrementHits() {
         hits++;
         if (hits == sections.length) {
            setSunk(true);
         }
      }

      public boolean isSunk() {
         return isSunk;
      }

      public void setSunk(boolean status) {
         isSunk = status;
         for (int i = 0; i < sections.length; i++) {
            sections[i].setHit();
         }
      }
   }

//------COMPARTMENT class------//
   private class Compartment {

      private int row;
      private int col;
      private boolean isHit;
      private Ship ship;

      public Compartment(int row, int col, Ship ship) {
         this.row = row;
         this.col = col;
         this.ship = ship;
         isHit = false;
      }

      public boolean checkHit(String rowColPair) {
         String[] coordinates = rowColPair.split("&");

         return checkHit(coordinates);
      }

      public boolean checkHit(String[] coordinates) {
         boolean hit = false;

         if (row == Integer.parseInt(coordinates[0].substring(1, coordinates.length - 1))
                 && col == Integer.parseInt(coordinates[1].substring(1, coordinates.length - 1))
                 && !isHit) {
            isHit = true;
            ship.incrementHits();
            hit = true;
         }

         return hit;
      }

      public void setHit() {
         if (!isHit) {
            isHit = true;
            ship.incrementHits();
         }
      }

      public int getRow() {
         return row;
      }

      public int getCol() {
         return col;
      }

      public boolean isHit() {
         return isHit;
      }

      public Ship getShip() {
         return ship;
      }

      @Override
      public String toString() {
         String x = alphabet.charAt(row) + ", " + col + ", " + isHit;
         return x;
      }
   }
}
