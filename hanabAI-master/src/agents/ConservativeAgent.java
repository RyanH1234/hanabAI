package agents;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.HashSet;
import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Card;
import hanabAI.Colour;
import hanabAI.IllegalActionException;
import hanabAI.State;

public class ConservativeAgent implements Agent{

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
	//records the current player's utility
	public int[] utility;
	//hashset memory used to store all previously given hints
	private HashSet<String> memory;
	
	int counter = 0;
	
	@Override
	public Action doAction(State s){
		
		//DEBUGGING
		counter++;
		System.out.println("-------------------");
		System.out.println("ROUND : " + counter + " CURRENT INDEX " + index + " TOKENS " + s.getHintTokens());
		System.out.println("-------------------");
		
		//initialise the state of the game
		if(firstAction)
		{
			init(s);
		}
		
		//update hints
		getHints(s);
				
		//get as much information as you can from the current state of the game
		Map<String,Integer> struct = stacksInfo(s);
		
		//update the current player's utility array
		currentHandUtility(s, struct);
			
		//If the current player has a playble card - play it!
		//e.g. make a move if we have a utility of "4"
		for(int i = 0; i < utility.length; i++)
		{
			if(utility[i] == 5)
			{ 
					try {
						//re-initialise relevant hint arrays..
				        colours[i] = null;
				        values[i] = 0;
						return new Action(index, toString(), ActionType.PLAY, i);
					} 
					catch (IllegalActionException e) 
					{
						e.printStackTrace();
					}
			}
			if(utility[i] == 4)
			{ 
				Card tempcard = new Card(colours[i],values[i]);
				if(playable(s,tempcard) == 1)
				{
					try {
						//re-initialise relevant hint arrays..
				        colours[i] = null;
				        values[i] = 0;
						return new Action(index, toString(), ActionType.PLAY, i);
					} 
					catch (IllegalActionException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}
		
		//else - find the utilities of the other players
		//if there is a utility of 4 returned for any of the OTHER players cards
		//give a hint to that card IFF you haven't given the same hint before
		for(int i = 0; i < numPlayers; i++)
		{
			//if we have no more hint tokens left..
			if(s.getHintTokens() <= 0)
			{
				break;
			}
			
			//if the current index is your own hand - skip it
			if(i == index)
			{
				continue;
			}
			
			//get the current hand of the player
			Card[] playersHand = s.getHand(i);
			
			System.out.println("PLAYER " + i);
			System.out.println(Arrays.toString(playersHand));
			
			//get the utilities for that player's hand
			int[] playersUtility = otherHandsUtility(s, playersHand, struct);
			
			//if any of the utilities are 4 - give a hint ONLY IF you have NOT given hint before
			for(int j = 0; j < playersUtility.length; j++)
			{
				if(playersUtility[j] == 4)
				{
					//get the card this utility is pointing to
					Card playableCard = playersHand[j];
					
					//check if you have given a hint about this card before to this specific player
					//give a hint to the player
					//0 -> hint colour, 1-> hint value
					
					//int hinttype;
					//if(Math.random()>0.5) {hinttype=0;}else{hinttype=1;}
					boolean[] h1c = makecolourbool(playersHand, playableCard);
					boolean[] h2c = makevalbool(playersHand, playableCard);
					//if this hint is not in memory
					if(!memory.contains(hint2string(i,1,playableCard.getValue(),h2c)))
					{
						try 
						{
							memory.add(hint2string(i,1,playableCard.getValue(),h2c));
							return new Action(index,toString(),ActionType.HINT_VALUE,i,h2c,playableCard.getValue());
						} 
						catch (IllegalActionException e) 
						{
							e.printStackTrace();
						}
					}else if(!memory.contains(hint2string(i,0,playableCard.getColour(),h1c))) 
					{
						try {
							//return the action to do a hint
							memory.add(hint2string(i,0,playableCard.getColour(),h1c));
							return new Action(index,toString(),ActionType.HINT_COLOUR,i,h1c,playableCard.getColour());
						} 
						catch (IllegalActionException e) 
						{
							e.printStackTrace();
						}
					}
				}
				//~Hint tokens cannot exceed 8, our agents should now also give hints for hands that will reveal the most amount of info
				else //if(s.getHintTokens()>=1)
				{
					Card playableCard;
					switch(playersUtility[j])
					{
					case 30: 
						playableCard = playersHand[j];
						boolean[] h1c = makecolourbool(playersHand, playableCard);
						try {
							//return the action to do a hint
							memory.add(hint2string(i,0,playableCard.getColour(),h1c));
							return new Action(index,toString(),ActionType.HINT_COLOUR,i,h1c,playableCard.getColour());
						} 
						catch (IllegalActionException e) 
						{
							e.printStackTrace();
						}
					
					case 31:
						playableCard = playersHand[j];
						boolean[] h2c = makevalbool(playersHand, playableCard);
						try 
						{
							memory.add(hint2string(i,1,playableCard.getValue(),h2c));
							return new Action(index,toString(),ActionType.HINT_VALUE,i,h2c,playableCard.getValue());
						} 
						catch (IllegalActionException e) 
						{
							e.printStackTrace();
						}
					}
				}
			}
			
			
		}
		
		//if we get here and we haven't given a hint despite having 8 hint tokens, do a random hint!
		if(s.getHintTokens() == 8)
		{
			try 
			{
				Action randoma = randhint(s);

				return randoma;
			} 
			catch (IllegalActionException e) 
			{
				e.printStackTrace();
			}
		}
		
		int minIndex = 0;
		int minValue = Integer.MAX_VALUE;
		//else, if all else fails - discard a card
		for(int i = 0; i < numCards; i++)
		{
			if(utility[i] < minValue)
			{
				minIndex = i;
				minValue = utility[i];
			}
		}
		
		try 
		{
	        colours[minIndex] = null;
	        values[minIndex] = 0;
			return new Action(index, toString(), ActionType.DISCARD,minIndex);
		} 
		catch (IllegalActionException e) 
		{
			e.printStackTrace();
		}
		
		//SHOULD THIS BE HERE??
		return null;
		
	
	}
	
	/**
	 * initialises all variables in the first round of the game
	 * @param s current state of the game
	 */
	public void init(State s)
	{
	    numPlayers = s.getPlayers().length;
	    
	    memory = new HashSet<String>();
	    
	    if(numPlayers>3){
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
	    
	    memory.clear();
	}

	/**
	 * updates "colours" and "values" from the information provided by the state of the game
	 * ACKNOWLEDGEMENT: BasicAgent.java
	 * @param s current state of the game
	 */
	public void getHints(State s)
	{
	    try{
	    	//clone the current state
	        State t = (State) s.clone();
	        
	        //increment through the minimum of either the number of players OR the number of states
	        for(int i = 0; i<Math.min(numPlayers-1,s.getOrder());i++){
	        	
	          Action a = t.getPreviousAction();
	          
	          //~add previous player's hints to memory too
	          if(a.getType()==ActionType.HINT_COLOUR && a.getHintReceiver()!=index)
	          {
	        	  	memory.add(hint2string(a.getHintReceiver(),0,a.getColour(),a.getHintedCards()));
	          }
	          if(a.getType()==ActionType.HINT_VALUE && a.getHintReceiver()!=index)
	          {
	        	  	memory.add(hint2string(a.getHintReceiver(),1,a.getValue(),a.getHintedCards()));
	          }
	          
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
	 * For the current players hand - based on the hints given, return an array of utilities for each card
	 * Utility scores are as defined below:
	 * 4 -> known card
	 * 3 -> partially known card
	 * 2 -> default unkown card
	 * 1 -> dead card, prioritise for discard
	 * @return an array with a utility for each card in the player's hand
	 */
	public void currentHandUtility(State s, Map<String, Integer> struct)
	{		
		//for each of our cards - we can identify its colour, its value, both, or neither
		for(int i = 0; i < colours.length;i++)
		{
			//initialise each card to a value of 2
			utility[i] = 2;			
		
			//if the card is "null" ignore it
			if(colours[i] == null)
			{
				utility[i] = 1;
			}
			
			//if we can identify both its colour and value
			if(colours[i] != null && values[i] != 0)
			{
				utility[i] = 4;				
			} 
			//if we can identify either its colour or value
			else if(colours[i] != null || values[i] != 0)
			{
				utility[i] = 3;
			}
			
			//if we know we have a one and the number of empty stacks is 5
			if(values[i] == 1 && struct.get("EMPTYSTACKS") == 5)
			{
				utility[i] = 5;
			}
			
			//if we have a number which is smaller than the minimum number on ALL the decks - discard it
			if(values[i] <= struct.get("MINNUMBER"))
			{
				utility[i] = 1;
			}
			
		}
	}
	
	/**
	 * For a given deck of cards - determines the utility of each of the cards based on a set of rules
	 * Utility scores are as defined below :
	 * 4 -> play card
	 * 3 -> keep card
	 * 2 -> average utility
	 * 1 -> discard card
	 * @return an array with a utility for each card in the player's hand
	 */
	public int[] otherHandsUtility(State s, Card[] c, Map<String, Integer> struct)
	{
		int[] utility = new int[numCards];
				
		//for each card in the players hand
		for(int i = 0; i < c.length; i++)
		{
			//initialise all utilities to 1
			utility[i] = 1;
			
			//if equal to null - skip it
			if(c[i] == null)
			{
				continue;
			}
			
			//if a card can be played - assign a utility of 4
			if(playable(s, c[i]) == 1)
			{
				utility[i] = 4;
			}
			
			//~now prioritize which cards hints will give the most information
			//~if hint will reveal 4 cards, very useful
			//~relaxing hints beyond that may decrease score, but necessary when there is an excess of hints
			//~tweak individual values to see what works
			//~IDEA! Make two loops, if no viewable card is playable then instead check for the card of maximum information
			if(boolcount(makecolourbool(c,c[i]))>=4)
			{
				utility[i] = 30;
			}else
			if(boolcount(makevalbool(c,c[i]))>=4)
			{
				utility[i] = 31;
			}
			if(s.getHintTokens()==8)
			{
				if(boolcount(makecolourbool(c,c[i]))>=3)
				{
					utility[i] = 30;
				}else
				if(boolcount(makevalbool(c,c[i]))>=3)
				{
					utility[i] = 31;
				}
			}
			
						
		}
		
		return utility;
	}
	
	/**
	 * a card is only playable if the top card on it stack is 1 less than the card
	 * @param s - current state of the came
	 * @param c - card that needs to be identified as playable or not
	 * @return - 1 if the card is playable, 0 if the card is NOT playable
	 */
	public int playable(State s, Card c)
	{
			
		if(s.getFirework(c.getColour()).isEmpty())
		{

			if(c.getValue() == 1)
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
		else
		{
			Stack<Card> fireworksStack = s.getFirework(c.getColour());
			
			if(fireworksStack.peek().getValue() == (c.getValue()-1))
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}		
	}

	
	/**
	 * if the top card value of a firework stack is equal or larger than your current card, then the card has been played
	 * @param s - current state of the game
	 * @param c - card you are comparing
	 * @return 1 if the card is in the stack or not
	 */
	public int inStack(State s, Card c)
	{
		Colour ccol = c.getColour();
		Stack<Card> pile = s.getFirework(ccol);
		Card thiscard = pile.pop();
		if(thiscard.getValue() >= c.getValue()) {return 1;}
		else {return 0;}
	}
	
	/**
	 * Given a state and a card, counts how many of that card is in the discard pile
	 * @param s - current state of the game
	 * @param c - card we are searching
	 * @return - the number of "c" that can be found in the discard pile
	 */
	public int inDiscards (State s, Card c)
	{
		int result = 0;
		Stack<Card> pile = s.getDiscards();
		Card thiscard;
	  
		while(pile.empty() != true)
		{
		  thiscard=pile.pop(); //pop card off stack
		  if(thiscard.toString().equals(c.toString()))
		  {
			  result++;
		  }
		}
	  
		return result;
	}
	
	/**
	 * Resets the HashSet which records all the hints given
	 */
	public void reset()
	{
		memory.clear();
	}
	
	/**
	 * Converts the hint to a string so you are able to store it in the HashSet
	 * @param receiver - target player
	 * @param colOrVal - identifies if the hint is a colour or value hint
	 * @param thiscard  - the card which the hint relates to
	 * @return a string representation of the hint
	 */

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
	 * @return returns the string representation of this agent's name 
	 */
	public String toString()
	{
		return "ConservativeAgent";
	}
	public boolean[] makecolourbool (Card[] playersHand, Card playableCard)
	{
		//make the boolean array
		boolean[] hinthand = new boolean[numCards];
		
		for(int v = 0; v<numCards;v++)
		{
			
			//if the players hand is null - ignore it (we've started running out of cards)
			if(playersHand[v] == null)
			{
				hinthand[v] = false;
			}
			//if the colour in the players hand is the colour we want to give a hint about...
			else if(playersHand[v].getColour().equals(playableCard.getColour()))
			{
				hinthand[v]=true;
			}
			else 
			{
				hinthand[v]=false;
			}
		}
		return hinthand;
	}
	
	public boolean[] makevalbool (Card[] playersHand, Card playableCard)
	{
		boolean[] hinthand = new boolean[numCards];
		for(int v = 0; v<numCards;v++)
		{
			//if the players hand is null - ignore it (we've started running out of cards)
			if(playersHand[v] == null)
			{
				hinthand[v] = false;
			} 
			//if the value in the players hand is the value we want to give a hint about...
			else if(playersHand[v].getValue()==playableCard.getValue())
			{
				hinthand[v]=true;
			}
			else 
			{
				hinthand[v]=false;
			}
		}
		return hinthand;
	}
	public int boolcount (boolean[] bools)
	{
		int sum = 0;
		for(boolean b : bools) {
		    sum += b ? 1 : 0;
		}
		return sum;
	}
	
	/*
	 * Important: There may be times when the agent doesn't see any highly valuable hints to give, but the hint tokens
	 * are already at 8 and the game rules say hint tokens cannot go above 8
	 * ergo, we bring the random hint utility and use it as a last resort
	 */
	public Action randhint(State s) throws IllegalActionException
	{
      for(int i = 1; i<numPlayers; i++){
        int hintee = (index+i)%numPlayers;
        Card[] hand = s.getHand(hintee);
        for(int j = 0; j<hand.length; j++){
          Card c = hand[j];
          if(c!=null){
            //flip coin
            if(Math.random()>0.5){//give colour hint
              boolean[] col = new boolean[hand.length];
              for(int k = 0; k< col.length; k++){
                col[k]=c.getColour().equals((hand[k]==null?null:hand[k].getColour()));
              }
              return new Action(index,toString(),ActionType.HINT_COLOUR,hintee,col,c.getColour());
            }
            else{//give value hint
              boolean[] val = new boolean[hand.length];
              for(int k = 0; k< val.length; k++){
                val[k]=c.getValue() == (hand[k]==null?-1:hand[k].getValue());
              }
              return new Action(index,toString(),ActionType.HINT_VALUE,hintee,val,c.getValue());
            }
          }
        }
      }
	  return null;
	}
	
}
