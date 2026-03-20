package com.tasteam.infra.webhook.discord;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DiscordMessage(
	List<Embed> embeds) {

	public record Embed(
		String title,
		String description,
		@JsonProperty("color")
		int color,
		List<Field> fields,
		String timestamp,
		Footer footer) {
	}

	public record Field(
		String name,
		String value,
		boolean inline) {
	}

	public record Footer(
		String text) {
	}

	public static DiscordMessage from(com.tasteam.infra.webhook.WebhookMessage message) {
		int colorInt = hexToInt(message.color());

		List<Field> fields = message.fields().entrySet().stream()
			.map(entry -> new Field(entry.getKey(), entry.getValue(), true))
			.toList();

		Embed embed = new Embed(
			message.title(),
			message.description(),
			colorInt,
			fields,
			message.timestamp().toString(),
			null);

		return new DiscordMessage(List.of(embed));
	}

	private static int hexToInt(String hex) {
		String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
		try {
			return Integer.parseInt(cleanHex, 16);
		} catch (NumberFormatException e) {
			return 0x000000;
		}
	}
}
