package agents;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Card;
import hanabAI.Colour;
import hanabAI.Hanabi;
import hanabAI.IllegalActionException;
import hanabAI.State;

/*
 * A simple reflex agent
 */
public class RyanAgent implements Agent{
	
	//records whether this is the first action the agent is taken
	private boolean firstAction = true;
	//identifies which of the cards in our hand we know the colour of
	private Colour[] colours;
	//identifies which of the cards in our hand we know the value of
	private int[] values;
	//records the index of the current agent
	private int index;
	//records the number of players in the game
	private int numPlayers;
	//records the number of cards we're playing with
	private int numCards;
	
	public int[] utility;
	
	@Override
	public String toString()
	{
		return "RyanAgent";
	}

	//intialises all variables upon first round of game
	public void init(State s)
	{
	    numPlayers = s.getPlayers().length;
	    
	    if(numPlayers==5){
	      colours = new Colour[4];
	      values = new int[4];
	      utility = new int[4];
	      numCards = 4;
	      
	    }
	    else{
	      colours = new Colour[5];
	      values = new int[5];
	      utility = new int[5];
	      numCards = 5;
	    }
	    
	    index = s.getNextPlayer();
	    firstAction = false;
	}
	
	//updates "colours" and "values" from the information 
	//ACKNOWLEDGEMENT: BasicAgent.java
	public void getHints(State s)
	{
	    try{
	    	//clone the current state
	        State t = (State) s.clone();
	        
	        //increment through the minimum of either the number of players OR the number of states
	        for(int i = 0; i<Math.min(numPlayers-1,s.getOrder());i++){
	        	
	          Action a = t.getPreviousAction();
	          
	          //if any of these actions are of type "hint"
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
	          
	          //go to the previous state
	          t = t.getPreviousState();
	        }
	      }
	      catch(IllegalActionException e){e.printStackTrace();}
	}
	
	@Override
	public Action doAction(State s) {
		
		//if this is the first round
		if(firstAction)
		{
			//set up variable
			init(s);
		}
		
		
		//actionUtility = run utility for ACTION for the player's current hand
		
		/*
		 CONSERVATIVE AGENT - safe
		  if playable(actionUtility) //i.e. if you have the colour and number
		  { 
		  	play
		  } 
		  else
		  {
		  	for each otherplayer
		  	{
		 		run utility for ACTION for the otherplayer's current hand
		 
		 		if(hint && not in memory)
		 		{
		 			return giveHint()
		 		}
		 	}
		 	
		 	return discard()
		 
		  }
		 
		 */
		
		/*
		 DARING AGENT - where we can add in some learning component 
		 currenthand = utility(playerhand)
		  if playable(currenthand) //i.e. if you have the colour and number
		  { 
		  	play
		  } 
		  else
		  currenthand2 = cardcountguess(currenthand)
		  if playable(currenthand2)
		  	play
		  else
		  {
		  	for each otherplayer
		  	{
		 		run utility for ACTION for the otherplayer's current hand
		 
		 		if(hint && not in memory)
		 		{
		 			return giveHint()
		 		}
		 	}
		 	
		 	return discard()
		 
		  }
		 
		 */

				
		//update information gained from hints (see BasicAgent)
		getHints(s);
		
		//work out our utilities for each of our cards based on a set of rules
		actionsUtility(s,stacksInfo(s));
		
		//gather an overview on the information gained from just looking at the stacks 
		System.out.println("STACK INFO : " + stacksInfo(s));
		System.out.println("COLOUR ARRAY : " + Arrays.toString(colours));
		System.out.println("VALUES ARRAY : " + Arrays.toString(values));
		System.out.println("UTILITY ARRAY : " + Arrays.toString(utility) + "\n");
		
		//from the cumulative information we have now gained -
		
		
			//out of the four possible actions we can play
			//which is the best action..
			
			//PLAY()
		
			//DISCARD()
		
			//HINT_COLOUR()
		
			//HINT_VALUE()
		java.util.Random rand = new java.util.Random();
		int cardIndex = rand.nextInt(colours.length);
		try {
			return new Action(index, toString(), ActionType.PLAY, cardIndex);
		} catch (IllegalActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Creates a structure which contains information on the current fireworks set
	 * "EMPTYSTACKS" -> number of empty stacks
	 * "MINNUMBER" -> the minimum card value played on the stack
	 * "BLUE", "GREEN", "RED", "WHITE", "YELLOW" -> 0 OR the maximum card placed in that pile
	 * @return a hashmap with <key, value> pairs which reflect important information on the current fireworks set
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
		int minimumCardValue = Integer.MAX_VALUE;
		
		//BLUE
		if(blueStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("BLUE", 0);
		}
		else
		{
			int blueValue = blueStack.pop().getValue();
			info.put("BLUE",blueValue);
			if(blueValue <= minimumCardValue) minimumCardValue = blueValue;
			
		}
		
		//GREEN
		if(greenStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("GREEN", 0);
		}
		else
		{
			int greenValue = greenStack.pop().getValue();
			info.put("GREEN", greenValue);
			if(greenValue <= minimumCardValue) minimumCardValue = greenValue;
			
		}
		
		//RED
		if(redStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("RED", 0);
		}
		else
		{
			int redValue = redStack.pop().getValue();
			info.put("RED",redValue);
			if(redValue <= minimumCardValue) minimumCardValue = redValue;
		}
		
		//WHITE
		if(whiteStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("WHITE", 0);
		}
		else
		{
			int whiteValue = whiteStack.pop().getValue();
			info.put("WHITE",whiteValue);
			if(whiteValue <= minimumCardValue) minimumCardValue = whiteValue;
		}
		
		//YELLOW
		if(yellowStack.isEmpty())
		{
			numEmptyStacks++;
			info.put("YELLOW", 0);
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
	 * From the information provided by the stacksInfo function AND from a given set of rules - 
	 * Determines which actions provide - 
	 * 4 -> play card
	 * 3 -> keep card
	 * 2 -> average utility
	 * 1 -> discard card
	 * @param s - state of the game
	 */
	public void actionsUtility(State s, Map<String, Integer> stacksInfo)
	{
		
		//for each of our cards - we can identify its colour, its value, both, or neither
		for(int i = 0; i < colours.length;i++)
		{
			//if we don't know anything about the cards
			if(colours[i] == null && values[i] == 0)
			{
				utility[i] = 1;
			}
			
			//if we can identify both it's colour and value
			if(colours[i] != null && values[i] != 0)
			{
				utility[i] = 3;
			}
			
			//if we can identify just it's colour
			if((colours[i] != null && values[i] == 0))
			{
				//get the fireworks stack for the current colour
				Stack<Card> colourStack = new Stack<Card>();			
				colourStack = s.getFirework(colours[i]);
				
				//if the firework stack
				if(colourStack.size() == 5)
				{
					utility[i] = 1;
				}
				//if the firework stack has some cards in it - is it worth the risk of placing the card down?
				else
				{
					utility[i] = 2;
				}

			}
						
			//if we can identify just it's value
			if(colours[i] == null && values[i] != 0)
			{			
				//do NOT discard the value 5 since it's the only one
				if(values[i] == 5)
				{
					utility[i] = 3;
				}
								
				//count how many stacks are empty...
				int emptyStacks = stacksInfo.get("EMPTYSTACKS");	
								
				//if all stacks are empty and you have a one - play it!
				if(emptyStacks == 5 && values[i] == 1)
				{
					utility[i] = 4;
				}
				
				//if only some of the stacks are empty - assign a relevant utility to it
				if(emptyStacks > 0 && emptyStacks <= 2 && values[i] <= 3)
				{
					//if the number of empty stacks is between 1 and 2
					//and the value of your cards is relatively low
					//assign a medium utility
					utility[i] = 2;
				}
				else
				{
					//assign a minimal utility
					utility[i] = 1;
				}
				
		
				//for the current number - if all the stacks are above that number, you can discard this card
				if(values[i] <= stacksInfo.get("MINNUMBER"))
				{
					utility[i] = 1;
				}
								
				//if the current card is a 1 AND at least one of the stacks is empty - keep the card 
				
				//if our card is a 2,3 or 4 AND there's a single 2,3,4 in the stack - DO NOT DISCARD IT
				Stack<Card> discardPile = s.getDiscards();
				//records how many times the current value of the known card pops up in the discard pile
				int counter = 0;
				if(values[i] == 2 || values[i] == 3 || values[i] == 4)
				{
					for(Card c : discardPile)
					{
						if(c.getValue() == values[i])
						{
							counter++;
						}
					}
				}
				//do NOT discard this card
				if(counter == 1)
				{
					utility[i] = 3;
				}
				else
				{
					utility[i] = 2;
				}	
				
			}			
		}
		
	}
	
	
	
	
	public void giveHints()
	{
		
	}
	
	public void randomHint()
	{
		
	}
	
	public void randomAction()
	{
		
	}
	
	
	
}


/*
 * HINTS -> MEMORY -> GLOBAL VARIABLE
 * FOR actionsUtility() INSTEAD OF PASSING A STATE, PASS AN ARRAY
 * 
 */
