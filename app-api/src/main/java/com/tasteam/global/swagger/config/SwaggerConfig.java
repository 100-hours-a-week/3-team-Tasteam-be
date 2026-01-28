package com.tasteam.global.swagger.config;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import com.tasteam.global.dto.api.ErrorResponse;
import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	private static final String BEARER_AUTH = "BearerAuth";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(apiInfo())
			.components(new Components()
				.addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}

	private Info apiInfo() {
		return new Info()
			.title("Tasteam API")
			.description("Tasteam 서비스 API 문서")
			.version("1.0.0");
	}

	@Bean
	public OperationCustomizer customOperationCustomizer() {
		return (operation, handlerMethod) -> {
			CustomErrorResponseDescription annotation = findCustomExceptionDescription(handlerMethod);
			addErrorResponses(operation, annotation);

			return operation;
		};
	}

	/// =========== Swagger 커스텀 에러 응답 ============== ///

	/**
	 * 컨트롤러 및 구현한 인터페이스에서 @CustomErrorResponseDescription을 탐색한다.
	 */
	private CustomErrorResponseDescription findCustomExceptionDescription(HandlerMethod handlerMethod) {
		CustomErrorResponseDescription annotation = handlerMethod
			.getMethodAnnotation(CustomErrorResponseDescription.class);

		if (annotation != null) {
			return annotation;
		}

		Class<?> beanType = handlerMethod.getBeanType();
		Class<?>[] interfaces = beanType.getInterfaces();

		for (Class<?> iface : interfaces) {
			try {
				java.lang.reflect.Method interfaceMethod = iface.getMethod(
					handlerMethod.getMethod().getName(),
					handlerMethod.getMethod().getParameterTypes());

				CustomErrorResponseDescription interfaceAnnotation = interfaceMethod
					.getAnnotation(CustomErrorResponseDescription.class);

				if (interfaceAnnotation != null) {
					return interfaceAnnotation;
				}
			} catch (NoSuchMethodException ignored) {
				// 인터페이스에 동일 시그니처 메서드가 없으면 건너뛴다.
			}
		}

		return null;
	}

	/**
	 * 찾은 @CustomErrorResponseDescription을 기반으로 각 ErrorCode에 대한 에러 응답 스펙을 Swagger 문서에 추가한다.
	 */
	private void addErrorResponses(Operation operation, CustomErrorResponseDescription annotation) {
		if (annotation == null) {
			return;
		}

		SwaggerErrorResponseDescription responseDescription = resolveErrorDescription(annotation);
		if (responseDescription == null) {
			return;
		}
		ApiResponses apiResponses = operation.getResponses();

		for (ErrorCode errorCode : responseDescription.getErrorCodeList()) {
			String statusCode = String.valueOf(errorCode.getHttpStatus().value());
			ApiResponse apiResponse = createErrorApiResponse(errorCode);
			apiResponses.addApiResponse(statusCode, apiResponse);
		}
	}

	private SwaggerErrorResponseDescription resolveErrorDescription(CustomErrorResponseDescription annotation) {
		Class<? extends SwaggerErrorResponseDescription> enumClass = annotation.value();
		String group = annotation.group();

		if (!enumClass.isEnum()) {
			return null;
		}

		SwaggerErrorResponseDescription[] constants = enumClass.getEnumConstants();
		for (SwaggerErrorResponseDescription constant : constants) {
			if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(group)) {
				return constant;
			}
		}

		return null;
	}

	private ApiResponse createErrorApiResponse(ErrorCode errorCode) {
		String description = errorCode.getMessage();

		ErrorResponse<Void> example = ErrorResponse.of(errorCode.name(), errorCode.getMessage());

		Schema<?> errorSchema = new Schema<>()
			.type("object")
			.addProperty("success", new Schema<>().type("boolean").example(false))
			.addProperty("code", new Schema<>().type("string").example(errorCode.name()))
			.addProperty("message", new Schema<>().type("string").example(errorCode.getMessage()))
			.addProperty("errors", new Schema<>().type("object"));

		return new ApiResponse()
			.description(description)
			.content(new Content()
				.addMediaType("application/json",
					new MediaType()
						.schema(errorSchema)
						.example(example)));
	}
}
