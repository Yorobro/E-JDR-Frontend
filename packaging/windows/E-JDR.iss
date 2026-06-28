;; Template Inno Setup personnalisé pour l'installeur EXE produit par jpackage (Compose Desktop).
;;
;; Injecté via `nativeDistributions.appResourcesRootDir = packaging/` (ce fichier est sous le
;; sous-dossier OS `windows/`) : Compose le transmet à jpackage en --resource-dir. Il REMPLACE le
;; template Inno par défaut (jpackage ne fusionne pas : le fichier « <packageName>.iss » = E-JDR.iss
;; se substitue au défaut). Il est volontairement calqué sur le template standard OpenJDK afin que
;; jpackage renseigne les mêmes variables {#...} (DisplayName, AppVersion, AppIdentifier, etc.).
;;
;; SEULE PERSONNALISATION métier : la section [Run] en fin de fichier (case « Lancer E-JDR »
;; précochée en première installation, ignorée lors des mises à jour silencieuses /SILENT).
;;
;; ⚠️ À RE-SYNCHRONISER au premier `gradlew packageExe` sur Windows : comparer ce fichier au .iss
;; réellement émis par jpackage (récupérable via --temp <dir>) et réintégrer toute divergence
;; propre à la version du JDK utilisée par la CI (Temurin 21). Ne pas laisser ce template diverger.

#define MyAppExeName "E-JDR.exe"

[Setup]
AppId={{#AppIdentifier}}
AppName={#DisplayName}
AppVersion={#AppVersion}
AppVerName={#DisplayName} {#AppVersion}
AppPublisher={#Vendor}
AppComments={#Description}
AppCopyright={#Copyright}
WizardStyle=modern
DefaultDirName={#InstallDirName}
DisableStartupPrompt=Yes
DisableDirPage={#DisableDirPage}
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=No
DisableWelcomePage=Yes
DefaultGroupName={#GroupName}
;; Inno passe en mode silencieux quand l'installeur est lancé avec /SILENT ou /VERYSILENT.
;; C'est ce que fait WindowsSystemLauncher lors des mises à jour : l'assistant n'apparaît pas.
Compression=lzma
SolidCompression=yes
PrivilegesRequired={#InstallPrivilege}
SetupIconFile={#InstallerIcon}
UninstallDisplayIcon={app}\{#MyAppExeName}
UninstallDisplayName={#DisplayName}
WizardImageStretch=No
WizardSmallImageFile={#InstallerSmallIcon}
ArchitecturesInstallIn64BitMode=x64compatible
ArchitecturesAllowed=x64compatible

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "french"; MessagesFile: "compiler:Languages\French.isl"

[Files]
Source: "{#InstallerSource}\{#DisplayName}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#DisplayName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppExeName}"
Name: "{commonprograms}\{#DisplayName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppExeName}"

[Run]
;; Case « Lancer E-JDR » précochée à la fin de l'assistant (postinstall).
;;  - postinstall   : affiche la case sur la page finale, COCHÉE par défaut.
;;  - nowait        : n'attend pas la fermeture de l'application.
;;  - skipifsilent  : IGNORÉE en mode /SILENT → n'apparaît donc qu'à la première installation
;;                    interactive, jamais pendant les mises à jour silencieuses.
Filename: "{app}\{#MyAppExeName}"; Description: "Lancer E-JDR"; Flags: postinstall nowait skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
