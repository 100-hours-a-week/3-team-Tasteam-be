package com.tasteam.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.notification.dto.response.NotificationResponse;
import com.tasteam.domain.notification.dto.response.UnreadCountResponse;
import com.tasteam.domain.notification.entity.Notification;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.repository.NotificationRepository;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.exception.business.BusinessException;

@ServiceIntegrationTest
@Transactional
@DisplayName("NotificationService 통합 테스트")
class NotificationServiceIntegrationTest {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private MemberRepository memberRepository;

	private Member member;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(MemberFixture.create("notification@test.com", "알림테스트유저"));
	}

	@Nested
	@DisplayName("알림 생성")
	class CreateNotification {

		@Test
		@DisplayName("알림을 생성하면 DB에 저장된다")
		void createNotification_success() {
			Notification notification = notificationService.createNotification(
				member.getId(),
				NotificationType.SYSTEM,
				"테스트 제목",
				"테스트 본문",
				"/test/link");

			assertThat(notification.getId()).isNotNull();
			assertThat(notification.getMemberId()).isEqualTo(member.getId());
			assertThat(notification.getNotificationType()).isEqualTo(NotificationType.SYSTEM);
			assertThat(notification.getTitle()).isEqualTo("테스트 제목");
			assertThat(notification.getBody()).isEqualTo("테스트 본문");
			assertThat(notification.getDeepLink()).isEqualTo("/test/link");
			assertThat(notification.getReadAt()).isNull();
		}

		@Test
		@DisplayName("deepLink가 null인 알림도 생성 가능하다")
		void createNotification_withoutDeepLink_success() {
			Notification notification = notificationService.createNotification(
				member.getId(),
				NotificationType.NOTICE,
				"공지사항",
				"공지 내용",
				null);

			assertThat(notification.getId()).isNotNull();
			assertThat(notification.getDeepLink()).isNull();
		}
	}

	@Nested
	@DisplayName("알림 목록 조회")
	class GetNotifications {

		@BeforeEach
		void setUp() {
			for (int i = 1; i <= 15; i++) {
				notificationRepository.save(Notification.create(
					member.getId(),
					NotificationType.SYSTEM,
					"제목 " + i,
					"본문 " + i,
					"/link/" + i));
			}
		}

		@Test
		@DisplayName("알림 목록을 페이지 단위로 조회한다")
		void getNotifications_pagination_success() {
			OffsetPageResponse<NotificationResponse> response = notificationService.getNotifications(
				member.getId(), 0, 10);

			assertThat(response.items()).hasSize(10);
			assertThat(response.pagination().totalElements()).isEqualTo(15);
			assertThat(response.pagination().totalPages()).isEqualTo(2);
		}

		@Test
		@DisplayName("알림은 ID 내림차순으로 정렬된다")
		void getNotifications_orderedByIdDesc() {
			OffsetPageResponse<NotificationResponse> response = notificationService.getNotifications(
				member.getId(), 0, 5);

			assertThat(response.items().get(0).title()).isEqualTo("제목 15");
			assertThat(response.items().get(4).title()).isEqualTo("제목 11");
		}

		@Test
		@DisplayName("다른 회원의 알림은 조회되지 않는다")
		void getNotifications_onlyOwnNotifications() {
			Member otherMember = memberRepository.save(MemberFixture.create("other@test.com", "다른유저"));
			notificationRepository.save(Notification.create(
				otherMember.getId(),
				NotificationType.CHAT,
				"다른 회원 알림",
				"다른 회원 본문",
				null));

			OffsetPageResponse<NotificationResponse> response = notificationService.getNotifications(
				member.getId(), 0, 100);

			assertThat(response.pagination().totalElements()).isEqualTo(15);
		}
	}

	@Nested
	@DisplayName("단일 알림 읽음 처리")
	class MarkAsRead {

		private Notification notification;

		@BeforeEach
		void setUp() {
			notification = notificationRepository.save(Notification.create(
				member.getId(),
				NotificationType.SYSTEM,
				"읽음 테스트",
				"읽음 테스트 본문",
				null));
		}

		@Test
		@DisplayName("알림을 읽음 처리하면 readAt이 설정된다")
		void markAsRead_success() {
			notificationService.markAsRead(member.getId(), notification.getId());

			Notification updated = notificationRepository.findById(notification.getId()).get();
			assertThat(updated.getReadAt()).isNotNull();
		}

		@Test
		@DisplayName("존재하지 않는 알림을 읽음 처리하면 예외가 발생한다")
		void markAsRead_notFound_throwsException() {
			assertThatThrownBy(() -> notificationService.markAsRead(member.getId(), 999999L))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("다른 회원의 알림을 읽음 처리하면 예외가 발생한다")
		void markAsRead_otherMember_throwsException() {
			Member otherMember = memberRepository.save(MemberFixture.create("other@test.com", "다른유저"));

			assertThatThrownBy(() -> notificationService.markAsRead(otherMember.getId(), notification.getId()))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("이미 읽은 알림을 다시 읽음 처리해도 에러가 발생하지 않는다")
		void markAsRead_alreadyRead_idempotent() {
			notificationService.markAsRead(member.getId(), notification.getId());
			Notification firstRead = notificationRepository.findById(notification.getId()).get();

			notificationService.markAsRead(member.getId(), notification.getId());

			Notification secondRead = notificationRepository.findById(notification.getId()).get();
			assertThat(secondRead.getReadAt()).isEqualTo(firstRead.getReadAt());
		}
	}

	@Nested
	@DisplayName("전체 알림 읽음 처리")
	class MarkAllAsRead {

		@BeforeEach
		void setUp() {
			for (int i = 1; i <= 5; i++) {
				notificationRepository.save(Notification.create(
					member.getId(),
					NotificationType.SYSTEM,
					"제목 " + i,
					"본문 " + i,
					null));
			}
		}

		@Test
		@DisplayName("모든 알림을 읽음 처리하면 readAt이 설정된다")
		void markAllAsRead_success() {
			notificationService.markAllAsRead(member.getId());

			long unreadCount = notificationRepository.countByMemberIdAndReadAtIsNull(member.getId());
			assertThat(unreadCount).isZero();
		}

		@Test
		@DisplayName("다른 회원의 알림은 영향받지 않는다")
		void markAllAsRead_onlyOwnNotifications() {
			Member otherMember = memberRepository.save(MemberFixture.create("other@test.com", "다른유저"));
			notificationRepository.save(Notification.create(
				otherMember.getId(),
				NotificationType.CHAT,
				"다른 회원 알림",
				"다른 회원 본문",
				null));

			notificationService.markAllAsRead(member.getId());

			long otherUnreadCount = notificationRepository.countByMemberIdAndReadAtIsNull(otherMember.getId());
			assertThat(otherUnreadCount).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("읽지 않은 알림 개수 조회")
	class GetUnreadCount {

		@Test
		@DisplayName("읽지 않은 알림 개수를 반환한다")
		void getUnreadCount_success() {
			for (int i = 1; i <= 3; i++) {
				notificationRepository.save(Notification.create(
					member.getId(),
					NotificationType.SYSTEM,
					"제목 " + i,
					"본문 " + i,
					null));
			}

			UnreadCountResponse response = notificationService.getUnreadCount(member.getId());

			assertThat(response.count()).isEqualTo(3);
		}

		@Test
		@DisplayName("읽은 알림은 개수에 포함되지 않는다")
		void getUnreadCount_excludesRead() {
			Notification readNotification = notificationRepository.save(Notification.create(
				member.getId(),
				NotificationType.SYSTEM,
				"읽은 알림",
				"읽은 알림 본문",
				null));
			notificationService.markAsRead(member.getId(), readNotification.getId());

			notificationRepository.save(Notification.create(
				member.getId(),
				NotificationType.SYSTEM,
				"안 읽은 알림",
				"안 읽은 알림 본문",
				null));

			UnreadCountResponse response = notificationService.getUnreadCount(member.getId());

			assertThat(response.count()).isEqualTo(1);
		}

		@Test
		@DisplayName("알림이 없으면 0을 반환한다")
		void getUnreadCount_noNotifications_returnsZero() {
			UnreadCountResponse response = notificationService.getUnreadCount(member.getId());

			assertThat(response.count()).isZero();
		}
	}

	@Nested
	@DisplayName("알림 생성/조회/읽음 처리 시나리오")
	class NotificationScenario {

		@Test
		@DisplayName("알림 생성 → 조회 → 읽음 처리 → 읽지 않은 개수 확인 시나리오")
		void fullScenario_success() {
			Notification notification1 = notificationService.createNotification(
				member.getId(),
				NotificationType.CHAT,
				"채팅 알림",
				"새 메시지가 도착했습니다",
				"/chat/1");

			Notification notification2 = notificationService.createNotification(
				member.getId(),
				NotificationType.NOTICE,
				"공지사항",
				"새 공지가 등록되었습니다",
				"/notice/1");

			Notification notification3 = notificationService.createNotification(
				member.getId(),
				NotificationType.SYSTEM,
				"시스템 알림",
				"시스템 점검 안내",
				null);

			OffsetPageResponse<NotificationResponse> notifications = notificationService.getNotifications(
				member.getId(), 0, 10);
			assertThat(notifications.items()).hasSize(3);
			assertThat(notifications.items().get(0).title()).isEqualTo("시스템 알림");

			UnreadCountResponse unreadBefore = notificationService.getUnreadCount(member.getId());
			assertThat(unreadBefore.count()).isEqualTo(3);

			notificationService.markAsRead(member.getId(), notification1.getId());

			UnreadCountResponse unreadAfterOne = notificationService.getUnreadCount(member.getId());
			assertThat(unreadAfterOne.count()).isEqualTo(2);

			notificationService.markAllAsRead(member.getId());

			UnreadCountResponse unreadAfterAll = notificationService.getUnreadCount(member.getId());
			assertThat(unreadAfterAll.count()).isZero();

			OffsetPageResponse<NotificationResponse> allRead = notificationService.getNotifications(
				member.getId(), 0, 10);
			assertThat(allRead.items()).allSatisfy(n -> assertThat(n.readAt()).isNotNull());
		}
	}
}
