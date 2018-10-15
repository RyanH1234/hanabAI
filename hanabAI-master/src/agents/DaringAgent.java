package agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

public class DaringAgent implements Agent{

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
	//a 2D array which contains the utilities of each card for each player
	private int[][] playersUtilities;
	//an ArrayList of HashSets which keeps track of the hints each player has recieved
	private ArrayList<HashSet<String>> playersHints;
	
	/**
	 * String representation of the Agent's name
	 */
	public String toString()
	{
		return "DaringAgent";
	}
		
	@Override
	public Action doAction(State s) {
		
		//initialise the state of the game
		if(firstAction)
		{
			init(s);
		}
		
		//update hints
		getHints(s);
				
		//get utilities for each players stack of cards
		Map<String, Integer> stackInfo = stacksInfo(s);
		thisUtility(s, stackInfo);
		otherUtility(s, stackInfo);
		
		//if no hints have been given to this agent - give a hint to a player
		if(playersHints.isEmpty())
		{
			try 
			{
				return bestHint(s);
			} 
			catch (IllegalActionException e) 
			{
				System.out.println("GREEDY SEARCH HAS FAILED");
				e.printStackTrace();
			}
		}
		else
		{
			//return the best action based on the MCTS...
			try 
			{
				return MCTS(s);
			} 
			catch (IllegalActionException e) 
			{
				System.out.println("MONTE CARLO TREE SEARCH HAS FAILED");
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	
	/**
	 * initialises all variables in the first round of the game
	 * @param s - current state of the game
	 */
	public void init(State s)
	{
	    numPlayers = s.getPlayers().length;
	    playersHints = new ArrayList<HashSet<String>>();
	    
	    if(numPlayers==5){
	      colours = new Colour[4];
	      values = new int[4];
	      numCards = 4;
	      playersUtilities = new int[5][4]; 
	    }
	    else{
	      colours = new Colour[5];
	      values = new int[5];
	      numCards = 5;
	      playersUtilities = new int[numPlayers][5];
	    }
	    
	    index = s.getNextPlayer();
	    firstAction = false;
	    
	    for(int i = 0; i < playersHints.size(); i++)
	    {
	    	HashSet<String> memory = playersHints.get(i);
	    	memory.clear();
	    }
	}
	
	/**
	 * updates the hints given throughout the game
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
	          
	          //if a hint about the colour of a card is given
	          //save it to the appropriate HashSet for the relevant player
	          if(a.getType()==ActionType.HINT_COLOUR){
	        	  int playerIndex = a.getHintReceiver();
	        	  HashSet<String> memory = playersHints.get(playerIndex);
	        	  memory.add(hint2string(a.getHintReceiver(),0,a.getColour(),a.getHintedCards()));
	          }
	          //if a hint about the value of a card is given
	          //save it to the appropriate HashSet for the relevant player
	          else if(a.getType() == ActionType.HINT_VALUE){
	        	  int playerIndex = a.getHintReceiver();
	        	  HashSet<String> memory = playersHints.get(playerIndex);
	        	  memory.add(hint2string(a.getHintReceiver(),1,a.getValue(),a.getHintedCards()));
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
	 * Updates the utility of THIS agent
	 * 5 -> play this card
	 * 4 -> known card
	 * 3 -> partially known card
	 * 2 -> default unkown card
	 * 1 -> dead card, prioritise for discard
	 * @param currentState - currentState of the game
	 */
	public void thisUtility(State currentState, Map<String, Integer> struct)
	{
		int[] cardUtilities = new int[numCards];
		
		for(int i = 0; i < numCards; i++)
		{
			//default value - set to two
			//initialise each card to a value of 2
			cardUtilities[i] = 2;			
		
			//if the card is "null" ignore it
			if(colours[i] == null)
			{
				cardUtilities[i] = 1;
			}
			
			//if we can identify both its colour and value
			if(colours[i] != null && values[i] != 0)
			{
				cardUtilities[i] = 4;				
			} 
			//if we can identify either its colour or value
			else if(colours[i] != null || values[i] != 0)
			{
				cardUtilities[i] = 3;
			}
			
			//if we know we have a one and the number of empty stacks is 5
			if(values[i] == 1 && struct.get("EMPTYSTACKS") == 5)
			{
				cardUtilities[i] = 5;
			}
			
			//if we have a number which is smaller than the minimum number on ALL the decks - discard it
			if(values[i] <= struct.get("MINNUMBER"))
			{
				cardUtilities[i] = 1;
			}				
		}
		
		playersUtilities[index] = cardUtilities;
		
	}
	
	/**
	 * Updates the utility of every OTHER agent in the game
	 * 5 -> play this card
	 * 4 -> known card
	 * 3 -> partially known card
	 * 2 -> default unkown card
	 * 1 -> dead card, prioritise for discard
	 * @param currentState - currentState of the game
	 */
	public void otherUtility(State currentState, Map<String, Integer> struct)
	{
		for(int i = 0; i < numPlayers; i++)
		{
			if(i == index) {continue;}
	
			int[] cardUtilities = new int[numCards];
			
			//get this other players hand
			Card[] playersHand = currentState.getHand(i);
			
			//for each card in the other players hand
			for(int j = 0; j < playersHand.length; j++)
			{
				//initialise all utilities to 1
				cardUtilities[j] = 1;
				
				//if equal to null - skip it
				if(playersHand[j] == null)
				{
					continue;
				}
				
				//if a card can be played - assign a utility of 5
				if(playable(currentState, playersHand[j] ) == 1)
				{
					cardUtilities[j] = 5;
				}
			}
			
			playersUtilities[i] = cardUtilities;
			
		}
	}
	
	/**
	 * a card is only playable if the top card on it stack is 1 less than the card
	 * @param s - current state of the came
	 * @param c - card that needs to be identified as playable or not
	 * @return - 1 if the card is playable, 0 if the card is NOT playable
	 */
	public int playable(State s, Card c)
	{
		if(s.getFirework(c.getColour()).isEmpty()) {
			if(c.getValue() == 1){
				return 1;
			}
			else{
				return 0;
			}
		}
		else{
			Stack<Card> fireworksStack = s.getFirework(c.getColour());
			if(fireworksStack.peek().getValue() == (c.getValue()-1)){
				return 1;
			}
			else{
				return 0;
			}
		}		
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
			if(hand[i].getColour().toString().equals(c))
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
			if(hand[i].getValue() == val)
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
	 *  Determines the best hint to give based on OTHER player's hands
	 *  Strictly from this player's P.O.V
	 *  @param currentState - current state of the game the player is facing
	 *  @return - the best action to take from a "greedy" perspective
	 * @throws IllegalActionException 
	 */
	public Action bestHint(State currentState) throws IllegalActionException
	{
		for(int i = 0; i < numPlayers; i++)
		{
			if(i == index) {continue;}
			
			int[] utility = playersUtilities[i];
			
			for(int j = 0; j < utility.length; j++)
			{
				Card[] hand = currentState.getHand(i);
				
				if(utility[j] == 5)
				{
					if(Math.random() < 0.5 && !inMemory(i, 0, hand[j])) {
						return new Action(index, toString(), ActionType.HINT_COLOUR, i, sameColour(hand[j].getColour(),hand), hand[j].getColour());
					}
					else if (Math.random() >= 5 && !inMemory(i, 1, hand[j])){
						return new Action(index, toString(), ActionType.HINT_VALUE, i, sameValue(hand[j].getValue(), hand), hand[j].getValue());
					}
				}
			}
			
		}
		
		//do a random hint
		
		int randomPlayer = (int) (Math.random() * numPlayers);
		int randomCard = (int) (Math.random() * numCards);
		
		return new Action(index, toString(), ActionType.HINT_VALUE, randomPlayer, sameValue(currentState.getHand(randomPlayer)[randomCard].getValue(), currentState.getHand(randomPlayer)), currentState.getHand(randomPlayer)[randomCard].getValue());
	}

	/**
	 * @return - true if the hint has been given (i.e. is in memory) or false if the hint has not been given
	 */
	public boolean inMemory(int receiver, int colOrVal, Card thiscard)
	{
		return playersHints.get(receiver).contains(hint2string(receiver,colOrVal,thiscard));
	}
	
	/**
	 * Implements the Monte Carlo Tree Search
	 * @param currentState - current state of the game
	 * @return - the best Action based on the algorithms tree search
	 * @throws IllegalActionException 
	 */
	public Action MCTS(State currentState) throws IllegalActionException
	{		
		//local copy of the playersHints array
		ArrayList<HashSet<String>> localPlayersHints = new ArrayList<HashSet<String>>();
		for(HashSet<String> h : playersHints)
		{
			localPlayersHints.add(h);
		}
				
		//create the root node of the tree
		Node rootNode = new Node(currentState, null, null);
				
		//add children to rootNode - availableActions()
		Action[] availableActions = availableActions(currentState, currentState.getNextPlayer());
		
		for(int i = 0; i < availableActions.length; i++)
		{
			if(availableActions[i].getType() == ActionType.HINT_VALUE)
			{
				String hint = hint2string(availableActions[i].getHintReceiver(),1, availableActions[i].getValue(), availableActions[i].getHintedCards());
				if(localPlayersHints.contains(hint))
				{
					continue;
				}
				
			}
			else if(availableActions[i].getType() == ActionType.HINT_COLOUR)
			{
				String hint = hint2string(availableActions[i].getHintReceiver(),0, availableActions[i].getColour(), availableActions[i].getHintedCards());
				if(localPlayersHints.contains(hint))
				{
					continue;
				}
			}
			
			Stack<Card> deck = cardsNotDrawn(currentState);		
			//infer what the next state will be from the provided action
			State nextState = currentState.nextState(availableActions[i], deck);			
			//create a childNode to append to the tree
			Node childNode = new Node(nextState, rootNode, availableActions[i]);
			rootNode.addChild(childNode);
		}
				
		//initialise currentNode to be the root node
		Node currentNode = rootNode;
		int StartingPlayer = currentNode.getState().getNextPlayer();
		
		//for some predefined unit of time
		for(int t = 0; t < 100; t++)
		{			
			if(currentNode.getState().getNextPlayer() == StartingPlayer)
			{
				break;
			}
			
			//if the current node is NOT a leaf node (e.g. is currentNode.children is NOT empty)
			if(!currentNode.getChildren().isEmpty())
			{
				//the new current node is the child node that maximises UCB1
				List<Node> childrenList = currentNode.getChildren();
				
				double maxUCB1 = 0;
				Node maximisingChild = childrenList.get(0);
				
				for(Node n : childrenList)
				{
					double UCB1Val = UCB1(n.getParent().getVisits(), n); 
					if(UCB1Val > maxUCB1)
					{
						maxUCB1 = UCB1Val;
						maximisingChild = n;
					}
				}
				
				currentNode = maximisingChild;

			}
			//else if the current node IS a leaf node
			else
			{
				//if we haven't visited the node before (e.g. if currentNode.noOfVisits == 0)
				if(currentNode.getVisits() == 0)
				{
					//rollout
					int rolloutVal = rollout(currentNode);
					
					//update the currentNodes "stats"
					currentNode.incrementVisits();
					currentNode.updateScore(rolloutVal);
					
					//backpropagate - all the way up the tree!
					Node traversingNode = currentNode;
					while(traversingNode.getParent() != null)
					{
						traversingNode = traversingNode.getParent();
						traversingNode.incrementVisits();
						traversingNode.updateScore(rolloutVal);
					}
					
					//reset the hints given...
					localPlayersHints = playersHints;
					
					
				}
				//else if we HAVE visited the node before (e.g. if currentNode.noOfVisits > 0)
				else
				{
					//availableActions() - find all the available actions
					Action[] possibleActions = availableActions(currentNode.getState(),currentNode.getState().getNextPlayer());
					
					//add all these available actions to the tree
					for(int i = 0; i < possibleActions.length; i++)
					{
						
						if(availableActions[i].getType() == ActionType.HINT_VALUE)
						{
							String hint = hint2string(availableActions[i].getHintReceiver(),1, availableActions[i].getValue(), availableActions[i].getHintedCards());
							if(localPlayersHints.contains(hint))
							{
								continue;
							}
						}else if(availableActions[i].getType() == ActionType.HINT_COLOUR){
							String hint = hint2string(availableActions[i].getHintReceiver(),0, availableActions[i].getColour(), availableActions[i].getHintedCards());
							if(localPlayersHints.contains(hint))
							{
								continue;
							}
						}
						
						
						Stack<Card> deck = cardsNotDrawn(currentNode.getState());
						//infer what the next state will be from the provided action
						State nextState = currentNode.getState().nextState(availableActions[i], deck);
						//create a childNode to append to the tree
						Node childNode = new Node(nextState, currentNode, availableActions[i]);
						currentNode.addChild(childNode);
					}
						
					//currentNode = first new child node
					currentNode = currentNode.getChildren().get(0);
					
					//rollout
					int rolloutVal = rollout(currentNode);
		
					//backtrack
					Node traversingNode = currentNode;
					while(traversingNode.getParent() != null)
					{
						traversingNode = traversingNode.getParent();
						traversingNode.incrementVisits();
						traversingNode.updateScore(rolloutVal);
					}
					
				}
			}	
		}
		
		//do a simple greedy search for child which has the greatest value e.g. whose .getScore() is greatest
		int maxScore = 0;
		Node optimalNode = rootNode.getChildren().get(0);
		for(Node n : rootNode.getChildren())
		{
			if(n.getScore() > maxScore)
			{
				optimalNode = n;
			}
		}
		
		return optimalNode.getAction();
	}
	
	/**
	 * Returns a deck of cards that haven't been drawn yet
	 * @param s - current state of the game
	 *  @return - returns a stack containing all the cards that haven't been drawn yet
	 */
	public Stack<Card> cardsNotDrawn(State s)
	{
		Stack<Card> deck = new Stack<Card>();
		return deck;
	}
	
	/**
	 * From a given state and player index - determines a list of playable/best actions to take
	 * @param currentState - current state of the game the player is facing
	 * @param playerIndex - index of the player
	 * @return - a list of actions which the current player can play
	 * @throws IllegalActionException 
	 */
	public Action[] availableActions(State currentState, int playerIndex) throws IllegalActionException
	{	
		//get the agents name
		String name = currentState.getName(playerIndex);
		
		//update the utilities of every agent
		Map<String, Integer> stackInfo = stacksInfo(currentState);
		thisUtility(currentState, stackInfo);
		otherUtility(currentState, stackInfo);
		
		//get the utility array for the current player
		int[] utilities = playersUtilities[playerIndex];
		ArrayList<Action> possibleActions = new ArrayList<Action>();
		
		//for every card in the current players hand
		for(int i = 0; i < utilities.length;i++)
		{
			Card currentCard = currentState.getHand(playerIndex)[i];
			
			if(utilities[i] == 5)
			{
				possibleActions.add(new Action(playerIndex, name, ActionType.PLAY, i));
			}
			
			if(utilities[i] == 1)
			{
				possibleActions.add(new Action(playerIndex, name, ActionType.PLAY, i));
			}
			
		}
		
		//for every other player - if they have a 5 or 1 - give a hint
		for(int j = 0; j < playersUtilities.length; j++)
		{
			if(j == playerIndex) {continue;}
			
			int[] otherPlayerUtilities = playersUtilities[j];
			
			for(int k = 0; k < otherPlayerUtilities.length; k++)
			{
				Card currentCard = currentState.getHand(j)[k];
				
				if(otherPlayerUtilities[k] == 5 || otherPlayerUtilities[k] == 1)
				{
					if(Math.random() < 0.5)
					{
						possibleActions.add(new Action(playerIndex, name, ActionType.HINT_COLOUR, j, sameColour(currentCard.getColour(), currentState.getHand(j)), currentCard.getColour()));
					}
					else
					{
						possibleActions.add(new Action(playerIndex, name, ActionType.HINT_VALUE, j, sameValue(currentCard.getValue(), currentState.getHand(j)), currentCard.getValue()));
					}
					
				}
			}
			
		}
		
		return possibleActions.toArray(new Action[possibleActions.size()]);
	}
		
	/**
	 * From a given state - randomly choose different actions to get to some end goal value
	 * @param currentState - the initial state which we start our exploration from
	 * @return - returns the value gained from the randomly generated actions over the initial state
	 * @throws IllegalActionException 
	 */
	public int rollout(Node currentNode) throws IllegalActionException
	{
		State currentState = currentNode.getState();
		
		Map<String,Integer> initialInfo = stacksInfo(currentState);
		int initialScore = initialInfo.get("BLUE") + initialInfo.get("GREEN") + initialInfo.get("RED") + initialInfo.get("YELLOW") + initialInfo.get("WHITE");
		
		int initialPlayer = currentState.getNextPlayer();
		while(true) {
			
			Action[] possibleActions = availableActions(currentState, currentState.getNextPlayer());
			
			int randomAction = (int) (Math.random()*possibleActions.length);
						
			currentState = currentState.nextState(possibleActions[randomAction], cardsNotDrawn(currentState));
			
			if(initialPlayer == currentState.getNextPlayer())
			{
				break;
			}
		}
		
		Map<String,Integer> presentInfo = stacksInfo(currentState);
		int presentScore = presentInfo.get("BLUE") + initialInfo.get("GREEN") + initialInfo.get("RED") + initialInfo.get("YELLOW") + initialInfo.get("WHITE");
		
		
		return (presentScore + initialScore);
	}
	
	
	
	/**
	 * Implements the UCB1 formula
	 * @param N - the number of visits the parent node has recieved
	 * @param currentNode - the current node we are working on
	 * @return - the result of the UCB1 formula
	 */
	public double UCB1(int N, Node currentNode)
	{
		//average value of the current state
		double V = (double) (currentNode.getScore()/currentNode.getVisits());
		
		//"c" - exploration parameter
		double c = (double) 2;
		
		//UCBI = V + c*sqrt(ln(N)/n)
		return V + (c*(Math.sqrt(Math.log(N)/currentNode.getVisits())));
	}
	
		
	
	/**
	 * A class to represent the nodes of the MCTS tree  
	 * @author Ryan
	 */
	public class Node {
		
		//score of the current state of the game
		private int score;
		//number of visits this node has recieved
		private int noOfVisits;
		//the actual state this node in the tree represents
		private State state;
		//the parent of the node
		private Node parent;
		//the children of this node
		private List<Node> children = new ArrayList<Node>();
		//the action that the parent node experienced to translate it into this current node
		Action parentAction;
		
		//constructor - initialises all the instance variables
		public Node(State state, Node parent, Action parentAction)
		{
			this.state = state;
			this.score = 0;
			this.noOfVisits = 0;
			this.children = null;
			this.parent = parent;
			this.parentAction = parentAction;
		}
		
		//returns the score of the node
		public int getScore()
		{
			return score;
		}
		
		//returns the number of vists this node has recieved
		public int getVisits()
		{
			return noOfVisits;
		}
		
		//returns the state that this node represents
		public State getState()
		{
			return state;
		}
		
		//returns the list containing all the children of the current node
		public List<Node> getChildren()
		{
			return children;
		}
		
		//returns the action which transformed the parent node to the current node
		public Action getAction()
		{
			return parentAction;
		}
		
		//returns the parent of the current ndoe
		public Node getParent()
		{
			return parent;
		}
		
		//updates the score of the node/state
		public void updateScore(int newScore)
		{
			score = score + newScore;
		}
		
		//updates the number of visits this node has recieved
		public void incrementVisits()
		{
			noOfVisits++;
		}
		
		//add a child to the node
		public void addChild(Node child)
		{
			children.add(child);
		}
		
	}
	
}
