package com.tasteam.domain.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupStatus;

public interface GroupRepository extends JpaRepository<Group, Long> {

	Optional<Group> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByNameAndDeletedAtIsNull(String name);

	@Query("""
		select g
		from Group g
		where g.deletedAt is null
		  and g.status = :status
		  and (
		    lower(g.name) like lower(concat('%', :keyword, '%'))
		    or lower(g.address) like lower(concat('%', :keyword, '%'))
		  )
		order by g.updatedAt desc, g.id desc
		""")
	List<Group> searchByKeyword(
		@Param("keyword")
		String keyword,
		@Param("status")
		GroupStatus status,
		Pageable pageable);

	boolean existsByIdAndDeletedAtIsNull(Long id);
}
