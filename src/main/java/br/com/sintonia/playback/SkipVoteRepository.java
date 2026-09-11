package br.com.sintonia.playback;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SkipVoteRepository extends JpaRepository<SkipVote, Long> {

    boolean existsByPlaybackIdAndUserId(Long playbackId, Long userId);

    long countByPlaybackId(Long playbackId);
}