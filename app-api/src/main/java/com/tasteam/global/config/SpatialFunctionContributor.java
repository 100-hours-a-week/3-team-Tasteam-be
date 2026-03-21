package com.tasteam.global.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

public class SpatialFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		functionContributions.getFunctionRegistry().registerPattern(
			"st_dwithin_geo",
			"CASE WHEN ST_DWithin(geography(?1), geography(ST_MakePoint(?2, ?3)), ?4) THEN 1 ELSE 0 END",
			functionContributions.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve(StandardBasicTypes.INTEGER));
	}
}
