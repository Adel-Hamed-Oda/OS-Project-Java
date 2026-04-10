package Mutex;

public class mutex {
  private int userInput, userOutput ,File ;
	int[] input,output,file;
	int inputcounter,outputcounter,filecounter ;
	public mutex(){
		 userOutput  = 1;
		 userInput = 1 ;
		 File = 1 ;
		 input = new int[5];
		 output = new int[5];
		 file = new int[5];
		 inputcounter = -1;
		 outputcounter = -1;
		 filecounter = -1 ;
	}
	public void waitinput(int processid){
		if (userInput == 1){
			userInput = 0 ;
			// system call for input device using the processid 
		}else {
			if(inputcounter != input.length ){
				input[inputcounter] = processid ;
				
				userInput -= 1 ;
				// add to blocked and call the schceduler to see what it will do 
				// move process to the list of blocked processes 
			}
		}
	}

	public void waitoutput(int processid){
		if (userOutput == 1){
			userOutput = 0 ;
			// system call for output device using the processid 
		}else {
			if(outputcounter != output.length ){
				output[outputcounter] = processid ;
				
				userOutput -= 1 ;
				// add to blocked and call the schceduler to see what it will do 
				// move process to the list of blocked processes  
			}
		}
	}

	public void waitmemory(int processid){
		if (File == 1){
			File = 0 ;
			// system call for memory using the processid 
		}else {
			if(filecounter != file.length ){
				file[filecounter] = processid ;
				
				File -= 1 ;
				// add to blocked and call the schceduler to see what it will do  
				// move process to the list of blocked processes 
			}
		}
	}

	public void signalinput(){
		userInput += 1 ;
		if(userInput != 1){
			// send input[inputcounter] to ready queue
			inputcounter -= 1;
			signalinput();
		}
	}
	public void signaloutput(){
		userOutput += 1 ;
		if(userOutput != 1){
			// send output[outputcounter] to ready queue
			outputcounter -= 1;
			signaloutput();
		}
	}
	public void signalmemory(){
		File += 1 ;
		if(File != 1){
			// send file[filecounter] to ready queue
			inputcounter -= 1;
			signalmemory();
		}
	}
	public int[] blockedinputlist(){
		
		return input ;
	}
	public int[] blockedmemorylist(){
		
		return file ;
	}
	public int[] blockedoutputlist(){
		
		return output ;
	}

}
