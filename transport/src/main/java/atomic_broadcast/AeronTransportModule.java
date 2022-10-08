package atomic_broadcast;

import atomic_broadcast.client.ClientTransport;
import atomic_broadcast.client.TransportSession;
import atomic_broadcast.sequencer.SequencerTransport;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.TransportParams;

public class AeronTransportModule implements Module {

    private final AeronModule aeronModule;
    private final TransportParams params;

    private TransportSession transport;

    public AeronTransportModule(AeronModule aeronModule, TransportParams params) {
        this.aeronModule = aeronModule;
        this.params = params;
    }


    @Override
    public void start() {
        switch (params.connectAs()) {
            case Client:
                transport = new ClientTransport(params);
            case Sequencer:
                transport = new SequencerTransport(params);
            default:
                throw new IllegalArgumentException("error: trying to connect as: " + params.connectAs());
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void poll() {

    }
}