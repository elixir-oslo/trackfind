package no.uio.ifi.trackfind.frontend.components;

import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.AbstractExtension;
import no.uio.ifi.trackfind.frontend.components.client.KeyboardInterceptorExtensionServerRpc;

/**
 * Extends for some component to make it able to intercept Shift, Alt, Control and Meta keys pressing.
 *
 * @author Dmytro Titov
 */
public class KeyboardInterceptorExtension extends AbstractExtension {

    private boolean shiftKeyDown;
    private boolean altKeyDown;
    private boolean controlKeyDown;
    private boolean metaKeyDown;

    /**
     * Constructor that extends input component.
     *
     * @param connector Component connector to extend.
     */
    public KeyboardInterceptorExtension(AbstractClientConnector connector) {
        super.extend(connector);
        registerRpc(new KeyboardInterceptorExtensionServerRpc() {
            @Override
            public void setShiftKeyDown(boolean state) {
                shiftKeyDown = state;
            }

            @Override
            public void setAltKeyDown(boolean state) {
                altKeyDown = state;
            }

            @Override
            public void setControlKeyDown(boolean state) {
                controlKeyDown = state;
            }

            @Override
            public void setMetaKeyDown(boolean state) {
                metaKeyDown = state;
            }
        });
    }

    /**
     * Checks the Shift key state (up or down).
     *
     * @return <code>true</code> for down state, <code>false</code> for up state.
     */
    public boolean isShiftKeyDown() {
        return shiftKeyDown;
    }

    /**
     * Checks the Alt key state (up or down).
     *
     * @return <code>true</code> for down state, <code>false</code> for up state.
     */
    public boolean isAltKeyDown() {
        return altKeyDown;
    }

    /**
     * Checks the Control key state (up or down).
     *
     * @return <code>true</code> for down state, <code>false</code> for up state.
     */
    public boolean isControlKeyDown() {
        return controlKeyDown;
    }

    /**
     * Checks the Meta key state (up or down).
     *
     * @return <code>true</code> for down state, <code>false</code> for up state.
     */
    public boolean isMetaKeyDown() {
        return metaKeyDown;
    }

}
