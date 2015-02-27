import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JOptionPane;


public class snakeCanvas extends Canvas implements Runnable, KeyListener
{
	//constants for the grid
	private final int BOX_HEIGHT = 15, BOX_WIDTH = 15;
	private final int GRID_WIDTH = 20, GRID_HEIGHT = 20;
	
	//This creates the point LL that is the snake
	private LinkedList<Point> snake;
	private Point fruit;
	private int theDirection = direction.NO_DIRECTION;
	
	private Thread runThread;
	
	//scoring
	private int score = 0;
	private String highScore = "";
	
	//menu variables
	private Image menuImage = null;
	private boolean inMenu = true;
	private boolean endGame = false;
	private boolean won = false;
	
	public void paint(Graphics g)
	{
		//start your engines
		if(runThread == null)
		{
			this.setPreferredSize(new Dimension(640,480));
					
			//make sure the paint function knows its the method listening for key strokes
			this.addKeyListener(this);	
			runThread = new Thread(this);
			runThread.start();
		}
		
		//if we are in the menu
		if(inMenu)
		{
			//only paint the menu
			drawMenu(g);
			
		}
		
		else if(endGame)
		{
			//draw end game image (win or lose)
			drawEndGame(g);
		}
		
		else
		{
			if(snake == null)
			{	
				snake = new LinkedList<Point>();
				spawnNewSnake();
				placeFruit();
			}
			//initialize high score
			if(highScore.equals(""))
			{
				highScore=this.getHighScore();
			}
			drawFruit(g);
			drawGrid(g);
			drawSnake(g);
			drawScore(g);
		}
	}
	
	//method designed for double buffering
	public void update(Graphics g)
	{
		Graphics offScreenGraphics;
		BufferedImage offscreen = null;
		Dimension d = this.getSize();
		
		//initialize the off screen canvas
		offscreen = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		offScreenGraphics = offscreen.getGraphics();
		offScreenGraphics.setColor(this.getBackground());
		offScreenGraphics.fillRect(0, 0, d.width, d.height);
		offScreenGraphics.setColor(this.getForeground());
		
		//now paint it
		paint(offScreenGraphics);
		
		//flip the image back so we can see it on screen
		g.drawImage(offscreen, 0, 0, this);
	}
	
	public void move()
	{
		//make sure we are actually moving
		if(theDirection == direction.NO_DIRECTION)
			return;
		
		Point head = snake.peekFirst();
		Point newPoint = head;
		
		//define what each direction means on the grid
		switch (theDirection)
		{
			case direction.NORTH:
				newPoint = new Point(head.x, head.y - 1);
				break;
			case direction.SOUTH:
				newPoint = new Point(head.x, head.y + 1);
				break; 	
			case direction.EAST:
				newPoint = new Point(head.x + 1, head.y);
				break;
			case direction.WEST:
				newPoint = new Point(head.x - 1, head.y);
				break;	
		}
		
		if(this.theDirection != direction.NO_DIRECTION)
		{	
			//remove the tail
			snake.remove(snake.peekLast());
		}
		//snake hits the fruit
		if(newPoint.equals(fruit))
		{
			score++;
			Point addPoint = (Point) newPoint.clone();
			switch (theDirection)
			{
				case direction.NORTH:
					newPoint = new Point(head.x, head.y - 1);
					break;
				case direction.SOUTH:
					newPoint = new Point(head.x, head.y + 1);
					break; 	
				case direction.EAST:
					newPoint = new Point(head.x + 1, head.y);
					break;
				case direction.WEST:
					newPoint = new Point(head.x - 1, head.y);
					break;	
			}
			//add an additional head to extend the snake
			snake.push(addPoint);
			placeFruit();
		}
		
		//Out of bounds horizontally -> reset
		else if(newPoint.x < 0 || newPoint.x > (GRID_WIDTH - 1))
		{
			checkScore();
			won = false;
			endGame = true;
			return;
		}
		
		//Out of bounds vertically -> reset
		else if(newPoint.y < 0 || newPoint.y > (GRID_HEIGHT - 1))
		{
			checkScore();
			won = false;
			endGame = true;
			return;
		}
		
		//snake hits itself -> reset
		else if(snake.contains(newPoint))
		{
			if(theDirection != direction.NO_DIRECTION)
			{
				checkScore();
				won = false;
				endGame = true;
				return;
			}
		}
		
		//the player won!
		else if(snake.size() == (GRID_WIDTH * GRID_HEIGHT))
		{
			checkScore();
			won = true;
			endGame = true;
			return;
		}
		
		//valid move without hitting anything so add a head as to make up for the severed tail
		snake.push(newPoint);
	}
	
	public void checkScore()
	{
		if(highScore.equals(""))
			return;
		//new record!
		//make the high score string into in integer by parsing the name and number and then casting
		if (score > Integer.parseInt(highScore.split(":")[1]))
		{
			String name = JOptionPane.showInputDialog("You just beat the current highscore! Please enter your name.");
			highScore = name + ":" + score;
			
			//create the file for the high score if it does not already exist
			File scoreFile = new File("highscore.dat");
			if(!scoreFile.exists())
			{	
				try {
					scoreFile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//store the file to write to
			FileWriter writeFile = null;
			//create the buffer to actually write
			BufferedWriter writer = null;
			try
			{
				writeFile = new FileWriter(scoreFile);
				writer = new BufferedWriter(writeFile);
				writer.write(this.highScore);
			}
			//catch any file errors
			catch (Exception e){}
			finally 
			{
				try
				{
					if(writer != null)
						writer.close();
				}
				catch (Exception e) {}
			}
		}
	}
	
	//draw our menu
	public void drawMenu(Graphics g)
	{
		if(this.menuImage == null)
		{
			try
			{
				URL imagePath = snakeCanvas.class.getResource("SnakeMenu.jpg");
				this.menuImage = Toolkit.getDefaultToolkit().getImage(imagePath);
			}
			catch (Exception e)
			{
				//image does not exist (for some reason)
				e.printStackTrace();
			}
		}
		g.drawImage(menuImage, 0, 0, 640, 480, this);
	}
	
	//This will draw the end game images (win or lose)
	public void drawEndGame(Graphics g)
	{
		BufferedImage endGameImage = new BufferedImage(this.getPreferredSize().width, this.getPreferredSize().height, BufferedImage.TYPE_INT_ARGB);
		Graphics endGameGraphics = endGameImage.getGraphics();
		endGameGraphics.setColor(Color.BLACK);
		if (won)
			endGameGraphics.drawString("You win!!", this.getPreferredSize().width / 2, this.getPreferredSize().height / 2);
		else
			endGameGraphics.drawString("You lose! Sucks to suck.", this.getPreferredSize().width / 2, this.getPreferredSize().height / 2); 
		endGameGraphics.drawString("Your score:" + this.score, this.getPreferredSize().width / 2, (this.getPreferredSize().height / 2) + 20);
		endGameGraphics.drawString("Press SPACE to start a new game!", this.getPreferredSize().width / 2, (this.getPreferredSize().height / 2) + 40);
	
		g.drawImage(endGameImage, 0, 0, this);
	}
	
	//This method will draw the grid on the canvas
	public void drawGrid(Graphics g)
	{
		//draw the rectangular game board
		g.drawRect(0,0,GRID_WIDTH * BOX_WIDTH,GRID_HEIGHT * BOX_HEIGHT);
		
		//now loop through to draw the grid
		for(int x = BOX_WIDTH; x < GRID_WIDTH * BOX_WIDTH; x+=BOX_WIDTH)
		{
			//draw horizontal lines
			g.drawLine(x, 0, x, GRID_HEIGHT * BOX_HEIGHT);
			
			//draw vertical lines
			g.drawLine(0, x, GRID_HEIGHT * BOX_HEIGHT, x);
		}
	}
	 
	public void drawSnake(Graphics g)
	{
		//make the snake green, because everybody knows snakes are green
		g.setColor(Color.GREEN);
		
		//loop through all of the elements in the snake LL
		for(Point p: snake)
		{
			g.fillRect(p.x * BOX_WIDTH, p.y * BOX_HEIGHT, BOX_WIDTH, BOX_HEIGHT);
		}
		g.setColor(Color.BLACK);
	}
	
	public void drawFruit(Graphics g)
	{
		g.setColor(Color.RED);
		g.fillOval(fruit.x * BOX_WIDTH, fruit.y * BOX_HEIGHT, BOX_WIDTH, BOX_HEIGHT);
		g.setColor(Color.BLACK);
	}
	
	public void drawScore(Graphics g)
	{
		g.drawString("Score: "+ score, 0, BOX_HEIGHT * GRID_HEIGHT + 10);
		g.drawString("High Score: " + highScore, 0, BOX_HEIGHT * GRID_HEIGHT + 25);
	}
	
	public void placeFruit()
	{
		Random rand = new Random();
		//generate an X and Y number that is contained in the grid
		int randX = rand.nextInt(GRID_WIDTH);
		int	randY = rand.nextInt(GRID_HEIGHT);
		Point randomPoint = new Point(randX, randY);
		
		//keep generating random points until it is outside of the snake
		while(snake.contains(randomPoint))
		{
			randX = rand.nextInt(GRID_WIDTH);
			randY = rand.nextInt(GRID_HEIGHT);
			randomPoint = new Point(randX, randY);
		}
		fruit = randomPoint;
	}
	
	public void spawnNewSnake()
	{
		score = 0;
		snake.clear();
		snake.add(new Point(0,0));
		snake.add(new Point(0,1));
		snake.add(new Point(0,2));
		theDirection = direction.NO_DIRECTION; 
	}
	
	//this is the constantly running method that keeps the game going
	public void run() 
	{
		while(true)
		{
			//basic applet functionality of updating and painting after each move
			repaint();
			if(!inMenu && !endGame)
				move();
			
			//try to sleep for 100ms
			try
			{
				//DIFFICULTY SETTINGS: 
				//VALERIE:200MS  EASY:150 NORMAL:100MS  CHALLENGING:75	 HARD:50
				Thread.currentThread();
				Thread.sleep(100);
			}
			
			//otherwise print the stack to find the error
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
	}
	
	public String getHighScore()
	{
		//returns a high score with this format: <name>:<score>
		FileReader readFile = null;
		BufferedReader reader = null;
		try
		{
			//read the high score data file
			readFile = new FileReader("highscore.dat");
			reader = new BufferedReader(readFile);
			return reader.readLine();
		}
		catch (Exception e)
		{
			//if there's an error, return a high score of 0
			return "Nobody:0";
		}
		//now close the reader
		finally
		{
			try {
				if(reader != null)
					reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	//NOT SURE WHAT THIS DOES YET WHEN I REMOVE THE CONSOLE GIVES ME ERRORS
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode())
		{
		 case KeyEvent.VK_UP:
			 if(theDirection != direction.SOUTH)
				 theDirection = direction.NORTH;
			 break;
		 case KeyEvent.VK_DOWN:
			 if(theDirection != direction.NORTH)
				 theDirection = direction.SOUTH;
			 break;
		 case KeyEvent.VK_RIGHT:
			 if(theDirection != direction.WEST)
				 theDirection = direction.EAST;
			 break;
		 case KeyEvent.VK_LEFT:
			 if(theDirection != direction.EAST)
				 theDirection = direction.WEST;
			 break;	
		 case KeyEvent.VK_ENTER:
			 if(inMenu)
			 {
				 inMenu = false;
				 repaint();
			 }
			 break;
		 case KeyEvent.VK_ESCAPE:
			 inMenu = true;
			 break;
		 case KeyEvent.VK_SPACE:
			 if(endGame)
			 {
				 endGame = false;
				 won = false;
				 spawnNewSnake();
				 repaint();
			 }
			 break;
		}
		
	}
	
	//NOT SURE WHAT THIS DOES YET WHEN I REMOVE THE CONSOLE GIVES ME ERRORS
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
}
