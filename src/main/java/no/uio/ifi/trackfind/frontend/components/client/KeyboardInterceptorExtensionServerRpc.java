package no.uio.ifi.trackfind.frontend.components.client;

import com.vaadin.shared.communication.ServerRpc;

/**
 * Vaadin ServerRpc for KeyboardInterceptorExtension (see https://vaadin.com/docs/-/part/framework/gwt/gwt-overview.html).
 *
 * @author Dmytro Titov
 */
public interface KeyboardInterceptorExtensionServerRpc extends ServerRpc {

    /**
     * Sets the state of Shift key (up or down).
     *
     * @param state Shift key state.
     */
    void setShiftKeyDown(boolean state);

    /**
     * Sets the state of Alt key (up or down).
     *
     * @param state Alt key state.
     */
    void setAltKeyDown(boolean state);

    /**
     * Sets the state of Control key (up or down).
     *
     * @param state Control key state.
     */
    void setControlKeyDown(boolean state);

    /**
     * Sets the state of Meta key (up or down).
     * Meta key is the Command key on Mac keyboard.
     *
     * @param state Meta key state.
     */
    void setMetaKeyDown(boolean state);

}
