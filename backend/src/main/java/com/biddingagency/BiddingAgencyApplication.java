package com.biddingagency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for SAM.gov Bidding Agency Platform
 *
 * This platform provides AI-powered premium bidding agency services for
 * US Government contracts, particularly USFK (주한미군) opportunities.
 *
 * Key Features:
 * - Automated SAM.gov opportunity collection (4x daily)
 * - AI-powered requirement extraction and document generation
 * - FSM-based bid request lifecycle management
 * - Version-controlled document editing with immutability
 * - Compliance validation with BLOCKER system
 * - Client confirmation with electronic consent
 * - Submission proof archiving
 */
@SpringBootApplication
@EnableScheduling  // For SAM.gov collection scheduler
@EnableAsync       // For asynchronous processing (AI generation, PDF export)
public class BiddingAgencyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiddingAgencyApplication.class, args);
    }
}
