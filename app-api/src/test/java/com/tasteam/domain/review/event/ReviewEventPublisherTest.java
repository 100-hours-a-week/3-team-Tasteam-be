package com.tasteam.domain.review.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("ReviewEventPublisher")
class ReviewEventPublisherTest {

	@Test
	@DisplayName("트랜잭션이 없으면 즉시 이벤트를 발행한다")
	void publishReviewCreated_whenNoTransaction_publishesImmediately() {
		ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
		ReviewEventPublisher publisher = new ReviewEventPublisher(mockPublisher);

		publisher.publishReviewCreated(1L);

		then(mockPublisher).should().publishEvent(any(ReviewCreatedEvent.class));
	}
}
