# Internal Company Policy - Pro Nurse Engineering

**Document ID**: BT-POL-004  
**Version**: 1.0.0  
**Effective Date**: July 17, 2026  
**Publisher**: **Biruma Technology Solutions Pvt Ltd.**

---

## 1. Project Ownership & Intellectual Property

1. **Proprietary Software Notice:** The **Pro Nurse Platform Backend API** and all associated resources, configuration files, test suites, and databases are the exclusive intellectual property of **Biruma Technology Solutions Pvt Ltd.**
2. **Confidentiality:** All source code and credentials (mock or production) are strictly confidential. Unauthorized copying, distribution, publishing, or sharing of any part of this repository outside the company's private hosting spaces is strictly prohibited and subject to legal action.

---

## 2. Authorized Workspace Usage

1. Developers must use authorized company-provided equipment or secure environments when working on this repository.
2. Storing or caching database dumps or production backups on unauthorized personal devices is strictly forbidden.
3. Access to PostgreSQL production databases is restricted to DevOps engineers and senior administrators only.

---

## 3. Git Workflow & Version Control Expectations

All code changes must follow our standard Git workflows:

### 3.1 Branch Naming Conventions
- **Feature Branches**: `feature/DTS-[IssueNumber]-short-desc`
- **Bug Fix Branches**: `bugfix/DTS-[IssueNumber]-short-desc`
- **Hotfix Branches**: `hotfix/short-desc`
- **System Branches**: `main` (production-ready stable branch), `develop` (integration and staging branch).

### 3.2 Pull Request (PR) & Code Review Guidelines
- Developers must never merge directly into `main` or `develop`.
- A minimum of **1 peer approval** is required before a Pull Request can be merged.
- PRs must pass the CI/CD pipeline, which compiles the application and runs the full test suite.
- Every PR must reference its target issue number in the commit message or PR title.

---

## 4. Secure Coding & Secret Management

1. **No Hardcoded Credentials:** Never commit credentials, JWT secrets, database passwords, Razorpay API tokens, or Firebase private keys to this repository. All credentials must be loaded via Environment Variables or Spring properties placeholder configs (e.g., `${JWT_SECRET}`).
2. **Local Properties Configuration:** Developers must keep their local secrets inside `application-local.properties` or system environment variables. This file is excluded from git tracking to prevent accidental commits of local secrets.
3. **Dependency Scanning:** All added third-party dependencies in `pom.xml` must undergo vulnerability vetting (using Maven dependency checks) before being approved for release.

---

## 5. Release & Incident Response

1. **Staging Verification:** No features can be deployed to production without first verifying integration builds on the `develop` staging server.
2. **Vulnerability Incidents:** If a dependency or configuration vulnerability is discovered, it must be reported immediately to [info@desitechsolutions.com](mailto:info@desitechsolutions.com). A hotfix branch must be checked out and deployed within 4 hours.

---

## 📞 Support & Company Information

This policy is maintained by the engineering team at:

**Company:** Biruma Technology Solutions Pvt Ltd.  
**Website:** [desitechsolutions.com](https://desitechsolutions.com)  
**Support Email:** [info@desitechsolutions.com](mailto:info@desitechsolutions.com)  

Copyright © 2026 Biruma Technology Solutions Pvt Ltd. All rights reserved.
