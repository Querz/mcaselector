[Setup]
AppId={{C6145D1F-C820-492A-A649-F4D4C063EECB}
AppName=${applicationName}
AppVersion=${applicationVersion}
;AppVerName=${applicationName} ${applicationVersion}
AppPublisher=${applicationAuthor}
AppPublisherURL=${applicationUrl}
AppSupportURL=${applicationUrl}
AppUpdatesURL=${applicationUrl}
DefaultDirName={autopf}\\${applicationName}
DisableProgramGroupPage=yes
LicenseFile=LICENSE
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputBaseFilename=${applicationName} Setup
SetupIconFile=icon.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\\BrazilianPortuguese.isl"
Name: "czech"; MessagesFile: "compiler:Languages\\Czech.isl"
Name: "dutch"; MessagesFile: "compiler:Languages\\Dutch.isl"
Name: "french"; MessagesFile: "compiler:Languages\\French.isl"
Name: "german"; MessagesFile: "compiler:Languages\\German.isl"
Name: "italian"; MessagesFile: "compiler:Languages\\Italian.isl"
Name: "polish"; MessagesFile: "compiler:Languages\\Polish.isl"
Name: "portuguese"; MessagesFile: "compiler:Languages\\Portuguese.isl"
Name: "russian"; MessagesFile: "compiler:Languages\\Russian.isl"
Name: "spanish"; MessagesFile: "compiler:Languages\\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "${applicationName}.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "jre\\*"; DestDir: "{app}\\jre"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "${applicationJar}"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\\${applicationName}"; Filename: "{app}\\${applicationName}.exe"
Name: "{autodesktop}\\${applicationName}"; Filename: "{app}\\${applicationName}.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\\${applicationName}.exe"; Description: "{cm:LaunchProgram,${applicationName}}"; Flags: nowait postinstall skipifsilent

