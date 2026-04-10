package mutex;

public class Mutex {
  private int userInput, userOutput ,File ;
	int[] input,output,file;
	int inputcounter,outputcounter,filecounter ;
	public Mutex(){
		 userOutput  = 1;
		 userInput = 1 ;
		 File = 1 ;
		
	}
	public boolean waitinput(int processid){
		if (userInput == 1){
			userInput = 0 ;
			return true ;
			// system call for input device using the processid 
		}else {
			return false ;
		}
	}

	public boolean waitoutput(int processid){
		if (userOutput == 1){
			userOutput = 0 ;
			// system call for output device using the processid 
			return true ;
		}else {
			return false ;
		}
	}

	public boolean waitmemory(int processid){
		if (File == 1){
			File = 0 ;
			return true ;
			// system call for memory using the processid 
		}else {
			return false ;
		}
	}

	public void signalinput(){
		userInput += 1 ;
	
	}
	public void signaloutput(){
		userOutput += 1 ;
	
	}
	public void signalmemory(){
		File += 1 ;
		
	}


}
