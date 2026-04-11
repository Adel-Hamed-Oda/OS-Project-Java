package Scheduler;
import java.util.ArrayList;

public class Queue {
    private ArrayList<Integer> arr;
    private int size; 

    public Queue(){
        arr = new ArrayList<Integer>();
        size=0;
    }

    public int getSize(){
        return size;
    }

    public void enqueue(int p){
        arr.add(p);
        size++;
    }

    public int dequeue(){
        if(size==0)
            return -1;
        int res = arr.get(0);
        arr.remove(0);
        size--;
        return res;
    }

    public ArrayList<Integer> getArr() {
        return arr;
    }


}
