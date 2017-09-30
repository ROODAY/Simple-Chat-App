public class TestThread {
   public static void main(String args[]) {
   
      MultiThreadExample T1 = new MultiThreadExample( "Thread-1");
      T1.start();
      
       MultiThreadExample T2 = new MultiThreadExample( "Thread-2");
      T2.start();
   }   
}


class MultiThreadExample extends Thread {
    private Thread t; 
    private String name; 
    
    
    MultiThreadExample (String threadName) {
        this.name = threadName; 
        System.out.println("Created the thread: " + this.name);
        
    }
    
    public void run() {
        System.out.println("Running " +  name );
        try {
            for(int i = 0; i < 5; i++) {
                System.out.println("Thread: " + name + ", " + i);
                // Let the thread sleep for a while.
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            System.out.println("Thread " +  name + " interrupted.");
        }
        System.out.println("Thread " +  name + " exiting.");
    }
    
    
    public void start() {
        System.out.println("Starting " +  name );
        if (t == null)
        {
            t = new Thread (this, name);
            t.start ();
        }
    }
}
