package net.floodlightcontroller.portmod;

/**
 * Exception thrown during port modification activities.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class PortModException extends Exception {
    public PortModException(String m) {
        super(m);
    }

    public PortModException(Throwable t) {
        super(t);
    }

    public PortModException(String m, Throwable t) {
        super(m, t);
    }
}
