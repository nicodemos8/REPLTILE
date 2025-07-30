# REPLTILE - Internal Distribution License Guide

**For Internal Company Use Only**

This document outlines the licensing requirements and permissions for internal distribution of REPLTILE within your organization.

---

## 📋 Executive Summary

**✅ REPLTILE can be distributed internally within your organization without additional licensing fees or restrictions.**

All core dependencies (Calva, repl-tooling, and others) use permissive open-source licenses that allow internal corporate distribution and use.

---

## 🏢 Internal Distribution Rights

### ✅ What You CAN Do:

- **Install and use REPLTILE** on unlimited workstations within your organization
- **Distribute the VSIX package** internally to team members and colleagues
- **Use REPLTILE for commercial development** and business purposes
- **Modify and customize** REPLTILE for internal use (subject to individual component licenses)
- **Include REPLTILE** in company development environment setups
- **Share configurations and customizations** internally
- **Use for training and onboarding** new team members

### ❌ What You CANNOT Do:

- **Redistribute publicly** or sell REPLTILE to external parties
- **Remove copyright notices** from the software or dependencies
- **Claim ownership** of REPLTILE or its components
- **Distribute modified versions externally** without complying with component licenses

---

## 📜 Component Licenses

### Core Dependencies

| Component | License | Internal Use | Commercial Use | Attribution Required |
|-----------|---------|--------------|----------------|---------------------|
| **Calva** | MIT | ✅ Yes | ✅ Yes | ✅ Required |
| **repl-tooling** | MIT | ✅ Yes | ✅ Yes | ✅ Required |
| **React** | MIT | ✅ Yes | ✅ Yes | ✅ Required |
| **WebSocket (ws)** | MIT | ✅ Yes | ✅ Yes | ✅ Required |
| **ANSI_UP** | MIT | ✅ Yes | ✅ Yes | ✅ Required |

### Clojure/ClojureScript Libraries

| Component | License | Internal Use | Commercial Use | Attribution Required |
|-----------|---------|--------------|----------------|---------------------|
| **nREPL** | EPL-1.0 | ✅ Yes | ✅ Yes | ✅ Required |
| **CIDER nREPL** | EPL-1.0 | ✅ Yes | ✅ Yes | ✅ Required |
| **Orchard** | EPL-1.0 | ✅ Yes | ✅ Yes | ✅ Required |
| **ClojureScript** | EPL-1.0 | ✅ Yes | ✅ Yes | ✅ Required |

---

## 🔍 License Details

### MIT License (Permissive)
**Components**: Calva, repl-tooling, React, WebSocket, ANSI_UP, and others

**Key Points**:
- ✅ **Commercial use permitted**
- ✅ **Internal distribution allowed**
- ✅ **Modification permitted**
- ✅ **Private use allowed**
- ⚠️ **Attribution required** (copyright notices must be preserved)
- ⚠️ **No warranty provided**

### Eclipse Public License 1.0 (EPL-1.0)
**Components**: nREPL, CIDER, Orchard, ClojureScript libraries

**Key Points**:
- ✅ **Commercial use permitted**
- ✅ **Internal distribution allowed**
- ✅ **Modification permitted**
- ✅ **Private use allowed**
- ⚠️ **Attribution required**
- ⚠️ **Modified versions must be made available** (if distributed externally)

---

## 📋 Attribution Requirements

When distributing REPLTILE internally, you must preserve all copyright notices and license files. The following attributions should be maintained:

### Primary Components
```
REPLTILE utilizes the following open-source components:

- Calva (MIT License) - BetterThanTomorrow
  https://github.com/BetterThanTomorrow/calva

- repl-tooling (MIT License) - Mauricio Szabo
  https://github.com/mauricioszabo/repl-tooling

- React (MIT License) - Facebook
  https://github.com/facebook/react

- nREPL (EPL-1.0) - nREPL Contributors
  https://github.com/nrepl/nrepl
```

---

## 🚀 Internal Distribution Best Practices

### 1. Documentation
- ✅ Include this LICENSE.md file with all internal distributions
- ✅ Document any internal modifications or customizations
- ✅ Maintain a record of which version is deployed internally

### 2. Updates and Maintenance
- ✅ Regularly update to latest REPLTILE versions for security and features
- ✅ Test updates in development environment before company-wide deployment
- ✅ Maintain backup of current working version before updates

### 3. Security
- ✅ Only download REPLTILE VSIX from official GitHub releases
- ✅ Verify checksums if provided
- ✅ Use internal package repositories when possible

### 4. Support and Training
- ✅ Designate internal REPLTILE experts for support
- ✅ Create internal documentation for company-specific workflows
- ✅ Provide training for new team members

---

## ⚖️ Legal Compliance

### For Legal/Compliance Teams

1. **No Additional Licensing Fees**: All components use permissive licenses allowing internal commercial use
2. **Attribution Compliance**: Ensure copyright notices remain intact
3. **No External Distribution**: Internal use only - external distribution requires review
4. **Audit Trail**: Maintain records of internal distribution and usage

### Risk Assessment: **LOW**
- All dependencies use well-established, permissive licenses
- No copyleft requirements for internal use
- No licensing fees or royalties required
- Established precedent for internal enterprise use

---

## 🔗 Additional Resources

### Official Documentation
- **Calva Documentation**: https://calva.io/
- **VS Code Extension API**: https://code.visualstudio.com/api
- **Clojure Licensing**: https://clojure.org/community/license

### License Texts
- **MIT License**: https://opensource.org/licenses/MIT
- **Eclipse Public License 1.0**: https://opensource.org/licenses/EPL-1.0

### Support Channels
- **GitHub Issues**: https://github.com/nubank/REPLTILE/issues
- **Clojure Community**: https://clojurians.slack.com/
- **Internal Support**: [Your internal support channel]

---

## 📝 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-01-27 | Initial internal distribution license guide |

---

## ✅ Summary for Management

**REPLTILE is safe for internal corporate distribution and use:**

- ✅ All licenses permit commercial internal use
- ✅ No licensing fees or royalties required
- ✅ Established track record in enterprise environments
- ✅ Low legal risk profile
- ✅ Supports business development objectives
- ✅ Enhances developer productivity and tooling

**Recommendation**: Approved for internal distribution and use across development teams.

---

*This document was prepared based on license analysis of REPLTILE v1.0.0-beta and its dependencies as of January 2025. For questions about licensing or compliance, consult your legal team.* 