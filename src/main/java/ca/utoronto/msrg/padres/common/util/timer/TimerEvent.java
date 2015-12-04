
package ca.utoronto.msrg.padres.common.util.timer;

/**
 * Event representing a timer. TimerClients set timers using the timer thread
 * and the timer thread returns expired timers (instances of this class) to
 * requester.
 */
public class TimerEvent
{
    public static final int TIMER_STATUS_NULL = 0;
    public static final int TIMER_STATUS_PENDING = 1;
    public static final int TIMER_STATUS_EXPIRED = 2;
    public static final int TIMER_STATUS_CANCELLED = 3;
    
    /** Pointer to next timer to allow for easy queueing. */
    protected TimerEvent m_Next = null;
    
    /** The status of this timer event. */
    protected int m_Status = TIMER_STATUS_NULL;
    
    /** Handle associated with this timer. */
    protected int m_TimerHandle = 0;
    
    /** The amout of time remaining until this timer expires. */
    protected long m_Delay = 0;
    
    /** Arbitrary type for this timer. */
    protected int m_Type = 0; // arbitrary type for timer
    
    /** Arbitrary object reference. */
    protected Object m_Attachment = null;
    
    /** The client that requested this timer. */
    protected TimerClient m_Client = null;
    
    /**
     * Constructs a new timer event with the specified client, delay in
     * milliseconds and arbitrary (application defined) type.
     * 
     * @param client  The TimerClient that will be using this timer
     * @param milliseconds  The amount of time before this timer expires
     * @param type  An arbitrary type for this timer
     */
    public TimerEvent(TimerClient client, long milliseconds, int type)
    {
        m_Next = null;
        m_Status = TIMER_STATUS_NULL;
        m_TimerHandle = 0;
        m_Delay = milliseconds;
        m_Client = client;
        m_Type = type;
    }
    
    /**
     * Constructs a new timer event with the specified client.
     * 
     * @param client  The TimerClient that will be using this timer
     */
    public TimerEvent(TimerClient client)
    {
        this(client, 0, 0);
    }
    
    /**
     * Set the amout of time before this timer expires.
     * 
     * @param milliseconds  The number of milliseconds before this timer expires
     */
    public void setDelay(long milliseconds)
    {
        m_Delay = milliseconds;
    }
    
    /**
     * Get the amount of time remaining before this timer expires.
     * 
     * @return  The amount of time before expiration
     */
    public long getDelay()
    {
        return m_Delay;
    }
    
    /**
     * Set the handle for this timer.
     * 
     * @param handle  The handle
     */
    public void setTimerHandle(int handle)
    {
        m_TimerHandle = handle;
    }
    
    /**
     * Get the handle associated with this timer.
     * 
     * @return  The handle
     */
    public int getTimerHandle()
    {
        return m_TimerHandle;
    }
    
    /**
     * Get the current status of this timer.
     * 
     * @return  The status
     */
    public int getStatus()
    {
        return m_Status;
    }
    
    /**
     * Get the client associated with this timer.
     * 
     * @return  The client
     */
    public TimerClient getClient()
    {
        return m_Client;
    }
    
    /**
     * Set the client associated with this timer.
     * 
     * @param client  The client to be associated with this timer
     */
    public void setClient(TimerClient client)
    {
        m_Client = client;
    }
    
    /**
     * Set this timer's status to cancelled.
     */
    public synchronized void cancelTimer()
    {
        m_Status = TIMER_STATUS_CANCELLED;
    }
    
    /**
     * Get the timer next to this one. This is used for easy queueing of
     * timer events.
     * 
     * @return  The next timer
     */
    public TimerEvent getNext()
    {
        return m_Next;
    }
    
    /**
     * Set the timer event next to this one. This is used for easy queueing of
     * timer events.
     * 
     * @param event  The event next to this one
     */
    public void setNext(TimerEvent event)
    {
        m_Next = event;
    }
    
    /**
     * Get the type of this timer. The type can be anything and is application
     * defined.
     * 
     * @return  The type
     */
    public int getType()
    {
        return m_Type;
    }
    
    /**
     * Set the type of this timer. The type can be anything and is application
     * defined.
     * 
     * @param type  New type for this timer
     */
    public void setType(int type)
    {
        m_Type = type;
    }
    
    public Object getAttachment()
    {
        return m_Attachment;
    }
    
    public void setAttachment(Object attachment)
    {
        m_Attachment = attachment;
    }
}
