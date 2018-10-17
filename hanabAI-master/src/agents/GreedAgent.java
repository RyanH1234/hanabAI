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

public class GreedAgent implements Agent{

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
	
	private int[][] othervalues;
	private Colour[][] othercolours;
	
	private int cardsplayed;
	
	int counter = 0;
	
	@Override
	public Action doAction(State s){
		
		//DEBUGGING
		counter++;

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
				        cardsplayed+=1;
						return new Action(index, toString(), ActionType.PLAY, i);
					} 
					catch (IllegalActionException e) 
					{
						e.printStackTrace();
					}
			}else
			if(utility[i] == 4)
			{ 
				Card tempcard = new Card(colours[i],values[i]);
				if(playable(s,tempcard) == 1)
				{
					try {
						//re-initialise relevant hint arrays..
				        colours[i] = null;
				        values[i] = 0;
				        cardsplayed+=1;
						return new Action(index, toString(), ActionType.PLAY, i);
					} 
					catch (IllegalActionException e) 
					{
						e.printStackTrace();
					}
				}
			}else
			if(utility[i] == 3&&cardsplayed>=25)
			{ 
				int possiblecard = thinkengine(s,i);
				if(possiblecard==1)
				{
					Card tempcard = new Card(colours[i],values[i]);
					if(playable(s,tempcard) == 1)
					{
						try {
							//re-initialise relevant hint arrays..
					        colours[i] = null;
					        values[i] = 0;
					        cardsplayed+=1;
							return new Action(index, toString(), ActionType.PLAY, i);
						} 
						catch (IllegalActionException e) 
						{
							e.printStackTrace();
						}
					}
				}
				if(possiblecard==2&&s.getHintTokens()>1)//50% chance card is playable
				{
					try {
						//re-initialise relevant hint arrays..
				        colours[i] = null;
				        values[i] = 0;
				        cardsplayed+=1;
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
			//i == player index
			
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
			
			//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			//use new simUtility first?
			int[] test = simUtility(s,i);
			if(test[0]==0)
			//that player has a playable card and needs only one hint to play it!
			//0=color,1=value, second int is index of card
			{
				try 
				{
					//NO DON'T ASSUME IT'S NOT IN MEMORY
					boolean[] h1c = makecolourbool(playersHand, playersHand[test[1]]);
					if(!memory.contains(hint2string(i,0,playersHand[test[1]].getColour(),h1c))) {
						memory.add(hint2string(i,0,playersHand[test[1]].getColour(),h1c));
						return new Action(index,toString(),ActionType.HINT_COLOUR,i,h1c,playersHand[test[1]].getColour());
					}
				} 
				catch (IllegalActionException e) 
				{
					e.printStackTrace();
				}
			}else if(test[0]==1)
			{
				try 
				{
					boolean[] h2c = makevalbool(playersHand, playersHand[test[1]]);
					if(!memory.contains(hint2string(i,1,playersHand[test[1]].getValue(),h2c))) {
						memory.add(hint2string(i,1,playersHand[test[1]].getValue(),h2c));
						return new Action(index,toString(),ActionType.HINT_VALUE,i,h2c,playersHand[test[1]].getValue());
					}
				} 
				catch (IllegalActionException e) 
				{
					e.printStackTrace();
				}
			}
			
			
			
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
	        cardsplayed+=1;
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
	    othervalues = new int[numPlayers][numCards];
	    othercolours = new Colour[numPlayers][numCards];
	    
	    index = s.getNextPlayer();
	    firstAction = false;
	    
	    memory.clear();
	    cardsplayed=0;
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
	          
	          //if previous action played/discarded card update other players hints
	          if((a.getType()==ActionType.PLAY||a.getType()==ActionType.DISCARD) && a.getPlayer()!=index)
	          {
	        	  //update other player values based on their moves
	        	  othercolours[a.getPlayer()][a.getCard()]=null;
	        	  othervalues[a.getPlayer()][a.getCard()]=0;
	        	  cardsplayed+=1;
	          }
	          
	          //~add previous player's hints to memory too
	          if(a.getType()==ActionType.HINT_COLOUR && a.getHintReceiver()!=index)
	          {
	        	  	//save an array of booleans indicating the cards that are subject of the hint
		          	boolean[] hints = t.getPreviousAction().getHintedCards();
	        	  	memory.add(hint2string(a.getHintReceiver(),0,a.getColour(),a.getHintedCards()));
	        	  	//update playersheads using the below function
	        	  	//move "hints" initialising to outside of the if statements
	        	  	//apply the hints as in the below for loop for if index==receiver but for playersheads
	        	  	for(int j = 0; j<hints.length; j++)
	        	  	{
	  	              if(hints[j])
	  	              {
	  	                  othercolours[a.getHintReceiver()][j] = a.getColour();
	  	              }
	  	            }
	          }
	          if(a.getType()==ActionType.HINT_VALUE && a.getHintReceiver()!=index)
	          {
		          //save an array of booleans indicating the cards that are subject of the hint
		          boolean[] hints = t.getPreviousAction().getHintedCards();
	        	  	memory.add(hint2string(a.getHintReceiver(),1,a.getValue(),a.getHintedCards()));
	        	  	for(int j = 0; j<hints.length; j++)
	        	  	{
	  	              if(hints[j])
	  	              {
	  	                  othervalues[a.getHintReceiver()][j] = a.getValue();
	  	              }
	  	            }
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
	
	
	
	public int[] simUtility(State s, int playerno)
	//taking only the state and the player index we're simulating
	//can check if there is any playable card the agent needs a single hint for help
	{		
		Card[] realhand = s.getHand(playerno);
		for(int i = 0; i < numCards;i++)
		{
			if(realhand[i]==null) {continue;}
			//If the other player needs only one hint
			if(othercolours[playerno][i] != null && othervalues[playerno][i] == 0)
			{
				if(playable(s,realhand[i])==1)
				{
					//this player needs only a hint about the value of the card
					return new int[]{0,i};
				}
			}else if(othercolours[playerno][i] == null && othervalues[playerno][i] != 0)
			{
				if(playable(s,realhand[i])==1)
				{
					//this player needs only a hint about the colour of the card
					return new int[]{1,i};
				}
			}
		}
		return new int[]{-1};
	}
		
	
	
	
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
	
	
	public int thinkengine(State s, int cindex)
	//given a card with partial knowledge in hint memory
	//get firework stack, graveyard stack and all other player hands, count the cards up that have the same known value
	//try to interpolate the unknown value from knowns
	//returns iff we were able to work out the missing info of a card
	{
		//if value is known, look for all cards of that value, enumerate them based on colour, see what's left
		if(values[cindex]!=0)
		{
			int[] counter = {0,0,0,0,0}; //that card in each of the 5 colours RBGYW
			boolean[] possible = {false,false,false,false,false};
			Stack<Card> discards = s.getDiscards();
			
			//also check your own hand if you have any cards that you know both values/colours but are not playable???
			for(int k = 0;k<numCards;k++)
			{
				if(k==cindex) {continue;}
				if(colours[k]!=null&&values[k]==values[cindex])
				{
					switch(colours[k])
					{
					case RED: counter[0]+=1;
					case BLUE: counter[1]+=1;
					case GREEN: counter[2]+=1;
					case YELLOW: counter[3]+=1;
					case WHITE: counter[4]+=1;
					}
				}
			}
			
			Stack<Card> rworks = s.getFirework(Colour.RED);
			Stack<Card> bworks = s.getFirework(Colour.BLUE);
			Stack<Card> gworks = s.getFirework(Colour.GREEN);
			Stack<Card> yworks = s.getFirework(Colour.YELLOW);
			Stack<Card> wworks = s.getFirework(Colour.WHITE);
			
			Card temp;
			while(!discards.isEmpty())
			{
				temp=discards.pop();
				if(temp.getValue()==values[cindex])
				{
					switch(temp.getColour())
					{
					case RED:
						counter[0]+=1;
					case BLUE:
						counter[1]+=1;
					case GREEN:
						counter[2]+=1;
					case YELLOW:
						counter[3]+=1;
					case WHITE:
						counter[4]+=1;
					}
					
						
				}
			}
			if(!rworks.isEmpty())
				{
					if(rworks.peek().getValue()>=values[cindex])
					{counter[0]+=1;}
				}
			if(!bworks.isEmpty())
			{
				if(bworks.peek().getValue()>=values[cindex])
				{counter[0]+=1;}
			}
			if(!gworks.isEmpty())
			{
				if(gworks.peek().getValue()>=values[cindex])
				{counter[0]+=1;}
			}
			if(!yworks.isEmpty())
			{
				if(yworks.peek().getValue()>=values[cindex])
				{counter[0]+=1;}
			}
			if(!wworks.isEmpty())
			{
				if(wworks.peek().getValue()>=values[cindex])
				{counter[0]+=1;}
			}
			Card[] temphand;
			for(int i = 0;i<numPlayers;i++)
			{
				if(i==index) {continue;}
				temphand = s.getHand(i);
				for(int j = 0;j<numCards;j++)
				{
					if(temphand[j]!=null&&temphand[j].getValue()==values[cindex])
					{
						switch(temphand[j].getColour())
						{
						case RED:
							counter[0]+=1;
						case BLUE:
							counter[1]+=1;
						case GREEN:
							counter[2]+=1;
						case YELLOW:
							counter[3]+=1;
						case WHITE:
							counter[4]+=1;
						}
					}
				}
			}
			//enumeration complete
			//now go through possibility array

			//if value is known
			//how many " a " of a card is there in one colour? that depends on the value!
			int a = 0;
			if(values[cindex]==1) {a=3;}
			else if(values[cindex]==2||values[cindex]==3||values[cindex]==4) {a=2;}
			else if(values[cindex]==5) {a=1;}
			
			if(counter[0]!=a){possible[0]=true;}
			if(counter[1]!=a){possible[1]=true;}
			if(counter[2]!=a){possible[2]=true;}
			if(counter[3]!=a){possible[3]=true;}
			if(counter[4]!=a){possible[4]=true;}
			int poss=boolcount(possible);
			if(poss==1)
			{
				if(possible[0]) {colours[cindex]=Colour.RED;}
				else if(possible[1]) {colours[cindex]=Colour.BLUE;}
				else if(possible[2]) {colours[cindex]=Colour.GREEN;}
				else if(possible[3]) {colours[cindex]=Colour.YELLOW;}
				else if(possible[4]) {colours[cindex]=Colour.WHITE;}
				return 1;
			}else if(poss==2)
			//{return 0;}
			{//else suggest a risky move
				int worthrisk=0;
				for(int v=0;v<5;v++)
				{
					if(possible[v]) {
						switch(v)
						{
						case 0:
							if(!rworks.isEmpty())
							{
								if(rworks.peek().getValue()==(values[cindex]-1))
								{worthrisk+=1;}
							}
						case 1:
							if(!bworks.isEmpty())
							{
								if(bworks.peek().getValue()==(values[cindex]-1))
								{worthrisk+=1;}
							}
						case 2:
							if(!gworks.isEmpty())
							{
								if(gworks.peek().getValue()==(values[cindex]-1))
								{worthrisk+=1;}
							}
						case 3:
							if(!yworks.isEmpty())
							{
								if(yworks.peek().getValue()==(values[cindex]-1))
								{worthrisk+=1;}
							}
						case 4:
							if(!wworks.isEmpty())
							{
								if(wworks.peek().getValue()==(values[cindex]-1))
								{worthrisk+=1;}
							}
						}
					}
				}
				if(worthrisk>=1) {return 2;}
			}

		}
		//if colour is known, look for all cards of that colour, enumerate and see what's left
		else if(colours[cindex]!=null)
		{
			int[] counter = {0,0,0,0,0}; //cards of value 1,2,3,4,5
			boolean[] possible = {false,false,false,false,false};
			
			Stack<Card> discards = s.getDiscards();
			
			//check your own hand also
			for (int k = 0;k<numCards;k++)
			{
				if(k==cindex) {continue;}
				if((colours[k]!=null)&&values[k]!=0&&colours[k]==colours[cindex])
				{
					switch(values[k])
					{
					case 1: counter[0]+=1;
					case 2: counter[1]+=1;
					case 3: counter[2]+=1;
					case 4: counter[3]+=1;
					case 5: counter[4]+=1;
					}
				}
			}
			
			Stack<Card> rworks = s.getFirework(Colour.RED);
			Stack<Card> bworks = s.getFirework(Colour.BLUE);
			Stack<Card> gworks = s.getFirework(Colour.GREEN);
			Stack<Card> yworks = s.getFirework(Colour.YELLOW);
			Stack<Card> wworks = s.getFirework(Colour.WHITE);
			
			Card temp;
			while(!discards.isEmpty())
			{
				temp=discards.pop();
				if(temp.getColour()==colours[cindex])
				{
					switch(temp.getValue())
					{
					case 1:
						counter[0]+=1;
					case 2:
						counter[1]+=1;
					case 3:
						counter[2]+=1;
					case 4:
						counter[3]+=1;
					case 5:
						counter[4]+=1;
					}
					
						
				}
			}
			Stack<Card> thisworks = null;
			switch(colours[cindex])
			{
			case RED:
				thisworks = rworks;
			case BLUE:
				thisworks = bworks;
			case GREEN:
				thisworks = gworks;
			case YELLOW:
				thisworks = yworks;
			case WHITE:
				thisworks = wworks;
			}
			if(!thisworks.isEmpty())
			{
				int t = thisworks.peek().getValue();
				switch(t)
				{
				case 1:
					counter[0]+=1;
				case 2:
					counter[1]+=1;
				case 3:
					counter[2]+=1;
				case 4:
					counter[3]+=1;
				case 5:
					counter[4]+=1;
				}
			}
			Card[] temphand;
			for(int i = 0;i<numPlayers;i++)
			{
				if(i==index) {continue;}
				temphand = s.getHand(i);
				for(int j = 0;j<numCards;j++)
				{
					if(temphand[j]!=null&&temphand[j].getColour()==colours[cindex])
					{
						switch(temphand[j].getValue())
						{
						case 1:
							counter[0]+=1;
						case 2:
							counter[1]+=1;
						case 3:
							counter[2]+=1;
						case 4:
							counter[3]+=1;
						case 5:
							counter[4]+=1;
						}
					}
				}
			}
			
			//if colour is known,
			//so how many of each card is in one colour? as follows:
			if(counter[0]!=3) {possible[0]=true;}
			if(counter[1]!=2) {possible[1]=true;}
			if(counter[2]!=2) {possible[2]=true;}
			if(counter[3]!=2) {possible[3]=true;}
			if(counter[4]!=1) {possible[4]=true;}
			int poss = boolcount(possible);
			if(poss==1)
			{
				if(possible[0]) {values[cindex]=1;}
				else if(possible[1]) {values[cindex]=2;}
				else if(possible[2]) {values[cindex]=3;}
				else if(possible[3]) {values[cindex]=4;}
				else if(possible[4]) {values[cindex]=5;}
				return 1;
			}else if(poss==2)
			//{return 0;}
			{//else suggest a risky move
				int worthrisk=0;
				for(int v=0;v<5;v++)
				{
					if(possible[v]) {
						if(thisworks.peek().getValue()==(v)) {worthrisk=1;}
					}
				}
				if(worthrisk>=1) {return 2;}
			}
			

		}
		
		return 0;
	}
}
