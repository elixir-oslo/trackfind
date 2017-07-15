package no.uio.ifi.trackfind.frontend.components.client;

import com.vaadin.shared.communication.ServerRpc;

public interface KeyboardInterceptorExtensionServerRpc extends ServerRpc {

    void setShiftKeyDown(boolean state);

    void setAltKeyDown(boolean state);

    void setControlKeyDown(boolean state);

    void setMetaKeyDown(boolean state);

}
