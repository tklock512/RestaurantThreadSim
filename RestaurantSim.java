import java.util.concurrent.Semaphore;
import java.util.Random;

public class RestaurantSim
{
    //global Random object
    public static class RandomGen
    {
	public static Random rand = new Random();
    }
    
    
    public static class Waiter extends Thread
    {
	private int ID;
	private Restaurant r;
	private int customerID;
	private Table table;
	
	public Waiter(Restaurant r, int ID)
	{
	    this.ID = ID;
	    this.r = r;
	}

	public void run()
	{
	    try{
		//waiter chooses table

		switch(ID) //choose table based on ID
	        {
		case 1:
		    table = r.table1;
		    break;
		case 2:
		    table = r.table2;
		    break;
		case 3:
		    table = r.table3;
		    break;
		default:
		    table = r.table1;
		    break;
		}
		while(r.customerCount > 0) //until restaurant is empty
		    {
			//customer calls waiter

			table.pendingOrder.acquire();
			
			//waiter gets customer ID

			customerID = table.callingID;
			
			System.out.println("Waiter " + ID + " has gotten Customer " + customerID + "'s order");
		
			//waiter goes to kitchen

			r.kitchen.acquire();

			int kitchenTime = RandomGen.rand.nextInt(401) + 100; //100-500
			System.out.println("Waiter " + ID + " is using the kitchen for " + kitchenTime + " milliseconds");
			Thread.sleep(kitchenTime);

			//waiter leaves kitchen, waits outside
			r.kitchen.release();
			int waitTime = RandomGen.rand.nextInt(701) + 300; //300-1000
			System.out.println("Waiter " + ID + " is waiting outside for " + waitTime + " milliseconds");
			Thread.sleep(waitTime);

			//waiter reenters kitchen
			r.kitchen.acquire();
			kitchenTime = RandomGen.rand.nextInt(401) + 100; //100-500
			System.out.println("Waiter " + ID + " is fetching from the kitchen for " + kitchenTime + " milliseconds");
			Thread.sleep(kitchenTime);
			r.kitchen.release();

			//waiter brings customer their order

			table.recievedOrder.release();
			System.out.println("Waiter " + ID + " has given Customer " + customerID + " their order");

			//waiter waits for next customer

			if(r.customerCount < 13 && table.seats.availablePermits() == 4) //if there are few customers and your table is empty
			    break; //then you're probably done for the night
		    }
		
		//restaurant is empty

		System.out.println("Waiter " + ID + " is cleaning their table. It's sparkling! They then leave.");
	    
		 
		
	    }
	    catch(Exception e)
		{
		    System.out.println(e);
		}
	}


    }
    
    public static class Customer extends Thread
    {
	private Restaurant r;
	private int ID;
	private Table chosenTable;
	private Line tableLine;
	private int tableNum;
	private int secondChoice;

	
	public Customer(Restaurant r, int ID)
	{
	    this.r = r;
	    this.ID = ID;

	    tableNum = RandomGen.rand.nextInt(3) + 1; //1-3
	    secondChoice = RandomGen.rand.nextInt(3) + 1; //if secondChoice is same as first, no second choice
	    
	    switch(tableNum)
		{
		case 1:
		    chosenTable = r.table1;
		    tableLine = r.line1;
		    break;
		case 2:
		    chosenTable = r.table2;
		    tableLine = r.line2;
		    break;
		case 3:
		    chosenTable = r.table3;
		    tableLine = r.line3;
		    break;
		default:
		    chosenTable = r.table1;
		    tableLine = r.line1;
		    break;
		}
	}

	public void run()
	{
	    try {

		System.out.println("Customer " + ID + " is trying to enter a door");
		r.doors.acquire();

		r.doors.release();
                System.out.println("Customer " + ID + " has entered the restaurant");

		
		//choosing line
		if(tableLine.isLong()) //if first choice is >6
                {
                    System.out.println("Customer " + ID + " found line " + tableNum + " too long.");
                    Line lineLooker;
		    Table tableLooker;
                    switch(secondChoice)
                        {
                        case 1:
                            lineLooker = r.line1;
                            tableLooker = r.table1;
                            break;
                        case 2:
                            lineLooker = r.line2;
                            tableLooker = r.table2;
                            break;
                        case 3:
                            lineLooker = r.line3;
                            tableLooker = r.table3;
                            break;
                        default:
                            lineLooker = r.line1;
                            tableLooker = r.table1;
                            break;
                        }
                    if(!lineLooker.isLong()) //second choice is not long
                        {
                            System.out.println("Customer " + ID + " found line " + tableNum + " too long. Going to line " + secondChoice);
                            chosenTable = tableLooker;
                            tableLine = lineLooker;
                        }
                    //if second choice is also long original choice is used
                }

		tableLine.enterLine(this.ID);
		
		//waiting in line
		
		//at end of line, waiting for table
		chosenTable.getSeat(this.ID);
		tableLine.leaveLine(this.ID);

		//call waiter

		chosenTable.callWaiter(this.ID);
		
		//gotten food
		int eatingTime = RandomGen.rand.nextInt(801) + 200; //in milliseconds, 200-1000
		System.out.println("Customer " + ID + " is eating for " + eatingTime + " milliseconds");
		Thread.sleep(eatingTime);

		//done eating, leave seat
		chosenTable.returnSeat(this.ID);
		r.exitRestaurant(this.ID);
	    }

	    catch(Exception e)
		{
		    System.out.println(e);
		}
	}

	
    }

    
    //class that representing a line
    public static class Line
    {
	public int length;
	public Semaphore queue; //I guess not a real queue, since threads are weird.
	public int num;
	
	public Line(int num)
	{
	    this.num = num;
	    queue = new Semaphore(40); //line is technically infinite, but limited due to >7 rule
	    length = 0;
	}

	public void enterLine(int ID) throws Exception
	{
	    System.out.println("Customer " + ID + " is entering line " + num);
	    queue.acquire();
	    length++;
	}

	public void leaveLine(int ID)
	{
	    System.out.println("Customer " + ID + " has left the line " + num);
	    queue.release();
	    length--;
	}

	public boolean isLong()
	{
	    if(length>6)
		{
		    return true;
		}
	    else
		return false;
	}
    }
    

    //class encompassing the parts of the restaurant: tables, lines, entrances, and kitchen
    static class Restaurant
    {
	private Table table1;
	private Table table2;
	private Table table3;

	private Line line1;
	private Line line2;
	private Line line3;
	
	private Semaphore kitchen;

	private Semaphore doors;

	private int customerCount; //starts at 40. When 0, all customers have left.

	private Semaphore exit;
	
	public Restaurant() throws Exception
	{
	    this.table1 = new Table(1);
	    this.table2 = new Table(2);
	    this.table3 = new Table(3);

	    this.line1 = new Line(1);
	    this.line2 = new Line(2);
	    this.line3 = new Line(3);
	    
	    this.doors = new Semaphore(2);

	    this.kitchen = new Semaphore(1);

	    customerCount = 40;

	    this.exit = new Semaphore(1);
	}

	public void exitRestaurant(int ID) throws Exception
	{
	    exit.acquire();
	    System.out.println("Customer " + ID + " is leaving the restaurant.");
	    customerCount--;
	    exit.release();
	}
    }

    //class for a table with 4 seats
    public static class Table
    {
	public Semaphore seats;
	public int num;
	public int callingID;

	public Semaphore pendingOrder;
	public Semaphore recievedOrder;
	
	public Table(int n) throws Exception
	{
	    seats = new Semaphore(4);
	    num = n;
	    pendingOrder = new Semaphore(1);
	    pendingOrder.acquire();
	    recievedOrder = new Semaphore(1);
	    
	}
	
	public void getSeat(int ID) throws Exception
	{
	    System.out.println("Customer " + ID + " is trying to get a seat at table" + num);
	    seats.acquire();
	    System.out.println("Customer " + ID + " got a seat at table" + num);
	}

	public void returnSeat(int ID)
	{
	    System.out.println("Customer " + ID + " is leaving table" + num);
	    seats.release();
	}

	public void callWaiter(int ID) throws Exception
	{
	    callingID = ID;
	    pendingOrder.release();
	    System.out.println("Customer " + ID + " is calling the waiter at table" + num);
	    Thread.sleep(100);
	    recievedOrder.acquire(); //waiting for waiter to return
	}
    }

    static public void main(String[] args) 
    {
	try{
	    
	Restaurant restaurant = new Restaurant();

	for(int i = 1; i<=3; i++)
	    {
		Waiter w = new Waiter(restaurant, i);
		w.start();
	    }
	
	for(int i = 1; i <= 40; i++)
	    {
		Customer c = new Customer(restaurant, i);
		c.start();
	    }
	}
	catch(Exception e)
	    {
		System.out.println(e);
	    }
    }
}
