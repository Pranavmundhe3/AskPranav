package com.askpranav.ai.ingestion;

/** Turns resume text into structured data. The production implementation asks the language model. */
public interface ResumeExtractor {

    ResumeData extract(String resumeText);
}
