public class OS_Process {
    private final int p_id;
    private final int arrival_time;
    private final int burst_time;
    private int executed_time;
    private boolean is_in_ready_queue;
    private boolean is_blocked;

    public OS_Process(int p_id, int arrival_time, int burst_time) {
        this.p_id = p_id;
        this.arrival_time = arrival_time;
        this.burst_time = burst_time;
        this.executed_time = 0;
        this.is_in_ready_queue = false;
        this.is_blocked = false;
    }

    public int getP_id() {
        return p_id;
    }

    public boolean isBlocked() {
        return is_blocked;
    }

    public void setBlocked(boolean b) {
        this.is_blocked = b;
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

    public boolean is_in_ready_queue() {
        return is_in_ready_queue;
    }

    public void set_in_ready_queue(boolean in_ready_queue) {
        this.is_in_ready_queue = in_ready_queue;
    }

}
