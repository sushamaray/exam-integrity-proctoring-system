package com.uiu.examintegrity.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.uiu.examintegrity.exam.domain.grading.GradingStrategy;
import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.similarity.domain.SimilarityStrategy;

@Configuration
@ComponentScan(
        basePackages = {
                "com.uiu.examintegrity.exam.domain.grading",
                "com.uiu.examintegrity.proctoring.domain.detectors",
                "com.uiu.examintegrity.similarity.domain.strategies" },
        useDefaultFilters = false,
        includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = { GradingStrategy.class, Detector.class, SimilarityStrategy.class }))
public class DomainComponentsConfig {
}
