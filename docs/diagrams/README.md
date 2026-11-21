# UML Diagrams – SESCAM Radiodiagnostics & Medical Imaging

This directory stores the version-controlled **PlantUML** source files (`puml/`) and rendered **PNG** images (`png/`) for the project's UML models. Keeping diagrams in the repository avoids saturating the GitHub Wiki while maintaining full Git traceability.

---

## Directory Structure

* **`puml/`** — Editable PlantUML source files (`.puml`) for Use Cases, Domain Class Diagrams, and Communication Diagrams.
* **`png/`** — Rendered PNG images for immediate viewing in GitHub and linking from the Wiki.
* **`UC-ADM-01/`** *(added with PR #119)* — Design Class Diagram and Communication Diagrams (Normal + Alternative Flows A1–A4) for `UC-ADM-01` (`C2 sescam-catalogue` `v1.0.0`).

---

## 1. Use Case Diagrams (All 4 Subsystems / 23 Macro Use Cases)

| Subsystem | Global Diagram (PNG) | Individual Use Case Diagrams (PNG) |
| :--- | :--- | :--- |
| **Patients** | [`pat-usecases.png`](png/pat-usecases.png) | [`uc-pat-01`](png/uc-pat-01.png), [`uc-pat-02`](png/uc-pat-02.png), [`uc-pat-03`](png/uc-pat-03.png), [`uc-pat-04`](png/uc-pat-04.png), [`uc-pat-05`](png/uc-pat-05.png), [`uc-pat-06`](png/uc-pat-06.png) |
| **Physicians** | [`phy-usecases.png`](png/phy-usecases.png) ([`.puml`](puml/phy-usecases.puml)) | [`uc-phy-01`](png/uc-phy-01.png), [`uc-phy-02`](png/uc-phy-02.png), [`uc-phy-03`](png/uc-phy-03.png), [`uc-phy-04`](png/uc-phy-04.png), [`uc-phy-05`](png/uc-phy-05.png), [`uc-phy-06`](png/uc-phy-06.png) |
| **Admissions / Admin** | [`aad-usecases.png`](png/aad-usecases.png) | [`uc-aad-01`](png/uc-aad-01.png), [`uc-aad-02`](png/uc-aad-02.png), [`uc-aad-03`](png/uc-aad-03.png), [`uc-aad-04`](png/uc-aad-04.png), [`uc-aad-05`](png/uc-aad-05.png), [`uc-aad-06`](png/uc-aad-06.png) |
| **System Administrators** | [`adm-usecases.png`](png/adm-usecases.png) | [`uc-adm-01`](png/uc-adm-01.png), [`uc-adm-02`](png/uc-adm-02.png), [`uc-adm-03`](png/uc-adm-03.png), [`uc-adm-04`](png/uc-adm-04.png), [`uc-adm-05`](png/uc-adm-05.png) |

---

## 2. Domain Class & Communication Diagrams (Orders & Diagnostics – C4 / C7)

* **Domain Class Diagram:** [`phy-class-domain.png`](png/phy-class-domain.png) ([`.puml`](puml/phy-class-domain.puml))
* **`UC-PHY-01` (Order & Prescribe Diagnostic Procedure — C4 `sescam-orders`):**
  * Normal Flow: [`comm-phy-01-normal.png`](png/comm-phy-01-normal.png) ([`.puml`](puml/comm-phy-01-normal.puml))
  * Alt 4a (Missing clinical data): [`comm-phy-01-alt4a.png`](png/comm-phy-01-alt4a.png) ([`.puml`](puml/comm-phy-01-alt4a.puml))
  * Alt 6a (Contrast & informed consent): [`comm-phy-01-alt6a.png`](png/comm-phy-01-alt6a.png) ([`.puml`](puml/comm-phy-01-alt6a.puml))
  * Alt 6b (Safety contraindication & override): [`comm-phy-01-alt6b.png`](png/comm-phy-01-alt6b.png) ([`.puml`](puml/comm-phy-01-alt6b.puml))
* **`UC-PHY-04` (Author & Review Diagnostic Reports — C7 `sescam-diagnostics`):**
  * Normal Flow: [`comm-phy-04-normal.png`](png/comm-phy-04-normal.png) ([`.puml`](puml/comm-phy-04-normal.puml))
  * Alt 2a (PACS failure): [`comm-phy-04-alt2a.png`](png/comm-phy-04-alt2a.png) ([`.puml`](puml/comm-phy-04-alt2a.puml))
  * Alt 5a (Save as draft): [`comm-phy-04-alt5a.png`](png/comm-phy-04-alt5a.png) ([`.puml`](puml/comm-phy-04-alt5a.puml))
  * Alt 6a (Report addendum): [`comm-phy-04-alt6a.png`](png/comm-phy-04-alt6a.png) ([`.puml`](puml/comm-phy-04-alt6a.puml))
* **`UC-PHY-05` (Log Procedure Execution & Incidents — C7 `sescam-diagnostics`):**
  * Normal Flow: [`comm-phy-05-normal.png`](png/comm-phy-05-normal.png) ([`.puml`](puml/comm-phy-05-normal.puml))
  * Alt 2a (Clinical incident): [`comm-phy-05-alt2a.png`](png/comm-phy-05-alt2a.png) ([`.puml`](puml/comm-phy-05-alt2a.puml))
  * Alt 2b (Procedure aborted): [`comm-phy-05-alt2b.png`](png/comm-phy-05-alt2b.png) ([`.puml`](puml/comm-phy-05-alt2b.puml))
