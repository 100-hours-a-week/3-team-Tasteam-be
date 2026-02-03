package db.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;

/**
 * Rebuilds enum-based CHECK constraints so DB stays aligned with Java enums.
 * The checksum is derived from the enum values; any enum change triggers re-run.
 */
public class R__sync_enum_check_constraints extends BaseJavaMigration {

	@Override
	public Integer getChecksum() {
		String domainValues = joinEnumNames(DomainType.values());
		String purposeValues = joinEnumNames(FilePurpose.values());
		return Arrays.hashCode(new String[] {domainValues, purposeValues});
	}

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();

		rebuildDomainTypeConstraint(connection);
		rebuildFilePurposeConstraint(connection);
	}

	private void rebuildDomainTypeConstraint(Connection connection) throws SQLException {
		String allowed = joinEnumNames(DomainType.values());
		String sql = "ALTER TABLE domain_image "
			+ "DROP CONSTRAINT IF EXISTS chk_domain_image_domain_type; "
			+ "ALTER TABLE domain_image "
			+ "ADD CONSTRAINT chk_domain_image_domain_type "
			+ "CHECK (domain_type IN (" + allowed + "))";
		execute(connection, sql);
	}

	private void rebuildFilePurposeConstraint(Connection connection) throws SQLException {
		String allowed = joinEnumNames(FilePurpose.values());
		String sql = "ALTER TABLE image "
			+ "DROP CONSTRAINT IF EXISTS chk_image_purpose; "
			+ "ALTER TABLE image "
			+ "ADD CONSTRAINT chk_image_purpose "
			+ "CHECK (purpose IN (" + allowed + "))";
		execute(connection, sql);
	}

	private void execute(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	private String joinEnumNames(Enum<?>[] enums) {
		return Arrays.stream(enums)
			.map(Enum::name)
			.map(name -> "'" + name + "'")
			.collect(Collectors.joining(", "));
	}
}
