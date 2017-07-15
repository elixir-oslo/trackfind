package no.uio.ifi.trackfind.frontend.components.client;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.shared.ui.Connect;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;

@Connect(KeyboardInterceptorExtension.class)
public class KeyboardInterceptorExtensionConnector extends AbstractExtensionConnector {

    @Override
    protected void extend(final ServerConnector target) {
        final KeyboardInterceptorExtensionServerRpc rpcProxy = getRpcProxy(KeyboardInterceptorExtensionServerRpc.class);
        final RootPanel rootPanel = RootPanel.get();

        rootPanel.addDomHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.isShiftKeyDown()) {
                    rpcProxy.setShiftKeyDown(true);
                }
                if (event.isAltKeyDown()) {
                    rpcProxy.setAltKeyDown(true);
                }
                if (event.isControlKeyDown()) {
                    rpcProxy.setControlKeyDown(true);
                }
                if (event.isMetaKeyDown()) {
                    rpcProxy.setMetaKeyDown(true);
                }
            }
        }, KeyDownEvent.getType());

        rootPanel.addDomHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (!event.isShiftKeyDown()) {
                    rpcProxy.setShiftKeyDown(false);
                }
                if (!event.isAltKeyDown()) {
                    rpcProxy.setAltKeyDown(false);
                }
                if (!event.isControlKeyDown()) {
                    rpcProxy.setControlKeyDown(false);
                }
                if (!event.isMetaKeyDown()) {
                    rpcProxy.setMetaKeyDown(false);
                }
            }
        }, KeyUpEvent.getType());
    }

}
