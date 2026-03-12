package com.tasteam.global.metrics;

import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.tasteam.global.aop.ObservedExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreadPoolMetricsBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private final ObjectProvider<MeterRegistry> meterRegistryProvider;
	private final ObjectProvider<ThreadPoolExecutorMetricsSupport> metricsSupportProvider;
	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory)beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (!(bean instanceof ThreadPoolTaskExecutor executor)) {
			return bean;
		}
		ObservedExecutor annotation = findAnnotation(beanName);
		if (annotation == null) {
			return bean;
		}

		try {
			executor.getThreadPoolExecutor();
			log.warn("[Metrics] {} is already initialized. RejectedExecutionHandler will NOT be instrumented.",
				beanName);
			return bean;
		} catch (IllegalStateException ignored) {
			// 아직 initialize 전 — 정상 경로
		}

		MeterRegistry registry = meterRegistryProvider.getIfAvailable();
		if (registry == null) {
			return bean;
		}
		ThreadPoolExecutorMetricsSupport support = metricsSupportProvider.getObject();
		executor.setRejectedExecutionHandler(support.rejectedExecutionHandler(registry, annotation.name()));
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (!(bean instanceof ThreadPoolTaskExecutor executor)) {
			return bean;
		}
		ObservedExecutor annotation = findAnnotation(beanName);
		if (annotation == null) {
			return bean;
		}

		MeterRegistry registry = meterRegistryProvider.getIfAvailable();
		if (registry == null) {
			return bean;
		}
		ThreadPoolExecutorMetricsSupport support = metricsSupportProvider.getObject();
		support.bind(registry, executor, annotation.name());
		return bean;
	}

	private ObservedExecutor findAnnotation(String beanName) {
		if (beanFactory == null) {
			return null;
		}
		try {
			RootBeanDefinition beanDefinition = (RootBeanDefinition)beanFactory.getMergedBeanDefinition(beanName);
			Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
			if (factoryMethod == null) {
				return null;
			}
			return factoryMethod.getAnnotation(ObservedExecutor.class);
		} catch (Exception e) {
			return null;
		}
	}
}
