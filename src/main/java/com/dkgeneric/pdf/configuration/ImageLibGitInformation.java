package com.dkgeneric.pdf.configuration;

import org.springframework.stereotype.Component;

import com.dkgeneric.commons.config.CommonsLibGitInformation;

@Component
public class ImageLibGitInformation extends CommonsLibGitInformation {
	@Override
	public String getProjectName() {
		return "ImageLib";
	}
}
