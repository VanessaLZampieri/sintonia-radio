package br.com.sintonia.websocket;

public record SkipVoteChangedEventPayload(Long playbackId, long votes, long requiredVotes) {
}
