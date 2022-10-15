/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package atomic_broadcast;

import atomic_broadcast.utils.CompositeModule;
import atomic_broadcast.utils.ConnectAs;
import atomic_broadcast.utils.ConnectUsing;
import atomic_broadcast.utils.TransportParams;

public class AllAppsMain {

    public static void main(String[] args) {
        CompositeModule modules = new CompositeModule();

        TransportParams params = new TransportParams();

        params.connectAs(ConnectAs.Client).connectUsing(ConnectUsing.Multicast);

        AeronModule aeronModule = new AeronModule(true, true, false);
        AeronTransportModule transportModule = new AeronTransportModule(aeronModule, params);

        modules.add(aeronModule);
        modules.add(transportModule);

        modules.start();

        modules.close();
    }
}