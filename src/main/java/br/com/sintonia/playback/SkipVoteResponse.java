package br.com.sintonia.playback;

public record SkipVoteResponse(Long playbackId, long votes, long requiredVotes, boolean skipped) {
}
