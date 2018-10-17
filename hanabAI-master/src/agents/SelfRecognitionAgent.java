package agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Card;
import hanabAI.Colour;
import hanabAI.IllegalActionException;
import hanabAI.State;

public class SelfRecognitionAgent implements Agent {

	//records whether this is the first action the agent is taken
	private boolean firstAction = true;
	//records the index of the current agent
	private int index;
	//records the number of players in the game
	private int numPlayers;
	//records the number of cards we're playing with
	private int numCards;
	//identifies which of the cards in our hand we know the colour of
	private Colour[] colours;
	//identifies which of the cards in our hand we know the value of
	private int[] values;
	//a 2D array which contains the utilities of each card for each player
	private int[][] playersUtilities;
	//an ArrayList of HashSets which keeps track of the hints each player has recieved
	private ArrayList<ArrayList<String>> playersHints;
	
	public void printInfo(Map<String,Integer> stackInfo)
	{
		System.out.println("----------------------------------------------");
		System.out.println("STACK INFO : " + stackInfo);
		System.out.println("THIS HINTS : " + playersHints.get(index));
		System.out.println("NUM CARDS: " + numCards + " NUM PLAYERS: " + numPlayers);
		System.out.println("COLOURS: " + Arrays.toString(colours));
		System.out.println("VALUES: " + Arrays.toString(values));
		System.out.println("HINTS SIZE : " + playersHints.size());
		System.out.println("INDEX : " + index);
		
		for(int i = 0; i < playersHints.size(); i++)
		{
			System.out.println(playersHints.get(i));
		}
		
		System.out.println("----------------------------------------------");
	}
	
	public Action doAction(State s) {
		
		//initialise all instance variables
		if(firstAction){
			init(s);
		}
				
		//update hints
		getHints(s);
		
		//get information about the fireworks stack
		Map<String,Integer> stackInfo = stacksInfo(s);
		
		//DEBUGGING PURPOSES ONLY
		printInfo(stacksInfo(s));
				
		
		try {
			return thisAction(s, stackInfo);
		}catch(IllegalActionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
				
		try {
			return new Action(index, toString(), ActionType.PLAY,0);
		} catch (IllegalActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String toString()
	{
		return "SelfRecognitionAgent";
	}

	/**
	 * Initialises all of the instance variables so none of them should return a null exception when accessed
	 */
	public void init(State s)
	{
	    numPlayers = s.getPlayers().length;
	    playersHints = new ArrayList<ArrayList<String>>(numPlayers);
	    
	    index = s.getNextPlayer();
	    firstAction = false;
	    
	    if(numPlayers==5){
	      numCards = 4;
	      playersUtilities = new int[numPlayers][4]; 
	    }
	    else{
	      numCards = 5;
	      playersUtilities = new int[numPlayers][5];
	    }
	    
	    colours = new Colour[numCards];
	    values = new int[numCards];
	    
	    //add a new HashSet to each index of the ArrayList
	    for(int i = 0; i < numPlayers; i++)
	    {
	    	playersHints.add(i,new ArrayList<String>());
	    }

	}
		
	/**
	 * updates the hints given throughout the previous round
	 * @param s - current state of the game
	 */
	public void getHints(State s)
	{
	    try{
	    	//clone the current state
	        State t = (State) s.clone();
	        
	        //increment through the minimum of either the number of players OR the number of states
	        for(int i = 0; i<Math.min(numPlayers-1,s.getOrder());i++){
	        	
	          Action a = t.getPreviousAction();
	          
	          //if any of these actions are of type "hint" and are for THIS agent
	          if((a.getType()==ActionType.HINT_COLOUR || a.getType() == ActionType.HINT_VALUE) && a.getHintReceiver()==index){
	            
	        	//save an array of booleans indicating the cards that are subject of the hint
	            boolean[] hints = t.getPreviousAction().getHintedCards();
	            
	            //save the hints into a local array - either colours[] or values[]
	            for(int j = 0; j<hints.length; j++){
	              if(hints[j]){
	                if(a.getType()==ActionType.HINT_COLOUR) 
	                  colours[j] = a.getColour();
	                else
	                  values[j] = a.getValue();  
	              }
	            }	            
	          }
	          //any other hints for other players should be recorded in the playersHints array..
	          else {
            	
	        	  if(a.getType() == ActionType.HINT_VALUE){
		        	  
	        		  int playerIndex = a.getHintReceiver();	        	  
		        	  int numHints = playersHints.get(playerIndex).size();
		        	  
		        	  if(numHints == 0)
		        	  {
		        		  ArrayList<String> hint = new ArrayList<String>();
		        		  hint.add(0, hint2string(playerIndex,1,a.getValue(),a.getHintedCards()));
		        		  playersHints.add(playerIndex, hint);
		        	  }
		        	  else
		        	  {
		        		  playersHints.get(playerIndex).add(numHints, hint2string(playerIndex,1,a.getValue(),a.getHintedCards()));		        		  
		        	  }
		        	  
		          }
	        	  else if(a.getType() == ActionType.HINT_COLOUR)
	        	  {
		        	  int playerIndex = a.getHintReceiver();
		        	  int numHints = playersHints.get(playerIndex).size();
		        	  
		        	  if(numHints == 0)
		        	  {
		        		  ArrayList<String> hint = new ArrayList<String>();
		        		  hint.add(0, hint2string(playerIndex,0,a.getColour(),a.getHintedCards()));
		        		  playersHints.add(playerIndex, hint);
		        	  }
		        	  else
		        	  {
		        		  playersHints.get(playerIndex).add(numHints, hint2string(playerIndex,0,a.getColour(),a.getHintedCards()));		        		  
		        	  }
		        	  
	        	  }
	        	  
	          }
          
	          //go to the previous state
	          t = t.getPreviousState();
	        }
	      }
	      catch(IllegalActionException e){e.printStackTrace();}
	}
	
	/**
	 * Converts the hint to a string so you are able to store it in the HashSet
	 * @param receiver - target player
	 * @param colOrVal - identifies if the hint is a colour or value hint
	 * @param thiscard  - the card which the hint relates to
	 * @return a string representation of the hint
	 */
	public String hint2string(int receiver, int colOrVal, Card thiscard)
	{
		String memkey = Integer.toString(receiver)+Integer.toString(colOrVal)+thiscard.toString();
		return memkey;
	}
	public String hint2string(int receiver, int colOrVal, Colour c, boolean[] hand)
	{
		String memkey = Integer.toString(receiver)+Integer.toString(colOrVal)+c.toString()+Arrays.toString(hand);
		return memkey;
	}
	public String hint2string(int receiver, int colOrVal, int val, boolean[] hand)
	{
		String memkey = Integer.toString(receiver)+Integer.toString(colOrVal)+Integer.toString(val)+Arrays.toString(hand);
		return memkey;
	}

	/**
	 * @return - true if the hint has been given (i.e. is in memory) or false if the hint has not been given
	 */
	public boolean inMemory(int receiver, int colOrVal, Card thiscard)
	{
		if(playersHints.get(receiver).isEmpty())
		{
			return false;
		}
		else
		{			
			return playersHints.get(receiver).contains(hint2string(receiver,colOrVal,thiscard));
		}
	}
	
	
	/**
	 * Creates a structure which contains information on the current fireworks set
	 * "EMPTYSTACKS" -> number of empty stacks
	 * "MINNUMBER" -> the minimum card value played on the stack
	 * "BLUE", "GREEN", "RED", "WHITE", "YELLOW" -> 0 OR the maximum card placed in that pile
	 * @return - a hashmap with <key, value> pairs which reflect important information on the current fireworks set
	 */
	public Map<String, Integer> stacksInfo(State s)
	{
		Map<String, Integer> info = new HashMap<String, Integer>();
		
		//number of empty stacks
		int numEmptyStacks = 0;
				
		//variables which record the size of each stack
		int blueSize = 0;
		int GreenSize = 0;
		int redSize = 0;
		int whiteSize = 0;
		int yellowSize = 0;
		
		//initialise variables for each of the stacks 
		Stack<Card> blueStack = s.getFirework(Colour.BLUE);
		Stack<Card> greenStack = s.getFirework(Colour.GREEN);
		Stack<Card> redStack = s.getFirework(Colour.RED);
		Stack<Card> whiteStack = s.getFirework(Colour.WHITE);
		Stack<Card> yellowStack = s.getFirework(Colour.YELLOW);
		
		//minimum valued card out of ALL the firework stacks
		int minimumCardValue = -1;
		
		//BLUE
		if(blueStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("BLUE", 0);
			minimumCardValue = 0;
		}
		else
		{
			int blueValue = blueStack.pop().getValue();
			info.put("BLUE",blueValue);
			if(blueValue <= minimumCardValue || minimumCardValue == -1) minimumCardValue = blueValue;
			
		}
		
		//GREEN
		if(greenStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("GREEN", 0);
			minimumCardValue = 0;
		}
		else
		{
			int greenValue = greenStack.pop().getValue();
			info.put("GREEN", greenValue);
			if(greenValue <= minimumCardValue || minimumCardValue == -1) minimumCardValue = greenValue;
			
		}
		
		//RED
		if(redStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("RED", 0);
			minimumCardValue = 0;
		}
		else
		{
			int redValue = redStack.pop().getValue();
			info.put("RED",redValue);
			if(redValue <= minimumCardValue || minimumCardValue == -1) minimumCardValue = redValue;
		}
		
		//WHITE
		if(whiteStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("WHITE", 0);
			minimumCardValue = 0;
		}
		else
		{
			int whiteValue = whiteStack.pop().getValue();
			info.put("WHITE",whiteValue);
			if(whiteValue <= minimumCardValue || minimumCardValue == -1) minimumCardValue = whiteValue;
		}
		
		//YELLOW
		if(yellowStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("YELLOW", 0);
			minimumCardValue = 0;
		}
		else
		{
			int yellowValue = yellowStack.pop().getValue();
			info.put("YELLOW",yellowValue);
			if(yellowValue <= minimumCardValue) minimumCardValue = yellowValue;
		}
		
		info.put("EMPTYSTACKS", numEmptyStacks);
		info.put("MINNUMBER", minimumCardValue);
		
		return info;
	}
	
	/**
	 * From a hand returns a boolean array of the cards which have the colour 'c'
	 * @param c - the colour to be found in hand
	 * @param hand - the hand to check for the above colour
	 */
	public boolean[] sameColour(Colour c, Card[] hand)
	{		
		boolean[] bool = new boolean[hand.length];
		for(int i = 0; i < hand.length; i++)
		{
			if(hand[i] == null)
			{
				bool[i] = false;
			}
			else if(hand[i].getColour().toString().equals(c.toString()))
			{
				bool[i] = true;
			}
			else
			{
				bool[i] = false;
			}
		}
		return bool;
	}
	
	/**
	 * From a hand returns a boolean array of the cards which have the same value 'val'
	 * @return
	 */
	public boolean[] sameValue(int val, Card[] hand)
	{
		boolean[] bool = new boolean[hand.length];
		for(int i = 0; i < hand.length; i++)
		{
			if(hand[i] == null)
			{
				bool[i] = false;
			}
			else if(hand[i].getValue() == val)
			{
				bool[i] = true;
			}
			else
			{
				bool[i] = false;
			}
		}
		return bool;
	}
	
	/**
	 * Represents the hirarchy of actions the agent should process through when making a descision on what action to play
	 * @return - the best action it can play
	 * @throws IllegalActionException 
	 */
	public Action thisAction(State s, Map<String,Integer> stackInfo) throws IllegalActionException
	{		
		//THIS player
		for(int i = 0; i < numCards; i++)
		{
			//if the player has a playable card -> play it
			//if the value is a 1 and 2 or less stacks have been filled then play it
			if(stackInfo.get("EMPTYSTACKS") == 5 && values[i] == 1 || stackInfo.get("EMPTYSTACKS") >= 3 && values[i] == 1)
			{
				return new Action(index, toString(), ActionType.PLAY, i);
			}
			
			if(colours[i] != null && values[i] != 0)
			{
				if(stackInfo.get(colours[i].toString().toUpperCase()) == values[i] + 1)
				{
					return new Action(index, toString(), ActionType.PLAY, i);
				}
			}
			
			
			//if the player has a discard-able card -> discard
			if(colours[i] != null)
			{
				Stack<Card> colourStack = s.getFirework(colours[i]);
				if(colourStack.size() == 5 || stackInfo.get("MINNUMBER") > values[i])
				{
					return new Action(index, toString(), ActionType.DISCARD, i);
				}
			}

		}
				
		//if the OTHER players have a playable card AND there is enough tokens -> give hint on one of the attributes
		if(s.getHintTokens() > 0)
		{
			for(int i = 0; i < numPlayers; i++)
			{
				if(i == index) {continue;}
				
				Card[] otherHand = s.getHand(i);
				
				for(int j = 0; j < otherHand.length; j++)
				{
					if(otherHand[j] == null) {continue;}
					
					Card currentCard = otherHand[j];
					
					//if all stacks are empty and the current card is  1
					if((currentCard.getValue() == 1 && stackInfo.get("EMPTYSTACKS") == 5))
					{
						//if we haven't already given this hint
						if(!inMemory(i,1,currentCard))
						{
							return new Action(index, toString(), ActionType.HINT_VALUE, i, sameValue(currentCard.getValue(),otherHand), currentCard.getValue());
						}
					}
					
					//if the current card is the next card in line to be played
					if((currentCard.getValue() - 1) == (stackInfo.get(currentCard.getColour().toString().toUpperCase())))
					{
						if(!inMemory(i,0,currentCard)) {
							return new Action(index, toString(), ActionType.HINT_COLOUR, i, sameColour(currentCard.getColour(),otherHand),currentCard.getColour());
						}
						else if(!inMemory(i,0,currentCard))
						{
							return new Action(index, toString(), ActionType.HINT_VALUE, i, sameValue(currentCard.getValue(),otherHand), currentCard.getValue());
						}
						
					}
					
				}
				
			}
		}
		
		//if there is enough tokens but no playable cards -> give random hint
		if(s.getHintTokens() > 0)
		{
			//random player
			int randomPlayer = index;
			while(randomPlayer == index)
			{
				randomPlayer = new Random().nextInt((numPlayers));
			}
			//random card
			Card[] currentHand = s.getHand(randomPlayer);
			int randomIndex = 0;
			Card randomCard = currentHand[randomIndex];
			
			while(randomCard == null)
			{
				randomIndex = new Random().nextInt((numCards - 1));
				randomCard = currentHand[randomIndex];
			}
			
			if(Math.random() > 0.5){
				return new Action(index, toString(), ActionType.HINT_COLOUR, randomPlayer, sameColour(randomCard.getColour(),currentHand),randomCard.getColour());
			}else {
				return new Action(index, toString(), ActionType.HINT_VALUE, randomPlayer, sameValue(randomCard.getValue(),currentHand), randomCard.getValue());
			}
			
		}
		
		//self-recognition
		
		//randomly discard a card
		Card[] currentHand = s.getHand(index);
		int randomIndex = new Random().nextInt(numCards);
		Card randomCard = currentHand[randomIndex];
	
		
		return new Action(index, toString(), ActionType.DISCARD, randomIndex);
		
	}
	
}
