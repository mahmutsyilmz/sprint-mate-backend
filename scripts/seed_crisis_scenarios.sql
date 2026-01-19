-- ============================================================================
-- SPRINT MATE: Crisis Mode Data Seed Script
-- Generates 1000+ unique enterprise crisis scenarios for AI project generation
-- ============================================================================

-- Clear existing data (optional - comment out if you want to append)
-- DELETE FROM project_prompt_contexts;

-- ============================================================================
-- STEP 1: Create temporary lookup tables for combinatorial generation
-- ============================================================================

-- Industries with sub-domains
CREATE TABLE #Industries (
    id INT IDENTITY(1,1),
    industry NVARCHAR(100),
    sub_domain NVARCHAR(100)
);

INSERT INTO #Industries (industry, sub_domain) VALUES
-- Healthcare
('Healthcare', 'Patient Records Management'),
('Healthcare', 'Telemedicine Platform'),
('Healthcare', 'Medical Billing System'),
('Healthcare', 'Lab Results Portal'),
('Healthcare', 'Appointment Scheduling'),
('Healthcare', 'Prescription Management'),
('Healthcare', 'Insurance Claims Processing'),
-- FinTech
('FinTech', 'Payment Processing'),
('FinTech', 'Digital Banking'),
('FinTech', 'Investment Platform'),
('FinTech', 'Fraud Detection System'),
('FinTech', 'Loan Origination'),
('FinTech', 'Cryptocurrency Exchange'),
('FinTech', 'Credit Scoring Engine'),
-- E-Commerce
('E-Commerce', 'Product Catalog'),
('E-Commerce', 'Shopping Cart System'),
('E-Commerce', 'Order Fulfillment'),
('E-Commerce', 'Inventory Management'),
('E-Commerce', 'Customer Reviews Platform'),
('E-Commerce', 'Recommendation Engine'),
('E-Commerce', 'Flash Sale System'),
-- Logistics
('Logistics', 'Fleet Management'),
('Logistics', 'Route Optimization'),
('Logistics', 'Warehouse Management'),
('Logistics', 'Last-Mile Delivery'),
('Logistics', 'Supply Chain Visibility'),
('Logistics', 'Shipment Tracking'),
-- EdTech
('EdTech', 'Learning Management System'),
('EdTech', 'Virtual Classroom'),
('EdTech', 'Student Assessment Platform'),
('EdTech', 'Course Content Delivery'),
('EdTech', 'Certification System'),
-- SaaS
('SaaS', 'Multi-tenant Platform'),
('SaaS', 'Subscription Billing'),
('SaaS', 'User Analytics Dashboard'),
('SaaS', 'API Gateway'),
('SaaS', 'White-label Solution'),
-- Media & Entertainment
('Media & Entertainment', 'Video Streaming Platform'),
('Media & Entertainment', 'Content Management System'),
('Media & Entertainment', 'Live Broadcasting'),
('Media & Entertainment', 'Social Feed Algorithm'),
('Media & Entertainment', 'Ad Serving Platform'),
-- Travel & Hospitality
('Travel & Hospitality', 'Booking Engine'),
('Travel & Hospitality', 'Loyalty Program'),
('Travel & Hospitality', 'Revenue Management'),
('Travel & Hospitality', 'Guest Experience Platform'),
-- Manufacturing
('Manufacturing', 'Production Line Monitoring'),
('Manufacturing', 'Quality Control System'),
('Manufacturing', 'Predictive Maintenance'),
('Manufacturing', 'Supply Chain ERP'),
-- Real Estate
('Real Estate', 'Property Listing Platform'),
('Real Estate', 'Virtual Tour System'),
('Real Estate', 'Mortgage Calculator'),
('Real Estate', 'Tenant Management'),
-- Insurance
('Insurance', 'Policy Management'),
('Insurance', 'Claims Processing'),
('Insurance', 'Risk Assessment Engine'),
('Insurance', 'Agent Portal'),
-- Telecom
('Telecom', 'Customer Self-Service Portal'),
('Telecom', 'Network Monitoring'),
('Telecom', 'Billing System'),
('Telecom', 'Service Provisioning');

-- Crisis Categories with detailed scenarios
CREATE TABLE #Crises (
    id INT IDENTITY(1,1),
    crisis_category NVARCHAR(100),
    crisis_scenario NVARCHAR(2000)
);

INSERT INTO #Crises (crisis_category, crisis_scenario) VALUES
-- System Failures
('System Outage', 'Production database crashed during peak hours. 50,000 users cannot access the platform. CEO is on a live TV interview in 2 hours talking about our reliability.'),
('System Outage', 'AWS us-east-1 region went down and our disaster recovery failed. We have no failover. Customer SLA breach in 4 hours.'),
('System Outage', 'Memory leak in production caused cascade failure across all microservices. System has been down for 3 hours. Revenue loss: $50,000/hour.'),
('System Outage', 'Kubernetes cluster ran out of resources during Black Friday traffic spike. Auto-scaling failed. Shopping carts are being lost.'),
('System Outage', 'Critical third-party payment API changed their endpoint without notice. All transactions are failing. Customers are abandoning carts.'),

-- Performance Crisis
('Performance Crisis', 'Page load times increased from 2s to 45s after last deployment. Bounce rate up 300%. Marketing just launched a $500K campaign driving traffic to the site.'),
('Performance Crisis', 'Database queries that used to take 100ms now take 30 seconds. The DBA quit last week and no one understands the schema.'),
('Performance Crisis', 'API response times degraded to 10+ seconds. Mobile app reviews dropped to 1 star overnight. App store threatening to remove us.'),
('Performance Crisis', 'Real-time features completely broken. WebSocket connections dropping every 30 seconds. Users in live auction losing bids.'),
('Performance Crisis', 'Search functionality returning results in 15+ seconds. Competitors average 200ms. Lost 3 enterprise deals this week because of it.'),

-- Security Breach
('Security Breach', 'Penetration test revealed SQL injection vulnerability in production. Auditors found it. We have 72 hours to fix before they report to regulators.'),
('Security Breach', 'Customer data potentially exposed through misconfigured S3 bucket. Legal team preparing breach notification. Media might pick this up any moment.'),
('Security Breach', 'Unauthorized API access detected. Someone is scraping our entire product catalog. Competitors suspected. Need to implement rate limiting NOW.'),
('Security Breach', 'JWT tokens are not expiring properly. Users can access accounts indefinitely with old tokens. Security audit failed.'),
('Security Breach', 'Cross-site scripting vulnerability discovered in user-generated content. Attackers could steal session cookies. 100K users at risk.'),
('Security Breach', 'Admin credentials were accidentally committed to public GitHub repo 6 months ago. We just found out. Full security audit required.'),

-- Scalability Crisis
('Scalability Crisis', 'Traffic 10x normal due to viral TikTok video. System cannot handle load. This is our moment and we are blowing it.'),
('Scalability Crisis', 'Enterprise client onboarding 500,000 users next week. Current system tested for 10,000. Contract worth $5M/year at stake.'),
('Scalability Crisis', 'Black Friday in 2 weeks. Last year we crashed at 10K concurrent users. Marketing promising 100K this year.'),
('Scalability Crisis', 'Database hitting connection limits. Pool exhausted. Queries queuing. Users seeing timeout errors during business hours.'),
('Scalability Crisis', 'Message queue backlog growing faster than we can process. 2 million messages pending. Real-time notifications delayed by hours.'),

-- Data Crisis
('Data Crisis', 'Production database accidentally truncated by junior developer. Backups are 2 weeks old. Financial records missing.'),
('Data Crisis', 'Data migration corrupted 30% of customer records. Cannot rollback. Duplicate entries everywhere. Reports are wrong.'),
('Data Crisis', 'GDPR deletion request came in but our system has no way to track all places where user data is stored. Legal deadline in 30 days.'),
('Data Crisis', 'Analytics pipeline broken for 2 months without anyone noticing. Executive dashboards showing wrong numbers. Board meeting in 3 days.'),
('Data Crisis', 'Primary and replica databases out of sync. Some users see old data, others see new. Consistency nightmare.'),

-- Integration Failure
('Integration Failure', 'Stripe API version deprecated. Payment processing will stop in 14 days. Need to migrate 50 integration points.'),
('Integration Failure', 'SSO provider changed their SAML implementation. 10,000 enterprise users locked out of their accounts.'),
('Integration Failure', 'Salesforce connector failing silently. Sales team has been working with stale CRM data for weeks. Pipeline reports meaningless.'),
('Integration Failure', 'Shipping carrier API rate limiting us. Cannot generate labels. 5,000 orders stuck in fulfillment. Customers furious.'),
('Integration Failure', 'Legacy ERP system upgrade broke all our integrations. Finance cannot close the books. Audit coming up.'),

-- Technical Debt
('Technical Debt Explosion', 'Monolith has grown to 2 million lines of code. Build takes 45 minutes. Developers cannot iterate. Velocity dropped 80%.'),
('Technical Debt Explosion', 'No tests in the codebase. Every deploy is Russian roulette. Last 3 releases had critical bugs. Team afraid to touch anything.'),
('Technical Debt Explosion', 'Documentation is 3 years outdated. Original architects left. No one knows why certain code exists. Onboarding takes 3 months.'),
('Technical Debt Explosion', 'Running on deprecated framework with known vulnerabilities. Security team mandated migration. No budget allocated.'),
('Technical Debt Explosion', 'Circular dependencies everywhere. Cannot deploy services independently. One change requires deploying everything.'),

-- Compliance Emergency
('Compliance Emergency', 'SOC 2 audit in 30 days. We have none of the required controls in place. Certification required for enterprise sales.'),
('Compliance Emergency', 'HIPAA violation risk identified. Patient data logging not compliant. Must fix before next audit or face $1M+ fines.'),
('Compliance Emergency', 'PCI DSS certification expiring. Credit card processing will be suspended if not renewed. 3 weeks to implement missing controls.'),
('Compliance Emergency', 'GDPR audit revealed we are storing data in non-compliant regions. Must migrate infrastructure. 60 days to comply.'),
('Compliance Emergency', 'Accessibility lawsuit filed. Platform not WCAG 2.1 compliant. Legal costs mounting. Need remediation plan NOW.'),

-- Team Crisis
('Team Crisis', 'Lead architect quit without notice. No documentation. Critical system knowledge walked out the door.'),
('Team Crisis', 'Entire backend team got poached by competitor. 2 developers left to maintain 50 microservices.'),
('Team Crisis', 'Offshore development partner went bankrupt. They have our source code. No handover. Project deadline in 6 weeks.'),
('Team Crisis', 'Key developer on medical leave for 3 months. They are the only one who understands the payment system.'),

-- Business Crisis
('Business Crisis', 'Competitor launched identical feature. Our version 6 months behind. Board demanding we catch up in 6 weeks.'),
('Business Crisis', 'Largest customer threatening to leave unless we build their requested feature by end of quarter.'),
('Business Crisis', 'Startup runway is 4 months. Investors want to see specific technical milestones before next funding round.'),
('Business Crisis', 'Acquisition due diligence revealed technical debt. Deal price being renegotiated down. Must show improvement fast.'),
('Business Crisis', 'Product-market fit pivot required. Need to rebuild core features for different market segment. 8 weeks to relaunch.');

-- Company Stages
CREATE TABLE #CompanyStages (
    id INT IDENTITY(1,1),
    company_stage NVARCHAR(100)
);

INSERT INTO #CompanyStages (company_stage) VALUES
('Early-Stage Startup'),
('Seed-Funded Startup'),
('Series A Scale-up'),
('Series B Growth'),
('Series C Enterprise'),
('Public Company'),
('Enterprise Corporation'),
('Digital Transformation Initiative'),
('Spin-off Venture'),
('Acquired Subsidiary');

-- Team Sizes
CREATE TABLE #TeamSizes (
    id INT IDENTITY(1,1),
    team_size NVARCHAR(100)
);

INSERT INTO #TeamSizes (team_size) VALUES
('Solo Developer'),
('Small Team (2-3)'),
('Startup Team (4-8)'),
('Growth Team (9-15)'),
('Scale Team (16-30)'),
('Enterprise Team (30+)'),
('Distributed Global Team'),
('Cross-functional Squad');

-- Urgency Levels
CREATE TABLE #UrgencyLevels (
    id INT IDENTITY(1,1),
    urgency_level NVARCHAR(100)
);

INSERT INTO #UrgencyLevels (urgency_level) VALUES
('CRITICAL - Hours to fix'),
('CRITICAL - 24-48 hours'),
('HIGH - This week'),
('HIGH - 1-2 weeks'),
('MEDIUM - This month'),
('MEDIUM - This quarter'),
('STANDARD - Next quarter');

-- Primary Constraints
CREATE TABLE #PrimaryConstraints (
    id INT IDENTITY(1,1),
    primary_constraint NVARCHAR(200)
);

INSERT INTO #PrimaryConstraints (primary_constraint) VALUES
('Zero budget for new tools or infrastructure'),
('Cannot hire - must use existing team only'),
('No downtime allowed - must be zero-disruption migration'),
('Regulatory deadline - date is immovable'),
('Must maintain backward compatibility with all clients'),
('Cannot change database schema - API only'),
('Security team must approve every change'),
('Must work on legacy infrastructure (no cloud)'),
('Performance cannot degrade during transition'),
('Must support offline functionality');

-- Secondary Constraints
CREATE TABLE #SecondaryConstraints (
    id INT IDENTITY(1,1),
    secondary_constraint NVARCHAR(200)
);

INSERT INTO #SecondaryConstraints (secondary_constraint) VALUES
('No breaking changes to public API'),
('Must pass penetration testing before launch'),
('Documentation required for compliance'),
('Must support mobile and web simultaneously'),
('Internationalization required from day one'),
('Must integrate with existing SSO'),
('Audit logging for all operations'),
('Real-time sync across all clients'),
('Must handle 10x current traffic'),
('WCAG 2.1 AA accessibility required');

-- Architecture Patterns
CREATE TABLE #ArchitecturePatterns (
    id INT IDENTITY(1,1),
    architecture_pattern NVARCHAR(100)
);

INSERT INTO #ArchitecturePatterns (architecture_pattern) VALUES
('Microservices Architecture'),
('Monolith First'),
('Event-Driven Architecture'),
('Serverless Functions'),
('CQRS with Event Sourcing'),
('Hexagonal Architecture'),
('Strangler Fig Pattern (Migration)'),
('API Gateway Pattern'),
('Backend for Frontend (BFF)'),
('Domain-Driven Design');

-- Legacy Backend Stacks (for flavor only)
CREATE TABLE #LegacyBackends (
    id INT IDENTITY(1,1),
    backend_stack NVARCHAR(200)
);

INSERT INTO #LegacyBackends (backend_stack) VALUES
('PHP 5.6 Monolith with MySQL'),
('Java 6 with Struts and Oracle'),
('Ruby on Rails 3 with PostgreSQL'),
('Node.js 8 with MongoDB'),
('Python 2.7 Django with SQLite'),
('.NET Framework 4.0 with SQL Server 2008'),
('Perl CGI scripts with flat files'),
('ColdFusion with Access database'),
('Classic ASP with SQL Server 2000'),
('COBOL on IBM Mainframe');

-- Legacy Frontend Stacks (for flavor only)
CREATE TABLE #LegacyFrontends (
    id INT IDENTITY(1,1),
    frontend_stack NVARCHAR(200)
);

INSERT INTO #LegacyFrontends (frontend_stack) VALUES
('jQuery spaghetti with Bootstrap 2'),
('AngularJS 1.x (not Angular)'),
('Backbone.js with Handlebars'),
('Server-rendered PHP templates'),
('Flash-based interface'),
('GWT (Google Web Toolkit)'),
('ExtJS 3.x components'),
('Vanilla JavaScript with no framework'),
('Dojo Toolkit'),
('YUI Library');

-- Database Requirements
CREATE TABLE #DatabaseRequirements (
    id INT IDENTITY(1,1),
    database_requirement NVARCHAR(200)
);

INSERT INTO #DatabaseRequirements (database_requirement) VALUES
('ACID compliance mandatory'),
('Must support 1M+ transactions per second'),
('Time-series data optimization needed'),
('Full-text search with relevance ranking'),
('Graph relationships required'),
('Geo-spatial queries critical'),
('Must support JSON documents'),
('Strong consistency over availability'),
('Eventual consistency acceptable for scale'),
('Must encrypt data at rest');

-- Infrastructure Options
CREATE TABLE #Infrastructures (
    id INT IDENTITY(1,1),
    infrastructure NVARCHAR(200)
);

INSERT INTO #Infrastructures (infrastructure) VALUES
('AWS Multi-region'),
('Azure Government Cloud'),
('Google Cloud Platform'),
('On-premise data center only'),
('Hybrid cloud setup'),
('Kubernetes on bare metal'),
('Serverless-first architecture'),
('Edge computing required'),
('Air-gapped secure environment'),
('Multi-cloud for redundancy');

-- Budget Constraints
CREATE TABLE #BudgetConstraints (
    id INT IDENTITY(1,1),
    budget_constraint NVARCHAR(200)
);

INSERT INTO #BudgetConstraints (budget_constraint) VALUES
('$0 - Must use free tier only'),
('Under $500/month infrastructure'),
('Under $5,000/month total'),
('Under $50,000/month enterprise'),
('Cost reduction mandate - cut by 50%'),
('Unlimited budget but ROI must be proven'),
('VC-funded - burn rate concerns'),
('Bootstrap - every dollar counts'),
('Enterprise budget with procurement delays'),
('Grant-funded with strict reporting');

-- Timelines
CREATE TABLE #Timelines (
    id INT IDENTITY(1,1),
    timeline NVARCHAR(100)
);

INSERT INTO #Timelines (timeline) VALUES
('24 hours'),
('48 hours'),
('1 week sprint'),
('2 week sprint'),
('30 days'),
('60 days'),
('90 days'),
('End of quarter'),
('End of fiscal year'),
('Before next funding round');

-- Stakeholder Pressure
CREATE TABLE #StakeholderPressures (
    id INT IDENTITY(1,1),
    stakeholder_pressure NVARCHAR(500)
);

INSERT INTO #StakeholderPressures (stakeholder_pressure) VALUES
('CEO personally monitoring progress daily'),
('Board presentation scheduled for demo'),
('Investors threatening to pull funding'),
('Major client CEO escalated to our CEO'),
('Press release already announced the feature'),
('Competitor just launched similar feature'),
('Regulatory body conducting audit'),
('Internal politics - CTO''s job on the line'),
('Sales team promised delivery to close deal'),
('Public company - Wall Street expectations'),
('Customer success team overwhelmed with complaints'),
('Engineering reputation at stake after failures'),
('Partnership deal contingent on delivery'),
('Product hunt launch scheduled'),
('Conference keynote demo planned');

-- Success Metrics
CREATE TABLE #SuccessMetrics (
    id INT IDENTITY(1,1),
    success_metric NVARCHAR(200)
);

INSERT INTO #SuccessMetrics (success_metric) VALUES
('99.99% uptime SLA'),
('Sub-100ms API response times'),
('Zero security vulnerabilities'),
('Pass SOC 2 Type II audit'),
('50% reduction in page load time'),
('Handle 100K concurrent users'),
('Zero customer-facing bugs'),
('100% test coverage'),
('MTTR under 15 minutes'),
('Customer satisfaction score > 4.5'),
('Reduce support tickets by 60%'),
('Increase conversion rate by 25%'),
('Reduce infrastructure costs by 40%'),
('Achieve WCAG 2.1 AA compliance'),
('Zero downtime deployment');

-- Legacy System Issues
CREATE TABLE #LegacyIssues (
    id INT IDENTITY(1,1),
    legacy_system_issue NVARCHAR(500)
);

INSERT INTO #LegacyIssues (legacy_system_issue) VALUES
('COBOL mainframe running batch jobs nightly'),
('FoxPro database with no documentation'),
('MS Access databases shared over network drive'),
('VB6 desktop application that must keep working'),
('Lotus Notes workflows still in production'),
('Crystal Reports dependencies everywhere'),
('Stored procedures with 5000+ lines each'),
('No version control - code on production server only'),
('Custom proprietary protocol for integrations'),
('Binary data format with no specification'),
('Undocumented tribal knowledge required'),
('Hardware-specific code with obsolete dependencies'),
('Vendor went bankrupt - no support available'),
('Source code partially lost'),
('Mixed encodings corrupting international data');

-- Compliance Requirements
CREATE TABLE #ComplianceRequirements (
    id INT IDENTITY(1,1),
    compliance_requirement NVARCHAR(200)
);

INSERT INTO #ComplianceRequirements (compliance_requirement) VALUES
('HIPAA for healthcare data'),
('PCI DSS Level 1 for payments'),
('SOC 2 Type II'),
('GDPR for EU customers'),
('CCPA for California residents'),
('FERPA for student data'),
('FedRAMP for government'),
('ISO 27001 certification'),
('FINRA for financial services'),
('FDA 21 CFR Part 11');

-- Integration Challenges
CREATE TABLE #IntegrationChallenges (
    id INT IDENTITY(1,1),
    integration_challenge NVARCHAR(500)
);

INSERT INTO #IntegrationChallenges (integration_challenge) VALUES
('Must sync with SAP ERP in real-time'),
('Salesforce integration with complex mapping'),
('Legacy SOAP APIs with no documentation'),
('EDI integration with major retailers'),
('Banking API with strict rate limits'),
('Healthcare HL7 FHIR compliance'),
('Government API with security clearance'),
('Real-time inventory sync across 50 warehouses'),
('Payment gateway migration mid-transaction'),
('SSO federation with 20+ identity providers'),
('IoT device fleet with unreliable connectivity'),
('Mobile app backward compatibility for 3 years'),
('Third-party vendor with 99% uptime SLA'),
('Webhook delivery with exactly-once semantics'),
('Cross-border data transfer restrictions');

-- ============================================================================
-- STEP 2: Generate combinatorial data (1000+ rows)
-- ============================================================================

-- Insert combined data
INSERT INTO project_prompt_contexts (
    id,
    industry,
    sub_domain,
    company_stage,
    team_size,
    crisis_category,
    crisis_scenario,
    urgency_level,
    primary_constraint,
    secondary_constraint,
    architecture_pattern,
    backend_stack,
    frontend_stack,
    database_requirement,
    infrastructure,
    budget_constraint,
    timeline,
    stakeholder_pressure,
    success_metric,
    legacy_system_issue,
    compliance_requirement,
    integration_challenge,
    difficulty_score,
    created_at
)
SELECT
    NEWID() as id,
    i.industry,
    i.sub_domain,
    cs.company_stage,
    ts.team_size,
    c.crisis_category,
    c.crisis_scenario,
    ul.urgency_level,
    pc.primary_constraint,
    sc.secondary_constraint,
    ap.architecture_pattern,
    lb.backend_stack,
    lf.frontend_stack,
    dr.database_requirement,
    inf.infrastructure,
    bc.budget_constraint,
    t.timeline,
    sp.stakeholder_pressure,
    sm.success_metric,
    CASE WHEN ABS(CHECKSUM(NEWID())) % 3 = 0 THEN li.legacy_system_issue ELSE NULL END as legacy_system_issue,
    CASE WHEN ABS(CHECKSUM(NEWID())) % 2 = 0 THEN cr.compliance_requirement ELSE NULL END as compliance_requirement,
    CASE WHEN ABS(CHECKSUM(NEWID())) % 2 = 0 THEN ic.integration_challenge ELSE NULL END as integration_challenge,
    (ABS(CHECKSUM(NEWID())) % 8) + 3 as difficulty_score, -- 3-10 range
    GETDATE() as created_at
FROM
    #Industries i
    CROSS JOIN #Crises c
    CROSS JOIN #CompanyStages cs
    CROSS JOIN #TeamSizes ts
    CROSS JOIN #UrgencyLevels ul
    CROSS JOIN #PrimaryConstraints pc
    CROSS JOIN #SecondaryConstraints sc
    CROSS JOIN #ArchitecturePatterns ap
    CROSS JOIN #LegacyBackends lb
    CROSS JOIN #LegacyFrontends lf
    CROSS JOIN #DatabaseRequirements dr
    CROSS JOIN #Infrastructures inf
    CROSS JOIN #BudgetConstraints bc
    CROSS JOIN #Timelines t
    CROSS JOIN #StakeholderPressures sp
    CROSS JOIN #SuccessMetrics sm
    CROSS JOIN #LegacyIssues li
    CROSS JOIN #ComplianceRequirements cr
    CROSS JOIN #IntegrationChallenges ic
WHERE
    -- Randomly sample to get ~1500 rows (otherwise would be billions)
    ABS(CHECKSUM(NEWID())) % 100000 = 0;

-- If we got less than 1000, add more with different random seed
DECLARE @CurrentCount INT;
SELECT @CurrentCount = COUNT(*) FROM project_prompt_contexts;

IF @CurrentCount < 1000
BEGIN
    -- Add more rows with simpler combination
    INSERT INTO project_prompt_contexts (
        id, industry, sub_domain, company_stage, team_size,
        crisis_category, crisis_scenario, urgency_level,
        primary_constraint, secondary_constraint, architecture_pattern,
        backend_stack, frontend_stack, database_requirement, infrastructure,
        budget_constraint, timeline, stakeholder_pressure, success_metric,
        legacy_system_issue, compliance_requirement, integration_challenge,
        difficulty_score, created_at
    )
    SELECT TOP (1500 - @CurrentCount)
        NEWID(),
        i.industry,
        i.sub_domain,
        (SELECT TOP 1 company_stage FROM #CompanyStages ORDER BY NEWID()),
        (SELECT TOP 1 team_size FROM #TeamSizes ORDER BY NEWID()),
        c.crisis_category,
        c.crisis_scenario,
        (SELECT TOP 1 urgency_level FROM #UrgencyLevels ORDER BY NEWID()),
        (SELECT TOP 1 primary_constraint FROM #PrimaryConstraints ORDER BY NEWID()),
        (SELECT TOP 1 secondary_constraint FROM #SecondaryConstraints ORDER BY NEWID()),
        (SELECT TOP 1 architecture_pattern FROM #ArchitecturePatterns ORDER BY NEWID()),
        (SELECT TOP 1 backend_stack FROM #LegacyBackends ORDER BY NEWID()),
        (SELECT TOP 1 frontend_stack FROM #LegacyFrontends ORDER BY NEWID()),
        (SELECT TOP 1 database_requirement FROM #DatabaseRequirements ORDER BY NEWID()),
        (SELECT TOP 1 infrastructure FROM #Infrastructures ORDER BY NEWID()),
        (SELECT TOP 1 budget_constraint FROM #BudgetConstraints ORDER BY NEWID()),
        (SELECT TOP 1 timeline FROM #Timelines ORDER BY NEWID()),
        (SELECT TOP 1 stakeholder_pressure FROM #StakeholderPressures ORDER BY NEWID()),
        (SELECT TOP 1 success_metric FROM #SuccessMetrics ORDER BY NEWID()),
        CASE WHEN ABS(CHECKSUM(NEWID())) % 3 = 0 THEN (SELECT TOP 1 legacy_system_issue FROM #LegacyIssues ORDER BY NEWID()) ELSE NULL END,
        CASE WHEN ABS(CHECKSUM(NEWID())) % 2 = 0 THEN (SELECT TOP 1 compliance_requirement FROM #ComplianceRequirements ORDER BY NEWID()) ELSE NULL END,
        CASE WHEN ABS(CHECKSUM(NEWID())) % 2 = 0 THEN (SELECT TOP 1 integration_challenge FROM #IntegrationChallenges ORDER BY NEWID()) ELSE NULL END,
        (ABS(CHECKSUM(NEWID())) % 8) + 3,
        GETDATE()
    FROM #Industries i
    CROSS JOIN #Crises c
    ORDER BY NEWID();
END

-- ============================================================================
-- STEP 3: Cleanup and verification
-- ============================================================================

-- Drop temporary tables
DROP TABLE #Industries;
DROP TABLE #Crises;
DROP TABLE #CompanyStages;
DROP TABLE #TeamSizes;
DROP TABLE #UrgencyLevels;
DROP TABLE #PrimaryConstraints;
DROP TABLE #SecondaryConstraints;
DROP TABLE #ArchitecturePatterns;
DROP TABLE #LegacyBackends;
DROP TABLE #LegacyFrontends;
DROP TABLE #DatabaseRequirements;
DROP TABLE #Infrastructures;
DROP TABLE #BudgetConstraints;
DROP TABLE #Timelines;
DROP TABLE #StakeholderPressures;
DROP TABLE #SuccessMetrics;
DROP TABLE #LegacyIssues;
DROP TABLE #ComplianceRequirements;
DROP TABLE #IntegrationChallenges;

-- Verify results
SELECT 'Total Crisis Scenarios Created:' as Info, COUNT(*) as Count FROM project_prompt_contexts;

-- Show sample distribution
SELECT
    'Industry Distribution' as Info,
    industry,
    COUNT(*) as count
FROM project_prompt_contexts
GROUP BY industry
ORDER BY count DESC;

SELECT
    'Crisis Category Distribution' as Info,
    crisis_category,
    COUNT(*) as count
FROM project_prompt_contexts
GROUP BY crisis_category
ORDER BY count DESC;

-- Show a few sample rows
SELECT TOP 5
    industry,
    sub_domain,
    crisis_category,
    LEFT(crisis_scenario, 100) as crisis_preview,
    urgency_level,
    company_stage,
    difficulty_score
FROM project_prompt_contexts
ORDER BY NEWID();

PRINT '============================================';
PRINT 'Crisis scenarios seeded successfully!';
PRINT 'Run your application and test Crisis Mode.';
PRINT '============================================';
