package com.tasteam.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class ArchitectureRulesTest {

	private static final String TARGET_PACKAGE = "com.tasteam..";

	private static final JavaClasses CLASSES = new ClassFileImporter()
		.importPackages("com.tasteam");

	@Nested
	class LombokConstructorRule {

		@Test
		void 인터페이스_필드가_있는_빈_클래스는_Lombok_생성자를_사용하면_안된다() {
			ArchRule rule = classes()
				.that().resideInAPackage(TARGET_PACKAGE)
				.and().areAnnotatedWith(Component.class)
				.or().areAnnotatedWith(Service.class)
				.or().areAnnotatedWith(Repository.class)
				.or().areAnnotatedWith(Configuration.class)
				.should(notHaveLombokConstructorWithInterfaceField())
				.because("인터페이스 타입 필드 주입 시 Lombok 생성자는 @Qualifier를 명시할 수 없습니다. "
					+ "명시적 생성자를 작성하고 각 파라미터에 @Qualifier를 붙여주세요.");

			rule.check(CLASSES);
		}
	}

	@Nested
	class QualifiedInterfaceInjectionRule {

		private static final Set<String> MULTI_BEAN_INTERFACE_NAMES = findMultiBeanInterfaceNames(CLASSES);

		@Test
		void 인터페이스_타입_생성자_파라미터에는_반드시_Qualifier가_있어야_한다() {
			ArchRule rule = classes()
				.that().resideInAPackage(TARGET_PACKAGE)
				.and().areAnnotatedWith(Component.class)
				.or().areAnnotatedWith(Service.class)
				.or().areAnnotatedWith(Repository.class)
				.or().areAnnotatedWith(Configuration.class)
				.should(haveQualifierOnInterfaceConstructorParameters())
				.because("인터페이스 타입 파라미터는 구현체가 여러 개일 수 있습니다. "
					+ "@Qualifier(\"빈이름\")으로 어떤 구현체를 주입받을지 명시해주세요.");

			rule.check(CLASSES);
		}

		private ArchCondition<JavaClass> haveQualifierOnInterfaceConstructorParameters() {
			return new ArchCondition<>("인터페이스 타입 생성자 파라미터에 @Qualifier가 있을 것") {
				@Override
				public void check(JavaClass clazz, ConditionEvents events) {
					for (JavaConstructor constructor : clazz.getConstructors()) {
						int parameterIndex = 0;
						for (JavaParameter parameter : constructor.getParameters()) {
							JavaClass paramType = parameter.getRawType();

							if (!paramType.getPackageName().startsWith("com.tasteam")) {
								parameterIndex++;
								continue;
							}
							if (!paramType.isInterface()) {
								parameterIndex++;
								continue;
							}
							if (!MULTI_BEAN_INTERFACE_NAMES.contains(paramType.getFullName())) {
								parameterIndex++;
								continue;
							}

							boolean hasQualifier = hasQualifierAnnotation(parameter);
							if (!hasQualifier) {
								String message = String.format(
									"[%s] 생성자 파라미터 #%d(%s)에 @Qualifier가 없습니다. "
										+ "수정: @Qualifier(\"빈이름\") 추가",
									clazz.getName(),
									parameterIndex,
									paramType.getSimpleName());
								events.add(SimpleConditionEvent.violated(clazz, message));
							}
							parameterIndex++;
						}
					}
				}
			};
		}
	}

	private static ArchCondition<JavaClass> notHaveLombokConstructorWithInterfaceField() {
		return new ArchCondition<>("Lombok 생성자와 인터페이스 타입 필드를 함께 사용하지 않을 것") {
			@Override
			public void check(JavaClass clazz, ConditionEvents events) {
				boolean hasLombokConstructor = hasLombokConstructorAnnotation(clazz);
				if (!hasLombokConstructor) {
					return;
				}

				for (JavaField field : clazz.getFields()) {
					if (!field.getModifiers().contains(JavaModifier.FINAL)) {
						continue;
					}
					if (!field.getRawType().isInterface()) {
						continue;
					}

					String message = String.format(
						"[%s] 필드 '%s'은 인터페이스 타입인데 Lombok 생성자가 사용되고 있습니다. "
							+ "수정: Lombok 생성자 제거 후 명시적 생성자 작성, 파라미터에 @Qualifier(\"%s\") 추가",
						clazz.getName(),
						field.getName(),
						field.getName());
					events.add(SimpleConditionEvent.violated(clazz, message));
				}
			}
		};
	}

	private static boolean hasLombokConstructorAnnotation(JavaClass clazz) {
		return clazz.getAnnotations().stream()
			.map(annotation -> annotation.getRawType().getFullName())
			.anyMatch(name -> name.equals("lombok.RequiredArgsConstructor")
				|| name.equals("lombok.AllArgsConstructor"));
	}

	private static boolean hasQualifierAnnotation(JavaParameter parameter) {
		if (parameter.isAnnotatedWith(Qualifier.class)) {
			return true;
		}
		return parameter.getAnnotations().stream()
			.anyMatch(annotation -> annotation.getRawType().isMetaAnnotatedWith(Qualifier.class));
	}

	private static Set<String> findMultiBeanInterfaceNames(JavaClasses classes) {
		Set<JavaClass> beanClasses = classes.stream()
			.filter(ArchitectureRulesTest::isSpringBean)
			.filter(clazz -> !clazz.isInterface())
			.filter(clazz -> !clazz.getModifiers().contains(JavaModifier.ABSTRACT))
			.filter(clazz -> !isConditionalBean(clazz))
			.collect(Collectors.toSet());

		Set<String> interfaceNames = new HashSet<>();
		for (JavaClass beanClass : beanClasses) {
			for (JavaClass iface : beanClass.getAllRawInterfaces()) {
				if (!iface.getPackageName().startsWith("com.tasteam")) {
					continue;
				}
				interfaceNames.add(iface.getFullName());
			}
		}

		return interfaceNames.stream()
			.filter(name -> countImplementingBeans(name, beanClasses) >= 2)
			.collect(Collectors.toSet());
	}

	private static int countImplementingBeans(String interfaceName, Set<JavaClass> beanClasses) {
		int count = 0;
		for (JavaClass beanClass : beanClasses) {
			boolean matches = beanClass.getAllRawInterfaces().stream()
				.anyMatch(iface -> iface.getFullName().equals(interfaceName));
			if (matches) {
				count++;
			}
		}
		return count;
	}

	private static boolean isSpringBean(JavaClass clazz) {
		return clazz.isAnnotatedWith(Component.class)
			|| clazz.isAnnotatedWith(Service.class)
			|| clazz.isAnnotatedWith(Repository.class)
			|| clazz.isAnnotatedWith(Configuration.class);
	}

	private static boolean isConditionalBean(JavaClass clazz) {
		return clazz.isAnnotatedWith(ConditionalOnProperty.class)
			|| clazz.isAnnotatedWith(Profile.class);
	}
}
