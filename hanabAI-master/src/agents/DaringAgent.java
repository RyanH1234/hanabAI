package agents;

import java.util.Arrays;
import java.util.HashSet;

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
	//records the current player's utility
	public int[] utility;
	//hashset memory used to store all previously given hints
	private HashSet<String> memory;

	/**
	 * initialises all variables in the first round of the game
	 * @param s current state of the game
	 */
	public void init(State s)
	{
	    numPlayers = s.getPlayers().length;
	    
	    memory = new HashSet<String>();
	    
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
		
		//return the best action based on the MCTS...
		Action bestAction = MCTS(s);
		
		return null;
	}

	/**
	 * Performs the MCTS algorithm returning the best action 
	 * @param currentState - current state of the game
	 * @return - the best Action based on the algorithms tree search
	 */
	public Action MCTS(State currentState)
	{
		return null;
	}
	
	/**
	 * From a given state and player index - determines a list of playable/best actions to take
	 * @param currentState - current state of the game the player is facing
	 * @param playerIndex - index of the player
	 * @return - a list of actions which the current player can play
	 */
	public Action availableActions(State currentState, int playerIndex)
	{
		return null;
	}
	
	/**
	 * From a given state - randomly choose different actions to get to some end goal value
	 * @param currentState - the initial state which we start our exploration from
	 * @return - returns the value gained from the randomly generated actions over the initial state
	 */
	public int rollout(State currentState)
	{
		return 0;
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
		int score;
		//number of visits this node has recieved
		int noOfVisits;
		//the actual state this node in the tree represents
		State state;
		
		//constructor - initialises all the instance variables
		public Node(State state)
		{
			this.state = state;
			this.score = 0;
			this.noOfVisits = 0;
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
		
		//updates the score of the node/state
		public void updateScore(int newScore)
		{
			score = newScore;
		}
		
		//updates the number of visits this node has recieved
		public void incrementVisits()
		{
			noOfVisits++;
		}
		
	}
	
}
