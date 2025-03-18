package com.wearezeta.auto.common.backend.models;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum AccentColor {
	Undefined(0, "Undefined", "#ffffff"),
	// Old colors
	StrongBlue(1, "StrongBlue", "#2391d3"),
	StrongLimeGreen(2, "StrongLimeGreen", "#00c800"),
	BrightYellow(3, "BrightYellow", "#febf02"),
	VividRed(4, "VividRed", "#fb0807"),
	BrightOrange(5, "BrightOrange", "#ff8900"),
	SoftPink(6, "SoftPink", "#fe5ebd"),
	Violet(7, "Violet", "#9c00fe"),
	// Redesign 2022
	Blue(1, "Blue", "#0667c8"),
	Green(2, "Green", "#1d7833"),
	Red(4, "Red", "#c20013"),
	Amber(5, "Amber", "#a25915"),
	Petrol(6, "Petrol", "#01718e"),
	Purple(7, "Purple", "#8944ab");

	private final int id;
	private final String name;
	private final String hexColor;

	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getHexColor() {
		return this.hexColor;
	}

	AccentColor(int id, String name, String hexColor) {
		this.id = id;
		this.name = name;
		this.hexColor = hexColor;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public static AccentColor getById(int colorId) {
		for (AccentColor color : AccentColor.values()) {
			if (color.getId() == colorId) {
				return color;
			}
		}
		throw new NoSuchElementException(String.format(
				"Accent color id '%d' is unknown", colorId));
	}

	public static AccentColor getByName(String colorName) {
		for (AccentColor color : AccentColor.values()) {
			if (color.getName().equalsIgnoreCase(colorName)) {
				return color;
			}
		}
		throw new NoSuchElementException(String.format(
				"Accent color name '%s' is unknown", colorName));
	}

	public static AccentColor getByHex(String colorHex) {
		for (AccentColor color : AccentColor.values()) {
			if (color.getHexColor().equalsIgnoreCase(colorHex)) {
				return color;
			}
		}
		throw new NoSuchElementException(String.format(
				"Accent color hex '%s' is unknown", colorHex));
	}

	public static AccentColor getByRgba(String colorRgba) {
		final String colorHex = rgbaToHexColor(colorRgba);
		for (AccentColor color : AccentColor.values()) {
			if (color.getHexColor().equalsIgnoreCase(colorHex)) {
				return color;
			}
		}
		throw new NoSuchElementException(String.format(
				"Accent color rgba '%s' (hex '%s') is unknown", colorRgba,
				colorHex));
	}

	private static String rgbaToHexColor(String colorRgba) {
		Pattern pattern = Pattern
				.compile("^rgba?\\((\\d+), (\\d+), (\\d+).*\\)$");
		Matcher matcher = pattern.matcher(colorRgba);
		Integer red = 255;
		Integer green = 255;
		Integer blue = 255;
		while (matcher.find()) {
			red = Integer.parseInt(matcher.group(1));
			green = Integer.parseInt(matcher.group(2));
			blue = Integer.parseInt(matcher.group(3));
		}
		String colorHex = "#" + startWithZero(Integer.toHexString(red))
				+ startWithZero(Integer.toHexString(green))
				+ startWithZero(Integer.toHexString(blue));
		return colorHex;
	}

	private static String startWithZero(String hex) {
		if (hex.length() < 2) {
			hex = "0" + hex;
		}
		return hex;
	}
}
