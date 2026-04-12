package scheduler;

public class Process {
    private int p_id;
    private int arrival_time;
    private int burst_time;
    private int executed_time;

    public Process(int p_id, int arrival_time, int burst_time) {
        this.p_id = p_id;
        this.arrival_time = arrival_time;
        this.burst_time = burst_time;
        this.executed_time = 0;
    }

    public int getP_id() {
        return p_id;
    }

    public int getArrival_time() {
        return arrival_time;
    }       

    public int getBurst_time() {
        return burst_time;
    }

    public int getExecuted_time() {
        return executed_time;
    }

    public void set_Executed_time(int executed_time) {
        this.executed_time = executed_time;
    }

}
