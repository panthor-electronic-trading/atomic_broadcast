package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportSession;
import atomic_broadcast.consensus.SeqNoProvider;
import atomic_broadcast.consensus.SeqNumSnapshot;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.concurrent.UnsafeBuffer;

import static atomic_broadcast.utils.TransportState.*;

public class SequencerTransportWorker implements TransportSession {

    private static final Log log = LogFactory.getLog(SequencerTransportWorker.class.getName());

    private final TransportParams params;
    private final SequencerClient transportClient;
    private final SeqNoProvider seqNoProvider;
    private boolean active = false;

    private TransportState state = NoState;

    public SequencerTransportWorker(
            TransportParams params,
            SequencerClient transportClient,
            SeqNoProvider seqNoProvider) {
        this.params = params;
        this.transportClient = transportClient;
        this.seqNoProvider = seqNoProvider;
    }


    @Override
    public boolean isSubscriptionConnected() {
        return transportClient.isSubscriptionConnected();
    }

    @Override
    public boolean isPublicationConnected() {
        return transportClient.isPublicationConnected();
    }

    @Override
    public void start() {
        state = FindLeader;
    }

    @Override
    public void stop() {
        try {
            transportClient.close();
        } catch (Exception e){
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public boolean poll() {
        switch (state) {
            case NoState:
                break;
            case FindLeader:
                determineLeader();
                break;
            case ConnectToJournalSource:
                connectToJournalSource();
                break;
            case FindJournal:
                findJournal();
                break;
            case CreateEventStream:
                createEventStream();
                break;
            case CreateEventJournal:
                createNewJournal();
                break;
            case ConnectToCommandStream:
                connectToCommandStream();
            case PollCommandStream:
                pollCommandStream();
            case StartReplication:
                startReplication();
                break;
            case StopRepliaction:
                stopReplication();
                break;
            case StartReplayMerge:
                state = transportClient.connectToEventStream() ? PollEventStream : StartReplayMerge;
                break;
            case StartReplay:
                break;
            case PollEventStream:
                transportClient.pollEventStream();
                break;
        }

        return true;
    }

    @Override
    public TransportState state() {
        return state;
    }

    private void determineLeader() {
        SeqNumSnapshot snapshot = seqNoProvider.takeSnapshot();
        if(snapshot.isReady()) {
            if (isLeader(snapshot)) {
                active = true;
                setState(ConnectToJournalSource);
            } else {
                setState(StartReplication);
            }
        }
    }

    private void connectToJournalSource() {
        if (transportClient.connectToJournalSource()) {
            setState(FindJournal);
        } else {
            setState(ConnectToJournalSource);
        }
    }

    private void findJournal() {
        boolean journalFound = transportClient.findJournal();
        if (!journalFound) {
            if (active) {
                setState(CreateEventStream);
            } else {
                setState(StartReplication);
            }
        } else {
            if (active) {
                setState(StartReplay);
            } else {
                setState(StartReplication);
            }
        }
    }

    private void createEventStream() {
        boolean eventStreamCreated = transportClient.createEventStream();
        if (eventStreamCreated && isPublicationConnected()) {
            setState(CreateEventJournal);
        }
    }

    private void createNewJournal() {
        boolean isJournalCreated = transportClient.createEventJournal();
        if (isJournalCreated) {
           setState(ConnectToCommandStream);
        }
    }

    private void connectToCommandStream() {
        boolean isSubscriptionCreated = transportClient.connectToCommandStream();
        if (isSubscriptionCreated && isSubscriptionConnected()) {
            setState(PollCommandStream);
        }
    }

    private void pollCommandStream() {
        if (isSubscriptionConnected()) {
            transportClient.pollCommandStream();
        } else {
            setState(ConnectToCommandStream);
        }
    }

    private void startReplication() {
        boolean replicationStarted = transportClient.startReplication();
        if (replicationStarted) {
            setState(StartReplayMerge);
        }
    }

    private void stopReplication() {
        boolean isReplicationStopped = transportClient.stopReplication();
    }

    private void setState(TransportState newState) {
        if (this.state != newState) {
            state = newState;
            log.info().append("new state: ").appendLast(state);
        }
    }

    @Override
    public boolean publish(UnsafeBuffer buffer, int offset, int length) {
        return false;
    }

    private boolean isLeader(SeqNumSnapshot snapshot) {
        return snapshot.leaderInstance() == params.instanceId();
    }
}
