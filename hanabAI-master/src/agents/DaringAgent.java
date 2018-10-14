package agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
		try 
		{
			return MCTS(s);
		} 
		catch (IllegalActionException e) 
		{
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * Implements the Monte Carlo Tree Search
	 * @param currentState - current state of the game
	 * @return - the best Action based on the algorithms tree search
	 * @throws IllegalActionException 
	 */
	public Action MCTS(State currentState) throws IllegalActionException
	{		
		//create the root node of the tree
		Node rootNode = new Node(currentState, null, null);
		
		//add children to rootNode - availableActions()
		Action[] availableActions = availableActions(currentState, index);
		for(int i = 0; i < availableActions.length; i++)
		{
			//??
			Stack<Card> deck = new Stack<Card>();
			//infer what the next state will be from the provided action
			State nextState = currentState.nextState(availableActions[i], deck);
			//create a childNode to append to the tree
			Node childNode = new Node(nextState, rootNode, availableActions[i]);
			rootNode.addChild(childNode);
		}
		
		//initialise currentNode to be the root node
		Node currentNode = rootNode;
		
		//for some predefined unit of time
		for(int t = 0; t < 100; t++)
		{
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
					
				}
				//else if we HAVE visited the node before (e.g. if currentNode.noOfVisits > 0)
				else
				{
					//availableActions() - find all the available actions
					//PLAYER INDEX - ISSUE?? ----------------------------
					//What happens when you reach the end of the game...
					Action[] possibleActions = availableActions(currentNode.getState(),currentNode.getState().getNextPlayer());
					//---------------------------------------------------
					
					//add all these available actions to the tree
					for(int i = 0; i < possibleActions.length; i++)
					{
						//??
						Stack<Card> deck = new Stack<Card>();
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
	 * From a given state and player index - determines a list of playable/best actions to take
	 * @param currentState - current state of the game the player is facing
	 * @param playerIndex - index of the player
	 * @return - a list of actions which the current player can play
	 */
	public Action[] availableActions(State currentState, int playerIndex)
	{
		//if the current player is THIS agent
		if(playerIndex == index)
		{
			
		}
		else
		{
			
		}
		
		return null;
	}
	
	/**
	 * From a given state - randomly choose different actions to get to some end goal value
	 * @param currentState - the initial state which we start our exploration from
	 * @return - returns the value gained from the randomly generated actions over the initial state
	 */
	public int rollout(Node currentNoded)
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
