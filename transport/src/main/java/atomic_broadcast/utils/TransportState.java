package atomic_broadcast.utils;

public enum TransportState implements CodedEnum {
    NoState(-1),
    ConnectToJournalSource(0),
    FindJournal(1),
    CreateEventJournal(2),
    StartReplayMerge(3),
    PollEventStream(4),

    //sequencer specific states
    FindLeader(5),
    StartReplication(6),
    StopRepliaction(7),
    StartReplay(8),
    ConnectToCommandStream(9),
    CreateEventStream(10),
    ExtendEventStream(11),
    ExtendEventJournal(12),
    PollCommandStream(13);



    private int code;

    TransportState(int code) {
        this.code = code;
    }
    @Override
    public int getCode() {
        return code;
    }
}
