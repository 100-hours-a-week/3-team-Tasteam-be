package com.tasteam.domain.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminSpaFallbackController {

	@GetMapping({"/admin", "/admin/"})
	public String serveAdminSpa() {
		return "forward:/admin/index.html";
	}

	@GetMapping({
		"/admin/pages",
		"/admin/pages/",
		"/admin/pages/{page}",
		"/admin/pages/{page}.html",
		"/admin/pages/{route1}/{route2}",
		"/admin/pages/{route1}/{route2}.html",
		"/admin/pages/index.html"
	})
	public String fallbackToAdminSpa() {
		return "forward:/admin/index.html";
	}
}
