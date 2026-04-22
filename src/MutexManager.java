public class MutexManager {
  	private static int userInput, userOutput, File ;
	
	public static void InitMutexes(){
		userOutput = 1;
		userInput = 1 ;
		File = 1 ;
	}
	
	public static boolean waitinput(int processid){
		if (userInput == 1){
			userInput = 0 ;
			return true ;
			// system call for input device using the processid 
		}else {
			return false ;
		}
	}

	public static boolean waitoutput(int processid){
		if (userOutput == 1){
			userOutput = 0 ;
			// system call for output device using the processid 
			return true ;
		}else {
			return false ;
		}
	}

	public static boolean waitmemory(int processid){
		if (File == 1){
			File = 0 ;
			return true ;
			// system call for memory using the processid 
		}else {
			return false ;
		}
	}

	public static void signalinput(){
		userInput += 1 ;
	}
	public static void signaloutput(){
		userOutput += 1 ;
	}
	public static void signalmemory(){
		File += 1 ;	
	}
}