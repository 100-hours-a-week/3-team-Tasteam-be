package com.tasteam.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.chat.entity.ChatMessage;
import com.tasteam.domain.chat.type.ChatMessageType;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("[유닛](Chat) Chat 시퀀스 정렬 테스트")
class ChatSequenceAlignmentRepositoryTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Test
	@DisplayName("채팅 시퀀스 정렬 마이그레이션은 JPA 시퀀스와 테이블 기본 시퀀스를 하나로 맞춘다")
	void alignChatSequences_unifiesJpaAndColumnDefaults() throws Exception {
		// given
		insertChatFixtures();
		createConflictingDefaultSequences();

		// when
		executeMigration("db/migration/V202603141730__align_chat_sequence_defaults.sql");

		// then
		assertDefaultSequence("chat_room", "chat_room_id_seq");
		assertDefaultSequence("chat_room_member", "chat_room_member_id_seq");
		assertDefaultSequence("chat_message", "chat_message_id_seq");
		assertDefaultSequence("chat_message_file", "chat_message_file_id_seq");

		ChatMessage savedMessage = chatMessageRepository.saveAndFlush(ChatMessage.builder()
			.chatRoomId(4001L)
			.memberId(1001L)
			.type(ChatMessageType.TEXT)
			.content("JPA message")
			.deletedAt(null)
			.build());

		Number sqlInsertedId = (Number)entityManager.createNativeQuery("""
			INSERT INTO chat_message (chat_room_id, member_id, type, content, created_at)
			VALUES (4001, 1001, 'TEXT', 'SQL message', now())
			RETURNING id
			""").getSingleResult();

		assertThat(savedMessage.getId()).isGreaterThan(5001L);
		assertThat(sqlInsertedId.longValue()).isGreaterThan(savedMessage.getId());
	}

	private void insertChatFixtures() {
		entityManager.createNativeQuery("""
			INSERT INTO member (id, nickname, role, status, created_at, updated_at)
			VALUES (1001, '채팅회원', 'USER', 'ACTIVE', now(), now())
			""").executeUpdate();
		entityManager.createNativeQuery("""
			INSERT INTO "group" (id, name, type, address, location, join_type, status, created_at, updated_at)
			VALUES (2001, '채팅그룹', 'UNOFFICIAL', '서울특별시 마포구',
			        ST_SetSRID(ST_MakePoint(126.9, 37.5), 4326), 'PASSWORD', 'ACTIVE', now(), now())
			""").executeUpdate();
		entityManager.createNativeQuery("""
			INSERT INTO subgroup (id, group_id, name, join_type, status, member_count, created_at, updated_at)
			VALUES (3001, 2001, '채팅서브그룹', 'OPEN', 'ACTIVE', 1, now(), now())
			""").executeUpdate();
		entityManager.createNativeQuery("""
			INSERT INTO chat_room (id, subgroup_id, created_at)
			VALUES (4001, 3001, now())
			""").executeUpdate();
		entityManager.createNativeQuery("""
			INSERT INTO chat_message (id, chat_room_id, member_id, type, content, created_at)
			VALUES (5001, 4001, 1001, 'TEXT', 'seed message', now())
			""").executeUpdate();
		entityManager.flush();
		entityManager.clear();
	}

	private void createConflictingDefaultSequences() {
		jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS chat_room_id_seq1 START WITH 1");
		jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS chat_room_member_id_seq1 START WITH 1");
		jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS chat_message_id_seq1 START WITH 1");
		jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS chat_message_file_id_seq1 START WITH 1");

		jdbcTemplate
			.execute("ALTER TABLE chat_room ALTER COLUMN id SET DEFAULT nextval('chat_room_id_seq1'::regclass)");
		jdbcTemplate.execute(
			"ALTER TABLE chat_room_member ALTER COLUMN id SET DEFAULT nextval('chat_room_member_id_seq1'::regclass)");
		jdbcTemplate.execute(
			"ALTER TABLE chat_message ALTER COLUMN id SET DEFAULT nextval('chat_message_id_seq1'::regclass)");
		jdbcTemplate.execute(
			"ALTER TABLE chat_message_file ALTER COLUMN id SET DEFAULT nextval('chat_message_file_id_seq1'::regclass)");

		entityManager.createNativeQuery("SELECT setval('chat_room_id_seq', 1, true)").getSingleResult();
		entityManager.createNativeQuery("SELECT setval('chat_room_member_id_seq', 1, true)").getSingleResult();
		entityManager.createNativeQuery("SELECT setval('chat_message_id_seq', 1, true)").getSingleResult();
		entityManager.createNativeQuery("SELECT setval('chat_message_file_id_seq', 1, true)").getSingleResult();
		entityManager.createNativeQuery("SELECT setval('chat_room_id_seq1', 4001, true)").getSingleResult();
		entityManager.createNativeQuery("SELECT setval('chat_message_id_seq1', 5001, true)").getSingleResult();
		entityManager.flush();
		entityManager.clear();
	}

	private void executeMigration(String classpathLocation) throws Exception {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		Connection connection = DataSourceUtils.getConnection(dataSource);
		try {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(resource), false, false,
				ScriptUtils.DEFAULT_COMMENT_PREFIX, ScriptUtils.EOF_STATEMENT_SEPARATOR,
				ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER, ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
	}

	private void assertDefaultSequence(String tableName, String expectedSequenceName) {
		String actualSequence = jdbcTemplate.queryForObject(
			"SELECT pg_get_serial_sequence(?, 'id')",
			String.class,
			tableName);
		String actualDefault = jdbcTemplate.queryForObject("""
			SELECT column_default
			FROM information_schema.columns
			WHERE table_schema = 'public'
			  AND table_name = ?
			  AND column_name = 'id'
			""", String.class, tableName);

		assertThat(actualSequence).isEqualTo("public." + expectedSequenceName);
		assertThat(actualDefault).isEqualTo("nextval('" + expectedSequenceName + "'::regclass)");
	}
}
