
package ca.utoronto.msrg.padres.common.util.timer;

/**
 * Interface for a timer client.
 */
public interface TimerClient
{
    /**
     * All timer clients have a queue for returned timer events. When the
     * events are returned they should have a status of expired.
     * 
     * @param event  The expired timer event
     */
    public void returnToClientQ(TimerEvent event);
}
