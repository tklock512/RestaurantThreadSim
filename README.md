# RestaurantThreadSim

A simulation for a restaurant using threads and semaphores. In the simulation, three waiters each wait a table, serving customers. Customers come in through the doors of the restaurant, then pick a line they want to wait in to get a table. They will choose their preferred line unless it is long (there are at least 7 customers waiting in the line), in which case they will look at a different line. If that line is long, they will go to their first choice; if not, their second. The simulation ends once all customers have left, and the waiters clean their tables.

To compile this project, use the make command with the included Makefile
Or, use the javac command on the included .java file.
To run the project, enter:
java RestaurantSim
The makefile also has the command:
make clean
Which deletes all class files in the folder
