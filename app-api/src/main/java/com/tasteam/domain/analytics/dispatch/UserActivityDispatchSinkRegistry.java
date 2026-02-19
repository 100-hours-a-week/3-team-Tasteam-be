package com.tasteam.domain.analytics.dispatch;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class UserActivityDispatchSinkRegistry {

	private final Map<UserActivityDispatchTarget, UserActivityDispatchSink> sinkByTarget;

	public UserActivityDispatchSinkRegistry(List<UserActivityDispatchSink> sinks) {
		EnumMap<UserActivityDispatchTarget, UserActivityDispatchSink> mapping = new EnumMap<>(
			UserActivityDispatchTarget.class);
		for (UserActivityDispatchSink sink : sinks) {
			UserActivityDispatchSink previous = mapping.putIfAbsent(sink.target(), sink);
			if (previous != null) {
				throw new IllegalStateException("동일 dispatch target sink가 중복 등록되었습니다. target=" + sink.target());
			}
		}
		this.sinkByTarget = Map.copyOf(mapping);
	}

	public UserActivityDispatchSink getRequired(UserActivityDispatchTarget dispatchTarget) {
		UserActivityDispatchSink sink = sinkByTarget.get(dispatchTarget);
		if (sink == null) {
			throw new IllegalStateException("dispatch target sink가 등록되지 않았습니다. target=" + dispatchTarget);
		}
		return sink;
	}
}
