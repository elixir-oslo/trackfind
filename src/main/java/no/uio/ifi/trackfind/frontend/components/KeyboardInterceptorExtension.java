package no.uio.ifi.trackfind.frontend.components;

import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.AbstractExtension;
import no.uio.ifi.trackfind.frontend.components.client.KeyboardInterceptorExtensionServerRpc;

public class KeyboardInterceptorExtension extends AbstractExtension {

    private boolean shiftKeyDown;
    private boolean altKeyDown;
    private boolean controlKeyDown;
    private boolean metaKeyDown;

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

    public boolean isShiftKeyDown() {
        return shiftKeyDown;
    }

    public boolean isAltKeyDown() {
        return altKeyDown;
    }

    public boolean isControlKeyDown() {
        return controlKeyDown;
    }

    public boolean isMetaKeyDown() {
        return metaKeyDown;
    }

}
