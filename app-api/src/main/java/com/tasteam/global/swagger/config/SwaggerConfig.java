package com.tasteam.global.swagger.config;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import com.tasteam.global.dto.api.ErrorResponse;
import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.tags.Tag;
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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class SwaggerConfig {

	private static final String BEARER_AUTH = "BearerAuth";

	private final ApplicationContext applicationContext;

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

	@Bean
	public GlobalOpenApiCustomizer sortTagsCustomizer() {
		return openApi -> {
			Map<String, Integer> tagOrderMap = new HashMap<>();

			applicationContext.getBeansWithAnnotation(RestController.class).values().stream()
				.map(AopUtils::getTargetClass)
				.flatMap(clazz -> Arrays.stream(clazz.getInterfaces()))
				.distinct()
				.filter(iface -> iface.isAnnotationPresent(SwaggerTagOrder.class)
					&& iface.isAnnotationPresent(Tag.class))
				.forEach(iface -> {
					int order = iface.getAnnotation(SwaggerTagOrder.class).value();
					String tagName = iface.getAnnotation(Tag.class).name();
					tagOrderMap.putIfAbsent(tagName, order);
				});

			if (openApi.getTags() != null) {
				openApi.getTags().sort(
					Comparator.comparingInt(tag -> tagOrderMap.getOrDefault(tag.getName(), Integer.MAX_VALUE)));
			}
		};
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
}
